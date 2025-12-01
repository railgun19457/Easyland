package io.github.railgun19457.easyland;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.railgun19457.easyland.command.LandCommandManager;
import io.github.railgun19457.easyland.config.PluginConfig;
import io.github.railgun19457.easyland.listener.LandEnterListener;
import io.github.railgun19457.easyland.listener.LandProtectionListener;
import io.github.railgun19457.easyland.listener.LandSelectListener;
import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.migration.DataMigration;
import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.repository.SqliteLandRepository;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.util.AsyncTaskManager;
import io.github.railgun19457.easyland.util.CacheManager;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.HandlerList;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class EasylandPlugin extends JavaPlugin {
    private static final Logger logger = Logger.getLogger(EasylandPlugin.class.getName());
    
    private ConfigManager configManager;
    private LanguageManager languageManager;

    // 新架构组件
    private LandRepository landRepository;
    private LandService landService;
    
    // 监听器实例，用于缓存管理和注销
    private LandProtectionListener landProtectionListener;
    private LandEnterListener landEnterListener;
    private LandSelectListener landSelectListener;

    // 选区存储（用于命令和监听器）- 使用CacheManager管理
    private final CacheManager<UUID, Location[]> selections;
    
    // 缓存监控任务
    private BukkitTask cacheMonitorTask;
    
    // 异步任务管理器
    private AsyncTaskManager asyncTaskManager;
    
    // 用于保护选区操作的锁
    private final Object selectionLock = new Object();
    
    // 构造函数，初始化选区缓存
    public EasylandPlugin() {
        // 初始化选区缓存 - 最大500个玩家，过期时间1小时，启用定时清理
        this.selections = new CacheManager<>("SelectionsCache", 500, 60 * 60 * 1000);
    }

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

        // 初始化异步任务管理器
        asyncTaskManager = new AsyncTaskManager(this);
        getLogger().info("AsyncTaskManager initialized");

        // 启动缓存监控任务
        startCacheMonitoring();

        // 输出保护规则状态
        logProtectionStatus();
    }

    @Override
    public void onDisable() {
        logger.info("Shutting down EasylandPlugin...");
        
        // 使用计数器跟踪清理步骤，确保所有步骤都尝试执行
        int totalSteps = 6;
        int completedSteps = 0;
        StringBuilder errors = new StringBuilder();
        
        // 1. 首先停止所有定时任务和异步操作
        try {
            stopAllTasks();
            completedSteps++;
            logger.info("Step 1/6: Tasks stopped successfully");
        } catch (Exception e) {
            String error = "Step 1/6 failed to stop tasks: " + e.getMessage();
            logger.severe(error);
            errors.append(error).append("\n");
        }
        
        // 2. 注销所有事件监听器
        try {
            unregisterAllListeners();
            completedSteps++;
            logger.info("Step 2/6: Listeners unregistered successfully");
        } catch (Exception e) {
            String error = "Step 2/6 failed to unregister listeners: " + e.getMessage();
            logger.severe(error);
            errors.append(error).append("\n");
        }
        
        // 3. 执行最后的缓存清理
        try {
            logger.info("Step 3/6: Performing final cache cleanup...");
            cleanupAllCaches();
            completedSteps++;
            logger.info("Step 3/6: Cache cleanup completed");
        } catch (Exception e) {
            String error = "Step 3/6 failed to cleanup caches: " + e.getMessage();
            logger.severe(error);
            errors.append(error).append("\n");
        }
        
        // 4. 输出最终缓存统计
        try {
            logger.info("Step 4/6: Final cache statistics:");
            logAllCacheStats();
            completedSteps++;
            logger.info("Step 4/6: Cache statistics logged");
        } catch (Exception e) {
            String error = "Step 4/6 failed to log cache statistics: " + e.getMessage();
            logger.severe(error);
            errors.append(error).append("\n");
        }
        
        // 5. 关闭服务和缓存
        try {
            shutdownServicesAndCaches();
            completedSteps++;
            logger.info("Step 5/6: Services and caches shutdown completed");
        } catch (Exception e) {
            String error = "Step 5/6 failed to shutdown services and caches: " + e.getMessage();
            logger.severe(error);
            errors.append(error).append("\n");
        }
        
        // 6. 关闭数据库连接
        try {
            closeDatabaseConnections();
            completedSteps++;
            logger.info("Step 6/6: Database connections closed");
        } catch (Exception e) {
            String error = "Step 6/6 failed to close database connections: " + e.getMessage();
            logger.severe(error);
            errors.append(error).append("\n");
        }
        
        // 输出最终状态
        try {
            getLogger().info(languageManager.getMessage("log.plugin-disabled"));
        } catch (Exception e) {
            logger.warning("Failed to get plugin disabled message: " + e.getMessage());
        }
        
        if (completedSteps == totalSteps) {
            logger.info("EasylandPlugin shutdown completed successfully (" + completedSteps + "/" + totalSteps + " steps)");
        } else {
            logger.severe("EasylandPlugin shutdown completed with errors (" + completedSteps + "/" + totalSteps + " steps)");
            logger.severe("Errors encountered:\n" + errors.toString());
        }
    }
    
    /**
     * 停止所有定时任务和异步操作
     */
    private void stopAllTasks() {
        try {
            // 停止缓存监控任务
            if (cacheMonitorTask != null) {
                cacheMonitorTask.cancel();
                cacheMonitorTask = null;
                logger.info("Cache monitoring task stopped");
            }
            
            // 关闭异步任务管理器
            if (asyncTaskManager != null) {
                asyncTaskManager.shutdown();
                logger.info("AsyncTaskManager shutdown completed");
            }
        } catch (Exception e) {
            logger.warning("Error stopping tasks: " + e.getMessage());
        }
    }
    
    /**
     * 注销所有事件监听器
     */
    private void unregisterAllListeners() {
        try {
            // 获取所有已注册的监听器并注销
            if (landProtectionListener != null) {
                HandlerList.unregisterAll(landProtectionListener);
                logger.info("LandProtectionListener unregistered");
            }
            
            if (landEnterListener != null) {
                HandlerList.unregisterAll(landEnterListener);
                logger.info("LandEnterListener unregistered");
            }
            
            if (landSelectListener != null) {
                HandlerList.unregisterAll(landSelectListener);
                logger.info("LandSelectListener unregistered");
            }
        } catch (Exception e) {
            logger.warning("Error unregistering listeners: " + e.getMessage());
        }
    }
    
    /**
     * 关闭服务和缓存
     */
    private void shutdownServicesAndCaches() {
        try {
            // 关闭缓存和服务
            if (landService != null) {
                landService.shutdown();
                logger.info("LandService shutdown completed");
            }
            
            if (landProtectionListener != null) {
                landProtectionListener.shutdown();
                logger.info("LandProtectionListener shutdown completed");
            }
            
            if (landEnterListener != null) {
                landEnterListener.shutdown();
                logger.info("LandEnterListener shutdown completed");
            }
            
            if (landSelectListener != null) {
                landSelectListener.shutdown();
                logger.info("LandSelectListener shutdown completed");
            }
            
            if (selections != null) {
                selections.logStats();
                selections.shutdown();
                logger.info("Selections cache shutdown completed");
            }
        } catch (Exception e) {
            logger.warning("Error shutting down services and caches: " + e.getMessage());
        }
    }
    
    /**
     * 关闭数据库连接
     */
    private void closeDatabaseConnections() {
        try {
            if (landRepository != null) {
                landRepository.close();
                logger.info("LandRepository closed");
            }
        } catch (Exception e) {
            logger.warning("Error closing database connections: " + e.getMessage());
        }
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
            
            getLogger().severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            getLogger().severe("自动数据迁移功能已在此版本中移除。");
            getLogger().severe("检测到旧的 lands.yml 文件，但无法自动迁移。");
            getLogger().severe("请使用旧版本的 Easyland 完成迁移，或手动转移您的数据。");
            getLogger().severe("插件将不会加载旧数据。");
            getLogger().severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
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
        PluginConfig currentConfig = configManager.getPluginConfig();
        int maxLandsPerPlayer = currentConfig.maxLandsPerPlayer();
        int maxChunksPerLand = currentConfig.maxChunksPerLand();
        Map<String, Boolean> defaultProtectionRules = configManager.getDefaultProtectionRules();

        landService = new LandService(landRepository, maxLandsPerPlayer, maxChunksPerLand, defaultProtectionRules);
        getLogger().info("领地服务层已初始化");
    }

    /**
     * 注册事件监听器
     */
    private void registerEventListeners() {
        int messageCooldownSeconds = configManager.getPluginConfig().messageCooldownSeconds();

        // 创建监听器（使用新架构）
        this.landSelectListener = new LandSelectListener(landService, languageManager,
            new HashMap<UUID, Location[]>()); // 临时Map，实际使用CacheManager
        landProtectionListener = new LandProtectionListener(landService, configManager,
                languageManager, messageCooldownSeconds, this);
        landEnterListener = new LandEnterListener(landService, languageManager);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this.landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);

        getLogger().info("事件监听器已注册（使用新架构）");
    }

    /**
     * 注册指令（使用新的命令管理器）
     */
    private void registerCommands() {
        // 创建适配器Map，将CacheManager转换为Map接口，使用String键
        Map<String, Location[]> selectionAdapter = new Map<String, Location[]>() {
            @Override
            public Location[] put(String key, Location[] value) {
                synchronized (selectionLock) {
                    UUID uuid = UUID.fromString(key);
                    selections.put(uuid, value);
                    return null;
                }
            }
            
            @Override
            public Location[] get(Object key) {
                synchronized (selectionLock) {
                    if (key instanceof String) {
                        UUID uuid = UUID.fromString((String) key);
                        return selections.get(uuid);
                    }
                    return null;
                }
            }
            
            @Override
            public Location[] remove(Object key) {
                synchronized (selectionLock) {
                    if (key instanceof String) {
                        UUID uuid = UUID.fromString((String) key);
                        selections.remove(uuid);
                        return null;
                    }
                    return null;
                }
            }
            
            @Override
            public int size() {
                synchronized (selectionLock) {
                    return (int) selections.size();
                }
            }
            
            @Override
            public boolean isEmpty() {
                synchronized (selectionLock) {
                    return selections.size() == 0;
                }
            }
            
            @Override
            public boolean containsKey(Object key) {
                synchronized (selectionLock) {
                    if (key instanceof String) {
                        UUID uuid = UUID.fromString((String) key);
                        return selections.containsKey(uuid);
                    }
                    return false;
                }
            }
            
            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException("Not implemented");
            }
            
            @Override
            public void putAll(Map<? extends String, ? extends Location[]> m) {
                synchronized (selectionLock) {
                    for (Map.Entry<? extends String, ? extends Location[]> entry : m.entrySet()) {
                        UUID uuid = UUID.fromString(entry.getKey());
                        selections.put(uuid, entry.getValue());
                    }
                }
            }
            
            @Override
            public void clear() {
                synchronized (selectionLock) {
                    selections.clear();
                }
            }
            
            @Override
            public java.util.Set<String> keySet() {
                throw new UnsupportedOperationException("Not implemented");
            }
            
            @Override
            public java.util.Collection<Location[]> values() {
                throw new UnsupportedOperationException("Not implemented");
            }
            
            @Override
            public java.util.Set<Map.Entry<String, Location[]>> entrySet() {
                throw new UnsupportedOperationException("Not implemented");
            }
        };

        // 使用新的命令管理器
        LandCommandManager commandManager = new LandCommandManager(
                landService,
                landRepository,
                languageManager,
                selectionAdapter,
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
     * 获取异步任务管理器
     */
    public AsyncTaskManager getAsyncTaskManager() {
        return asyncTaskManager;
    }
    
    /**
     * 获取领地保护监听器
     */
    public LandProtectionListener getLandProtectionListener() {
        return landProtectionListener;
    }
    
    /**
     * 获取领地进入监听器
     */
    public LandEnterListener getLandEnterListener() {
        return landEnterListener;
    }

    /**
     * 获取选区存储
     */
    public Map<UUID, Location[]> getSelections() {
        // 返回适配器，保持API兼容性
        return new HashMap<UUID, Location[]>() {
            @Override
            public Location[] put(UUID key, Location[] value) {
                synchronized (selectionLock) {
                    selections.put(key, value);
                    return null;
                }
            }
            
            @Override
            public Location[] get(Object key) {
                synchronized (selectionLock) {
                    return selections.get((UUID) key);
                }
            }
            
            @Override
            public Location[] remove(Object key) {
                synchronized (selectionLock) {
                    selections.remove((UUID) key);
                    return null;
                }
            }
            
            @Override
            public int size() {
                synchronized (selectionLock) {
                    return (int) selections.size();
                }
            }
            
            @Override
            public boolean isEmpty() {
                synchronized (selectionLock) {
                    return selections.size() == 0;
                }
            }
            
            @Override
            public boolean containsKey(Object key) {
                synchronized (selectionLock) {
                    return selections.containsKey((UUID) key);
                }
            }
            
            @Override
            public boolean containsValue(Object value) {
                throw new UnsupportedOperationException("Not implemented");
            }
            
            @Override
            public void putAll(Map<? extends UUID, ? extends Location[]> m) {
                synchronized (selectionLock) {
                    for (Map.Entry<? extends UUID, ? extends Location[]> entry : m.entrySet()) {
                        selections.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            
            @Override
            public void clear() {
                synchronized (selectionLock) {
                    selections.clear();
                }
            }
            
            @Override
            public java.util.Set<UUID> keySet() {
                throw new UnsupportedOperationException("Not implemented");
            }
            
            @Override
            public java.util.Collection<Location[]> values() {
                throw new UnsupportedOperationException("Not implemented");
            }
            
            @Override
            public java.util.Set<Map.Entry<UUID, Location[]>> entrySet() {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }
    
    /**
     * 获取选区缓存统计信息
     */
    public CacheStats getSelectionsCacheStats() {
        return selections.getStats();
    }
    
    /**
     * 清理所有缓存
     */
    public void cleanupAllCaches() {
        logger.info("Cleaning up all caches...");
        
        if (landService != null) {
            landService.cleanupCache();
        }
        
        if (landProtectionListener != null) {
            landProtectionListener.cleanupCache();
        }
        
        if (landEnterListener != null) {
            landEnterListener.cleanupNameCache();
        }
        
        if (selections != null) {
            selections.clear(); // Or just let it expire
        }
        
        logger.info("All caches cleaned up");
    }
    
    /**
     * 获取所有缓存统计信息
     */
    public void logAllCacheStats() {
        logger.info("=== Cache Statistics ===");
        
        if (landService != null) {
            logger.info("LandService: " + landService.getCacheStats());
        }
        
        if (landProtectionListener != null) {
            logger.info("LandProtectionListener: " + landProtectionListener.getCacheStats());
        }
        
        if (landEnterListener != null) {
            logger.info("LandEnterListener: " + landEnterListener.getCacheStats());
        }
        
        if (selections != null) {
            logger.info("Selections: " + selections.getStats());
        }
        
        logger.info("=== End Cache Statistics ===");
    }
    
    /**
     * 启动缓存监控任务
     */
    private void startCacheMonitoring() {
        // 每10分钟输出一次缓存统计信息
        long intervalTicks = 10 * 60 * 20; // 10分钟 = 600秒 = 12000 ticks
        
        cacheMonitorTask = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            try {
                logger.info("=== Periodic Cache Monitor ===");
                logAllCacheStats();
                
                // 检查缓存健康状况
                checkCacheHealth();
                
                logger.info("=== End Periodic Cache Monitor ===");
            } catch (Exception e) {
                logger.warning("Error in cache monitoring task: " + e.getMessage());
                e.printStackTrace();
            }
        }, intervalTicks, intervalTicks);
        
        logger.info("Cache monitoring task started (interval: 10 minutes)");
    }
    
    /**
     * 检查缓存健康状况
     */
    public void checkCacheHealth() {
        boolean hasIssues = false;
        
        // 检查LandService缓存
        if (landService != null) {
            CacheStats stats = landService.getCacheStats();
            // Note: Caffeine does not expose maxSize directly in stats. This check needs adjustment or removal.
            // We'll check estimated size vs a configured max, if available elsewhere.
            // For now, let's just log the size.
            logger.info("LandService cache size: " + stats.requestCount());

            if (stats.hitRate() < 0.5 && stats.requestCount() > 100) {
                logger.warning("LandService cache has low hit rate: " + String.format("%.2f%%", stats.hitRate() * 100));
                hasIssues = true;
            }
        }
        
        // 检查LandProtectionListener缓存
        if (landProtectionListener != null) {
            CacheStats stats = landProtectionListener.getCacheStats();
            logger.info("LandProtectionListener cache request count: " + stats.requestCount());
        }
        
        // 检查LandEnterListener缓存
        if (landEnterListener != null) {
            CacheStats stats = landEnterListener.getCacheStats();
            logger.info("LandEnterListener cache request count: " + stats.requestCount());
        }
        
        // 检查选区缓存
        if (selections != null) {
            CacheStats stats = selections.getStats();
            logger.info("Selections cache request count: " + stats.requestCount());
        }
        
        if (!hasIssues) {
            logger.info("All caches are healthy");
        } else {
            logger.warning("Cache health issues detected, consider running cleanup");
        }
    }
    
    /**
     * 手动触发缓存清理（可通过命令调用）
     */
    public void manualCacheCleanup() {
        logger.info("Manual cache cleanup triggered...");
        cleanupAllCaches();
        logAllCacheStats();
        logger.info("Manual cache cleanup completed");
    }
    
    /**
     * 获取详细的缓存报告
     */
    public String getDetailedCacheReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Easyland Cache Report ===\n");
        
        if (landService != null) {
            CacheStats stats = landService.getCacheStats();
            report.append("LandService Cache:\n");
            report.append(String.format("  Stats: %s\n", stats.toString()));
            report.append("\n");
        }
        
        if (landProtectionListener != null) {
            CacheStats stats = landProtectionListener.getCacheStats();
            report.append("LandProtectionListener Cache:\n");
            report.append(String.format("  Stats: %s\n", stats.toString()));
            report.append("\n");
        }
        
        if (landEnterListener != null) {
            CacheStats stats = landEnterListener.getCacheStats();
            report.append("LandEnterListener Cache:\n");
            report.append(String.format("  Stats: %s\n", stats.toString()));
            report.append("\n");
        }
        
        if (selections != null) {
            CacheStats stats = selections.getStats();
            report.append("Selections Cache:\n");
            report.append(String.format("  Stats: %s\n", stats.toString()));
            report.append("\n");
        }
        
        report.append("=== End Cache Report ===");
        return report.toString();
    }
}
