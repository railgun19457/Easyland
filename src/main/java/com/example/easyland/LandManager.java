package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class LandManager {
    private final Map<String, ChunkLand> lands = new HashMap<>();
    private final Map<String, ChunkLand> unclaimedLands = new HashMap<>();
    private final File dataFile;
    private final int maxLandsPerPlayer;
    private final int maxChunksPerLand;

    public LandManager(File dataFile, int maxLandsPerPlayer, int maxChunksPerLand) {
        this.dataFile = dataFile;
        this.maxLandsPerPlayer = maxLandsPerPlayer;
        this.maxChunksPerLand = maxChunksPerLand;
        loadLands();
    }

    public void saveLands() {
        YamlConfiguration config = new YamlConfiguration();
        int i = 0;
        for (ChunkLand land : lands.values()) {
            String path = "lands." + i++;
            config.set(path + ".owner", land.getOwner());
            config.set(path + ".world", land.getWorldName());
            config.set(path + ".minX", land.getMinX());
            config.set(path + ".maxX", land.getMaxX());
            config.set(path + ".minZ", land.getMinZ());
            config.set(path + ".maxZ", land.getMaxZ());
            config.set(path + ".trusted", new ArrayList<>(land.getTrusted()));
            config.set(path + ".id", land.getId());
        }
        i = 0;
        for (ChunkLand land : unclaimedLands.values()) {
            String path = "unclaimed." + i++;
            config.set(path + ".owner", land.getOwner());
            config.set(path + ".world", land.getWorldName());
            config.set(path + ".minX", land.getMinX());
            config.set(path + ".maxX", land.getMaxX());
            config.set(path + ".minZ", land.getMinZ());
            config.set(path + ".maxZ", land.getMaxZ());
            config.set(path + ".trusted", new ArrayList<>(land.getTrusted()));
            config.set(path + ".id", land.getId());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadLands() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        lands.clear();
        unclaimedLands.clear();
        if (config.contains("lands")) {
            for (String key : config.getConfigurationSection("lands").getKeys(false)) {
                String path = "lands." + key + ".";
                String owner = config.getString(path + "owner");
                String world = config.getString(path + "world");
                int minX = config.getInt(path + "minX");
                int maxX = config.getInt(path + "maxX");
                int minZ = config.getInt(path + "minZ");
                int maxZ = config.getInt(path + "maxZ");
                List<String> trusted = config.getStringList(path + "trusted");
                String id = config.getString(path + "id", "");
                ChunkLand land = new ChunkLand(id, owner, world, minX, maxX, minZ, maxZ, trusted);
                lands.put(owner, land);
            }
        }
        if (config.contains("unclaimed")) {
            for (String key : config.getConfigurationSection("unclaimed").getKeys(false)) {
                String path = "unclaimed." + key + ".";
                String owner = config.getString(path + "owner");
                String world = config.getString(path + "world");
                int minX = config.getInt(path + "minX");
                int maxX = config.getInt(path + "maxX");
                int minZ = config.getInt(path + "minZ");
                int maxZ = config.getInt(path + "maxZ");
                List<String> trusted = config.getStringList(path + "trusted");
                String id = config.getString(path + "id", "");
                ChunkLand land = new ChunkLand(id, owner, world, minX, maxX, minZ, maxZ, trusted);
                unclaimedLands.put(world + ":" + minX + ":" + maxX + ":" + minZ + ":" + maxZ, land);
            }
        }
    }

    public boolean createLandByChunk(Chunk pos1, Chunk pos2) {
        return createLandByChunk(pos1, pos2, "");
    }

    public boolean createLandByChunk(Chunk pos1, Chunk pos2, String id) {
        String key = getChunkKey(pos1, pos2);
        if (lands.containsKey(key) || unclaimedLands.containsKey(key)) return false;
        unclaimedLands.put(key, new ChunkLand(id, null, pos1, pos2));
        saveLands();
        return true;
    }

    public boolean claimLand(Player player, Chunk pos1, Chunk pos2) {
        String key = getChunkKey(pos1, pos2);
        ChunkLand land = unclaimedLands.get(key);
        if (land == null || land.getOwner() != null) return false;
        land.setOwner(player.getUniqueId().toString());
        lands.put(player.getUniqueId().toString(), land);
        unclaimedLands.remove(key);
        saveLands(); // 认领后立即保存
        return true;
    }

    public boolean unclaimLand(Player player) {
        ChunkLand land = lands.remove(player.getUniqueId().toString());
        if (land == null) return false;
        land.setOwner(null);
        String key = getChunkKey(land);
        unclaimedLands.put(key, land);
        return true;
    }

    public boolean trustPlayer(Player owner, String trustedUuid) {
        ChunkLand land = lands.get(owner.getUniqueId().toString());
        if (land == null) return false;
        return land.trust(trustedUuid);
    }

    public boolean untrustPlayer(Player owner, String trustedUuid) {
        ChunkLand land = lands.get(owner.getUniqueId().toString());
        if (land == null) return false;
        return land.untrust(trustedUuid);
    }

    public boolean isTrusted(ChunkLand land, String uuid) {
        return land != null && (uuid.equals(land.getOwner()) || land.getTrusted().contains(uuid));
    }

    public ChunkLand getLand(Player player) {
        return lands.get(player.getUniqueId().toString());
    }

    public ChunkLand getLandByChunk(Chunk chunk) {
        for (ChunkLand land : lands.values()) {
            if (land.contains(chunk)) return land;
        }
        return null;
    }

    public boolean isInLand(Location loc) {
        for (ChunkLand land : lands.values()) {
            if (land.contains(loc)) return true;
        }
        return false;
    }

    private String getChunkKey(Chunk pos1, Chunk pos2) {
        String world = pos1.getWorld().getName();
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return world + ":" + minX + ":" + maxX + ":" + minZ + ":" + maxZ;
    }
    private String getChunkKey(ChunkLand land) {
        return land.getWorldName() + ":" + land.getMinX() + ":" + land.getMaxX() + ":" + land.getMinZ() + ":" + land.getMaxZ();
    }

    public boolean canCreateLand(Player player, Chunk pos1, Chunk pos2) {
        // 检查玩家已拥有的领地数
        int count = 0;
        for (ChunkLand land : lands.values()) {
            if (player.getUniqueId().toString().equals(land.getOwner())) count++;
        }
        if (count >= maxLandsPerPlayer) return false;
        // 检查新领地区块数
        int chunkCount = (Math.abs(pos1.getX() - pos2.getX()) + 1) * (Math.abs(pos1.getZ() - pos2.getZ()) + 1);
        return chunkCount <= maxChunksPerLand;
    }

    public Collection<ChunkLand> getAllUnclaimedLands() {
        return unclaimedLands.values();
    }

    public Collection<ChunkLand> getAllClaimedLands() {
        return lands.values();
    }

    public int getMaxLandsPerPlayer() { return maxLandsPerPlayer; }
    public int getMaxChunksPerLand() { return maxChunksPerLand; }

    public boolean removeLandById(String id) {
        for (Iterator<ChunkLand> it = lands.values().iterator(); it.hasNext(); ) {
            ChunkLand land = it.next();
            if (land.getId() != null && land.getId().equals(id)) {
                it.remove();
                saveLands();
                return true;
            }
        }
        for (Iterator<ChunkLand> it = unclaimedLands.values().iterator(); it.hasNext(); ) {
            ChunkLand land = it.next();
            if (land.getId() != null && land.getId().equals(id)) {
                it.remove();
                saveLands();
                return true;
            }
        }
        return false;
    }

    public boolean removeLand(Player player) {
        Chunk playerChunk = player.getLocation().getChunk();
        for (Iterator<ChunkLand> it = lands.values().iterator(); it.hasNext(); ) {
            ChunkLand land = it.next();
            if (player.getUniqueId().toString().equals(land.getOwner()) && land.contains(playerChunk)) {
                it.remove();
                saveLands();
                return true;
            }
        }
        for (Iterator<ChunkLand> it = lands.values().iterator(); it.hasNext(); ) {
            ChunkLand land = it.next();
            if (player.getUniqueId().toString().equals(land.getOwner())) {
                it.remove();
                saveLands();
                return true;
            }
        }
        return false;
    }

    public boolean claimLandById(Player player, String id) {
        for (Map.Entry<String, ChunkLand> entry : unclaimedLands.entrySet()) {
            ChunkLand land = entry.getValue();
            if (land.getId() != null && land.getId().equalsIgnoreCase(id)) {
                land.setOwner(player.getUniqueId().toString());
                lands.put(player.getUniqueId().toString(), land);
                unclaimedLands.remove(entry.getKey());
                saveLands();
                return true;
            }
        }
        return false;
    }

    public boolean unclaimLandById(Player player, String id) {
        for (Iterator<Map.Entry<String, ChunkLand>> it = lands.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, ChunkLand> entry = it.next();
            ChunkLand land = entry.getValue();
            if (land.getId() != null && land.getId().equalsIgnoreCase(id) && player.getUniqueId().toString().equals(land.getOwner())) {
                land.setOwner(null);
                String key = getChunkKey(land);
                unclaimedLands.put(key, land);
                it.remove();
                saveLands();
                return true;
            }
        }
        return false;
    }
}

class ChunkLand {
    private String id;
    private String owner;
    private final String worldName;
    private final int minX, maxX, minZ, maxZ;
    private final Set<String> trusted = new HashSet<>();

    public ChunkLand(String id, String owner, Chunk pos1, Chunk pos2) {
        this.id = id;
        this.owner = owner;
        this.worldName = pos1.getWorld().getName();
        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
    }
    public ChunkLand(String id, String owner, String worldName, int minX, int maxX, int minZ, int maxZ, List<String> trusted) {
        this.id = id;
        this.owner = owner;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        if (trusted != null) this.trusted.addAll(trusted);
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int chunkX = loc.getChunk().getX();
        int chunkZ = loc.getChunk().getZ();
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    public boolean contains(Chunk chunk) {
        if (!chunk.getWorld().getName().equals(worldName)) return false;
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMaxX() { return maxX; }
    public int getMinZ() { return minZ; }
    public int getMaxZ() { return maxZ; }
    public Set<String> getTrusted() { return trusted; }
    public boolean trust(String uuid) { return trusted.add(uuid); }
    public boolean untrust(String uuid) { return trusted.remove(uuid); }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}
