package com.example.easyland;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LandManager {
    // 使用ConcurrentHashMap提高并发性能
    private final Map<String, ChunkLand> lands = new ConcurrentHashMap<>();
    private final Map<String, ChunkLand> unclaimedLands = new ConcurrentHashMap<>();

    // 添加空间索引以提高查找性能
    private final Map<String, Set<ChunkLand>> worldLandIndex = new ConcurrentHashMap<>();

    private final File dataFile;
    private final int maxLandsPerPlayer;
    private final int maxChunksPerLand;
    private final Map<String, Boolean> defaultProtectionRules;

    public LandManager(File dataFile, int maxLandsPerPlayer, int maxChunksPerLand,
            Map<String, Boolean> defaultProtectionRules) {
        this.dataFile = dataFile;
        this.maxLandsPerPlayer = maxLandsPerPlayer;
        this.maxChunksPerLand = maxChunksPerLand;
        this.defaultProtectionRules = defaultProtectionRules != null ? defaultProtectionRules : new HashMap<>();
        loadLands();
    }

    /**
     * 添加领地到空间索引
     */
    private void addToIndex(ChunkLand land) {
        worldLandIndex.computeIfAbsent(land.getWorldName(), k -> ConcurrentHashMap.newKeySet()).add(land);
    }

    /**
     * 从空间索引移除领地
     */
    private void removeFromIndex(ChunkLand land) {
        Set<ChunkLand> worldLands = worldLandIndex.get(land.getWorldName());
        if (worldLands != null) {
            worldLands.remove(land);
            if (worldLands.isEmpty()) {
                worldLandIndex.remove(land.getWorldName());
            }
        }
    }

    public void saveLands() {
        YamlConfiguration config = new YamlConfiguration();

        // 保存已认领领地
        saveLandsToConfig(config, "lands", lands.values());

        // 保存未认领领地
        saveLandsToConfig(config, "unclaimed", unclaimedLands.values());

        try {
            config.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存领地集合到配置文件
     */
    private void saveLandsToConfig(YamlConfiguration config, String section, Collection<ChunkLand> landsCollection) {
        int i = 0;
        for (ChunkLand land : landsCollection) {
            String path = section + "." + i++;
            config.set(path + ".owner", land.getOwner());
            config.set(path + ".world", land.getWorldName());
            config.set(path + ".minX", land.getMinX());
            config.set(path + ".maxX", land.getMaxX());
            config.set(path + ".minZ", land.getMinZ());
            config.set(path + ".maxZ", land.getMaxZ());
            config.set(path + ".trusted", new ArrayList<>(land.getTrusted()));
            config.set(path + ".id", land.getId());

            // 保存保护规则
            Map<String, Boolean> rules = land.getProtectionRules();
            for (Map.Entry<String, Boolean> entry : rules.entrySet()) {
                config.set(path + ".protection." + entry.getKey(), entry.getValue());
            }
        }
    }

    public void loadLands() {
        if (!dataFile.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        lands.clear();
        unclaimedLands.clear();
        worldLandIndex.clear();

        // 加载已认领领地
        loadLandsFromConfig(config, "lands", true);

        // 加载未认领领地
        loadLandsFromConfig(config, "unclaimed", false);
    }

    /**
     * 从配置文件加载领地
     */
    private void loadLandsFromConfig(YamlConfiguration config, String section, boolean isClaimed) {
        if (!config.contains(section))
            return;

        for (String key : config.getConfigurationSection(section).getKeys(false)) {
            String path = section + "." + key + ".";

            ChunkLand land = createLandFromConfig(config, path);
            if (land != null) {
                addToIndex(land);

                if (isClaimed) {
                    lands.put(land.getOwner(), land);
                } else {
                    String mapKey = getChunkKey(land);
                    unclaimedLands.put(mapKey, land);
                }
            }
        }
    }

    /**
     * 从配置创建领地对象
     */
    private ChunkLand createLandFromConfig(YamlConfiguration config, String path) {
        try {
            String owner = config.getString(path + "owner");
            String world = config.getString(path + "world");
            int minX = config.getInt(path + "minX");
            int maxX = config.getInt(path + "maxX");
            int minZ = config.getInt(path + "minZ");
            int maxZ = config.getInt(path + "maxZ");
            List<String> trusted = config.getStringList(path + "trusted");
            String id = config.getString(path + "id", "");

            ChunkLand land = new ChunkLand(id, owner, world, minX, maxX, minZ, maxZ, trusted);

            // 加载保护规则
            Map<String, Boolean> protectionRules = new HashMap<>();
            if (config.contains(path + "protection")) {
                for (String ruleName : config.getConfigurationSection(path + "protection").getKeys(false)) {
                    protectionRules.put(ruleName, config.getBoolean(path + "protection." + ruleName));
                }
            }

            // 设置默认规则（如果没有配置）
            for (Map.Entry<String, Boolean> entry : defaultProtectionRules.entrySet()) {
                protectionRules.putIfAbsent(entry.getKey(), entry.getValue());
            }

            land.setProtectionRulesFromMap(protectionRules);
            return land;

        } catch (Exception e) {
            System.err.println("Failed to load land from config path: " + path);
            e.printStackTrace();
            return null;
        }
    }

    public boolean createLandByChunk(Chunk pos1, Chunk pos2, String id) {
        String key = getChunkKey(pos1, pos2);
        if (lands.containsKey(key) || unclaimedLands.containsKey(key))
            return false;

        ChunkLand land = new ChunkLand(id, null, pos1, pos2);
        land.setDefaultProtectionRules(defaultProtectionRules);
        unclaimedLands.put(key, land);
        addToIndex(land);
        saveLands();
        return true;
    }

    public boolean createLandByChunk(Chunk pos1, Chunk pos2) {
        return createLandByChunk(pos1, pos2, "");
    }

    public boolean claimLand(Player player, Chunk pos1, Chunk pos2) {
        String key = getChunkKey(pos1, pos2);
        ChunkLand land = unclaimedLands.get(key);
        if (land == null || land.getOwner() != null)
            return false;

        land.setOwner(player.getUniqueId().toString());
        lands.put(player.getUniqueId().toString(), land);
        unclaimedLands.remove(key);
        saveLands();
        return true;
    }

    public boolean unclaimLand(Player player) {
        ChunkLand land = lands.remove(player.getUniqueId().toString());
        if (land == null)
            return false;

        land.setOwner(null);
        String key = getChunkKey(land);
        unclaimedLands.put(key, land);
        saveLands();
        return true;
    }

    public boolean trustPlayer(Player owner, String trustedUuid) {
        ChunkLand land = lands.get(owner.getUniqueId().toString());
        if (land == null)
            return false;
        return land.trust(trustedUuid);
    }

    public boolean untrustPlayer(Player owner, String trustedUuid) {
        ChunkLand land = lands.get(owner.getUniqueId().toString());
        if (land == null)
            return false;
        return land.untrust(trustedUuid);
    }

    public boolean isTrusted(ChunkLand land, String uuid) {
        return land != null && (uuid.equals(land.getOwner()) || land.getTrusted().contains(uuid));
    }

    public ChunkLand getLand(Player player) {
        return lands.get(player.getUniqueId().toString());
    }

    /**
     * 使用空间索引优化的区块查找
     */
    public ChunkLand getLandByChunk(Chunk chunk) {
        String worldName = chunk.getWorld().getName();
        Set<ChunkLand> worldLands = worldLandIndex.get(worldName);

        if (worldLands == null)
            return null;

        // 在该世界的领地中查找包含此区块的领地
        for (ChunkLand land : worldLands) {
            if (land.contains(chunk)) {
                return land;
            }
        }
        return null;
    }

    public boolean isInLand(Location loc) {
        return getLandByChunk(loc.getChunk()) != null;
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
        return land.getWorldName() + ":" + land.getMinX() + ":" + land.getMaxX() + ":" + land.getMinZ() + ":"
                + land.getMaxZ();
    }

    public boolean canCreateLand(Player player, Chunk pos1, Chunk pos2) {
        // 检查玩家已拥有的领地数
        int count = 0;
        for (ChunkLand land : lands.values()) {
            if (player.getUniqueId().toString().equals(land.getOwner()))
                count++;
        }
        if (count >= maxLandsPerPlayer)
            return false;
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

    public int getMaxLandsPerPlayer() {
        return maxLandsPerPlayer;
    }

    public int getMaxChunksPerLand() {
        return maxChunksPerLand;
    }

    public boolean removeLandById(String id) {
        // 从已认领领地中查找并删除
        for (Iterator<Map.Entry<String, ChunkLand>> it = lands.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ChunkLand> entry = it.next();
            ChunkLand land = entry.getValue();
            if (land.getId() != null && land.getId().equals(id)) {
                removeFromIndex(land);
                it.remove();
                saveLands();
                return true;
            }
        }

        // 从未认领领地中查找并删除
        for (Iterator<Map.Entry<String, ChunkLand>> it = unclaimedLands.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ChunkLand> entry = it.next();
            ChunkLand land = entry.getValue();
            if (land.getId() != null && land.getId().equals(id)) {
                removeFromIndex(land);
                it.remove();
                saveLands();
                return true;
            }
        }
        return false;
    }

    public boolean removeLand(Player player) {
        Chunk playerChunk = player.getLocation().getChunk();
        String playerUuid = player.getUniqueId().toString();

        // 优先删除玩家当前位置的领地
        for (Iterator<Map.Entry<String, ChunkLand>> it = lands.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ChunkLand> entry = it.next();
            ChunkLand land = entry.getValue();
            if (playerUuid.equals(land.getOwner()) && land.contains(playerChunk)) {
                removeFromIndex(land);
                it.remove();
                saveLands();
                return true;
            }
        }

        // 如果当前位置没有领地，删除玩家的任意一个领地
        ChunkLand land = lands.remove(playerUuid);
        if (land != null) {
            removeFromIndex(land);
            saveLands();
            return true;
        }

        return false;
    }

    public boolean claimLandById(Player player, String id) {
        for (Iterator<Map.Entry<String, ChunkLand>> it = unclaimedLands.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ChunkLand> entry = it.next();
            ChunkLand land = entry.getValue();
            if (land.getId() != null && land.getId().equalsIgnoreCase(id)) {
                land.setOwner(player.getUniqueId().toString());
                lands.put(player.getUniqueId().toString(), land);
                it.remove();
                saveLands();
                return true;
            }
        }
        return false;
    }

    public boolean unclaimLandById(Player player, String id) {
        String playerUuid = player.getUniqueId().toString();

        for (Iterator<Map.Entry<String, ChunkLand>> it = lands.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, ChunkLand> entry = it.next();
            ChunkLand land = entry.getValue();
            if (land.getId() != null && land.getId().equalsIgnoreCase(id) && playerUuid.equals(land.getOwner())) {
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

    // 领地保护规则设置
    private final Map<String, Boolean> protectionRules = new HashMap<>();

    public ChunkLand(String id, String owner, Chunk pos1, Chunk pos2) {
        this.id = id;
        this.owner = owner;
        this.worldName = pos1.getWorld().getName();
        this.minX = Math.min(pos1.getX(), pos2.getX());
        this.maxX = Math.max(pos1.getX(), pos2.getX());
        this.minZ = Math.min(pos1.getZ(), pos2.getZ());
        this.maxZ = Math.max(pos1.getZ(), pos2.getZ());
        // 初始化默认保护规则（将在外部设置）
        initializeDefaultRules();
    }

    public ChunkLand(String id, String owner, String worldName, int minX, int maxX, int minZ, int maxZ,
            List<String> trusted) {
        this.id = id;
        this.owner = owner;
        this.worldName = worldName;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        if (trusted != null)
            this.trusted.addAll(trusted);
        // 初始化默认保护规则（将在外部设置）
        initializeDefaultRules();
    }

    /**
     * 初始化默认保护规则
     */
    private void initializeDefaultRules() {
        protectionRules.put("block-protection", false);
        protectionRules.put("explosion-protection", false);
        protectionRules.put("container-protection", false);
        protectionRules.put("player-protection", false);
    }

    /**
     * 设置保护规则的默认值
     */
    public void setDefaultProtectionRules(Map<String, Boolean> defaultRules) {
        for (Map.Entry<String, Boolean> entry : defaultRules.entrySet()) {
            protectionRules.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 获取保护规则状态
     */
    public boolean getProtectionRule(String ruleName) {
        return protectionRules.getOrDefault(ruleName, false);
    }

    /**
     * 设置保护规则状态
     */
    public void setProtectionRule(String ruleName, boolean enabled) {
        protectionRules.put(ruleName, enabled);
    }

    /**
     * 获取所有保护规则
     */
    public Map<String, Boolean> getProtectionRules() {
        return new HashMap<>(protectionRules);
    }

    /**
     * 从Map设置保护规则
     */
    public void setProtectionRulesFromMap(Map<String, Boolean> rules) {
        if (rules != null) {
            protectionRules.putAll(rules);
        }
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(worldName))
            return false;
        int chunkX = loc.getChunk().getX();
        int chunkZ = loc.getChunk().getZ();
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    public boolean contains(Chunk chunk) {
        if (!chunk.getWorld().getName().equals(worldName))
            return false;
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public Set<String> getTrusted() {
        return trusted;
    }

    public boolean trust(String uuid) {
        return trusted.add(uuid);
    }

    public boolean untrust(String uuid) {
        return trusted.remove(uuid);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
