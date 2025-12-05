package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.api.EasylandAPI;
import io.github.railgun19457.easyland.command.MainCommand;
import io.github.railgun19457.easyland.core.ConfigManager;
import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.core.LandCache;
import io.github.railgun19457.easyland.core.LandManager;
import io.github.railgun19457.easyland.core.PermissionManager;
import io.github.railgun19457.easyland.listener.BlockProtectionListener;
import io.github.railgun19457.easyland.listener.ContainerProtectionListener;
import io.github.railgun19457.easyland.listener.ExplosionProtectionListener;
import io.github.railgun19457.easyland.listener.LandEnterLeaveListener;
import io.github.railgun19457.easyland.listener.PlayerProtectionListener;
import io.github.railgun19457.easyland.listener.SelectionToolListener;
import io.github.railgun19457.easyland.core.SelectionManager;
import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.storage.DatabaseManager;
import io.github.railgun19457.easyland.storage.LandDAO;
import io.github.railgun19457.easyland.storage.LandTrustDAO;
import io.github.railgun19457.easyland.storage.PlayerDAO;
import io.github.railgun19457.easyland.storage.SqliteLandDAO;
import io.github.railgun19457.easyland.storage.SqliteLandTrustDAO;
import io.github.railgun19457.easyland.storage.SqlitePlayerDAO;
import io.github.railgun19457.easyland.visualization.LandVisualizer;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * EasyLand 插件的主类。
 * 负责初始化插件、注册监听器和提供核心功能。
 */
public class EasyLand extends JavaPlugin implements EasylandAPI {
    private static EasyLand instance;
    private Logger logger;
    private DatabaseManager databaseManager;
    private LandDAO landDAO;
    private PlayerDAO playerDAO;
    private LandTrustDAO landTrustDAO;
    private I18nManager i18nManager;
    private ConfigManager configManager;
    private LandCache landCache;
    private PermissionManager permissionManager;
    private LandManager landManager;
    private FlagManager flagManager;
    private LandVisualizer landVisualizer;
    private SelectionManager selectionManager;

    @Override
    public void onEnable() {
        // 设置实例
        instance = this;
        
        // 初始化日志记录器
        logger = getLogger();
        logger.info("EasyLand 插件正在启动...");

        // 保存默认配置
        saveDefaultConfig();

        try {
            // 初始化数据库
            initializeDatabase();

            // 初始化管理器
            initializeManagers();

            // 确保所有领地都有完整的标志
            ensureDatabaseIntegrity();

            // 注册事件监听器
            registerListeners();
            
            // 注册命令
            registerCommands();

            logger.info("EasyLand 插件已成功启动！");
        } catch (Exception e) {
            logger.severe("启动 EasyLand 插件时出错: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        logger.info("EasyLand 插件正在关闭...");

        // 关闭数据库连接
        if (databaseManager != null) {
            try {
                databaseManager.close();
            } catch (SQLException e) {
                logger.warning("关闭数据库连接时出错: " + e.getMessage());
            }
        }

        logger.info("EasyLand 插件已关闭。");
    }

    /**
     * 初始化数据库连接和表。
     *
     * @throws SQLException 如果数据库操作失败
     */
    private void initializeDatabase() throws SQLException {
        logger.info("初始化数据库...");
        
        // 初始化数据库管理器
        databaseManager = new DatabaseManager(getDataFolder(), logger);
        try {
            databaseManager.initialize();
        } catch (IOException e) {
            throw new SQLException("无法初始化数据库: " + e.getMessage(), e);
        }
        
        // 初始化 DAO
        landDAO = new SqliteLandDAO(databaseManager);
        playerDAO = new SqlitePlayerDAO(databaseManager);
        landTrustDAO = new SqliteLandTrustDAO(databaseManager);
        
        // 修复旧数据
        databaseManager.fixMissingData();
        
        logger.info("数据库初始化完成。");
    }

    /**
     * 确保数据库完整性，特别是补全缺失的标志。
     */
    private void ensureDatabaseIntegrity() {
        logger.info("正在检查数据库完整性...");
        try {
            // 获取所有标志的默认值
            java.util.Map<String, Boolean> defaultFlags = new java.util.HashMap<>();
            for (io.github.railgun19457.easyland.model.LandFlag flag : io.github.railgun19457.easyland.model.LandFlag.values()) {
                defaultFlags.put(flag.getName(), configManager.getDefaultRuleValue(flag.getName()));
            }
            
            // 确保所有领地都有这些标志
            landDAO.ensureAllFlagsExist(defaultFlags);
            logger.info("数据库完整性检查完成。");
        } catch (SQLException e) {
            logger.severe("检查数据库完整性时出错: " + e.getMessage());
        }
    }

    /**
     * 初始化各种管理器。
     */
    private void initializeManagers() {
        logger.info("初始化管理器...");
        
        // 初始化配置管理器
        configManager = new ConfigManager(logger, this);
        
        // 初始化国际化管理器
        i18nManager = new I18nManager(logger, getDataFolder(), this);
        i18nManager.initialize();
        
        // 初始化领地缓存
        landCache = new LandCache(logger, landDAO);
        
        // 初始化权限管理器
        permissionManager = new PermissionManager(logger, playerDAO, landTrustDAO);
        
        // 初始化领地管理器
        landManager = new LandManager(logger, landDAO, playerDAO, landTrustDAO, i18nManager, configManager, landCache, permissionManager);
        
        // 初始化标志管理器
        flagManager = new FlagManager(logger, landManager, landDAO, configManager, landCache);
        flagManager.setPermissionManager(permissionManager);
        
        // 初始化可视化器
        landVisualizer = new LandVisualizer(this);
        
        // 初始化选区管理器
        selectionManager = new SelectionManager();
        
        logger.info("管理器初始化完成。");
    }

    /**
     * 注册所有事件监听器。
     */
    private void registerListeners() {
        logger.info("注册事件监听器...");
        
        // 注册方块保护监听器
        getServer().getPluginManager().registerEvents(
            new BlockProtectionListener(flagManager), this);
        
        // 注册爆炸保护监听器
        getServer().getPluginManager().registerEvents(
            new ExplosionProtectionListener(flagManager), this);
        
        // 注册玩家保护监听器
        getServer().getPluginManager().registerEvents(
            new PlayerProtectionListener(flagManager), this);
        
        // 注册容器保护监听器
        getServer().getPluginManager().registerEvents(
            new ContainerProtectionListener(flagManager), this);
        
        // 注册选择工具监听器
        getServer().getPluginManager().registerEvents(
            new SelectionToolListener(selectionManager, i18nManager, permissionManager), this);

        // 注册领地进出监听器
        getServer().getPluginManager().registerEvents(
            new LandEnterLeaveListener(this), this);
        
        logger.info("事件监听器注册完成。");
    }
    
    /**
     * 注册所有命令。
     */
    private void registerCommands() {
        logger.info("注册命令...");
        
        // 注册主命令（el 现在是 easyland 的别名，在 plugin.yml 中定义）
        MainCommand mainCommand = new MainCommand(this);
        getCommand("easyland").setExecutor(mainCommand);
        getCommand("easyland").setTabCompleter(mainCommand);
        
        logger.info("命令注册完成。");
    }

    /**
     * 获取领地管理器。
     *
     * @return 领地管理器实例
     */
    public LandManager getLandManager() {
        return landManager;
    }

    /**
     * 获取权限管理器。
     *
     * @return 权限管理器实例
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * 获取标志管理器。
     *
     * @return 标志管理器实例
     */
    public FlagManager getFlagManager() {
        return flagManager;
    }

    /**
     * 获取国际化管理器。
     *
     * @return 国际化管理器实例
     */
    public I18nManager getI18nManager() {
        return i18nManager;
    }

    /**
     * 获取配置管理器。
     *
     * @return 配置管理器实例
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }

    /**
     * 获取领地可视化器。
     *
     * @return 领地可视化器实例
     */
    public LandVisualizer getLandVisualizer() {
        return landVisualizer;
    }

    /**
     * 获取领地缓存管理器。
     *
     * @return 领地缓存管理器实例
     */
    public LandCache getLandCache() {
        return landCache;
    }
    
    /**
     * 获取选区管理器。
     *
     * @return 选区管理器实例
     */
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    /**
     * 获取数据库管理器。
     *
     * @return 数据库管理器实例
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * 获取玩家数据访问对象。
     *
     * @return 玩家DAO实例
     */
    public PlayerDAO getPlayerDAO() {
        return playerDAO;
    }

    /**
     * 获取领地数据访问对象。
     *
     * @return 领地DAO实例
     */
    public LandDAO getLandDAO() {
        return landDAO;
    }

    /**
     * 获取领地信任数据访问对象。
     *
     * @return 领地信任DAO实例
     */
    public LandTrustDAO getLandTrustDAO() {
        return landTrustDAO;
    }

    /**
     * 获取 Easyland API 实例。
     * 允许其他插件安全地访问 Easyland 的核心功能。
     *
     * @return EasylandAPI 实例
     */
    public static EasylandAPI getAPI() {
        return instance;
    }

    // 实现 EasylandAPI 接口方法

    @Override
    public Optional<Land> getLandAt(Location location) {
        if (landManager == null) {
            return Optional.empty();
        }
        Land land = landManager.getLandAt(location);
        return land != null ? Optional.of(land) : Optional.empty();
    }

    @Override
    public boolean isProtected(Location location) {
        return getLandAt(location).isPresent();
    }

    @Override
    public Optional<UUID> getOwner(Location location) {
        Optional<Land> landOpt = getLandAt(location);
        if (landOpt.isPresent()) {
            Land land = landOpt.get();
            try {
                if (playerDAO != null) {
                    return playerDAO.getPlayerById(land.getOwnerId())
                                    .map(io.github.railgun19457.easyland.model.Player::getUuid);
                }
            } catch (SQLException e) {
                logger.warning("获取领地所有者时出错: " + e.getMessage());
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<Land> getLandById(int landId) {
        if (landManager == null) {
            return Optional.empty();
        }
        return landManager.getLandById(landId);
    }

    @Override
    public List<Land> getPlayerLands(UUID playerUuid) {
        // 为了保持向后兼容性，默认返回第一页的10条数据
        return getPlayerLands(playerUuid, 1, 10);
    }
    
    @Override
    public List<Land> getPlayerLands(UUID playerUuid, int page, int pageSize) {
        if (landManager == null) {
            return List.of();
        }
        // 获取所有领地，然后进行分页处理
        try {
            // 获取玩家数据库ID
            Optional<io.github.railgun19457.easyland.model.Player> playerOpt = playerDAO.getPlayerByUuid(playerUuid);
            if (!playerOpt.isPresent()) {
                return List.of();
            }
            
            // 获取所有领地
            List<Land> allLands = landDAO.getLandsByOwner(playerOpt.get().getId());
            
            // 应用分页
            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, allLands.size());
            
            if (startIndex >= allLands.size()) {
                return List.of();
            }
            
            return allLands.subList(startIndex, endIndex);
        } catch (SQLException e) {
            logger.warning("获取玩家领地时出错: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean hasPermission(UUID playerUuid, Land land, String action) {
        if (landManager == null) {
            return false;
        }
        // 获取 Bukkit 玩家对象
        org.bukkit.entity.Player player = getServer().getPlayer(playerUuid);
        if (player == null) {
            return false;
        }
        return landManager.hasPermission(player, land, action);
    }

    @Override
    public boolean isTrusted(UUID playerUuid, int landId) {
        try {
            if (playerDAO == null || landTrustDAO == null) {
                return false;
            }
            // 获取玩家数据库 ID
            Optional<io.github.railgun19457.easyland.model.Player> playerOpt = playerDAO.getPlayerByUuid(playerUuid);
            if (playerOpt.isPresent()) {
                return landTrustDAO.isPlayerTrusted(landId, playerOpt.get().getId());
            }
        } catch (SQLException e) {
            logger.warning("检查玩家信任状态时出错: " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<UUID> getTrustedPlayers(int landId) {
        try {
            if (landManager == null) {
                return List.of();
            }
            // 获取被信任的玩家列表
            List<io.github.railgun19457.easyland.model.Player> trustedPlayers =
                    landManager.listTrustedPlayers(String.valueOf(landId));
            
            // 转换为 UUID 列表
            return trustedPlayers.stream()
                    .map(io.github.railgun19457.easyland.model.Player::getUuid)
                    .toList();
        } catch (Exception e) {
            logger.warning("获取被信任玩家列表时出错: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Land> getLandsInWorld(String worldName) {
        try {
            if (landDAO == null) {
                return List.of();
            }
            return landDAO.getLandsByWorld(worldName);
        } catch (SQLException e) {
            logger.warning("获取世界领地列表时出错: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isInLand(Location location) {
        return getLandAt(location).isPresent();
    }
}