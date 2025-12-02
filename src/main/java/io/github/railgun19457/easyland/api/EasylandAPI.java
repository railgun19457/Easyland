package io.github.railgun19457.easyland.api;

import io.github.railgun19457.easyland.model.Land;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Easyland 插件的公共 API 接口。
 * 提供给其他插件安全访问 Easyland 核心功能的方法。
 */
public interface EasylandAPI {
    
    /**
     * 获取指定位置的领地。
     *
     * @param location 要检查的位置
     * @return 如果位置有领地则返回领地对象，否则返回 Optional.empty()
     */
    Optional<Land> getLandAt(Location location);
    
    /**
     * 检查指定位置是否受保护。
     *
     * @param location 要检查的位置
     * @return 如果位置受保护则返回 true，否则返回 false
     */
    boolean isProtected(Location location);
    
    /**
     * 获取指定位置的领地所有者 UUID。
     *
     * @param location 要检查的位置
     * @return 如果位置有领地则返回所有者 UUID，否则返回 Optional.empty()
     */
    Optional<UUID> getOwner(Location location);
    
    /**
     * 根据领地 ID 获取领地。
     *
     * @param landId 领地 ID
     * @return 如果找到领地则返回领地对象，否则返回 Optional.empty()
     */
    Optional<Land> getLandById(int landId);
    
    /**
     * 获取玩家拥有的所有领地。
     *
     * @param playerUuid 玩家的 UUID
     * @return 玩家拥有的领地列表
     */
    List<Land> getPlayerLands(UUID playerUuid);
    
    /**
     * 获取玩家拥有的领地（分页）。
     *
     * @param playerUuid 玩家的 UUID
     * @param page 页码（从1开始）
     * @param pageSize 每页大小
     * @return 玩家拥有的领地列表
     */
    List<Land> getPlayerLands(UUID playerUuid, int page, int pageSize);
    
    /**
     * 检查玩家在指定领地是否有权限执行特定操作。
     *
     * @param playerUuid 玩家的 UUID
     * @param land       要检查的领地
     * @param action     要执行的操作
     * @return 如果玩家有权限则返回 true，否则返回 false
     */
    boolean hasPermission(UUID playerUuid, Land land, String action);
    
    /**
     * 检查玩家是否被信任在指定领地。
     *
     * @param playerUuid 玩家的 UUID
     * @param landId     领地 ID
     * @return 如果玩家被信任则返回 true，否则返回 false
     */
    boolean isTrusted(UUID playerUuid, int landId);
    
    /**
     * 获取指定领地的所有被信任的玩家。
     *
     * @param landId 领地 ID
     * @return 被信任的玩家 UUID 列表
     */
    List<UUID> getTrustedPlayers(int landId);
    
    /**
     * 获取指定世界的所有领地。
     *
     * @param worldName 世界名称
     * @return 该世界的所有领地列表
     */
    List<Land> getLandsInWorld(String worldName);
    
    /**
     * 检查指定位置是否在领地内。
     *
     * @param location 要检查的位置
     * @return 如果位置在领地内则返回 true，否则返回 false
     */
    boolean isInLand(Location location);
}