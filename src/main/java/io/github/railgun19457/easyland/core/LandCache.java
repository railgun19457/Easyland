package io.github.railgun19457.easyland.core;

import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.storage.LandDAO;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 领地缓存管理器。
 * 提供基于区块的领地缓存，以减少数据库查询次数，提高性能。
 */
public class LandCache {
    private final Logger logger;
    private final LandDAO landDAO;
    
    // 使用区块坐标作为键的缓存映射
    // 格式: worldName:chunkX:chunkZ -> List<Land>
    private final Map<String, List<Land>> chunkCache;
    
    // 领地ID到领地对象的缓存，用于快速获取完整领地信息
    private final Map<Integer, Land> landCache;
    
    // 缓存大小限制
    private static final int MAX_CHUNK_CACHE_SIZE = 1000;
    private static final int MAX_LAND_CACHE_SIZE = 2000;
    
    /**
     * LandCache 构造函数。
     *
     * @param logger  插件日志记录器
     * @param landDAO 领地数据访问对象
     */
    public LandCache(Logger logger, LandDAO landDAO) {
        this.logger = logger;
        this.landDAO = landDAO;
        this.chunkCache = new ConcurrentHashMap<>();
        this.landCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取指定位置的领地。
     * 首先检查缓存，如果未命中则从数据库加载。
     *
     * @param location 要检查的位置
     * @return 该位置的领地，如果没有领地则返回 null
     */
    public Land getLandAt(Location location) {
        try {
            // 获取区块坐标
            String chunkKey = getChunkKey(location);
            
            // 检查区块缓存
            List<Land> landsInChunk = chunkCache.get(chunkKey);
            if (landsInChunk != null) {
                // 在缓存的领地中查找包含该位置的领地
                for (Land land : landsInChunk) {
                    if (land.contains(location.getBlockX(), location.getBlockZ())) {
                        // 确保返回的是完整的领地信息
                        return getCompleteLand(land);
                    }
                }
                // 如果区块已缓存但没有找到领地，说明该位置确实没有领地
                return null;
            }
            
            // 缓存未命中，从数据库加载该区块的领地
            return loadLandsForChunk(location);
            
        } catch (Exception e) {
            logger.severe("获取领地时出错: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取完整的领地信息，包括标志和信任玩家。
     * 如果缓存中的领地信息不完整，则从数据库加载完整信息。
     *
     * @param land 可能不完整的领地对象
     * @return 完整的领地对象
     */
    private Land getCompleteLand(Land land) {
        // 检查是否已经有完整信息
        if (land.getFlags() != null && land.getTrustedPlayers() != null) {
            return land;
        }
        
        // 从缓存中获取完整信息
        Land cachedLand = landCache.get(land.getId());
        if (cachedLand != null && cachedLand.getFlags() != null && cachedLand.getTrustedPlayers() != null) {
            return cachedLand;
        }
        
        // 从数据库加载完整信息
        try {
            Optional<Land> fullLandOpt = landDAO.getLandById(land.getId());
            if (fullLandOpt.isPresent()) {
                Land fullLand = fullLandOpt.get();
                // 更新缓存
                landCache.put(land.getId(), fullLand);
                
                // 检查缓存大小，必要时清理
                if (landCache.size() > MAX_LAND_CACHE_SIZE) {
                    cleanupLandCache();
                }
                
                return fullLand;
            }
        } catch (SQLException e) {
            logger.warning("无法获取领地 " + land.getId() + " 的完整信息: " + e.getMessage());
        }
        
        // 如果无法获取完整信息，返回基本领地对象
        return land;
    }
    
    /**
     * 从数据库加载指定区块的领地数据。
     *
     * @param location 区块内的位置
     * @return 该位置的领地，如果没有领地则返回 null
     */
    private Land loadLandsForChunk(Location location) {
        try {
            // 计算区块范围
            int chunkX = location.getBlockX() >> 4; // 除以16
            int chunkZ = location.getBlockZ() >> 4;
            int minX = chunkX * 16;
            int minZ = chunkZ * 16;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            
            // 从数据库获取该区块范围内的所有领地
            List<Land> landsInChunk = landDAO.getOverlappingLands(
                location.getWorld().getName(), minX, minZ, maxX, maxZ);
            
            // 排序：面积小的优先（通常是子领地），如果面积相同，有父领地的优先
            landsInChunk.sort((l1, l2) -> {
                int area1 = l1.getArea();
                int area2 = l2.getArea();
                if (area1 != area2) {
                    return Integer.compare(area1, area2);
                }
                // 面积相同，优先子领地
                boolean isSub1 = l1.getParentLandId() != null;
                boolean isSub2 = l2.getParentLandId() != null;
                if (isSub1 && !isSub2) return -1;
                if (!isSub1 && isSub2) return 1;
                return 0;
            });
            
            // 缓存结果
            String chunkKey = getChunkKey(location);
            chunkCache.put(chunkKey, landsInChunk);
            
            // 检查缓存大小，必要时清理
            if (chunkCache.size() > MAX_CHUNK_CACHE_SIZE) {
                cleanupChunkCache();
            }
            
            // 在加载的领地中查找包含该位置的领地
            for (Land land : landsInChunk) {
                if (land.contains(location.getBlockX(), location.getBlockZ())) {
                    return getCompleteLand(land);
                }
            }
            
            // 该位置没有领地
            return null;
            
        } catch (SQLException e) {
            logger.severe("加载区块领地时出错: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 使指定位置的缓存失效。
     * 当领地被创建、删除或修改时调用此方法。
     *
     * @param location 需要失效缓存的位置
     */
    public void invalidateCacheAt(Location location) {
        String chunkKey = getChunkKey(location);
        chunkCache.remove(chunkKey);
    }
    
    /**
     * 使指定领地的缓存失效。
     * 当领地被修改时调用此方法。
     *
     * @param landId 需要失效缓存的领地ID
     */
    public void invalidateLandCache(int landId) {
        landCache.remove(landId);
        
        // 由于不知道领地具体位置，需要清理所有区块缓存
        // 在实际应用中，可以考虑优化这一点
        chunkCache.clear();
    }
    
    /**
     * 使指定区域的缓存失效。
     * 当领地被创建或删除时调用此方法。
     *
     * @param world 世界名称
     * @param x1    区域第一个X坐标
     * @param z1    区域第一个Z坐标
     * @param x2    区域第二个X坐标
     * @param z2    区域第二个Z坐标
     */
    public void invalidateCacheInArea(String world, int x1, int z1, int x2, int z2) {
        // 计算区域覆盖的所有区块
        int minChunkX = Math.min(x1, x2) >> 4;
        int maxChunkX = Math.max(x1, x2) >> 4;
        int minChunkZ = Math.min(z1, z2) >> 4;
        int maxChunkZ = Math.max(z1, z2) >> 4;
        
        // 清理这些区块的缓存
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                String chunkKey = world + ":" + chunkX + ":" + chunkZ;
                chunkCache.remove(chunkKey);
            }
        }
    }
    
    /**
     * 通用缓存清理方法。
     * 移除指定缓存的一半条目。
     *
     * @param cache     要清理的缓存映射
     * @param cacheName 缓存名称（用于日志记录）
     * @param <K>       键类型
     * @param <V>       值类型
     */
    private <K, V> void cleanupCache(Map<K, V> cache, String cacheName) {
        int toRemove = cache.size() / 2;
        cache.keySet().stream().limit(toRemove).forEach(cache::remove);
        logger.info("清理了 " + toRemove + " 个" + cacheName + "条目");
    }
    
    /**
     * 清理区块缓存，移除最旧的条目。
     */
    private void cleanupChunkCache() {
        cleanupCache(chunkCache, "区块缓存");
    }
    
    /**
     * 清理领地缓存，移除最旧的条目。
     */
    private void cleanupLandCache() {
        cleanupCache(landCache, "领地缓存");
    }
    
    /**
     * 清空所有缓存。
     */
    public void clearAllCache() {
        chunkCache.clear();
        landCache.clear();
        logger.info("已清空所有领地缓存");
    }
    
    /**
     * 获取缓存统计信息。
     *
     * @return 包含缓存统计信息的字符串
     */
    public String getCacheStats() {
        return "区块缓存: " + chunkCache.size() + "/" + MAX_CHUNK_CACHE_SIZE + 
               ", 领地缓存: " + landCache.size() + "/" + MAX_LAND_CACHE_SIZE;
    }
    
    /**
     * 根据位置生成区块键。
     *
     * @param location 位置
     * @return 区块键
     */
    private String getChunkKey(Location location) {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        return location.getWorld().getName() + ":" + chunkX + ":" + chunkZ;
    }
    
    /**
     * 生成区块键。
     *
     * @param world 世界名称
     * @param chunkX 区块X坐标
     * @param chunkZ 区块Z坐标
     * @return 区块键
     */
    @SuppressWarnings("unused")
    private String getChunkKey(String world, int chunkX, int chunkZ) {
        return world + ":" + chunkX + ":" + chunkZ;
    }
}