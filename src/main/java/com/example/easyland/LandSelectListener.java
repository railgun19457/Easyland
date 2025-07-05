package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LandSelectListener implements Listener {
    // 记录每个玩家最近的两次区块选择
    private final Map<UUID, Chunk[]> selectMap = new HashMap<>();
    private final LandManager landManager;

    public LandSelectListener(LandManager landManager) {
        this.landManager = landManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.WOODEN_HOE) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        Component display = meta.displayName();
        if (display == null || !display.equals(Component.text("§a领地选择"))) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Chunk chunk = player.getLocation().getChunk();
        // 检查区块是否已被任何领地占用
        for (ChunkLand land : landManager.getAllClaimedLands()) {
            if (land.contains(chunk)) {
                player.sendMessage("§c该区块已被其他领地占用，无法选点。");
                event.setCancelled(true);
                return;
            }
        }
        for (ChunkLand land : landManager.getAllUnclaimedLands()) {
            if (land.contains(chunk)) {
                player.sendMessage("§c该区块已被其他领地占用，无法选点。");
                event.setCancelled(true);
                return;
            }
        }
        Chunk[] selects = selectMap.getOrDefault(player.getUniqueId(), new Chunk[2]);
        selects[0] = selects[1]; // 上一次的变为第一次
        selects[1] = chunk;     // 本次为第二次
        selectMap.put(player.getUniqueId(), selects);
        player.sendMessage("已选择区块: [" + chunk.getX() + ", " + chunk.getZ() + "]");
        event.setCancelled(true);
    }

    public Chunk[] getPlayerSelects(Player player) {
        return selectMap.getOrDefault(player.getUniqueId(), new Chunk[2]);
    }
}
