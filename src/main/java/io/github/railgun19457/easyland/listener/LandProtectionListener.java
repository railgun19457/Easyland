package io.github.railgun19457.easyland.listener;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.railgun19457.easyland.domain.Land;
import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.service.LandService;
import io.github.railgun19457.easyland.util.CacheManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class LandProtectionListener implements Listener {
    private static final Logger logger = Logger.getLogger(LandProtectionListener.class.getName());
    
    private final LandService landService;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private static final String BYPASS_PERMISSION = "easyland.bypass";

    // 消息冷却系统，防止刷屏 - 使用CacheManager管理
    private CacheManager<String, Long> messageCooldowns;
    private long messageCooldownMs;
    
    // 读写锁用于保护消息冷却操作
    private final ReentrantReadWriteLock messageLock = new ReentrantReadWriteLock();

    public LandProtectionListener(LandService landService, ConfigManager configManager,
            LanguageManager languageManager, int messageCooldownSeconds, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.landService = landService;
        this.configManager = configManager;
        this.languageManager = languageManager;
        this.messageCooldownMs = messageCooldownSeconds * 1000L; // 转换为毫秒
        this.plugin = plugin;
        
        // 初始化消息冷却缓存 - 最大1000个玩家，过期时间为冷却时间的10倍，启用定时清理
        this.messageCooldowns = new CacheManager<>("MessageCooldowns", 1000, messageCooldownMs * 10);
        
        logger.info("LandProtectionListener initialized with message cooldown: " + messageCooldownSeconds + " seconds");
    }

    /**
     * 检查玩家是否有权限在指定位置进行操作
     */
    private boolean hasPermission(Player player, Location location) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }

        Optional<Land> landOpt = landService.getLandByLocation(location);

        // 如果不在任何领地内，允许操作
        if (landOpt.isEmpty()) {
            return true;
        }

        Land land = landOpt.get();

        // 如果是无主领地，统一处理策略：允许破坏但不允许放置
        // 这个方法主要用于破坏类操作，所以无主领地允许操作
        if (!land.isClaimed()) {
            return true;
        }

        // 检查是否为领地主人或受信任的玩家
        return land.isTrusted(player.getUniqueId().toString());
    }
    
    /**
     * 检查玩家是否有权限在指定位置进行放置操作
     * 与 hasPermission 的区别在于无主领地的处理策略
     */
    private boolean hasPlacePermission(Player player, Location location) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }

        Optional<Land> landOpt = landService.getLandByLocation(location);

        // 如果不在任何领地内，允许操作
        if (landOpt.isEmpty()) {
            return true;
        }

        Land land = landOpt.get();

        // 如果是无主领地，禁止放置操作
        if (!land.isClaimed()) {
            return false;
        }

        // 检查是否为领地主人或受信任的玩家
        return land.isTrusted(player.getUniqueId().toString());
    }

    /**
     * 发送保护消息（带冷却系统防止刷屏）
     */
    private void sendProtectionMessage(Player player, String message) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        
        messageLock.readLock().lock();
        try {
            Long lastMessageTime = messageCooldowns.get(playerId);

            if (lastMessageTime == null || (currentTime - lastMessageTime) >= messageCooldownMs) {
                messageLock.readLock().unlock();
                messageLock.writeLock().lock();
                try {
                    // 双重检查锁定模式
                    lastMessageTime = messageCooldowns.get(playerId);
                    if (lastMessageTime == null || (currentTime - lastMessageTime) >= messageCooldownMs) {
                        player.sendMessage(message);
                        messageCooldowns.put(playerId, currentTime);
                    }
                } finally {
                    messageLock.writeLock().unlock();
                    messageLock.readLock().lock();
                }
            }
        } finally {
            messageLock.readLock().unlock();
        }
    }

    private void sendLocalizedProtectionMessage(Player player, String messageKey, Object... args) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        
        messageLock.readLock().lock();
        try {
            Long lastMessageTime = messageCooldowns.get(playerId);

            if (lastMessageTime == null || (currentTime - lastMessageTime) >= messageCooldownMs) {
                messageLock.readLock().unlock();
                messageLock.writeLock().lock();
                try {
                    // 双重检查锁定模式
                    lastMessageTime = messageCooldowns.get(playerId);
                    if (lastMessageTime == null || (currentTime - lastMessageTime) >= messageCooldownMs) {
                        languageManager.sendMessage(player, messageKey, args);
                        messageCooldowns.put(playerId, currentTime);
                    }
                } finally {
                    messageLock.writeLock().unlock();
                    messageLock.readLock().lock();
                }
            }
        } finally {
            messageLock.readLock().unlock();
        }
    }

    /**
     * 获取指定位置的领地
     */
    private Optional<Land> getLandAt(Location location) {
        return landService.getLandByLocation(location);
    }

    /**
     * 检查指定保护规则是否在该领地启用
     */
    private boolean isProtectionEnabled(Optional<Land> landOpt, String ruleName) {
        // 首先检查服务器是否允许此规则
        if (!configManager.isProtectionRuleEnabled(ruleName)) {
            return false;
        }

        // 如果在领地内，检查领地的规则设置
        if (landOpt.isPresent()) {
            return landOpt.get().getProtectionRule(ruleName);
        }

        // 不在领地内，不需要保护
        return false;
    }

    /**
     * 清理指定位置附近的掉落物，防止VeinMiner等插件造成物品复制
     */
    private void clearNearbyDrops(Location location) {
        if (location == null || location.getWorld() == null)
            return;

        // 清理以该位置为中心3x3x3范围内的掉落物
        location.getWorld().getNearbyEntities(location, 2, 2, 2)
                .stream()
                .filter(entity -> entity instanceof org.bukkit.entity.Item)
                .forEach(org.bukkit.entity.Entity::remove);
    }
    
    /**
     * 增强的VeinMiner漏洞防护，清理更大范围内的掉落物
     */
    private void clearNearbyDropsEnhanced(Location location) {
        if (location == null || location.getWorld() == null)
            return;

        // 清理以该位置为中心5x5x5范围内的掉落物，增强防护
        location.getWorld().getNearbyEntities(location, 3, 3, 3)
                .stream()
                .filter(entity -> entity instanceof org.bukkit.entity.Item)
                .forEach(org.bukkit.entity.Entity::remove);
                
        // 额外清理可能因连锁反应产生的掉落物
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    Location checkLoc = location.clone().add(x, y, z);
                    location.getWorld().getNearbyEntities(checkLoc, 1, 1, 1)
                            .stream()
                            .filter(entity -> entity instanceof org.bukkit.entity.Item)
                            .forEach(org.bukkit.entity.Entity::remove);
                }
            }
        }
    }

    // ================= 方块保护规则 =================

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Optional<Land> land = getLandAt(location);

        if (!isProtectionEnabled(land, "block-protection"))
            return;

        if (!hasPermission(player, location)) {
            // 防止VeinMiner等插件造成的物品复制漏洞
            // 先取消事件，防止方块被破坏
            event.setCancelled(true);

            // 使用增强的清理方法，更彻底地防止VeinMiner漏洞
            // 延迟1tick确保掉落物已生成
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                    () -> clearNearbyDropsEnhanced(location), 1L);

            sendLocalizedProtectionMessage(player, "protection-message.block");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Optional<Land> land = getLandAt(location);

        if (!isProtectionEnabled(land, "block-protection"))
            return;

        if (!hasPlacePermission(player, location)) {
            event.setCancelled(true);
            sendLocalizedProtectionMessage(player, "protection-message.block");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Optional<Land> land = getLandAt(location);

        if (!isProtectionEnabled(land, "block-protection"))
            return;

        if (!hasPlacePermission(player, location)) {
            event.setCancelled(true);
            sendProtectionMessage(player, languageManager.getMessage("protection.bucket-place"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        Optional<Land> land = getLandAt(location);

        if (!isProtectionEnabled(land, "block-protection"))
            return;

        if (!hasPermission(player, location)) {
            event.setCancelled(true);
            sendProtectionMessage(player, languageManager.getMessage("protection.bucket-empty"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEndermanChangeBlock(EntityChangeBlockEvent event) {
        if (event.isCancelled())
            return;

        if (event.getEntityType() != org.bukkit.entity.EntityType.ENDERMAN)
            return;

        Location location = event.getBlock().getLocation();
        Optional<Land> land = getLandAt(location);

        if (!isProtectionEnabled(land, "block-protection"))
            return;

        event.setCancelled(true);
    }

    // ================= 容器保护规则 =================

    @EventHandler(priority = EventPriority.HIGH)
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null)
            return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        BlockState state = block.getState();

        // 只处理容器类型的方块
        if (!(state instanceof Container))
            return;

        Location location = block.getLocation();
        Optional<Land> land = getLandAt(location);

        if (!isProtectionEnabled(land, "container-protection"))
            return;

        if (!hasPermission(player, location)) {
            sendProtectionMessage(player, languageManager.getMessage("protection.container"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled() || !(event.getPlayer() instanceof Player))
            return;

        Player player = (Player) event.getPlayer();

        // 检查是否是方块容器
        if (event.getInventory().getLocation() != null) {
            Location location = event.getInventory().getLocation();
            Optional<Land> land = getLandAt(location);

            if (!isProtectionEnabled(land, "container-protection"))
                return;

            if (!hasPermission(player, location)) {
                sendProtectionMessage(player, languageManager.getMessage("protection.container"));
                event.setCancelled(true);
            }
        }
    }

    // ================= 爆炸保护规则 =================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        boolean isExplosive = switch (event.getEntityType()) {
            case CREEPER, TNT, FIREBALL, SMALL_FIREBALL, WITHER_SKULL, DRAGON_FIREBALL -> true;
            default -> {
                if (event.getEntity() != null && event.getEntity().getType().name().equals("ENDER_CRYSTAL")) {
                    yield true;
                }
                if (event.getLocation() != null) {
                    String blockName = event.getLocation().getBlock().getType().name();
                    yield blockName.contains("BED") || blockName.contains("RESPAWN_ANCHOR");
                }
                yield false;
            }
        };

        if (!isExplosive) {
            return;
        }

        event.blockList().removeIf(block -> {
            Optional<Land> land = getLandAt(block.getLocation());
            return isProtectionEnabled(land, "explosion-protection");
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.isCancelled())
            return;
        Location location = event.getBlock().getLocation();
        Optional<Land> land = getLandAt(location);
        if (!isProtectionEnabled(land, "explosion-protection"))
            return;

        // 只拦截破坏性实体，允许玩家、蜜蜂、村民等友好行为
        org.bukkit.entity.EntityType type = event.getEntity().getType();
        if (type == org.bukkit.entity.EntityType.WITHER ||
                type == org.bukkit.entity.EntityType.ENDERMAN ||
                type == org.bukkit.entity.EntityType.ENDER_DRAGON) {
            event.setCancelled(true);
        }
    }

    // ================= 玩家保护规则 =================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        if (event.getEntity() instanceof Player player) {
            Optional<Land> land = getLandAt(player.getLocation());
            if (!isProtectionEnabled(land, "player-protection")) {
                return;
            }

            boolean isExternalDamage = switch (event.getCause()) {
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, MAGIC, BLOCK_EXPLOSION, ENTITY_EXPLOSION, FIRE_TICK, HOT_FLOOR, THORNS -> true;
                default -> false;
            };

            if (isExternalDamage && !hasPermission(player, player.getLocation())) {
                event.setCancelled(true);
                sendProtectionMessage(player, languageManager.getMessage("protection.self-protected"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled())
            return;

        // 保护被攻击的玩家
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            Optional<Land> land = getLandAt(victim.getLocation());

            if (!isProtectionEnabled(land, "player-protection"))
                return;

            // 检查被攻击者是否为领主或受信任的玩家
            boolean victimHasPermission = hasPermission(victim, victim.getLocation());

            if (victimHasPermission) {
                // 如果攻击者也是玩家，检查是否有权限
                if (event.getDamager() instanceof Player) {
                    Player attacker = (Player) event.getDamager();
                    if (!hasPermission(attacker, victim.getLocation())) {
                        sendProtectionMessage(attacker, languageManager.getMessage("protection.cannot-attack"));
                        event.setCancelled(true);
                        return;
                    }
                }

                // 非玩家攻击，保护受信任的玩家
                sendProtectionMessage(victim, languageManager.getMessage("protection.protected"));
                event.setCancelled(true);
            }
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return messageCooldowns.getStats();
    }
    
    /**
     * 清理缓存
     */
    public void cleanupCache() {
        messageCooldowns.clear();
    }
    
    /**
     * 重载监听器配置
     *
     * @param newConfigManager 新的配置管理器
     * @param newLanguageManager 新的语言管理器
     * @param messageCooldownSeconds 新的消息冷却时间
     * @return 重载结果
     */
    public ReloadResult reload(ConfigManager newConfigManager, LanguageManager newLanguageManager,
                              int messageCooldownSeconds) {
        logger.info("重载 LandProtectionListener 配置...");
        
        try {
            // 更新配置管理器和语言管理器引用
            this.configManager = newConfigManager;
            this.languageManager = newLanguageManager;
            
            // 更新消息冷却时间
            long newMessageCooldownMs = messageCooldownSeconds * 1000L;
            if (this.messageCooldownMs != newMessageCooldownMs) {
                this.messageCooldownMs = newMessageCooldownMs;
                
                // 重新创建消息冷却缓存
                messageLock.writeLock().lock();
                try {
                    messageCooldowns.shutdown();
                    this.messageCooldowns = new CacheManager<>("MessageCooldowns", 1000, messageCooldownMs * 10);
                } finally {
                    messageLock.writeLock().unlock();
                }
            }
            
            String message = "LandProtectionListener 配置已重载，消息冷却时间: " + messageCooldownSeconds + " 秒";
            logger.info(message);
            
            return new ReloadResult(true, message, null);
        } catch (Exception e) {
            String errorMessage = "重载 LandProtectionListener 配置时出错: " + e.getMessage();
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
        logger.info("Shutting down LandProtectionListener...");
        
        try {
            // 清理消息冷却缓存
            messageCooldowns.logStats();
            messageCooldowns.shutdown();
            
            logger.info("LandProtectionListener shutdown completed");
        } catch (Exception e) {
            logger.warning("Error during LandProtectionListener shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
