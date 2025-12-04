package io.github.railgun19457.easyland.core;

import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.model.LandFlag;
import io.github.railgun19457.easyland.storage.LandDAO;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.Optional;
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
     * 获取标志的默认值。
     *
     * @param flag 要获取默认值的标志
     * @return 标志的默认值
     */
    private boolean getDefaultFlagValue(LandFlag flag) {
        return configManager.getDefaultRuleValue(flag.getName());
    }

    /**
     * 检查特定保护规则是否在服务器级别启用。
     *
     * @param flag 要检查的保护标志
     * @return 如果保护规则启用返回 true，否则返回 false
     */
    private boolean isProtectionRuleEnabled(LandFlag flag) {
        return configManager.isRuleEnabled(flag.getName());
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
            java.util.Map<LandFlag, Boolean> flags = fullLand.getFlagMap();

            // 如果没有设置标志，则使用默认值
            if (flags == null || flags.isEmpty()) {
                return getDefaultFlagValue(flag);
            }

            // 检查标志是否被启用
            return Boolean.TRUE.equals(flags.get(flag));

        } catch (SQLException e) {
            logger.severe("检查标志时出错: " + e.getMessage());
            // 出错时默认允许操作，以避免意外阻止玩家
            return true;
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
    

}