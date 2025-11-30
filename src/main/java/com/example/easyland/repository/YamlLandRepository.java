package com.example.easyland.repository;

import com.example.easyland.domain.Land;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * YAML 实现的领地数据访问层
 * 主要用于从旧格式读取数据和数据迁移
 */
public class YamlLandRepository implements LandRepository {
    private final File dataFile;
    private final Map<Long, Land> landsCache = new HashMap<>();
    private long nextId = 1;

    public YamlLandRepository(File dataFile) {
        this.dataFile = dataFile;
    }

    @Override
    public void initialize() {
        if (!dataFile.exists()) {
            return;
        }
        loadAllLands();
    }

    private void loadAllLands() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        landsCache.clear();

        // 加载已认领领地
        loadLandsFromSection(config, "lands");

        // 加载未认领领地
        loadLandsFromSection(config, "unclaimed");
    }

    private void loadLandsFromSection(YamlConfiguration config, String section) {
        if (!config.contains(section)) {
            return;
        }

        for (String key : config.getConfigurationSection(section).getKeys(false)) {
            String path = section + "." + key + ".";

            try {
                String owner = config.getString(path + "owner");
                String world = config.getString(path + "world");
                int minX = config.getInt(path + "minX");
                int maxX = config.getInt(path + "maxX");
                int minZ = config.getInt(path + "minZ");
                int maxZ = config.getInt(path + "maxZ");

                // 尝试加载Y坐标（新格式），如果不存在则为null（旧格式）
                Integer minY = config.contains(path + "minY") ? config.getInt(path + "minY") : null;
                Integer maxY = config.contains(path + "maxY") ? config.getInt(path + "maxY") : null;

                List<String> trustedList = config.getStringList(path + "trusted");
                String landId = config.getString(path + "id", "");

                Set<String> trusted = new HashSet<>(trustedList);

                // 加载保护规则
                Map<String, Boolean> protectionRules = new HashMap<>();
                if (config.contains(path + "protection")) {
                    for (String ruleName : config.getConfigurationSection(path + "protection").getKeys(false)) {
                        protectionRules.put(ruleName, config.getBoolean(path + "protection." + ruleName));
                    }
                }

                Land land = new Land(nextId, landId, owner, world, minX, maxX, minZ, maxZ, minY, maxY, trusted, protectionRules);
                landsCache.put(nextId, land);
                nextId++;

            } catch (Exception e) {
                System.err.println("Failed to load land from YAML path: " + path);
                e.printStackTrace();
            }
        }
    }

    @Override
    public Land save(Land land) {
        if (land.getId() == null) {
            land.setId(nextId++);
        }
        landsCache.put(land.getId(), land);
        saveToFile();
        return land;
    }

    @Override
    public List<Land> saveAll(List<Land> lands) {
        if (lands == null || lands.isEmpty()) {
            return new ArrayList<>();
        }

        List<Land> savedLands = new ArrayList<>();
        for (Land land : lands) {
            if (land.getId() == null) {
                land.setId(nextId++);
            }
            landsCache.put(land.getId(), land);
            savedLands.add(land);
        }

        // 批量保存时只写入一次文件，提高性能
        saveToFile();
        return savedLands;
    }

    private void saveToFile() {
        YamlConfiguration config = new YamlConfiguration();

        // 分离已认领和未认领的领地
        List<Land> claimedLands = landsCache.values().stream()
                .filter(Land::isClaimed)
                .collect(Collectors.toList());

        List<Land> unclaimedLands = landsCache.values().stream()
                .filter(land -> !land.isClaimed())
                .collect(Collectors.toList());

        // 保存已认领领地
        saveLandsToSection(config, "lands", claimedLands);

        // 保存未认领领地
        saveLandsToSection(config, "unclaimed", unclaimedLands);

        try {
            config.save(dataFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save lands to YAML", e);
        }
    }

    private void saveLandsToSection(YamlConfiguration config, String section, List<Land> lands) {
        int i = 0;
        for (Land land : lands) {
            String path = section + "." + i++ + ".";
            config.set(path + "owner", land.getOwner());
            config.set(path + "world", land.getWorldName());
            config.set(path + "minX", land.getMinX());
            config.set(path + "maxX", land.getMaxX());
            config.set(path + "minZ", land.getMinZ());
            config.set(path + "maxZ", land.getMaxZ());

            // 保存Y坐标（如果存在）
            if (land.getMinY() != null) {
                config.set(path + "minY", land.getMinY());
            }
            if (land.getMaxY() != null) {
                config.set(path + "maxY", land.getMaxY());
            }

            config.set(path + "trusted", new ArrayList<>(land.getTrusted()));
            config.set(path + "id", land.getLandId());

            // 保存保护规则
            Map<String, Boolean> rules = land.getProtectionRules();
            for (Map.Entry<String, Boolean> entry : rules.entrySet()) {
                config.set(path + "protection." + entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public Optional<Land> findById(Long id) {
        return Optional.ofNullable(landsCache.get(id));
    }

    @Override
    public Optional<Land> findByLandId(String landId) {
        return landsCache.values().stream()
                .filter(land -> landId.equals(land.getLandId()))
                .findFirst();
    }

    @Override
    public List<Land> findByOwner(String ownerUuid) {
        return landsCache.values().stream()
                .filter(land -> ownerUuid.equals(land.getOwner()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Land> findByWorld(String worldName) {
        return landsCache.values().stream()
                .filter(land -> worldName.equals(land.getWorldName()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Land> findByChunk(Chunk chunk) {
        return landsCache.values().stream()
                .filter(land -> land.contains(chunk))
                .findFirst();
    }

    @Override
    public List<Land> findAllClaimed() {
        return landsCache.values().stream()
                .filter(Land::isClaimed)
                .collect(Collectors.toList());
    }

    @Override
    public List<Land> findAllUnclaimed() {
        return landsCache.values().stream()
                .filter(land -> !land.isClaimed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Land> findAll() {
        return new ArrayList<>(landsCache.values());
    }

    @Override
    public boolean deleteById(Long id) {
        boolean removed = landsCache.remove(id) != null;
        if (removed) {
            saveToFile();
        }
        return removed;
    }

    @Override
    public boolean deleteByLandId(String landId) {
        Optional<Land> land = findByLandId(landId);
        if (land.isPresent()) {
            return deleteById(land.get().getId());
        }
        return false;
    }

    @Override
    public int countByOwner(String ownerUuid) {
        return (int) landsCache.values().stream()
                .filter(land -> ownerUuid.equals(land.getOwner()))
                .count();
    }

    @Override
    public boolean existsByLandId(String landId) {
        return landsCache.values().stream()
                .anyMatch(land -> landId.equals(land.getLandId()));
    }

    @Override
    public void close() {
        // YAML 不需要关闭连接
    }
}
