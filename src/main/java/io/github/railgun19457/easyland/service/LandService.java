package io.github.railgun19457.easyland.service;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.util.CacheManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * 领地服务层
 * 处理所有领地相关的业务逻辑
 */
public class LandService {
    private static final Logger logger = Logger.getLogger(LandService.class.getName());

    private final LandRepository repository;
    private int maxLandsPerPlayer;
    private int maxChunksPerLand;
    private Map<String, Boolean> defaultProtectionRules;

    // 空间索引缓存，提高查询性能 - 使用CacheManager管理
    private final CacheManager<String, Set<Land>> worldLandCache;
    
    // 读写锁用于保护缓存操作
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // 用于保护数据库操作的锁
    private final Object dbLock = new Object();

    public LandService(LandRepository repository, int maxLandsPerPlayer, int maxChunksPerLand,
                       Map<String, Boolean> defaultProtectionRules) {
        this.repository = repository;
        this.maxLandsPerPlayer = maxLandsPerPlayer;
        this.maxChunksPerLand = maxChunksPerLand;
        this.defaultProtectionRules = defaultProtectionRules != null ? defaultProtectionRules : new HashMap<>();
        
        // 初始化缓存管理器 - 最大1000个世界，永不过期，启用定时清理
        this.worldLandCache = new CacheManager<>("WorldLandCache", 1000, 0);
        
        rebuildCache();
        logger.info("LandService initialized with maxLandsPerPlayer=" + maxLandsPerPlayer +
                    ", maxChunksPerLand=" + maxChunksPerLand);
    }

    /**
     * 重建缓存
     */
    public void rebuildCache() {
        cacheLock.writeLock().lock();
        try {
            logger.info("Starting cache rebuild - current cache size: " + worldLandCache.size());
            worldLandCache.clear();
            
            List<Land> allLands;
            synchronized (dbLock) {
                allLands = repository.findAll();
            }
            
            logger.info("Found " + allLands.size() + " lands to cache");
            for (Land land : allLands) {
                addToCacheInternal(land);
            }
            logger.info("Cache rebuild completed - new cache size: " + worldLandCache.size());
            worldLandCache.logStats(); // 输出缓存统计
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * 添加到缓存（线程安全版本）
     */
    private void addToCache(Land land) {
        cacheLock.writeLock().lock();
        try {
            addToCacheInternal(land);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * 内部添加到缓存方法（调用者必须持有写锁）
     */
    private void addToCacheInternal(Land land) {
        String worldName = land.worldName();
        Set<Land> worldLands = worldLandCache.get(worldName);
        
        if (worldLands == null) {
            worldLands = ConcurrentHashMap.newKeySet();
            worldLandCache.put(worldName, worldLands);
        }
        
        worldLands.add(land);
        logger.fine("Added land " + land.landId() + " to cache for world " + worldName +
                   " (world now has " + worldLands.size() + " lands)");
    }

    /**
     * 从缓存移除（线程安全版本）
     */
    private void removeFromCache(Land land) {
        cacheLock.writeLock().lock();
        try {
            removeFromCacheInternal(land);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * 内部从缓存移除方法（调用者必须持有写锁）
     */
    private void removeFromCacheInternal(Land land) {
        String worldName = land.worldName();
        Set<Land> worldLands = worldLandCache.get(worldName);
        if (worldLands != null) {
            boolean removed = worldLands.remove(land);
            if (removed) {
                logger.fine("Removed land " + land.landId() + " from cache for world " + worldName);
            }
            if (worldLands.isEmpty()) {
                worldLandCache.remove(worldName);
                logger.fine("Removed empty cache entry for world " + worldName);
            }
        } else {
            logger.warning("Attempted to remove land " + land.landId() + " from cache but world " + worldName + " not found");
        }
    }

    /**
     * 创建未认领的领地（使用世界坐标）
     */
    public ServiceResult<Land> createLand(Location pos1, Location pos2, String landId) {
        logger.info("Creating land: " + landId + " in world " + pos1.getWorld().getName());

        // 验证领地面积
        int area = calculateArea(pos1, pos2);
        int maxArea = maxChunksPerLand * 256; // 转换为方块数
        if (area > maxArea) {
            logger.warning("Failed to create land " + landId + ": area " + area + " exceeds limit " + maxArea);
            return ServiceResult.failure("领地面积超过限制: " + area + " > " + maxArea + " 方块");
        }

        // 检查是否重叠（使用读锁）
        cacheLock.readLock().lock();
        try {
            if (hasOverlapInternal(pos1, pos2)) {
                logger.warning("Failed to create land " + landId + ": overlaps with existing land");
                return ServiceResult.failure("领地与现有领地重叠");
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // 检查领地ID是否已存在
        boolean landExists;
        synchronized (dbLock) {
            landExists = landId != null && !landId.isEmpty() && repository.existsByLandId(landId);
        }
        
        if (landExists) {
            logger.warning("Failed to create land " + landId + ": land ID already exists");
            return ServiceResult.failure("领地ID已存在: " + landId);
        }

        // 创建领地
        Land land = new Land(landId, null, pos1, pos2);
        Land landWithRules = land.withDefaultProtectionRules(defaultProtectionRules);
        
        Land savedLand;
        synchronized (dbLock) {
            savedLand = repository.save(landWithRules);
        }
        
        addToCache(savedLand);

        logger.info("Successfully created land: " + landId + " with area " + area + " blocks");
        return ServiceResult.success(savedLand);
    }

    /**
     * 创建未认领的领地（兼容旧版本，使用区块）
     * @deprecated 使用 Location 版本代替
     */
    @Deprecated
    public ServiceResult<Land> createLand(Chunk pos1, Chunk pos2, String landId) {
        // 将区块转换为世界坐标（区块的中心点）
        Location loc1 = new Location(pos1.getWorld(), pos1.getX() * 16 + 8, 64, pos1.getZ() * 16 + 8);
        Location loc2 = new Location(pos2.getWorld(), pos2.getX() * 16 + 8, 64, pos2.getZ() * 16 + 8);
        return createLand(loc1, loc2, landId);
    }

    /**
     * 认领领地
     */
    public ServiceResult<Land> claimLand(Player player, String landId) {
        String playerUuid = player.getUniqueId().toString();
        logger.info("Player " + player.getName() + " attempting to claim land: " + landId);

        // 检查玩家已拥有的领地数量
        int ownedCount;
        synchronized (dbLock) {
            ownedCount = repository.countByOwner(playerUuid);
        }
        
        if (ownedCount >= maxLandsPerPlayer) {
            logger.warning("Player " + player.getName() + " cannot claim land: reached limit " + maxLandsPerPlayer);
            return ServiceResult.failure("已达到最大领地数量限制: " + maxLandsPerPlayer);
        }

        // 查找未认领的领地
        Optional<Land> landOpt;
        synchronized (dbLock) {
            landOpt = repository.findByLandId(landId);
        }
        
        if (landOpt.isEmpty()) {
            logger.warning("Player " + player.getName() + " cannot claim land: land " + landId + " not found");
            return ServiceResult.failure("领地不存在: " + landId);
        }

        Land land = landOpt.get();
        if (land.isClaimed()) {
            logger.warning("Player " + player.getName() + " cannot claim land: land " + landId + " already claimed");
            return ServiceResult.failure("领地已被认领");
        }

        // 认领领地
        Land claimedLand = land.withOwner(playerUuid);
        Land savedLand;
        synchronized (dbLock) {
            savedLand = repository.save(claimedLand);
        }

        // 更新缓存
        removeFromCache(land);
        addToCache(savedLand);

        logger.info("Player " + player.getName() + " successfully claimed land: " + landId);
        return ServiceResult.success(savedLand);
    }

    /**
     * 取消认领领地
     */
    public ServiceResult<Land> unclaimLand(Player player, String landId) {
        String playerUuid = player.getUniqueId().toString();

        Optional<Land> landOpt;
        synchronized (dbLock) {
            landOpt = repository.findByLandId(landId);
        }
        
        if (landOpt.isEmpty()) {
            return ServiceResult.failure("领地不存在: " + landId);
        }

        Land land = landOpt.get();
        if (!playerUuid.equals(land.owner())) {
            return ServiceResult.failure("你不是该领地的所有者");
        }

        // 取消认领
        Land unclaimedLand = land.withOwner(null);
        Land savedLand;
        synchronized (dbLock) {
            savedLand = repository.save(unclaimedLand);
        }

        // 更新缓存
        removeFromCache(land);
        addToCache(savedLand);

        return ServiceResult.success(savedLand);
    }

    /**
     * 删除领地
     */
    public ServiceResult<Void> removeLand(Player player, String landId) {
        String playerUuid = player.getUniqueId().toString();
        logger.info("Player " + player.getName() + " attempting to remove land: " + landId);

        Optional<Land> landOpt;
        synchronized (dbLock) {
            landOpt = repository.findByLandId(landId);
        }
        
        if (landOpt.isEmpty()) {
            logger.warning("Player " + player.getName() + " cannot remove land: land " + landId + " not found");
            return ServiceResult.failure("领地不存在: " + landId);
        }

        Land land = landOpt.get();
        if (!playerUuid.equals(land.owner())) {
            logger.warning("Player " + player.getName() + " cannot remove land: not the owner of " + landId);
            return ServiceResult.failure("你不是该领地的所有者");
        }

        removeFromCache(land);
        boolean deleted;
        synchronized (dbLock) {
            deleted = repository.deleteById(land.id());
        }
        
        if (deleted) {
            logger.info("Player " + player.getName() + " successfully removed land: " + landId);
            return ServiceResult.success(null);
        } else {
            logger.severe("Failed to delete land " + landId + " from database");
            return ServiceResult.failure("删除领地失败");
        }
    }

    /**
     * 信任玩家
     */
    public ServiceResult<Void> trustPlayer(Player owner, String trustedUuid) {
        String ownerUuid = owner.getUniqueId().toString();
        List<Land> lands;
        synchronized (dbLock) {
            lands = repository.findByOwner(ownerUuid);
        }

        if (lands.isEmpty()) {
            return ServiceResult.failure("你没有领地");
        }

        // 信任到所有领地
        List<Land> updatedLands = new ArrayList<>();
        for (Land land : lands) {
            updatedLands.add(land.trust(trustedUuid));
        }

        // 使用批量保存提高性能
        List<Land> savedLands;
        synchronized (dbLock) {
            savedLands = repository.saveAll(updatedLands);
        }

        // 更新缓存
        cacheLock.writeLock().lock();
        try {
            for (int i = 0; i < updatedLands.size(); i++) {
                removeFromCacheInternal(updatedLands.get(i));
                addToCacheInternal(savedLands.get(i));
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        return ServiceResult.success(null);
    }

    /**
     * 取消信任玩家
     */
    public ServiceResult<Void> untrustPlayer(Player owner, String trustedUuid) {
        String ownerUuid = owner.getUniqueId().toString();
        List<Land> lands;
        synchronized (dbLock) {
            lands = repository.findByOwner(ownerUuid);
        }

        if (lands.isEmpty()) {
            return ServiceResult.failure("你没有领地");
        }

        // 从所有领地取消信任
        List<Land> updatedLands = new ArrayList<>();
        for (Land land : lands) {
            updatedLands.add(land.untrust(trustedUuid));
        }

        // 使用批量保存提高性能
        List<Land> savedLands;
        synchronized (dbLock) {
            savedLands = repository.saveAll(updatedLands);
        }

        // 更新缓存
        cacheLock.writeLock().lock();
        try {
            for (int i = 0; i < updatedLands.size(); i++) {
                removeFromCacheInternal(updatedLands.get(i));
                addToCacheInternal(savedLands.get(i));
            }
        } finally {
            cacheLock.writeLock().unlock();
        }

        return ServiceResult.success(null);
    }

    /**
     * 设置保护规则
     */
    public ServiceResult<Void> setProtectionRule(Player owner, String landId, String ruleName, boolean enabled) {
        String ownerUuid = owner.getUniqueId().toString();

        Optional<Land> landOpt;
        synchronized (dbLock) {
            landOpt = repository.findByLandId(landId);
        }
        
        if (landOpt.isEmpty()) {
            return ServiceResult.failure("领地不存在: " + landId);
        }

        Land land = landOpt.get();
        if (!ownerUuid.equals(land.owner())) {
            return ServiceResult.failure("你不是该领地的所有者");
        }

        Land updatedLand = land.withProtectionRule(ruleName, enabled);
        Land savedLand;
        synchronized (dbLock) {
            savedLand = repository.save(updatedLand);
        }

        // 更新缓存
        removeFromCache(land);
        addToCache(savedLand);

        return ServiceResult.success(null);
    }

    /**
     * 获取玩家的领地
     */
    public List<Land> getPlayerLands(Player player) {
        synchronized (dbLock) {
            return repository.findByOwner(player.getUniqueId().toString());
        }
    }

    /**
     * 根据区块查找领地（使用缓存优化）
     */
    public Optional<Land> getLandByChunk(Chunk chunk) {
        cacheLock.readLock().lock();
        try {
            String worldName = chunk.getWorld().getName();
            Set<Land> worldLands = worldLandCache.get(worldName);

            if (worldLands == null) {
                return Optional.empty();
            }

            // 在该世界的领地中查找包含此区块的领地
            for (Land land : worldLands) {
                if (land.intersectsChunk(chunk)) {
                    return Optional.of(land);
                }
            }
            return Optional.empty();
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * 根据位置查找领地
     */
    public Optional<Land> getLandByLocation(Location location) {
        return getLandByChunk(location.getChunk());
    }

    /**
     * 检查玩家是否被信任
     */
    public boolean isTrusted(Land land, Player player) {
        return land.isTrusted(player.getUniqueId().toString());
    }

    /**
     * 检查玩家是否可以创建领地（使用世界坐标）
     */
    public boolean canCreateLand(Player player, Location pos1, Location pos2) {
        // 检查玩家已拥有的领地数
        int ownedCount;
        synchronized (dbLock) {
            ownedCount = repository.countByOwner(player.getUniqueId().toString());
        }
        
        if (ownedCount >= maxLandsPerPlayer) {
            return false;
        }

        // 检查新领地面积
        int area = calculateArea(pos1, pos2);
        int maxArea = maxChunksPerLand * 256;
        return area <= maxArea;
    }

    /**
     * 检查玩家是否可以创建领地（兼容旧版本，使用区块）
     * @deprecated 使用 Location 版本代替
     */
    @Deprecated
    public boolean canCreateLand(Player player, Chunk pos1, Chunk pos2) {
        Location loc1 = new Location(pos1.getWorld(), pos1.getX() * 16, 64, pos1.getZ() * 16);
        Location loc2 = new Location(pos2.getWorld(), (pos2.getX() + 1) * 16 - 1, 64, (pos2.getZ() + 1) * 16 - 1);
        return canCreateLand(player, loc1, loc2);
    }

    /**
     * 获取所有已认领的领地
     */
    public List<Land> getAllClaimedLands() {
        synchronized (dbLock) {
            return repository.findAllClaimed();
        }
    }

    /**
     * 获取所有未认领的领地
     */
    public List<Land> getAllUnclaimedLands() {
        synchronized (dbLock) {
            return repository.findAllUnclaimed();
        }
    }

    /**
     * 查找玩家已认领的领地
     */
    public List<Land> findClaimedLandsByOwner(UUID ownerUuid) {
        synchronized (dbLock) {
            return repository.findByOwner(ownerUuid.toString());
        }
    }

    /**
     * 计算领地面积（使用世界坐标）
     */
    private int calculateArea(Location pos1, Location pos2) {
        int width = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int length = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
        return width * length;
    }

    /**
     * 计算区块数量（兼容旧版本）
     * @deprecated 使用 calculateArea 代替
     */
    @Deprecated
    private int calculateChunkCount(Chunk pos1, Chunk pos2) {
        int width = Math.abs(pos1.getX() - pos2.getX()) + 1;
        int length = Math.abs(pos1.getZ() - pos2.getZ()) + 1;
        return width * length;
    }

    /**
     * 检查是否与现有领地重叠（使用世界坐标）
     */
    private boolean hasOverlap(Location pos1, Location pos2) {
        cacheLock.readLock().lock();
        try {
            return hasOverlapInternal(pos1, pos2);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * 内部重叠检查方法（调用者必须持有读锁）
     */
    private boolean hasOverlapInternal(Location pos1, Location pos2) {
        String worldName = pos1.getWorld().getName();
        Set<Land> worldLands = worldLandCache.get(worldName);

        if (worldLands == null) {
            return false;
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        for (Land land : worldLands) {
            if (isOverlapping(minX, maxX, minZ, maxZ, land)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算是否与现有领地重叠（兼容旧版本，使用区块）
     */
    @Deprecated
    private boolean hasOverlap(Chunk pos1, Chunk pos2) {
        Location loc1 = new Location(pos1.getWorld(), pos1.getX() * 16, 64, pos1.getZ() * 16);
        Location loc2 = new Location(pos2.getWorld(), (pos2.getX() + 1) * 16 - 1, 64, (pos2.getZ() + 1) * 16 - 1);
        return hasOverlap(loc1, loc2);
    }

    /**
     * 检查两个矩形区域是否重叠
     */
    private boolean isOverlapping(int minX1, int maxX1, int minZ1, int maxZ1, Land land) {
        int minX2 = land.getMinX();
        int maxX2 = land.getMaxX();
        int minZ2 = land.getMinZ();
        int maxZ2 = land.getMaxZ();

        return !(maxX1 < minX2 || minX1 > maxX2 || maxZ1 < minZ2 || minZ1 > maxZ2);
    }

    public int getMaxLandsPerPlayer() {
        return maxLandsPerPlayer;
    }

    public int getMaxChunksPerLand() {
        return maxChunksPerLand;
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return worldLandCache.getStats();
    }
    
    /**
     * 清理缓存
     */
    public void cleanupCache() {
        worldLandCache.clear(); // Or just let it expire
    }
    
    /**
     * 更新配置参数
     *
     * @param maxLandsPerPlayer 每个玩家最大领地数
     * @param maxChunksPerLand 每个领地最大区块数
     * @param defaultProtectionRules 默认保护规则
     * @return 更新结果
     */
    public ReloadResult updateConfiguration(int maxLandsPerPlayer, int maxChunksPerLand,
                                          Map<String, Boolean> defaultProtectionRules) {
        logger.info("更新 LandService 配置...");
        
        try {
            // 更新配置参数
            this.maxLandsPerPlayer = maxLandsPerPlayer;
            this.maxChunksPerLand = maxChunksPerLand;
            this.defaultProtectionRules.clear();
            if (defaultProtectionRules != null) {
                this.defaultProtectionRules.putAll(defaultProtectionRules);
            }
            
            // 重建缓存以确保一致性
            rebuildCache();
            
            String message = String.format("LandService 配置已更新: maxLands=%d, maxChunks=%d",
                                          maxLandsPerPlayer, maxChunksPerLand);
            logger.info(message);
            
            return new ReloadResult(true, message, null);
        } catch (Exception e) {
            String errorMessage = "更新 LandService 配置时出错: " + e.getMessage();
            logger.severe(errorMessage);
            e.printStackTrace();
            
            return new ReloadResult(false, errorMessage, e);
        }
    }
    
    /**
     * 重载结果类
     */
    public static class ReloadResult {
        private final boolean success;
        private final String message;
        private final Exception exception;
        
        public ReloadResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.exception = exception;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Exception getException() {
            return exception;
        }
    }

    /**
     * 关闭服务，清理资源
     */
    public void shutdown() {
        logger.info("Shutting down LandService...");
        worldLandCache.logStats();
        worldLandCache.shutdown();
        logger.info("LandService shutdown completed");
    }
}
