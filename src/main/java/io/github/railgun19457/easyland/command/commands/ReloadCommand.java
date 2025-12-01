package io.github.railgun19457.easyland.command.commands;

import io.github.railgun19457.easyland.EasylandPlugin;
import io.github.railgun19457.easyland.config.PluginConfig;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.command.SubCommand;
import io.github.railgun19457.easyland.service.LandService;
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
        sender.sendMessage("§e开始重载配置...");
        
        try {
            // 1. 重载配置管理器
            sender.sendMessage("§7正在重载配置管理器...");
            io.github.railgun19457.easyland.manager.ConfigManager.ReloadResult configResult =
                plugin.getConfigManager().reload();
            
            if (!configResult.isSuccess()) {
                sender.sendMessage("§c配置管理器重载失败: " + configResult.getMessage());
                return false;
            }
            sender.sendMessage("§a配置管理器重载成功: " + configResult.getMessage());
            
            // 2. 重载语言管理器
            sender.sendMessage("§7正在重载语言管理器...");
            io.github.railgun19457.easyland.manager.LanguageManager.ReloadResult langResult =
                languageManager.reload();
            
            if (!langResult.isSuccess()) {
                sender.sendMessage("§c语言管理器重载失败: " + langResult.getMessage());
                return false;
            }
            sender.sendMessage("§a语言管理器重载成功: " + langResult.getMessage());
            
            // 3. 更新 LandService 配置
            sender.sendMessage("§7正在更新服务层配置...");
            PluginConfig newConfig = plugin.getConfigManager().getPluginConfig();
            int maxLandsPerPlayer = newConfig.maxLandsPerPlayer();
            int maxChunksPerLand = newConfig.maxChunksPerLand();
            java.util.Map<String, Boolean> defaultProtectionRules = plugin.getConfigManager().getDefaultProtectionRules();
            
            io.github.railgun19457.easyland.service.LandService.ReloadResult serviceResult =
                landService.updateConfiguration(maxLandsPerPlayer, maxChunksPerLand, defaultProtectionRules);
            
            if (!serviceResult.isSuccess()) {
                sender.sendMessage("§c服务层配置更新失败: " + serviceResult.getMessage());
                return false;
            }
            sender.sendMessage("§a服务层配置更新成功: " + serviceResult.getMessage());
            
            // 4. 更新监听器配置
            sender.sendMessage("§7正在更新监听器配置...");
            
            // 更新 LandProtectionListener
            int messageCooldownSeconds = newConfig.messageCooldownSeconds();
            io.github.railgun19457.easyland.listener.LandProtectionListener.ReloadResult protectionResult =
                plugin.getLandProtectionListener().reload(plugin.getConfigManager(), languageManager, messageCooldownSeconds);
            
            if (!protectionResult.isSuccess()) {
                sender.sendMessage("§c保护监听器配置更新失败: " + protectionResult.getMessage());
                return false;
            }
            sender.sendMessage("§a保护监听器配置更新成功: " + protectionResult.getMessage());
            
            // 更新 LandEnterListener
            io.github.railgun19457.easyland.listener.LandEnterListener.ReloadResult enterResult =
                plugin.getLandEnterListener().reload(languageManager);
            
            if (!enterResult.isSuccess()) {
                sender.sendMessage("§c进入监听器配置更新失败: " + enterResult.getMessage());
                return false;
            }
            sender.sendMessage("§a进入监听器配置更新成功: " + enterResult.getMessage());
            
            // 5. 清理所有缓存
            sender.sendMessage("§7正在清理缓存...");
            plugin.cleanupAllCaches();
            sender.sendMessage("§a缓存清理完成");
            
            // 6. 输出重载完成信息
            sender.sendMessage("§a========================================");
            sender.sendMessage("§a配置重载完成！");
            sender.sendMessage("§a当前语言: " + languageManager.getDefaultLanguage());
            sender.sendMessage("§a最大领地数/玩家: " + maxLandsPerPlayer);
            sender.sendMessage("§a最大区块数/领地: " + maxChunksPerLand);
            sender.sendMessage("§a消息冷却时间: " + messageCooldownSeconds + " 秒");
            sender.sendMessage("§a========================================");
            
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c重载配置时出错: " + e.getMessage());
            plugin.getLogger().severe("配置重载失败: " + e.getMessage());
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
