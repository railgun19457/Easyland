package com.example.easyland;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LandEnterListener implements Listener {
    private final LandManager landManager;
    private final Map<UUID, String> lastLandKey = new HashMap<>();

    public LandEnterListener(LandManager landManager) {
        this.landManager = landManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Chunk toChunk = event.getTo().getChunk();
        Chunk fromChunk = event.getFrom().getChunk();
        if (toChunk.equals(fromChunk)) return;
        ChunkLand toLand = landManager.getLandByChunk(toChunk);
        ChunkLand fromLand = landManager.getLandByChunk(fromChunk);
        String toKey = toLand == null ? "" : toLand.getOwner();
        String fromKey = fromLand == null ? "" : fromLand.getOwner();
        String lastKey = lastLandKey.getOrDefault(player.getUniqueId(), "");
        if (toKey != null && !toKey.isEmpty() && !toKey.equals(lastKey)) {
            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(toKey)).getName();
            player.sendMessage("§a你已进入 " + (ownerName != null ? ownerName : "未知") + " 的领地");
        }
        if ((fromKey != null && !fromKey.isEmpty()) && !fromKey.equals(toKey)) {
            String ownerName = Bukkit.getOfflinePlayer(UUID.fromString(fromKey)).getName();
            player.sendMessage("§e你已离开 " + (ownerName != null ? ownerName : "未知") + " 的领地");
        }
        lastLandKey.put(player.getUniqueId(), toKey);
    }
}
