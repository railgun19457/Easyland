package com.example.easyland.command.commands;

import com.example.easyland.EasylandPlugin;
import com.example.easyland.manager.LanguageManager;
import com.example.easyland.command.SubCommand;
import com.example.easyland.service.LandService;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * 重载配置命令
 */
public class ReloadCommand extends SubCommand {
    private final EasylandPlugin plugin;
    private final LandService landService;
    private final LanguageManager languageManager;

    public ReloadCommand(EasylandPlugin plugin, LandService landService, LanguageManager languageManager) {
        this.plugin = plugin;
        this.landService = landService;
        this.languageManager = languageManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        try {
            // 重载配置文件
            plugin.reloadConfig();

            // 重建缓存
            landService.rebuildCache();

            sender.sendMessage("§a配置已重载！");
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c重载配置时出错: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return languageManager.getMessage("command.reload.description");
    }

    @Override
    public String getUsage() {
        return "/easyland reload";
    }

    @Override
    public String getPermission() {
        return "easyland.admin.reload";
    }

    @Override
    public boolean requiresPlayer() {
        return false;
    }
}
