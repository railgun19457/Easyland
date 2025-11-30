package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.command.LandCommandManager;
import io.github.railgun19457.easyland.listener.LandEnterListener;
import io.github.railgun19457.easyland.listener.LandProtectionListener;
import io.github.railgun19457.easyland.listener.LandSelectListener;
import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.migration.DataMigration;
import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.repository.SqliteLandRepository;
import io.github.railgun19457.easyland.service.LandService;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EasylandPlugin extends JavaPlugin {
    private ConfigManager configManager;
    private LanguageManager languageManager;

    // 新架构组件
    private LandRepository landRepository;
    private LandService landService;

    // 选区存储（用于命令和监听器）- 使用世界坐标
    private final Map<UUID, Location[]> selections = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // 初始化配置管理器并检查配置文件
        configManager = new ConfigManager(this);
        try {
            boolean hasChanges = configManager.checkAndFixConfig();
            if (hasChanges) {
                getLogger().info("Config file automatically fixed");
            }
        } catch (Exception e) {
            getLogger().severe("Config check failed: " + e.getMessage());
            e.printStackTrace();
        }

        // 初始化语言管理器
        languageManager = new LanguageManager(this);
        getLogger().info(languageManager.getMessage("log.plugin-enabled"));

        // 初始化新架构的数据层和服务层
        initializeNewArchitecture();

        // 注册事件监听器
        registerEventListeners();

        // 注册指令（使用新的命令管理器）
        registerCommands();

        // 输出保护规则状态
        logProtectionStatus();
    }

    @Override
    public void onDisable() {
        if (landRepository != null) {
            landRepository.close();
        }
        getLogger().info(languageManager.getMessage("log.plugin-disabled"));
    }

    /**
     * 初始化新架构（Repository + Service）
     */
    private void initializeNewArchitecture() {
        File dataFolder = getDataFolder();
        File yamlFile = new File(dataFolder, "lands.yml");
        File sqliteFile = new File(dataFolder, "lands.db");

        // 检查是否需要数据迁移
        DataMigration migration = new DataMigration(getLogger());
        if (migration.needsMigration(yamlFile, sqliteFile)) {
            getLogger().info("========================================");
            getLogger().info("检测到旧的 YAML 数据格式");
            getLogger().info("开始迁移到 SQLite 数据库...");
            getLogger().info("========================================");

            DataMigration.MigrationResult result = migration.migrateFromYamlToSqlite(yamlFile, sqliteFile);

            if (result.isSuccess()) {
                getLogger().info("========================================");
                getLogger().info("数据迁移成功完成！");
                getLogger().info("已迁移 " + result.getSuccessCount() + " 个领地到 SQLite");
                getLogger().info("原 YAML 文件已备份为 lands.yml.backup");
                getLogger().info("========================================");
            } else {
                getLogger().warning("========================================");
                getLogger().warning("数据迁移失败: " + result.getMessage());
                getLogger().warning("插件将继续使用 SQLite，但数据可能不完整");
                getLogger().warning("========================================");
            }
        }

        // 初始化 SQLite Repository
        landRepository = new SqliteLandRepository(sqliteFile);
        landRepository.initialize();
        getLogger().info("SQLite 数据库已初始化: " + sqliteFile.getAbsolutePath());

        // 检查并执行坐标系统迁移
        io.github.railgun19457.easyland.migration.CoordinateMigration coordMigration =
            new io.github.railgun19457.easyland.migration.CoordinateMigration(getLogger());
        if (coordMigration.needsMigration(sqliteFile)) {
            getLogger().info("========================================");
            getLogger().info("检测到旧的区块坐标格式");
            getLogger().info("开始迁移到世界坐标系统...");
            getLogger().info("========================================");

            io.github.railgun19457.easyland.migration.CoordinateMigration.MigrationResult coordResult =
                coordMigration.migrate(sqliteFile);

            if (coordResult.isSuccess()) {
                getLogger().info("========================================");
                getLogger().info("坐标系统迁移成功完成！");
                getLogger().info("已验证 " + coordResult.getMigratedCount() + " 个领地的坐标数据");
                getLogger().info("========================================");
            } else {
                getLogger().warning("========================================");
                getLogger().warning("坐标系统迁移失败: " + coordResult.getMessage());
                getLogger().warning("插件将继续运行，但可能存在数据问题");
                getLogger().warning("========================================");
            }
        }

        // 初始化 Service 层
        int maxLandsPerPlayer = configManager.getConfigValue("max-lands-per-player", 5);
        int maxChunksPerLand = configManager.getConfigValue("max-chunks-per-land", 256);
        Map<String, Boolean> defaultProtectionRules = configManager.getDefaultProtectionRules();

        landService = new LandService(landRepository, maxLandsPerPlayer, maxChunksPerLand, defaultProtectionRules);
        getLogger().info("领地服务层已初始化");
    }

    /**
     * 注册事件监听器
     */
    private void registerEventListeners() {
        int messageCooldownSeconds = configManager.getConfigValue("message-cooldown-seconds", 3);

        // 创建监听器（使用新架构）
        LandSelectListener landSelectListener = new LandSelectListener(landService, languageManager, selections);
        LandProtectionListener landProtectionListener = new LandProtectionListener(landService, configManager,
                languageManager, messageCooldownSeconds, this);
        LandEnterListener landEnterListener = new LandEnterListener(landService, languageManager);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);

        getLogger().info("事件监听器已注册（使用新架构）");
    }

    /**
     * 注册指令（使用新的命令管理器）
     */
    private void registerCommands() {
        // 将 UUID -> Location[] 转换为 String -> Location[]
        Map<String, Location[]> stringKeySelections = new HashMap<>();
        for (Map.Entry<UUID, Location[]> entry : selections.entrySet()) {
            stringKeySelections.put(entry.getKey().toString(), entry.getValue());
        }

        // 使用新的命令管理器
        LandCommandManager commandManager = new LandCommandManager(
                landService,
                landRepository,
                languageManager,
                stringKeySelections,
                this
        );
        this.getCommand("easyland").setExecutor(commandManager);
        this.getCommand("easyland").setTabCompleter(commandManager);

        getLogger().info("命令系统已注册（使用新架构）");
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
     * 获取语言管理器
     */
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    /**
     * 获取领地服务
     */
    public LandService getLandService() {
        return landService;
    }

    /**
     * 获取领地仓储
     */
    public LandRepository getLandRepository() {
        return landRepository;
    }

    /**
     * 获取选区存储
     */
    public Map<UUID, Location[]> getSelections() {
        return selections;
    }
}
