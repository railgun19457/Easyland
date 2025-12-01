package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.EasylandPlugin;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.manager.LanguageManager;

/**
 * 缓存管理命令
 * 用于查看和管理插件缓存状态
 */
public class CacheCommand extends SubCommand {
    private final LanguageManager languageManager;
    private final EasylandPlugin plugin;

    public CacheCommand(LanguageManager languageManager, EasylandPlugin plugin) {
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cache";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.cache.description");
    }

    @Override
    public String getUsage() {
        return "/easyland cache [cleanup|stats|monitor|help]";
    }

    @Override
    public String getPermission() {
        return "easyland.command.cache";
    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }

    @Override
    public boolean execute(org.bukkit.command.CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(languageManager.getMessage("error.no-permission"));
            return true;
        }

        if (args.length == 0) {
            // 显示缓存状态
            String report = plugin.getDetailedCacheReport();
            sender.sendMessage("§6=== Easyland 缓存状态 ===");
            String[] lines = report.split("\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sender.sendMessage("§e" + line);
                }
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "cleanup":
                // 手动清理缓存
                sender.sendMessage("§6正在清理缓存...");
                plugin.manualCacheCleanup();
                sender.sendMessage("§a缓存清理完成！");
                break;
                
            case "stats":
                // 显示详细统计
                plugin.logAllCacheStats();
                sender.sendMessage("§a缓存统计已输出到控制台");
                break;
                
            case "monitor":
                // 检查缓存健康状况
                sender.sendMessage("§6检查缓存健康状况...");
                plugin.checkCacheHealth();
                sender.sendMessage("§a缓存健康检查完成，详情请查看控制台");
                break;
                
            case "help":
                // 显示帮助信息
                sender.sendMessage("§6=== 缓存管理命令帮助 ===");
                sender.sendMessage("§e/easyland cache §7- 显示缓存状态");
                sender.sendMessage("§e/easyland cache cleanup §7- 手动清理缓存");
                sender.sendMessage("§e/easyland cache stats §7- 输出详细统计到控制台");
                sender.sendMessage("§e/easyland cache monitor §7- 检查缓存健康状况");
                sender.sendMessage("§e/easyland cache help §7- 显示此帮助信息");
                break;
                
            default:
                sender.sendMessage("§c未知的子命令: " + subCommand);
                sender.sendMessage("§7使用 §e/easyland cache help §7查看可用命令");
                break;
        }

        return true;
    }

    @Override
    public java.util.List<String> tabComplete(org.bukkit.command.CommandSender sender, String[] args) {
        if (args.length == 1) {
            java.util.List<String> subCommands = java.util.Arrays.asList("cleanup", "stats", "monitor", "help");
            return subCommands.stream()
                    .filter(cmd -> cmd.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return java.util.Collections.emptyList();
    }
}