package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class EasylandPlugin extends JavaPlugin {
    private LandManager landManager;
    private ConfigManager configManager;
    private LanguageManager languageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 初始化配置管理器并检查配置文件
        configManager = new ConfigManager(this);
        try {
            boolean hasChanges = configManager.checkAndFixConfig();
            if (hasChanges) {
                getLogger().info("Config file automatically fixed"); // 临时使用英文，后续会改为语言管理器
            }
        } catch (Exception e) {
            getLogger().severe("Config check failed: " + e.getMessage()); // 临时使用英文
            e.printStackTrace();
        }

        // 初始化语言管理器
        languageManager = new LanguageManager(this);

        getLogger().info(languageManager.getMessage("log.plugin-enabled"));

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
        getLogger().info(languageManager.getMessage("log.plugin-disabled"));
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
        LandSelectListener landSelectListener = new LandSelectListener(landManager, languageManager);
        LandProtectionListener landProtectionListener = new LandProtectionListener(landManager, configManager,
                languageManager, messageCooldownSeconds);
        LandEnterListener landEnterListener = new LandEnterListener(landManager, languageManager);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);

        // 注册指令
        LandCommand landCommand = new LandCommand(this, landManager, landSelectListener,
                configManager, languageManager, showDurationSeconds, maxShowDurationSeconds);
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
        getLogger().info(languageManager.getMessage("log.protection-status"));
        String[] ruleNames = ConfigManager.getProtectionRules();

        for (String ruleName : ruleNames) {
            boolean enabled = configManager.isProtectionRuleEnabled(ruleName);
            String displayName = languageManager.getMessage("protection.rules." + ruleName.replace("-protection", ""));
            String status = enabled ? languageManager.getMessage("log.enabled")
                    : languageManager.getMessage("log.disabled");
            getLogger()
                    .info(String.format(languageManager.getMessage("log.protection-rule-status"), displayName, status));
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

    /**
     * 获取语言管理器
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }
}
