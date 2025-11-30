package com.example.easyland.listener;

import com.example.easyland.manager.LanguageManager;
import com.example.easyland.service.LandService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;

public class LandSelectListener implements Listener {
    private static final int PARTICLE_LAYERS = 10;
    private static final int CIRCLE_SEGMENTS = 16;

    // 记录每个玩家最近的两次坐标选择
    private final Map<UUID, Location[]> selectMap;
    private final LandService landService;
    private final LanguageManager languageManager;

    public LandSelectListener(LandService landService, LanguageManager languageManager, Map<UUID, Location[]> selectMap) {
        this.landService = landService;
        this.languageManager = languageManager;
        this.selectMap = selectMap;
    }

    @EventHandler
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

        Location[] selects = selectMap.getOrDefault(player.getUniqueId(), new Location[2]);
        selects[0] = selects[1]; // 上一次的变为第一次
        selects[1] = clickedLocation.clone(); // 本次为第二次
        selectMap.put(player.getUniqueId(), selects);

        // 显示选择的坐标
        languageManager.sendMessage(player, "select.point-selected",
            clickedLocation.getBlockX(), clickedLocation.getBlockZ());

        // 添加显眼的粒子效果
        spawnSelectionParticles(clickedLocation, player);

        event.setCancelled(true);
    }

    public Location[] getPlayerSelects(Player player) {
        return selectMap.getOrDefault(player.getUniqueId(), new Location[2]);
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
}
