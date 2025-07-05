package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class LandManager {
    private final Map<String, ChunkLand> lands = new HashMap<>();

    public boolean createLandByChunk(Player player, Chunk pos1, Chunk pos2) {
        if (lands.containsKey(player.getUniqueId().toString())) return false;
        lands.put(player.getUniqueId().toString(), new ChunkLand(player.getUniqueId().toString(), pos1, pos2));
        return true;
    }

    public ChunkLand getLand(Player player) {
        return lands.get(player.getUniqueId().toString());
    }

    public boolean isInLand(Location loc) {
        for (ChunkLand land : lands.values()) {
            if (land.contains(loc)) return true;
        }
        return false;
    }
}

class ChunkLand {
    private final String owner;
    private final String worldName;
    private final int minX, maxX, minZ, maxZ;

    public ChunkLand(String owner, Chunk pos1, Chunk pos2) {
        this.owner = owner;
        this.worldName = pos1.getWorld().getName();
        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int chunkX = loc.getChunk().getX();
        int chunkZ = loc.getChunk().getZ();
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    public String getOwner() { return owner; }
}
