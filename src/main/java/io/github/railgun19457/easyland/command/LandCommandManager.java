package io.github.railgun19457.easyland.command;

import io.github.railgun19457.easyland.EasylandPlugin;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.commands.*;
import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.service.LandService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 领地命令管理器
 * 使用命令模式管理所有子命令
 */
public class LandCommandManager implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> commands;
    private final LanguageManager languageManager;
    private final EasylandPlugin plugin;

    public LandCommandManager(LandService landService, LandRepository landRepository,
                              LanguageManager languageManager, Map<String, Location[]> selections,
                              EasylandPlugin plugin) {
        this.languageManager = languageManager;
        this.plugin = plugin;
        this.commands = new HashMap<>();

        // 注册所有子命令
        registerCommand(new SelectCommand(languageManager));
        registerCommand(new CreateCommand(landService, languageManager, selections));
        registerCommand(new ClaimCommand(landService, languageManager));
        registerCommand(new UnclaimCommand(landService, languageManager));
        registerCommand(new TrustCommand(landService, languageManager));
        registerCommand(new UntrustCommand(landService, languageManager));
        registerCommand(new RemoveCommand(landService, languageManager));
        registerCommand(new ListCommand(landService, languageManager));
        registerCommand(new ShowCommand(landService, landRepository, languageManager));
        registerCommand(new RuleCommand(landService, languageManager));
        registerCommand(new TrustListCommand(landService, languageManager));
        registerCommand(new ReloadCommand(plugin, landService, languageManager));
        registerCommand(new CacheCommand(languageManager, plugin));

        // 帮助命令需要在所有其他命令注册后进行，以获取完整列表
        List<SubCommand> allCommands = new ArrayList<>(this.commands.values());
        HelpCommand helpCommand = new HelpCommand(languageManager, allCommands);
        this.commands.put(helpCommand.getName().toLowerCase(), helpCommand);
        allCommands.add(helpCommand);
    }

    /**
     * 注册子命令
     */
    private void registerCommand(SubCommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 添加调试日志：记录命令执行
        plugin.getLogger().info("Command executed: " + label + " by " + sender.getName() +
                               " with args: " + Arrays.toString(args));
        
        // 如果没有参数，显示帮助
        if (args.length == 0) {
            SubCommand helpCommand = commands.get("help");
            if (helpCommand != null) {
                return helpCommand.execute(sender, new String[0]);
            }
            return false;
        }

        // 获取子命令
        String subCommandName = args[0].toLowerCase();
        SubCommand subCommand = commands.get(subCommandName);

        if (subCommand == null) {
            // 添加调试日志：记录未知命令尝试
            plugin.getLogger().warning("Unknown command attempted: " + subCommandName +
                                     " by " + sender.getName());
            sender.sendMessage(languageManager.getMessage("error.unknown-command"));
            return false;
        }

        // 检查权限
        if (!sender.hasPermission(subCommand.getPermission())) {
            // 添加调试日志：记录权限不足尝试
            plugin.getLogger().warning("Permission denied: " + sender.getName() +
                                     " attempted to use " + subCommandName +
                                     " without permission " + subCommand.getPermission());
            sender.sendMessage(languageManager.getMessage("error.no-permission"));
            return false;
        }

        // 检查是否需要玩家执行
        if (subCommand.requiresPlayer() && !subCommand.checkPlayer(sender)) {
            // 添加调试日志：记录非玩家尝试执行玩家专用命令
            plugin.getLogger().warning("Non-player " + sender.getName() +
                                     " attempted to use player-only command: " + subCommandName);
            sender.sendMessage(languageManager.getMessage("error.player-only"));
            return false;
        }

        // 执行子命令（移除第一个参数，即子命令名称）
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        // 添加调试日志：记录子命令执行
        plugin.getLogger().info("Executing subcommand: " + subCommandName +
                               " with subArgs: " + Arrays.toString(subArgs));
        return subCommand.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String subCommandName = args[0].toLowerCase();

        // 第一个参数：补全子命令名称
        if (args.length == 1) {
            return commands.values().stream()
                    .filter(sub -> sub.getName().toLowerCase().startsWith(subCommandName))
                    .filter(sub -> sender.hasPermission(sub.getPermission()))
                    .map(SubCommand::getName)
                    .collect(Collectors.toList());
        }

        // 后续参数：交给对应的子命令处理
        SubCommand subCommand = commands.get(subCommandName);
        if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return subCommand.tabComplete(sender, subArgs);
        }

        return Collections.emptyList();
    }

    /**
     * 获取所有已注册的命令
     */
    public List<SubCommand> getCommands() {
        return new ArrayList<>(commands.values());
    }
}
