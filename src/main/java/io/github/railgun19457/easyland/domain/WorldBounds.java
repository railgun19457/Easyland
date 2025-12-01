package io.github.railgun19457.easyland.domain;

import org.bukkit.Location;

/**
 * 世界坐标边界记录类
 * 使用Java 21 record特性封装领地的坐标范围
 */
public record WorldBounds(int minX, int maxX, int minZ, int maxZ, Integer minY, Integer maxY) {
    
    /**
     * 从两个位置创建边界
     */
    public static WorldBounds fromLocations(Location pos1, Location pos2) {
        if (!pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
            throw new IllegalArgumentException("两个位置必须在同一个世界");
        }
        
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        
        return new WorldBounds(minX, maxX, minZ, maxZ, null, null);
    }
    
    /**
     * 从两个位置创建边界（包含Y坐标）
     */
    public static WorldBounds fromLocationsWithY(Location pos1, Location pos2) {
        if (!pos1.getWorld().getName().equals(pos2.getWorld().getName())) {
            throw new IllegalArgumentException("两个位置必须在同一个世界");
        }
        
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        
        return new WorldBounds(minX, maxX, minZ, maxZ, minY, maxY);
    }
    
    /**
     * 检查位置是否在边界内
     */
    public boolean contains(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        
        boolean inXZ = x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        
        // 如果设置了Y坐标限制，则检查Y坐标
        if (minY != null && maxY != null) {
            return inXZ && y >= minY && y <= maxY;
        }
        
        return inXZ;
    }
    
    /**
     * 检查区块是否与边界有交集
     */
    public boolean intersectsChunk(org.bukkit.Chunk chunk) {
        int chunkMinX = chunk.getX() * 16;
        int chunkMaxX = chunkMinX + 15;
        int chunkMinZ = chunk.getZ() * 16;
        int chunkMaxZ = chunkMinZ + 15;
        
        // 检查区块是否与边界有交集
        return !(chunkMaxX < minX || chunkMinX > maxX || chunkMaxZ < minZ || chunkMinZ > maxZ);
    }
    
    /**
     * 计算面积（XZ平面，方块数量）
     */
    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }
    
    /**
     * 检查是否与另一个边界重叠
     */
    public boolean overlaps(WorldBounds other) {
        return !(maxX < other.minX || minX > other.maxX || maxZ < other.minZ || minZ > other.maxZ);
    }
    
    /**
     * 获取宽度（X轴）
     */
    public int getWidth() {
        return maxX - minX + 1;
    }
    
    /**
     * 获取长度（Z轴）
     */
    public int getLength() {
        return maxZ - minZ + 1;
    }
    
    /**
     * 获取高度（Y轴，如果有限制）
     */
    public Integer getHeight() {
        if (minY != null && maxY != null) {
            return maxY - minY + 1;
        }
        return null;
    }
}