package io.github.railgun19457.easyland.listener;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.util.CacheManager;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class LandEnterListener implements Listener {
    private static final Logger logger = Logger.getLogger(LandEnterListener.class.getName());
    
    private final LandService landService;
    private LanguageManager languageManager;
    private final Map<UUID, String> lastLandOwner = new ConcurrentHashMap<>();
    private final CacheManager<String, String> ownerNameCache; // 缓存UUID到玩家名的映射
    
    // 读写锁用于保护玩家状态管理
    private final ReentrantReadWriteLock playerStateLock = new ReentrantReadWriteLock();

    public LandEnterListener(LandService landService, LanguageManager languageManager) {
        this.landService = landService;
        this.languageManager = languageManager;
        
        // 初始化玩家名称缓存 - 最大1000个玩家，过期时间30分钟，启用定时清理
        this.ownerNameCache = new CacheManager<>("OwnerNameCache", 1000, 30 * 60 * 1000);
        
        logger.info("LandEnterListener initialized with name cache (30min expiry)");
    }

    @EventHandler(priority = EventPriority.MONITOR) // 使用MONITOR优先级，确保在其他插件处理完成后才检测
    public void onPlayerMove(PlayerMoveEvent event) {
        // 优化1：检查玩家是否真的移动了方块
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY()) {
            return;
        }
        
        // 只在玩家跨区块移动时触发
        Chunk fromChunk = event.getFrom().getChunk();
        Chunk toChunk = event.getTo().getChunk();

        // 改进跨区块检测：使用区块坐标而不是对象比较，提高准确性
        if (fromChunk.getX() == toChunk.getX() && fromChunk.getZ() == toChunk.getZ() &&
            fromChunk.getWorld().equals(toChunk.getWorld())) {
            return; // 未跨区块，直接返回
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 获取目标和来源区块的领地
        Optional<Land> toLandOpt = landService.getLandByChunk(toChunk);
        String toOwner = toLandOpt.map(Land::owner).orElse(null);

        // 优化2：减少锁的使用范围
        String lastOwner;
        playerStateLock.readLock().lock();
        try {
            lastOwner = lastLandOwner.get(playerId);
        } finally {
            playerStateLock.readLock().unlock();
        }
        
        // 如果领地所有者没有变化，则无需进一步操作
        if (Objects.equals(toOwner, lastOwner)) {
            return;
        }

        Optional<Land> fromLandOpt = landService.getLandByChunk(fromChunk);
        String fromOwner = fromLandOpt.map(Land::owner).orElse(null);
        
        // 检查是否进入了新的领地
        if (toOwner != null) {
            String ownerName = getOwnerName(toOwner);
            String landInfo = toLandOpt
                    .map(land -> (land.landId() != null && !land.landId().isEmpty())
                            ? " (ID: " + land.landId() + ")"
                            : "")
                    .orElse("");
            languageManager.sendMessage(player, "enter.message", ownerName, landInfo);
        }

        // 检查是否离开了领地
        if (fromOwner != null && !Objects.equals(fromOwner, toOwner)) {
            String ownerName = getOwnerName(fromOwner);
            String landInfo = fromLandOpt
                    .map(land -> (land.landId() != null && !land.landId().isEmpty())
                            ? " (ID: " + land.landId() + ")"
                            : "")
                    .orElse("");
            languageManager.sendMessage(player, "leave.message", ownerName, landInfo);
        }
        
        // 更新玩家当前所在领地的主人
        playerStateLock.writeLock().lock();
        try {
            if (toOwner != null) {
                lastLandOwner.put(playerId, toOwner);
            } else {
                lastLandOwner.remove(playerId);
            }
        } finally {
            playerStateLock.writeLock().unlock();
        }
    }

    /**
     * 获取玩家名称，使用缓存减少数据库查询
     */
    private String getOwnerName(String ownerUuid) {
        if (ownerUuid == null)
            return languageManager.getMessage("land.unknown-owner");

        // 先尝试从缓存获取
        String cachedName = ownerNameCache.get(ownerUuid);
        if (cachedName != null) {
            return cachedName;
        }
        
        // 缓存中没有，查询并缓存
        String name;
        try {
            UUID playerUuid = UUID.fromString(ownerUuid);
            name = Bukkit.getOfflinePlayer(playerUuid).getName();
            if (name == null) {
                name = languageManager.getMessage("land.unknown-player");
            }
        } catch (IllegalArgumentException e) {
            name = languageManager.getMessage("land.invalid-uuid");
        }
        
        // 存入缓存
        ownerNameCache.put(ownerUuid, name);
        return name;
    }

    /**
     * 清理断开连接玩家的缓存数据
     */
    public void cleanupPlayer(UUID playerId) {
        playerStateLock.writeLock().lock();
        try {
            lastLandOwner.remove(playerId);
        } finally {
            playerStateLock.writeLock().unlock();
        }
    }

    /**
     * 清理玩家名称缓存（定期调用以防止内存泄漏）
     */
    public void cleanupNameCache() {
        ownerNameCache.clear(); // Or just let it expire
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return ownerNameCache.getStats();
    }
    
    /**
     * 重载监听器配置
     *
     * @param newLanguageManager 新的语言管理器
     * @return 重载结果
     */
    public ReloadResult reload(LanguageManager newLanguageManager) {
        logger.info("重载 LandEnterListener 配置...");
        
        try {
            // 更新语言管理器引用
            this.languageManager = newLanguageManager;
            
            // 清理名称缓存以确保使用新的语言设置
            ownerNameCache.clear();
            
            String message = "LandEnterListener 配置已重载";
            logger.info(message);
            
            return new ReloadResult(true, message, null);
        } catch (Exception e) {
            String errorMessage = "重载 LandEnterListener 配置时出错: " + e.getMessage();
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
     * 关闭监听器，清理资源
     */
    public void shutdown() {
        logger.info("Shutting down LandEnterListener...");
        
        try {
            // 清理玩家状态数据
            playerStateLock.writeLock().lock();
            try {
                int clearedCount = lastLandOwner.size();
                lastLandOwner.clear();
                if (clearedCount > 0) {
                    logger.info("Cleared " + clearedCount + " player land states");
                }
            } finally {
                playerStateLock.writeLock().unlock();
            }
            
            // 关闭名称缓存
            ownerNameCache.logStats();
            ownerNameCache.shutdown();
            
            logger.info("LandEnterListener shutdown completed");
        } catch (Exception e) {
            logger.warning("Error during LandEnterListener shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
