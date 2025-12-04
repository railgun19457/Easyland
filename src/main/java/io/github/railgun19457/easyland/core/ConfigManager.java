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
    
    // 保护相关配置 - 规则启用状态和默认值
    private java.util.Map<String, Boolean> ruleEnabled = new java.util.HashMap<>();
    private java.util.Map<String, Boolean> ruleDefault = new java.util.HashMap<>();
    
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
        ruleEnabled.clear();
        ruleDefault.clear();
        for (io.github.railgun19457.easyland.model.LandFlag flag : io.github.railgun19457.easyland.model.LandFlag.values()) {
            String name = flag.getName();
            ruleEnabled.put(name, config.getBoolean("rule." + name + ".enable", true));
            
            // 默认值处理
            boolean def = false;
            if (name.equals("enter") || name.equals("mob_spawning")) {
                def = true;
            }
            ruleDefault.put(name, config.getBoolean("rule." + name + ".default", def));
        }
        
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
     * 获取规则是否启用。
     *
     * @param ruleName 规则名称
     * @return 是否启用
     */
    public boolean isRuleEnabled(String ruleName) {
        return ruleEnabled.getOrDefault(ruleName, true);
    }

    /**
     * 获取规则的默认值。
     *
     * @param ruleName 规则名称
     * @return 默认值
     */
    public boolean getDefaultRuleValue(String ruleName) {
        return ruleDefault.getOrDefault(ruleName, false);
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