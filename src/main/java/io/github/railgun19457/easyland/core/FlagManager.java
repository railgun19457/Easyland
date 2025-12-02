package io.github.railgun19457.easyland.core;

import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.model.LandFlag;
import io.github.railgun19457.easyland.storage.LandDAO;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * 管理领地标志的核心类。
 * 负责检查特定位置的特定标志是否被启用。
 */
public class FlagManager {
    private final Logger logger;
    private final LandManager landManager;
    private final LandDAO landDAO;
    private final ConfigManager configManager;
    private final LandCache landCache;
    private PermissionManager permissionManager;

    /**
     * 保护类型枚举。
     * 定义不同类型的保护规则。
     */
    public enum ProtectionType {
        BLOCK,      // 方块保护（放置/破坏）
        CONTAINER,  // 容器保护（交互/使用）
        PLAYER,     // 玩家保护（PvP）
        EXPLOSION,  // 爆炸保护
        OTHER       // 其他类型
    }
    
    /**
     * 标志到保护类型的静态映射。
     */
    private static final Map<LandFlag, ProtectionType> FLAG_PROTECTION_TYPES = Map.of(
        LandFlag.BUILD, ProtectionType.BLOCK,
        LandFlag.BREAK, ProtectionType.BLOCK,
        LandFlag.INTERACT, ProtectionType.CONTAINER,
        LandFlag.USE, ProtectionType.CONTAINER,
        LandFlag.PVP, ProtectionType.PLAYER,
        LandFlag.EXPLOSIONS, ProtectionType.EXPLOSION,
        LandFlag.FIRE_SPREAD, ProtectionType.EXPLOSION,
        LandFlag.ENTER, ProtectionType.OTHER,
        LandFlag.MOB_SPAWNING, ProtectionType.OTHER
    );

    /**
     * FlagManager 构造函数（带缓存支持）。
     *
     * @param logger      插件日志记录器
     * @param landManager 领地管理器
     * @param landDAO     领地数据访问对象
     * @param configManager 配置管理器
     * @param landCache   领地缓存管理器
     */
    public FlagManager(Logger logger, LandManager landManager, LandDAO landDAO, ConfigManager configManager, LandCache landCache) {
        this.logger = logger;
        this.landManager = landManager;
        this.landDAO = landDAO;
        this.configManager = configManager;
        this.landCache = landCache;
    }
    
    /**
     * 设置权限管理器。
     *
     * @param permissionManager 权限管理器
     */
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }
    
    /**
     * 获取标志的保护类型。
     *
     * @param flag 要检查的标志
     * @return 保护类型
     */
    public static ProtectionType getProtectionType(LandFlag flag) {
        return FLAG_PROTECTION_TYPES.getOrDefault(flag, ProtectionType.OTHER);
    }

    /**
     * 检查特定位置的特定标志是否被启用。
     *
     * @param location 要检查的位置
     * @param flag     要检查的标志
     * @return 如果标志被启用返回 true，否则返回 false
     */
    public boolean isFlagEnabled(Location location, LandFlag flag) {
        try {
            // 首先检查服务器级保护规则是否启用
            if (!isProtectionRuleEnabled(flag)) {
                // 如果服务器级保护规则被禁用，则允许所有操作
                return true;
            }
            
            // 获取该位置的领地
            Land land;
            if (landCache != null) {
                // 使用缓存获取领地
                land = landCache.getLandAt(location);
            } else {
                // 回退到使用 LandManager
                land = landManager.getLandAt(location);
            }
            
            if (land == null) {
                // 如果没有领地，则默认允许所有操作
                return true;
            }

            // 获取领地的完整信息，包括标志
            Optional<Land> fullLandOpt = landDAO.getLandById(land.getId());
            if (!fullLandOpt.isPresent()) {
                logger.warning("无法获取领地 " + land.getId() + " 的完整信息");
                return true;
            }

            Land fullLand = fullLandOpt.get();
            Set<LandFlag> flags = fullLand.getFlags();

            // 如果没有设置标志，则使用默认值
            if (flags == null || flags.isEmpty()) {
                return getDefaultFlagValue(flag);
            }

            // 检查标志是否被启用
            return flags.contains(flag);

        } catch (SQLException e) {
            logger.severe("检查标志时出错: " + e.getMessage());
            // 出错时默认允许操作，以避免意外阻止玩家
            return true;
        }
    }

    /**
     * 获取标志的默认值。
     * 对于大多数保护标志，默认值是 false（不允许）。
     * 对于一些允许性标志，默认值是 true（允许）。
     *
     * @param flag 要获取默认值的标志
     * @return 标志的默认值
     */
    private boolean getDefaultFlagValue(LandFlag flag) {
        // 使用保护类型映射来确定默认值
        ProtectionType type = getProtectionType(flag);
        
        switch (type) {
            case BLOCK:
                return !configManager.isDefaultBlockProtection();
            case CONTAINER:
                return !configManager.isDefaultContainerProtection();
            case PLAYER:
                return !configManager.isDefaultPlayerProtection();
            case EXPLOSION:
                return !configManager.isDefaultExplosionProtection();
            case OTHER:
                // 这些是允许标志，默认启用（如ENTER, MOB_SPAWNING）
                return true;
            default:
                return false;
        }
    }

    /**
     * 检查玩家是否有权限在特定位置执行特定操作。
     *
     * @param player   要检查的玩家
     * @param location 要检查的位置
     * @param flag     要检查的标志
     * @return 如果玩家有权限返回 true，否则返回 false
     */
    public boolean hasPermission(org.bukkit.entity.Player player, Location location, LandFlag flag) {
        try {
            // 管理员可以绕过所有保护
            if (permissionManager != null && permissionManager.isAdmin(player)) {
                return true;
            }
            
            // 首先检查服务器级保护规则是否启用
            if (!isProtectionRuleEnabled(flag)) {
                // 如果服务器级保护规则被禁用，则允许所有操作
                return true;
            }
            
            // 获取该位置的领地
            Land land;
            if (landCache != null) {
                // 使用缓存获取领地
                land = landCache.getLandAt(location);
            } else {
                // 回退到使用 LandManager
                land = landManager.getLandAt(location);
            }
            
            if (land == null) {
                // 如果没有领地，则允许所有操作
                return true;
            }

            // 检查玩家是否是领地所有者或受信任的玩家
            if (landManager.hasPermission(player, land, flag.getName())) {
                return true;
            }

            // 如果玩家没有权限，则检查标志是否允许该操作
            return isFlagEnabled(location, flag);

        } catch (Exception e) {
            logger.severe("检查玩家权限时出错: " + e.getMessage());
            // 出错时默认允许操作，以避免意外阻止玩家
            return true;
        }
    }
    
    /**
     * 检查特定保护规则是否在服务器级别启用。
     *
     * @param flag 要检查的保护标志
     * @return 如果保护规则启用返回 true，否则返回 false
     */
    private boolean isProtectionRuleEnabled(LandFlag flag) {
        // 使用保护类型映射来确定服务器级别的保护规则
        ProtectionType type = getProtectionType(flag);
        
        switch (type) {
            case BLOCK:
                return configManager.isBlockProtectionEnabled();
            case CONTAINER:
                return configManager.isContainerProtectionEnabled();
            case PLAYER:
                return configManager.isPlayerProtectionEnabled();
            case EXPLOSION:
                return configManager.isExplosionProtectionEnabled();
            case OTHER:
                // 这些标志不受服务器级开关控制，总是启用
                return true;
            default:
                return true;
        }
    }
}