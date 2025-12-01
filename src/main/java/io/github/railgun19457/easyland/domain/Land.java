package io.github.railgun19457.easyland.domain;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;

/**
 * 领地实体类 - 重构为Java 21 record
 * 使用世界坐标系统，支持精确的领地范围控制和子领地功能
 */
public record Land(
    Long id,
    String landId,
    String owner,
    String worldName,
    WorldBounds bounds,
    Set<String> trusted,
    Map<String, Boolean> protectionRules
) {
    
    /**
     * 从位置创建领地（用于新建领地，使用世界坐标）
     */
    public Land(String landId, String owner, Location pos1, Location pos2) {
        this(null, landId, owner, pos1.getWorld().getName(), 
             WorldBounds.fromLocations(pos1, pos2), 
             new HashSet<>(), createDefaultProtectionRules());
    }
    
    /**
     * 从位置创建领地（包含Y坐标限制）
     */
    public Land(String landId, String owner, Location pos1, Location pos2, boolean includeY) {
        this(null, landId, owner, pos1.getWorld().getName(), 
             includeY ? WorldBounds.fromLocationsWithY(pos1, pos2) : WorldBounds.fromLocations(pos1, pos2), 
             new HashSet<>(), createDefaultProtectionRules());
    }
    
    /**
     * 从区块创建领地（兼容旧版本，用于数据迁移）
     * @deprecated 使用 Location 构造函数代替
     */
    @Deprecated
    public Land(String landId, String owner, Chunk pos1, Chunk pos2) {
        this(null, landId, owner, pos1.getWorld().getName(), 
             createBoundsFromChunks(pos1, pos2), 
             new HashSet<>(), createDefaultProtectionRules());
    }
    
    /**
     * 从数据库加载领地（完整构造函数）
     */
    public Land(Long id, String landId, String owner, String worldName,
                int minX, int maxX, int minZ, int maxZ,
                Integer minY, Integer maxY,
                Set<String> trusted, Map<String, Boolean> protectionRules) {
        this(id, landId, owner, worldName, 
             new WorldBounds(minX, maxX, minZ, maxZ, minY, maxY),
             trusted != null ? new HashSet<>(trusted) : new HashSet<>(),
             protectionRules != null ? new HashMap<>(protectionRules) : createDefaultProtectionRules());
    }
    
    /**
     * 从数据库加载领地（兼容旧版本，不包含Y坐标）
     * @deprecated 使用包含Y坐标的构造函数代替
     */
    @Deprecated
    public Land(Long id, String landId, String owner, String worldName,
                int minX, int maxX, int minZ, int maxZ,
                Set<String> trusted, Map<String, Boolean> protectionRules) {
        this(id, landId, owner, worldName, minX, maxX, minZ, maxZ, null, null, trusted, protectionRules);
    }
    
    /**
     * 创建默认保护规则
     */
    private static Map<String, Boolean> createDefaultProtectionRules() {
        Map<String, Boolean> rules = new HashMap<>();
        rules.put("block-protection", false);
        rules.put("explosion-protection", false);
        rules.put("container-protection", false);
        rules.put("player-protection", false);
        return rules;
    }
    
    /**
     * 从区块创建边界（兼容旧版本）
     */
    private static WorldBounds createBoundsFromChunks(Chunk pos1, Chunk pos2) {
        // 区块坐标转换为世界坐标（区块的起始坐标）
        int minX = Math.min(pos1.getX(), pos2.getX()) * 16;
        int maxX = (Math.max(pos1.getX(), pos2.getX()) + 1) * 16 - 1;
        int minZ = Math.min(pos1.getZ(), pos2.getZ()) * 16;
        int maxZ = (Math.max(pos1.getZ(), pos2.getZ()) + 1) * 16 - 1;
        return new WorldBounds(minX, maxX, minZ, maxZ, null, null);
    }
    
    /**
     * 检查位置是否在领地内（使用世界坐标）
     */
    public boolean contains(Location loc) {
        return loc.getWorld().getName().equals(worldName) && bounds.contains(loc);
    }
    
    /**
     * 检查区块是否与领地有交集
     */
    public boolean intersectsChunk(Chunk chunk) {
        return chunk.getWorld().getName().equals(worldName) && bounds.intersectsChunk(chunk);
    }
    
    /**
     * 检查区块是否完全在领地内
     * @deprecated 使用 intersectsChunk 代替，因为现在使用坐标而非区块
     */
    @Deprecated
    public boolean contains(Chunk chunk) {
        return intersectsChunk(chunk);
    }
    
    /**
     * 添加信任玩家
     */
    public Land trust(String uuid) {
        Set<String> newTrusted = new HashSet<>(trusted);
        newTrusted.add(uuid);
        return new Land(id, landId, owner, worldName, bounds, newTrusted, protectionRules);
    }
    
    /**
     * 移除信任玩家
     */
    public Land untrust(String uuid) {
        Set<String> newTrusted = new HashSet<>(trusted);
        newTrusted.remove(uuid);
        return new Land(id, landId, owner, worldName, bounds, newTrusted, protectionRules);
    }
    
    /**
     * 检查玩家是否被信任
     */
    public boolean isTrusted(String uuid) {
        return uuid.equals(owner) || trusted.contains(uuid);
    }
    
    /**
     * 设置保护规则的默认值
     */
    public Land withDefaultProtectionRules(Map<String, Boolean> defaultRules) {
        Map<String, Boolean> newRules = new HashMap<>(protectionRules);
        for (Map.Entry<String, Boolean> entry : defaultRules.entrySet()) {
            newRules.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return new Land(id, landId, owner, worldName, bounds, trusted, newRules);
    }
    
    /**
     * 获取保护规则状态
     */
    public boolean getProtectionRule(String ruleName) {
        return protectionRules.getOrDefault(ruleName, false);
    }
    
    /**
     * 设置保护规则状态
     */
    public Land withProtectionRule(String ruleName, boolean enabled) {
        Map<String, Boolean> newRules = new HashMap<>(protectionRules);
        newRules.put(ruleName, enabled);
        return new Land(id, landId, owner, worldName, bounds, trusted, newRules);
    }
    
    /**
     * 设置所有者
     */
    public Land withOwner(String newOwner) {
        return new Land(id, landId, newOwner, worldName, bounds, trusted, protectionRules);
    }
    
    /**
     * 设置ID
     */
    public Land withId(Long newId) {
        return new Land(newId, landId, owner, worldName, bounds, trusted, protectionRules);
    }
    
    /**
     * 设置领地ID
     */
    public Land withLandId(String newLandId) {
        return new Land(id, newLandId, owner, worldName, bounds, trusted, protectionRules);
    }
    
    /**
     * 计算领地的面积（XZ平面，方块数量）
     */
    public int getArea() {
        return bounds.getArea();
    }
    
    /**
     * 计算领地包含的区块数量（估算）
     * @deprecated 使用 getArea() 代替
     */
    @Deprecated
    public int getChunkCount() {
        // 估算：向上取整
        int chunkWidth = (bounds.maxX() - bounds.minX()) / 16 + 1;
        int chunkLength = (bounds.maxZ() - bounds.minZ()) / 16 + 1;
        return chunkWidth * chunkLength;
    }
    
    /**
     * 检查是否已被认领
     */
    public boolean isClaimed() {
        return owner != null;
    }
    
    // 便捷访问方法，委托给bounds
    public int getMinX() { return bounds.minX(); }
    public int getMaxX() { return bounds.maxX(); }
    public int getMinZ() { return bounds.minZ(); }
    public int getMaxZ() { return bounds.maxZ(); }
    public Integer getMinY() { return bounds.minY(); }
    public Integer getMaxY() { return bounds.maxY(); }
    
    @Override
    public String toString() {
        return "Land{" +
                "id=" + id +
                ", landId='" + landId + '\'' +
                ", owner='" + owner + '\'' +
                ", worldName='" + worldName + '\'' +
                ", bounds=(" + bounds.minX() + "," + bounds.minZ() + ")~(" + bounds.maxX() + "," + bounds.maxZ() + ")" +
                ", area=" + getArea() + " blocks" +
                '}';
    }
}
