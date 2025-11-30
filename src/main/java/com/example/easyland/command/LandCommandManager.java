package com.example.easyland.command;

import com.example.easyland.EasylandPlugin;
import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.commands.*;
import com.example.easyland.repository.LandRepository;
import com.example.easyland.service.LandService;
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
    private final Map<String, SubCommand> commands = new HashMap<>();
    private final List<SubCommand> commandList = new ArrayList<>();
    private final LanguageManager languageManager;

    public LandCommandManager(LandService landService, LandRepository landRepository,
                              LanguageManager languageManager, Map<String, Location[]> selections,
                              EasylandPlugin plugin) {
        this.languageManager = languageManager;

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
        // 帮助命令需要所有命令列表
        registerCommand(new HelpCommand(languageManager, commandList));
    }

    /**
     * 注册子命令
     */
    private void registerCommand(SubCommand command) {
        commands.put(command.getName().toLowerCase(), command);
        commandList.add(command);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
            sender.sendMessage(languageManager.getMessage("error.unknown-command"));
            return false;
        }

        // 检查权限
        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(languageManager.getMessage("error.no-permission"));
            return false;
        }

        // 检查是否需要玩家执行
        if (subCommand.requiresPlayer() && !subCommand.checkPlayer(sender)) {
            sender.sendMessage(languageManager.getMessage("error.player-only"));
            return false;
        }

        // 执行子命令（移除第一个参数，即子命令名称）
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subCommand.execute(sender, subArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // 第一个参数：补全子命令名称
        if (args.length == 1) {
            return commands.keySet().stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .filter(name -> sender.hasPermission(commands.get(name).getPermission()))
                    .collect(Collectors.toList());
        }

        // 后续参数：交给对应的子命令处理
        if (args.length > 1) {
            String subCommandName = args[0].toLowerCase();
            SubCommand subCommand = commands.get(subCommandName);

            if (subCommand != null && sender.hasPermission(subCommand.getPermission())) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return subCommand.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }

    /**
     * 获取所有已注册的命令
     */
    public List<SubCommand> getCommands() {
        return new ArrayList<>(commandList);
    }
}
