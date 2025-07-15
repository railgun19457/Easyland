package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class EasylandPlugin extends JavaPlugin {
    private LandManager landManager;
    private ConfigManager configManager;

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
        
        // 初始化管理器
        initializeManagers();
        
        // 注册事件监听器
        registerEventListeners();
        
        // 注册指令
        registerCommands();
        
        // 输出保护规则状态
        logProtectionStatus();
    }

    @Override
    public void onDisable() {
        if (landManager != null) {
            landManager.saveLands();
        }
        getLogger().info("EasylandPlugin 已禁用！");
    }
    
    /**
     * 初始化管理器
     */
    private void initializeManagers() {
        File dataFile = new File(getDataFolder(), "lands.yml");
        
        // 使用配置管理器获取配置值
        int maxLandsPerPlayer = configManager.getConfigValue("max-lands-per-player", 5);
        int maxChunksPerLand = configManager.getConfigValue("max-chunks-per-land", 256);
        int showDurationSeconds = configManager.getConfigValue("show-duration-seconds", 10);
        int maxShowDurationSeconds = configManager.getConfigValue("max-show-duration-seconds", 300);
        int messageCooldownSeconds = configManager.getConfigValue("message-cooldown-seconds", 3);
        
        landManager = new LandManager(dataFile, maxLandsPerPlayer, maxChunksPerLand, 
                                    configManager.getDefaultProtectionRules());
        
        // 创建监听器
        LandSelectListener landSelectListener = new LandSelectListener(landManager);
        LandProtectionListener landProtectionListener = new LandProtectionListener(landManager, configManager, messageCooldownSeconds);
        LandEnterListener landEnterListener = new LandEnterListener(landManager);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);
        
        // 注册指令
        LandCommand landCommand = new LandCommand(this, landManager, landSelectListener, 
                                                configManager, showDurationSeconds, maxShowDurationSeconds);
        this.getCommand("easyland").setExecutor(landCommand);
        this.getCommand("easyland").setTabCompleter(landCommand);
    }
    
    /**
     * 注册事件监听器
     */
    private void registerEventListeners() {
        // 监听器已在initializeManagers中注册
    }
    
    /**
     * 注册指令
     */
    private void registerCommands() {
        // 指令已在initializeManagers中注册
    }
    
    /**
     * 输出保护规则状态
     */
    private void logProtectionStatus() {
        getLogger().info("领地保护规则状态:");
        String[] ruleNames = ConfigManager.getProtectionRules();
        String[] displayNames = {"方块保护", "爆炸保护", "容器保护", "玩家保护"};
        
        for (int i = 0; i < ruleNames.length; i++) {
            boolean enabled = configManager.isProtectionRuleEnabled(ruleNames[i]);
            getLogger().info("- " + displayNames[i] + ": " + (enabled ? "启用" : "禁用"));
        }
    }
    
    /**
     * 获取配置管理器
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 获取领地管理器
     */
    public LandManager getLandManager() {
        return landManager;
    }
}
