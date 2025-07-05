package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;

public class LandProtectionListener implements Listener {
    private final LandManager landManager;
    private static final String BYPASS_PERMISSION = "easyland.bypass";
    private final boolean protectFromMobGriefing;

    public LandProtectionListener(LandManager landManager, boolean protectFromMobGriefing) {
        this.landManager = landManager;
        this.protectFromMobGriefing = protectFromMobGriefing;
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
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        Location loc = event.getBlock().getLocation();
        Chunk chunk = loc.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        if (land != null && (land.getOwner() == null || !landManager.isTrusted(land, player.getUniqueId().toString()))) {
            player.sendMessage("§c你不能在他人的领地内放置方块！");
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

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(BYPASS_PERMISSION)) return;
        Location loc = event.getBlock().getLocation();
        Chunk chunk = loc.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        if (land != null && (land.getOwner() == null || !landManager.isTrusted(land, player.getUniqueId().toString()))) {
            player.sendMessage("§c你不能在他人的领地内倒水或岩浆！");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!protectFromMobGriefing) return;
        Location loc = event.getLocation();
        Chunk chunk = loc.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        if (land != null) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!protectFromMobGriefing) return;
        Location loc = event.getBlock().getLocation();
        Chunk chunk = loc.getChunk();
        ChunkLand land = landManager.getLandByChunk(chunk);
        if (land != null) {
            event.setCancelled(true);
        }
    }
}
