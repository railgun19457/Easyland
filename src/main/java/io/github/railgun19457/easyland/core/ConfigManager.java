package io.github.railgun19457.easyland.core;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * 配置管理器，负责加载和管理插件的所有配置项。
 * 这个类提供了一个统一的接口来访问配置文件中的值，
 * 避免在代码中直接访问配置文件。
 */
public class ConfigManager {
    private final Logger logger;
    private final JavaPlugin plugin;
    private FileConfiguration config;
    
    // 领地相关配置
    private int maxLandsPerPlayer;
    private int maxLandArea;
    private int minLandArea;
    private int minLandDistance;
    private int listPerPage;
    
    // 可视化相关配置
    private int defaultVisualizationDuration;
    private int maxVisualizationDuration;
    
    // 保护相关配置
    private boolean blockProtectionEnabled;
    private boolean containerProtectionEnabled;
    private boolean explosionProtectionEnabled;
    private boolean playerProtectionEnabled;
    
    private boolean defaultBlockProtection;
    private boolean defaultContainerProtection;
    private boolean defaultExplosionProtection;
    private boolean defaultPlayerProtection;
    
    // 子领地相关配置
    private int maxSubClaimsPerLand;
    private int maxSubClaimDepth;
    
    /**
     * 构造函数，初始化配置管理器。
     *
     * @param logger 插件日志记录器
     * @param plugin 插件实例
     */
    public ConfigManager(Logger logger, JavaPlugin plugin) {
        this.logger = logger;
        this.plugin = plugin;
        
        // 保存默认配置文件
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        
        // 加载配置值
        loadConfigValues();
        
        logger.info("配置管理器已初始化，已加载所有配置项。");
    }
    
    /**
     * 从配置文件中加载所有配置值。
     */
    private void loadConfigValues() {
        // 加载领地相关配置
        this.maxLandsPerPlayer = config.getInt("land.max-per-player", 10);
        this.maxLandArea = config.getInt("land.max-area", 10000);
        this.minLandArea = config.getInt("land.min-area", 100);
        this.minLandDistance = config.getInt("land.min-distance", 5);
        this.listPerPage = config.getInt("land.list-per-page", 10);
        
        // 加载可视化相关配置
        this.defaultVisualizationDuration = config.getInt("visualization.default-duration", 10);
        this.maxVisualizationDuration = config.getInt("visualization.max-duration", 60);
        
        // 加载保护相关配置
        this.blockProtectionEnabled = config.getBoolean("protection.block-protection.enable", true);
        this.containerProtectionEnabled = config.getBoolean("protection.container-protection.enable", true);
        this.explosionProtectionEnabled = config.getBoolean("protection.explosion-protection.enable", true);
        this.playerProtectionEnabled = config.getBoolean("protection.player-protection.enable", true);
        
        // 加载默认保护状态
        this.defaultBlockProtection = config.getBoolean("protection.block-protection.default", true);
        this.defaultContainerProtection = config.getBoolean("protection.container-protection.default", true);
        this.defaultExplosionProtection = config.getBoolean("protection.explosion-protection.default", true);
        this.defaultPlayerProtection = config.getBoolean("protection.player-protection.default", true);
        
        // 加载子领地相关配置
        this.maxSubClaimsPerLand = config.getInt("sub-claim.max-per-land", 5);
        this.maxSubClaimDepth = config.getInt("sub-claim.max-depth", 2);
    }
    
    /**
     * 重新加载配置文件。
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        
        // 重新加载配置值
        loadConfigValues();
        
        logger.info("配置文件已重新加载。");
    }
    
    // 领地相关配置的 getter 方法
    
    /**
     * 获取每个玩家最多拥有的领地数量。
     *
     * @return 最大领地数量
     */
    public int getMaxLandsPerPlayer() {
        return maxLandsPerPlayer;
    }
    
    /**
     * 获取单个领地的最大面积。
     *
     * @return 最大面积
     */
    public int getMaxLandArea() {
        return maxLandArea;
    }
    
    /**
     * 获取单个领地的最小面积。
     *
     * @return 最小面积
     */
    public int getMinLandArea() {
        return minLandArea;
    }
    
    /**
     * 获取领地之间的最小距离。
     *
     * @return 最小距离
     */
    public int getMinLandDistance() {
        return minLandDistance;
    }
    
    /**
     * 获取每页显示的领地数量。
     *
     * @return 每页显示数量
     */
    public int getListPerPage() {
        return listPerPage;
    }
    
    // 可视化相关配置的 getter 方法
    
    /**
     * 获取领地边界显示的默认持续时间。
     *
     * @return 默认持续时间（秒）
     */
    public int getDefaultVisualizationDuration() {
        return defaultVisualizationDuration;
    }
    
    /**
     * 获取领地边界显示的最大持续时间。
     *
     * @return 最大持续时间（秒）
     */
    public int getMaxVisualizationDuration() {
        return maxVisualizationDuration;
    }
    
    // 保护相关配置的 getter 方法
    
    /**
     * 获取方块保护的服务器级开关状态。
     *
     * @return 是否启用方块保护
     */
    public boolean isBlockProtectionEnabled() {
        return blockProtectionEnabled;
    }
    
    /**
     * 获取容器保护的服务器级开关状态。
     *
     * @return 是否启用容器保护
     */
    public boolean isContainerProtectionEnabled() {
        return containerProtectionEnabled;
    }
    
    /**
     * 获取爆炸保护的服务器级开关状态。
     *
     * @return 是否启用爆炸保护
     */
    public boolean isExplosionProtectionEnabled() {
        return explosionProtectionEnabled;
    }
    
    /**
     * 获取玩家保护的服务器级开关状态。
     *
     * @return 是否启用玩家保护
     */
    public boolean isPlayerProtectionEnabled() {
        return playerProtectionEnabled;
    }
    
    /**
     * 获取默认的方块保护状态。
     *
     * @return 是否默认启用方块保护
     */
    public boolean isDefaultBlockProtection() {
        return defaultBlockProtection;
    }
    
    /**
     * 获取默认的容器保护状态。
     *
     * @return 是否默认启用容器保护
     */
    public boolean isDefaultContainerProtection() {
        return defaultContainerProtection;
    }
    
    /**
     * 获取默认的爆炸保护状态。
     *
     * @return 是否默认启用爆炸保护
     */
    public boolean isDefaultExplosionProtection() {
        return defaultExplosionProtection;
    }
    
    /**
     * 获取默认的玩家保护状态。
     *
     * @return 是否默认启用玩家保护
     */
    public boolean isDefaultPlayerProtection() {
        return defaultPlayerProtection;
    }
    
    // 子领地相关配置的 getter 方法
    
    /**
     * 获取每个父领地允许的最大子领地数量。
     *
     * @return 最大子领地数量
     */
    public int getMaxSubClaimsPerLand() {
        return maxSubClaimsPerLand;
    }
    
    /**
     * 获取允许的子领地嵌套层级。
     *
     * @return 最大嵌套层级
     */
    public int getMaxSubClaimDepth() {
        return maxSubClaimDepth;
    }
    
    /**
     * 获取原始配置对象，用于访问未预定义的配置项。
     *
     * @return 配置对象
     */
    public FileConfiguration getConfig() {
        return config;
    }
}