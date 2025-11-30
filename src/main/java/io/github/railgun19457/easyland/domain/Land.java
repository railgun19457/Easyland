package io.github.railgun19457.easyland.domain;

import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.*;

/**
 * 领地实体类
 * 使用世界坐标系统，支持精确的领地范围控制和子领地功能
 */
public class Land {
    private Long id;
    private String landId;
    private String owner;
    private final String worldName;
    // 使用世界坐标而非区块坐标，支持更精确的领地范围
    private final int minX, maxX, minZ, maxZ;
    private final Integer minY, maxY; // Y坐标，可选（null表示不限制高度）
    private final Set<String> trusted = new HashSet<>();
    private final Map<String, Boolean> protectionRules = new HashMap<>();

    /**
     * 从位置创建领地（用于新建领地，使用世界坐标）
     */
    public Land(String landId, String owner, Location pos1, Location pos2) {
        this.landId = landId;
        this.owner = owner;
        this.worldName = pos1.getWorld().getName();
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        this.minY = null; // 不限制高度
        this.maxY = null;
        initializeDefaultRules();
    }

    /**
     * 从区块创建领地（兼容旧版本，用于数据迁移）
     * @deprecated 使用 Location 构造函数代替
     */
    @Deprecated
    public Land(String landId, String owner, Chunk pos1, Chunk pos2) {
        this.landId = landId;
        this.owner = owner;
        this.worldName = pos1.getWorld().getName();
        // 区块坐标转换为世界坐标（区块的起始坐标）
        this.minX = Math.min(pos1.getX(), pos2.getX()) * 16;
        this.maxX = (Math.max(pos1.getX(), pos2.getX()) + 1) * 16 - 1;
        this.minZ = Math.min(pos1.getZ(), pos2.getZ()) * 16;
        this.maxZ = (Math.max(pos1.getZ(), pos2.getZ()) + 1) * 16 - 1;
        this.minY = null; // 不限制高度
        this.maxY = null;
        initializeDefaultRules();
    }

    /**
     * 从数据库加载领地（包含所有字段，包括Y坐标）
     */
    public Land(Long id, String landId, String owner, String worldName,
                int minX, int maxX, int minZ, int maxZ,
                Integer minY, Integer maxY,
                Set<String> trusted, Map<String, Boolean> protectionRules) {
        this.id = id;
        this.landId = landId;
        this.owner = owner;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
        if (trusted != null) {
            this.trusted.addAll(trusted);
        }
        if (protectionRules != null) {
            this.protectionRules.putAll(protectionRules);
        } else {
            initializeDefaultRules();
        }
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
     * 初始化默认保护规则
     */
    private void initializeDefaultRules() {
        protectionRules.put("block-protection", false);
        protectionRules.put("explosion-protection", false);
        protectionRules.put("container-protection", false);
        protectionRules.put("player-protection", false);
    }

    /**
     * 检查位置是否在领地内（使用世界坐标）
     */
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) {
            return false;
        }
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        boolean inXZ = x >= minX && x <= maxX && z >= minZ && z <= maxZ;

        // 如果设置了Y坐标限制，则检查Y坐标
        if (minY != null && maxY != null) {
            return inXZ && y >= minY && y <= maxY;
        }

        return inXZ;
    }

    /**
     * 检查区块是否与领地有交集
     */
    public boolean intersectsChunk(Chunk chunk) {
        if (!chunk.getWorld().getName().equals(worldName)) {
            return false;
        }
        int chunkMinX = chunk.getX() * 16;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getZ() * 16;
        int chunkMaxZ = chunkMinZ + 15;

        // 检查区块是否与领地有交集
        return !(chunkMaxX < minX || chunkMinX > maxX || chunkMaxZ < minZ || chunkMinZ > maxZ);
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
    public boolean trust(String uuid) {
        return trusted.add(uuid);
    }

    /**
     * 移除信任玩家
     */
    public boolean untrust(String uuid) {
        return trusted.remove(uuid);
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
    public void setDefaultProtectionRules(Map<String, Boolean> defaultRules) {
        for (Map.Entry<String, Boolean> entry : defaultRules.entrySet()) {
            protectionRules.putIfAbsent(entry.getKey(), entry.getValue());
        }
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
    public void setProtectionRule(String ruleName, boolean enabled) {
        protectionRules.put(ruleName, enabled);
    }

    /**
     * 计算领地的面积（XZ平面，方块数量）
     */
    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    /**
     * 计算领地包含的区块数量（估算）
     * @deprecated 使用 getArea() 或 getVolume() 代替
     */
    @Deprecated
    public int getChunkCount() {
        // 估算：向上取整
        int chunkWidth = (maxX - minX) / 16 + 1;
        int chunkLength = (maxZ - minZ) / 16 + 1;
        return chunkWidth * chunkLength;
    }

    /**
     * 检查是否已被认领
     */
    public boolean isClaimed() {
        return owner != null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLandId() {
        return landId;
    }

    public void setLandId(String landId) {
        this.landId = landId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public Integer getMinY() {
        return minY;
    }

    public Integer getMaxY() {
        return maxY;
    }

    public Set<String> getTrusted() {
        return new HashSet<>(trusted);
    }

    public Map<String, Boolean> getProtectionRules() {
        return new HashMap<>(protectionRules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Land land = (Land) o;
        return Objects.equals(id, land.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Land{" +
                "id=" + id +
                ", landId='" + landId + '\'' +
                ", owner='" + owner + '\'' +
                ", worldName='" + worldName + '\'' +
                ", pos=(" + minX + "," + minZ + ")~(" + maxX + "," + maxZ + ")" +
                ", area=" + getArea() + " blocks" +
                '}';
    }
}
