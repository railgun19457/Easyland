package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;

public class LandProtectionListener implements Listener {
    private final LandManager landManager;
    private static final String BYPASS_PERMISSION = "easyland.bypass";

    public LandProtectionListener(LandManager landManager) {
        this.landManager = landManager;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        Location loc = event.getBlock().getLocation();
        Chunk chunk = loc.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        if (land != null && (land.getOwner() == null || !landManager.isTrusted(land, player.getUniqueId().toString()))) {
            player.sendMessage("§c你不能破坏他人的领地！");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onContainerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        Block block = event.getClickedBlock();
        BlockState state = block.getState();
        if (!(state instanceof Container)) return;
        Location loc = block.getLocation();
        Chunk chunk = loc.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        if (land != null && (land.getOwner() == null || !landManager.isTrusted(land, player.getUniqueId().toString()))) {
            player.sendMessage("§c你不能与他人领地内的容器交互！");
            event.setCancelled(true);
        }
    }
}
