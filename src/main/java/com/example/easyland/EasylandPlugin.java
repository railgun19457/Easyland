package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class EasylandPlugin extends JavaPlugin {
    private LandManager landManager;
    private LandSelectListener landSelectListener;
    private LandProtectionListener landProtectionListener;
    private LandEnterListener landEnterListener;
    private ConfigManager configManager;
    private int maxLandsPerPlayer;
    private int maxChunksPerLand;
    private int showDurationSeconds;
    private int maxShowDurationSeconds;
    private boolean enableBlockProtection;
    private boolean enableExplosionProtection;
    private boolean enableContainerProtection;
    private boolean enablePlayerProtection;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        
        // 初始化配置管理器并检查配置文件
        configManager = new ConfigManager(this);
        try {
            boolean hasChanges = configManager.checkAndFixConfig();
            if (hasChanges) {
                getLogger().info("配置文件已自动修复");
            }
        } catch (Exception e) {
            getLogger().severe("配置文件检查失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        getLogger().info("EasylandPlugin 已启用！");
        File dataFile = new File(getDataFolder(), "lands.yml");
        
        // 使用配置管理器获取配置值
        maxLandsPerPlayer = configManager.getConfigValue("max-lands-per-player", 5);
        maxChunksPerLand = configManager.getConfigValue("max-chunks-per-land", 256);
        showDurationSeconds = configManager.getConfigValue("show-duration-seconds", 10);
        maxShowDurationSeconds = configManager.getConfigValue("max-show-duration-seconds", 300);
        int messageCooldownSeconds = configManager.getConfigValue("message-cooldown-seconds", 3);
        
        // 读取保护规则配置 - 默认为false，确保不影响原版体验
        enableBlockProtection = configManager.getConfigValue("protection.block-protection", false);
        enableExplosionProtection = configManager.getConfigValue("protection.explosion-protection", false);
        enableContainerProtection = configManager.getConfigValue("protection.container-protection", false);
        enablePlayerProtection = configManager.getConfigValue("protection.player-protection", false);
        
        landManager = new LandManager(dataFile, maxLandsPerPlayer, maxChunksPerLand);
        landSelectListener = new LandSelectListener(landManager);
        landProtectionListener = new LandProtectionListener(landManager, enableBlockProtection, 
                                                          enableExplosionProtection, enableContainerProtection, 
                                                          enablePlayerProtection, messageCooldownSeconds);
        landEnterListener = new LandEnterListener(landManager);
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);
        // 注册 /easyland 指令（含别名）
        LandCommand landCommand = new LandCommand(this, landManager, landSelectListener, showDurationSeconds, maxShowDurationSeconds);
        this.getCommand("easyland").setExecutor(landCommand);
        this.getCommand("easyland").setTabCompleter(landCommand);
        
        // 输出保护规则状态
        getLogger().info("领地保护规则状态:");
        getLogger().info("- 方块保护: " + (enableBlockProtection ? "启用" : "禁用"));
        getLogger().info("- 爆炸保护: " + (enableExplosionProtection ? "启用" : "禁用"));
        getLogger().info("- 容器保护: " + (enableContainerProtection ? "启用" : "禁用"));
        getLogger().info("- 玩家保护: " + (enablePlayerProtection ? "启用" : "禁用"));
    }

    @Override
    public void onDisable() {
        if (landManager != null) landManager.saveLands();
        getLogger().info("EasylandPlugin 已禁用！");
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}
