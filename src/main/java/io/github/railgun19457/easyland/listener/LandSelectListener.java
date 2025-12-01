package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.service.LandService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class LandSelectListener implements Listener {
    private static final Logger logger = Logger.getLogger(LandSelectListener.class.getName());
    private static final int PARTICLE_LAYERS = 10;
    private static final int CIRCLE_SEGMENTS = 16;

    // 记录每个玩家最近的两次坐标选择
    private final Map<UUID, Location[]> selectMap;
    private final LandService landService;
    private final LanguageManager languageManager;
    
    // 读写锁用于保护选区操作
    private final ReentrantReadWriteLock selectLock = new ReentrantReadWriteLock();

    public LandSelectListener(LandService landService, LanguageManager languageManager, Map<UUID, Location[]> selectMap) {
        this.landService = landService;
        this.languageManager = languageManager;
        this.selectMap = selectMap;
    }

    @EventHandler(priority = EventPriority.HIGH) // 设置高优先级，确保在其他插件之前处理选区
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.WOODEN_HOE)
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        Component display = meta.displayName();
        if (display == null || !display.equals(Component.text(languageManager.getMessage("select.tool-name"))))
            return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND)
            return;

        // 获取点击的方块位置（使用世界坐标）
        if (event.getClickedBlock() == null)
            return;
        Location clickedLocation = event.getClickedBlock().getLocation();

        // 检查该位置是否已被占用
        if (landService.getLandByLocation(clickedLocation).isPresent()) {
            languageManager.sendMessage(player, "select.chunk-occupied");
            event.setCancelled(true);
            return;
        }

        selectLock.writeLock().lock();
        try {
            Location[] selects = selectMap.getOrDefault(player.getUniqueId(), new Location[2]);
            
            // 修复选区逻辑：第一次选择时，selects[0] 和 selects[1] 都为 null
            // 第二次选择时，selects[0] 应该保留第一次的选择，selects[1] 更新为第二次的选择
            if (selects[0] == null) {
                // 第一次选择
                selects[0] = clickedLocation.clone();
                selects[1] = null; // 确保第二次选择为空
            } else {
                // 第二次选择
                selects[1] = clickedLocation.clone();
            }
            
            selectMap.put(player.getUniqueId(), selects);
        } finally {
            selectLock.writeLock().unlock();
        }

        // 显示选择的坐标
        languageManager.sendMessage(player, "select.point-selected",
            clickedLocation.getBlockX(), clickedLocation.getBlockZ());

        // 添加显眼的粒子效果
        spawnSelectionParticles(clickedLocation, player);

        // 检查是否已完成两次选择
        Location[] selects = getPlayerSelects(player);
        if (selects[0] != null && selects[1] != null) {
            languageManager.sendMessage(player, "select.selection-complete",
                selects[0].getBlockX(), selects[0].getBlockZ(),
                selects[1].getBlockX(), selects[1].getBlockZ());
        }

        event.setCancelled(true);
    }

    public Location[] getPlayerSelects(Player player) {
        selectLock.readLock().lock();
        try {
            Location[] selects = selectMap.getOrDefault(player.getUniqueId(), new Location[2]);
            // 返回副本以避免外部修改
            Location[] result = new Location[2];
            if (selects[0] != null) result[0] = selects[0].clone();
            if (selects[1] != null) result[1] = selects[1].clone();
            return result;
        } finally {
            selectLock.readLock().unlock();
        }
    }

    private void spawnSelectionParticles(Location location, Player player) {
        // 在选择点上方生成一个显眼的粒子柱
        Location particleLocation = location.clone().add(0.5, 1, 0.5);

        // 生成多层粒子效果,形成一个明显的标记柱
        for (int i = 0; i < PARTICLE_LAYERS; i++) {
            Location layerLocation = particleLocation.clone().add(0, i * 0.3, 0);

            // 使用多种粒子类型增强视觉效果
            player.spawnParticle(Particle.FLAME, layerLocation, 8, 0.3, 0.1, 0.3, 0.02);
            player.spawnParticle(Particle.HAPPY_VILLAGER, layerLocation, 5, 0.2, 0.1, 0.2, 0);
            player.spawnParticle(Particle.END_ROD, layerLocation, 3, 0.1, 0.1, 0.1, 0.01);
        }

        // 在地面生成一个圆形标记
        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / CIRCLE_SEGMENTS) {
            double x = Math.cos(angle) * 0.8;
            double z = Math.sin(angle) * 0.8;
            Location circleLocation = location.clone().add(0.5 + x, 0.1, 0.5 + z);
            player.spawnParticle(Particle.DUST, circleLocation, 1, 0, 0, 0, 0,
                new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.5f));
        }
    }
    
    /**
     * 关闭监听器，清理资源
     */
    public void shutdown() {
        logger.info("Shutting down LandSelectListener...");
        
        // 清理选区数据
        selectLock.writeLock().lock();
        try {
            int clearedCount = selectMap.size();
            selectMap.clear();
            if (clearedCount > 0) {
                logger.info("Cleared " + clearedCount + " player selections");
            }
        } finally {
            selectLock.writeLock().unlock();
        }
        
        logger.info("LandSelectListener shutdown completed");
    }
}
