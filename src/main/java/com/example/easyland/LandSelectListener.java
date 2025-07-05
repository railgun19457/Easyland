package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.WOODEN_SHOVEL) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        Component display = meta.displayName();
        if (display == null || !Component.text("领地选择").equals(display)) return;
        if (!event.getAction().toString().contains("RIGHT_CLICK")) return;
        Chunk chunk = player.getLocation().getChunk();
        Chunk[] selects = selectMap.getOrDefault(player.getUniqueId(), new Chunk[2]);
        selects[0] = selects[1]; // 上一次的变为第一次
        selects[1] = chunk;     // 本次为第二次
        selectMap.put(player.getUniqueId(), selects);
        player.sendMessage("已选择区块: [" + chunk.getX() + ", " + chunk.getZ() + "]");
    }

    public Chunk[] getPlayerSelects(Player player) {
        return selectMap.getOrDefault(player.getUniqueId(), new Chunk[2]);
    }
}
