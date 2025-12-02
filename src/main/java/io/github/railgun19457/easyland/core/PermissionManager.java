package io.github.railgun19457.easyland.core;

import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.model.LandFlag;
import io.github.railgun19457.easyland.storage.LandTrustDAO;
import io.github.railgun19457.easyland.storage.PlayerDAO;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 权限管理器 - 统一管理所有权限检查逻辑
 * 提供管理员权限提升功能
 */
public class PermissionManager {
    private final Logger logger;
    private final PlayerDAO playerDAO;
    private final LandTrustDAO landTrustDAO;

    public PermissionManager(Logger logger, PlayerDAO playerDAO, LandTrustDAO landTrustDAO) {
        this.logger = logger;
        this.playerDAO = playerDAO;
        this.landTrustDAO = landTrustDAO;
    }

    /**
     * 检查玩家是否有权限管理领地（所有者或管理员）
     *
     * @param player 玩家
     * @param land   领地
     * @return true如果玩家有权限，否则false
     */
    public boolean canManageLand(Player player, Land land) {
        // 管理员可以管理所有领地
        if (isAdmin(player)) {
            return true;
        }

        // 检查是否是领地所有者
        return isLandOwner(player, land);
    }

    /**
     * 检查玩家是否是领地的所有者
     *
     * @param player 玩家
     * @param land   领地
     * @return true如果是所有者，否则false
     */
    public boolean isLandOwner(Player player, Land land) {
        try {
            Optional<io.github.railgun19457.easyland.model.Player> dbPlayerOpt = 
                playerDAO.getPlayerByUuid(player.getUniqueId());
            
            if (!dbPlayerOpt.isPresent()) {
                return false;
            }

            return land.getOwnerId() == dbPlayerOpt.get().getId();
        } catch (SQLException e) {
            logger.severe("检查领地所有权时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查玩家是否有权限在领地上执行操作（所有者、受信任玩家或管理员）
     *
     * @param player 玩家
     * @param land   领地
     * @param action 操作类型
     * @return true如果有权限，否则false
     */
    public boolean hasLandPermission(Player player, Land land, String action) {
        // 管理员可以在所有领地上执行任何操作
        if (isAdmin(player)) {
            return true;
        }

        try {
            Optional<io.github.railgun19457.easyland.model.Player> dbPlayerOpt = 
                playerDAO.getPlayerByUuid(player.getUniqueId());
            
            if (!dbPlayerOpt.isPresent()) {
                return false;
            }

            io.github.railgun19457.easyland.model.Player dbPlayer = dbPlayerOpt.get();
            
            // 检查是否是领地所有者
            if (land.getOwnerId() == dbPlayer.getId()) {
                return true;
            }
            
            // 检查是否是受信任的玩家
            return landTrustDAO.isPlayerTrusted(land.getId(), dbPlayer.getId());
            
        } catch (SQLException e) {
            logger.severe("检查领地权限时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查玩家是否有权限与领地保护规则交互
     *
     * @param player   玩家
     * @param land     领地
     * @param flag     标志类型
     * @return true如果有权限，否则false
     */
    public boolean hasProtectionPermission(Player player, Land land, LandFlag flag) {
        // 管理员可以绕过所有保护
        if (isAdmin(player)) {
            return true;
        }

        // 使用通用的领地权限检查
        return hasLandPermission(player, land, flag.getName());
    }

    /**
     * 检查玩家是否是管理员（OP）
     *
     * @param player 玩家
     * @return true如果是管理员，否则false
     */
    public boolean isAdmin(Player player) {
        return player.isOp() || player.hasPermission("easyland.admin");
    }

    /**
     * 检查玩家是否有指定的权限节点
     * 如果玩家是OP，会自动拥有所有easyland权限
     *
     * @param player     玩家
     * @param permission 权限节点
     * @return true如果有权限，否则false
     */
    public boolean hasPermission(Player player, String permission) {
        // OP自动拥有所有easyland权限
        if (player.isOp() && permission.startsWith("easyland.")) {
            return true;
        }
        
        return player.hasPermission(permission);
    }

    /**
     * 检查玩家是否可以删除领地
     * 管理员可以删除任何领地，普通玩家只能删除自己的领地
     *
     * @param player 玩家
     * @param land   领地
     * @return true如果可以删除，否则false
     */
    public boolean canDeleteLand(Player player, Land land) {
        // 管理员可以删除任何领地
        if (isAdmin(player)) {
            return true;
        }

        // 普通玩家只能删除自己的领地
        return isLandOwner(player, land);
    }

    /**
     * 检查玩家是否可以修改领地信任列表
     *
     * @param player 玩家
     * @param land   领地
     * @return true如果可以修改，否则false
     */
    public boolean canModifyTrust(Player player, Land land) {
        // 管理员可以修改任何领地的信任列表
        if (isAdmin(player)) {
            return true;
        }

        // 只有领地所有者可以修改信任列表
        return isLandOwner(player, land);
    }

    /**
     * 检查玩家是否可以查看领地信息
     * 管理员可以查看所有领地，普通玩家可以查看自己的领地
     *
     * @param player 玩家
     * @param land   领地（可选）
     * @return true如果可以查看，否则false
     */
    public boolean canViewLandInfo(Player player, Land land) {
        // 管理员可以查看所有领地
        if (isAdmin(player)) {
            return true;
        }

        // 如果没有指定领地，普通玩家可以查看列表
        if (land == null) {
            return true;
        }

        // 检查是否是所有者或受信任玩家
        return hasLandPermission(player, land, "info");
    }

    /**
     * 检查玩家是否可以修改领地保护规则
     *
     * @param player 玩家
     * @param land   领地
     * @return true如果可以修改，否则false
     */
    public boolean canModifyProtection(Player player, Land land) {
        // 管理员可以修改任何领地的保护规则
        if (isAdmin(player)) {
            return true;
        }

        // 只有领地所有者可以修改保护规则
        return isLandOwner(player, land);
    }

    /**
     * 检查玩家是否可以重命名领地
     *
     * @param player 玩家
     * @param land   领地
     * @return true如果可以重命名，否则false
     */
    public boolean canRenameLand(Player player, Land land) {
        // 管理员可以重命名任何领地
        if (isAdmin(player)) {
            return true;
        }

        // 只有领地所有者可以重命名
        return isLandOwner(player, land);
    }

    /**
     * 检查玩家是否可以创建子领地
     *
     * @param player     玩家
     * @param parentLand 父领地
     * @return true如果可以创建，否则false
     */
    public boolean canCreateSubClaim(Player player, Land parentLand) {
        // 管理员可以在任何领地创建子领地
        if (isAdmin(player)) {
            return true;
        }

        // 检查是否有权限（所有者或受信任玩家）
        return hasLandPermission(player, parentLand, "subclaim");
    }
}
