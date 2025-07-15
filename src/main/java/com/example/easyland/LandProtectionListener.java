package com.example.easyland;

import org.bukkit.Chunk;
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
import java.util.concurrent.ConcurrentHashMap;

public class LandProtectionListener implements Listener {
    private final LandManager landManager;
    private final ConfigManager configManager;
    private static final String BYPASS_PERMISSION = "easyland.bypass";
    
    // 消息冷却系统，防止刷屏
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();
    private final long messageCooldownMs;

    public LandProtectionListener(LandManager landManager, ConfigManager configManager, int messageCooldownSeconds) {
        this.landManager = landManager;
        this.configManager = configManager;
        this.messageCooldownMs = messageCooldownSeconds * 1000L; // 转换为毫秒
    }

    /**
     * 检查玩家是否有权限在指定位置进行操作
     */
    private boolean hasPermission(Player player, Location location) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }
        
        Chunk chunk = location.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        
        // 如果不在任何领地内，允许操作
        if (land == null) {
            return true;
        }
        
        // 如果是无主领地，禁止操作
        if (land.getOwner() == null) {
            return false;
        }
        
        // 检查是否为领地主人或受信任的玩家
        return landManager.isTrusted(land, player.getUniqueId().toString());
    }

    /**
     * 发送保护消息（带冷却系统防止刷屏）
     */
    private void sendProtectionMessage(Player player, String message) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        Long lastMessageTime = messageCooldowns.get(playerId);
        
        if (lastMessageTime == null || (currentTime - lastMessageTime) >= messageCooldownMs) {
            player.sendMessage(message);
            messageCooldowns.put(playerId, currentTime);
            
            // 定期清理过期的冷却记录（每100次调用清理一次）
            if (messageCooldowns.size() > 100 && Math.random() < 0.01) {
                cleanupExpiredCooldowns();
            }
        }
    }
    
    /**
     * 清理过期的冷却记录
     */
    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        messageCooldowns.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > messageCooldownMs * 10); // 保留10倍冷却时间的记录
    }

    /**
     * 获取指定位置的领地
     */
    private ChunkLand getLandAt(Location location) {
        Chunk chunk = location.getChunk();
        return landManager.getLandByChunk(chunk);
    }

    /**
     * 检查指定保护规则是否在该领地启用
     */
    private boolean isProtectionEnabled(ChunkLand land, String ruleName) {
        // 首先检查服务器是否允许此规则
        if (!configManager.isProtectionRuleEnabled(ruleName)) {
            return false;
        }
        
        // 如果在领地内，检查领地的规则设置
        if (land != null) {
            return land.getProtectionRule(ruleName);
        }
        
        // 不在领地内，不需要保护
        return false;
    }

    // ================= 方块保护规则 =================
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        ChunkLand land = getLandAt(location);
        
        if (!isProtectionEnabled(land, "block-protection")) return;
        
        if (!hasPermission(player, location)) {
            sendProtectionMessage(player, "§c你不能破坏他人的领地！");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        ChunkLand land = getLandAt(location);
        
        if (!isProtectionEnabled(land, "block-protection")) return;
        
        if (!hasPermission(player, location)) {
            sendProtectionMessage(player, "§c你不能在他人的领地内放置方块！");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        ChunkLand land = getLandAt(location);
        
        if (!isProtectionEnabled(land, "block-protection")) return;
        
        if (!hasPermission(player, location)) {
            sendProtectionMessage(player, "§c你不能在他人的领地内倒水或岩浆！");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();
        ChunkLand land = getLandAt(location);
        
        if (!isProtectionEnabled(land, "block-protection")) return;
        
        if (!hasPermission(player, location)) {
            sendProtectionMessage(player, "§c你不能在他人的领地内取水或岩浆！");
            event.setCancelled(true);
        }
    }

    // ================= 容器保护规则 =================
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        BlockState state = block.getState();
        
        // 只处理容器类型的方块
        if (!(state instanceof Container)) return;
        
        Location location = block.getLocation();
        ChunkLand land = getLandAt(location);
        
        if (!isProtectionEnabled(land, "container-protection")) return;
        
        if (!hasPermission(player, location)) {
            sendProtectionMessage(player, "§c你不能与他人领地内的容器交互！");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled() || !(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        
        // 检查是否是方块容器
        if (event.getInventory().getLocation() != null) {
            Location location = event.getInventory().getLocation();
            ChunkLand land = getLandAt(location);
            
            if (!isProtectionEnabled(land, "container-protection")) return;
            
            if (!hasPermission(player, location)) {
                sendProtectionMessage(player, "§c你不能访问他人领地内的容器！");
                event.setCancelled(true);
            }
        }
    }

    // ================= 爆炸保护规则 =================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) return;
        
        // 移除领地内启用了爆炸保护的方块，防止被爆炸破坏
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            ChunkLand land = getLandAt(block.getLocation());
            if (isProtectionEnabled(land, "explosion-protection")) {
                blockIterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;
        
        Location location = event.getBlock().getLocation();
        ChunkLand land = getLandAt(location);
        
        if (isProtectionEnabled(land, "explosion-protection")) {
            event.setCancelled(true);
        }
    }

    // ================= 玩家保护规则 =================

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.isCancelled()) return;
        
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        ChunkLand land = getLandAt(player.getLocation());
        
        if (!isProtectionEnabled(land, "player-protection")) return;
        
        // 只保护领主或受信任的玩家
        if (hasPermission(player, player.getLocation())) {
            event.setCancelled(true);
            sendProtectionMessage(player, "§a你在自己的领地内受到保护！");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        
        // 保护被攻击的玩家
        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();
            ChunkLand land = getLandAt(victim.getLocation());
            
            if (!isProtectionEnabled(land, "player-protection")) return;
            
            // 检查被攻击者是否为领主或受信任的玩家
            boolean victimHasPermission = hasPermission(victim, victim.getLocation());
            
            if (victimHasPermission) {
                // 如果攻击者也是玩家，检查是否有权限
                if (event.getDamager() instanceof Player) {
                    Player attacker = (Player) event.getDamager();
                    if (!hasPermission(attacker, victim.getLocation())) {
                        sendProtectionMessage(attacker, "§c你不能在他人的领地内攻击领主或受信任的玩家！");
                        event.setCancelled(true);
                        return;
                    }
                }
                
                // 非玩家攻击，保护受信任的玩家
                sendProtectionMessage(victim, "§a你在领地内受到保护！");
                event.setCancelled(true);
            }
        }
    }
}
