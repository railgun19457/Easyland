package io.github.railgun19457.easyland.migration;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.core.ConfigManager;
import io.github.railgun19457.easyland.exception.MigrationFileNotFoundException;
import io.github.railgun19457.easyland.model.Land;
import io.github.railgun19457.easyland.storage.DatabaseManager;
import io.github.railgun19457.easyland.storage.LandDAO;
import io.github.railgun19457.easyland.storage.LandTrustDAO;
import io.github.railgun19457.easyland.storage.PlayerDAO;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * 管理从旧版YAML文件到新版SQLite数据库的数据迁移。
 */
public class MigrationManager {
    private final EasyLand plugin;
    private final Logger logger;
    private final I18nManager i18nManager;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    @SuppressWarnings("unused")
    private final PlayerDAO playerDAO;
    private final LandDAO landDAO;
    @SuppressWarnings("unused")
    private final LandTrustDAO landTrustDAO;

    // 玩家UUID到数据库ID的缓存，避免重复查询
    private final Map<UUID, Integer> playerIdCache = new HashMap<>();

    /**
     * 构造函数。
     *
     * @param plugin 插件主类实例
     */
    public MigrationManager(EasyLand plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.i18nManager = plugin.getI18nManager();
        this.configManager = plugin.getConfigManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.playerDAO = plugin.getPlayerDAO();
        this.landDAO = plugin.getLandDAO();
        this.landTrustDAO = plugin.getLandTrustDAO();
    }
    
    /**
     * 检查所有必需的旧数据文件是否存在。
     *
     * @throws MigrationFileNotFoundException 如果任何必需文件不存在
     */
    private void checkRequiredFiles() throws MigrationFileNotFoundException {
        List<String> missingFiles = new ArrayList<>();
        
        // 检查领地数据文件
        File landsFile = new File(plugin.getDataFolder(), "lands.yml");
        if (!landsFile.exists()) {
            missingFiles.add("lands.yml");
        }
        
        // 检查配置文件（可选，但建议存在）
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            logger.warning("未找到旧版配置文件 config.yml，将跳过配置迁移。");
        }
        
        // 如果有缺失的必需文件，抛出异常
        if (!missingFiles.isEmpty()) {
            // 只抛出第一个缺失文件的异常，但消息中包含所有缺失文件
            throw new MigrationFileNotFoundException(missingFiles.get(0));
        }
    }
    
    /**
     * 执行完整的数据迁移流程。
     *
     * @param sender 发起迁移的命令发送者
     * @return 迁移是否成功
     */
    public boolean runMigration(CommandSender sender) {
        sender.sendMessage(i18nManager.getMessage("migrate.started"));
        logger.info("开始数据迁移...");
        
        long startTime = System.currentTimeMillis();
        
        // 使用try-with-resources来管理数据库连接
        try (Connection conn = databaseManager.createNewConnection()) {
            // 0. 检查必需的文件是否存在
            sender.sendMessage(i18nManager.getMessage("migrate.checking-files"));
            checkRequiredFiles();
            sender.sendMessage(i18nManager.getMessage("migrate.files-checked"));
            
            // 1. 迁移配置文件
            sender.sendMessage(i18nManager.getMessage("migrate.migrating-config"));
            migrateConfig();
            sender.sendMessage(i18nManager.getMessage("migrate.config-migrated"));
            
            // 2. 迁移领地数据，传入新创建的连接
            sender.sendMessage(i18nManager.getMessage("migrate.migrating-lands"));
            int migratedLands = migrateLands(sender, conn);
            sender.sendMessage(i18nManager.getMessage("migrate.lands-migrated", String.valueOf(migratedLands)));
            
            long duration = System.currentTimeMillis() - startTime;
            
            // 在所有迁移操作完成后，重载配置和重新初始化相关组件
            sender.sendMessage(i18nManager.getMessage("migrate.reloading-config"));
            configManager.reloadConfig();
            sender.sendMessage(i18nManager.getMessage("migrate.config-reloaded"));
            
            sender.sendMessage(i18nManager.getMessage("migrate.success", String.valueOf(duration / 1000.0)));
            logger.info("数据迁移成功完成，共迁移 " + migratedLands + " 个领地，耗时: " + (duration / 1000.0) + " 秒。");
            
            return true;
        } catch (MigrationFileNotFoundException e) {
            sender.sendMessage(i18nManager.getMessage("migrate.failed-with-reason", e.getMessage()));
            logger.severe("数据迁移失败: " + e.getMessage());
            return false;
        } catch (Exception e) {
            sender.sendMessage(i18nManager.getMessage("migrate.failed-with-reason", e.getMessage()));
            logger.severe("数据迁移失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 迁移配置文件。
     */
    private void migrateConfig() throws IOException {
        File oldConfigFile = new File(plugin.getDataFolder(), "config.yml");
        if (!oldConfigFile.exists()) {
            logger.info("未找到旧版配置文件，跳过配置迁移。");
            return;
        }
        
        // 1. 从磁盘加载旧配置文件
        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(oldConfigFile);
        
        // 2. 从JAR包中加载默认配置文件
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream == null) {
            throw new IOException("无法从JAR包中加载默认配置文件 config.yml");
        }
        FileConfiguration newDefaultConfig = YamlConfiguration.loadConfiguration(
            new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
        
        // 3. 创建最终配置对象，作为新默认配置的副本
        FileConfiguration finalConfig = new YamlConfiguration();
        // 复制新默认配置的所有内容到最终配置
        for (String key : newDefaultConfig.getKeys(true)) {
            finalConfig.set(key, newDefaultConfig.get(key));
        }
        
        // 4. 使用旧配置的值更新最终配置
        int[] counters = new int[2]; // [migratedCount, ignoredCount]
        migrateConfigValues(oldConfig, finalConfig, "", counters);
        
        // 5. 将最终配置保存到磁盘
        finalConfig.save(oldConfigFile);
        
        logger.info("配置文件迁移完成。共迁移 " + counters[0] + " 个配置项，保持 " + counters[1] + " 个新版本默认配置项。");
    }
    
    /**
     * 递归迁移配置值
     *
     * @param oldConfig 旧配置文件
     * @param newConfig 新配置文件
     * @param path 当前配置路径
     * @param counters 计数器数组，[migratedCount, ignoredCount]
     */
    private void migrateConfigValues(FileConfiguration oldConfig, FileConfiguration newConfig,
                                   String path, int[] counters) {
        // 获取新配置中的所有键（这是关键改变）
        for (String key : newConfig.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            // 检查旧配置中是否存在此键
            if (oldConfig.contains(key)) {
                // 获取旧配置中的值
                Object value = oldConfig.get(key);
                
                // 如果是配置节，递归处理子项
                if (value instanceof org.bukkit.configuration.ConfigurationSection) {
                    org.bukkit.configuration.ConfigurationSection oldSection = oldConfig.getConfigurationSection(key);
                    org.bukkit.configuration.ConfigurationSection newSection = newConfig.getConfigurationSection(key);
                    if (oldSection != null && newSection != null) {
                        migrateConfigValues(oldSection, newSection, fullPath, counters);
                    }
                } else {
                    // 如果不是配置节（即原始值），则用旧值覆盖新值
                    newConfig.set(key, value);
                    counters[0]++; // 增加迁移计数
                    logger.info("已迁移配置项: " + fullPath + " = " + value);
                }
            } else {
                // 如果旧配置中不存在此键，说明是新版本的配置项，保持默认值
                counters[1]++; // 增加保持默认值计数
                logger.info("保持新版本默认配置项: " + fullPath + " = " + newConfig.get(key));
            }
        }
    }
    
    /**
     * 重载的递归迁移配置值方法，用于处理ConfigurationSection
     */
    private void migrateConfigValues(org.bukkit.configuration.ConfigurationSection oldSection,
                                   org.bukkit.configuration.ConfigurationSection newSection,
                                   String path, int[] counters) {
        // 获取新配置节中的所有键（这是关键改变）
        for (String key : newSection.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            // 检查旧配置节中是否存在此键
            if (oldSection.isSet(key)) {
                // 获取旧配置中的值
                Object value = oldSection.get(key);
                
                // 如果是配置节，递归处理子项
                if (value instanceof org.bukkit.configuration.ConfigurationSection) {
                    org.bukkit.configuration.ConfigurationSection oldSubSection = oldSection.getConfigurationSection(key);
                    org.bukkit.configuration.ConfigurationSection newSubSection = newSection.getConfigurationSection(key);
                    if (oldSubSection != null && newSubSection != null) {
                        migrateConfigValues(oldSubSection, newSubSection, fullPath, counters);
                    }
                } else {
                    // 如果不是配置节（即原始值），则用旧值覆盖新值
                    newSection.set(key, value);
                    counters[0]++; // 增加迁移计数
                    logger.info("已迁移配置项: " + fullPath + " = " + value);
                }
            } else {
                // 如果旧配置中不存在此键，说明是新版本的配置项，保持默认值
                counters[1]++; // 增加保持默认值计数
                logger.info("保持新版本默认配置项: " + fullPath + " = " + newSection.get(key));
            }
        }
    }
    
    /**
     * 迁移领地数据。
     *
     * @param sender 发起迁移的命令发送者
     * @return 迁移的领地数量
     */
    private int migrateLands(CommandSender sender, Connection conn) throws SQLException, IOException {
        File oldLandsFile = new File(plugin.getDataFolder(), "lands.yml");
        if (!oldLandsFile.exists()) {
            throw new IOException("未找到旧版领地数据文件: lands.yml");
        }

        // 读取旧领地数据
        FileConfiguration oldLandsConfig = YamlConfiguration.loadConfiguration(oldLandsFile);

        if (!oldLandsConfig.contains("lands")) {
            logger.info("旧版领地数据文件中没有领地数据，跳过数据迁移。");
            return 0;
        }

        // 获取领地配置节
        org.bukkit.configuration.ConfigurationSection landsSection = oldLandsConfig.getConfigurationSection("lands");
        if (landsSection == null) {
            logger.warning("无法获取领地配置节，跳过数据迁移。");
            return 0;
        }

        int migratedCount = 0;

        // 设置连接为手动提交模式
        conn.setAutoCommit(false);

        int landIndex = 0; // 用于生成备用ID的索引

        try {
            // 遍历所有领地键
            for (String key : landsSection.getKeys(false)) {
                landIndex++;

                try {
                    // 获取领地数据
                    Object landValue = landsSection.get(key);
                    Map<String, Object> landData;

                    // 检查是否是 ConfigurationSection 类型
                    if (landValue instanceof org.bukkit.configuration.ConfigurationSection) {
                        // 如果是 ConfigurationSection，转换为 Map
                        logger.info("领地索引 " + landIndex + " 是 ConfigurationSection 类型，正在转换为 Map...");
                        landData = ((org.bukkit.configuration.ConfigurationSection) landValue).getValues(false);
                    }
                    // 检查是否已经是 Map 类型
                    else if (landValue instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> castedMap = (Map<String, Object>) landValue;
                        landData = castedMap;
                    }
                    // 类型不匹配，记录警告并跳过
                    else {
                        logger.warning("领地索引 " + landIndex + " 的数据格式不正确，期望为Map或ConfigurationSection但得到: " +
                                     (landValue != null ? landValue.getClass().getSimpleName() : "null") + "，跳过迁移。");
                        continue;
                    }

                    // 解析领地名称
                    // 旧版数据中，"id" 字段实际存储的是领地名称（如 "小恶龙之家"）
                    String landName = null;
                    if (landData.containsKey("id") && landData.get("id") != null) {
                        landName = String.valueOf(landData.get("id"));
                    }
                    // 如果没有 id 字段，尝试 name 字段
                    else if (landData.containsKey("name") && landData.get("name") != null) {
                        landName = String.valueOf(landData.get("name"));
                    }

                    // 确定数据库中使用的领地ID（从YAML键获取）
                    int landId;
                    try {
                        landId = Integer.parseInt(key);
                    } catch (NumberFormatException e) {
                        // 如果YAML键不是数字，使用索引作为ID
                        landId = landIndex;
                        logger.warning("领地键 '" + key + "' 不是数字，使用索引 " + landIndex + " 作为数据库ID");
                    }

                    // 如果没有领地名称，生成一个默认名称
                    if (landName == null || landName.isEmpty()) {
                        landName = "领地_" + landId;
                        logger.warning("领地 ID " + landId + " 没有名称，使用生成的名称: " + landName);
                    }

                    // 检查是否已存在该领地（幂等性）
                    if (landDAO.getLandById(landId).isPresent()) {
                        logger.info("领地 ID " + landId + " (" + landName + ") 已存在，跳过迁移。");
                        continue;
                    }

                    // 验证必需的字段
                    if (!landData.containsKey("owner") || landData.get("owner") == null) {
                        logger.warning("领地 " + landName + " 缺少所有者信息，跳过迁移。");
                        continue;
                    }

                    if (!landData.containsKey("world") || landData.get("world") == null) {
                        logger.warning("领地 " + landName + " 缺少世界信息，跳过迁移。");
                        continue;
                    }

                    // 处理所有者
                    UUID ownerUuid;
                    try {
                        ownerUuid = UUID.fromString(String.valueOf(landData.get("owner")));
                    } catch (IllegalArgumentException e) {
                        logger.warning("领地 " + landName + " 的所有者UUID格式不正确: " + landData.get("owner") + "，跳过迁移。");
                        continue;
                    }
                    int ownerId = getOrCreatePlayer(conn, ownerUuid);

                    // 获取区块坐标信息，提供默认值
                    int chunkMinX = landData.containsKey("minX") ? (Integer) landData.get("minX") : 0;
                    int chunkMaxX = landData.containsKey("maxX") ? (Integer) landData.get("maxX") : 0;
                    int chunkMinZ = landData.containsKey("minZ") ? (Integer) landData.get("minZ") : 0;
                    int chunkMaxZ = landData.containsKey("maxZ") ? (Integer) landData.get("maxZ") : 0;

                    // 将区块坐标转换为世界坐标
                    // 一个区块坐标 (x, z) 对应一个 16x16 的区域
                    // 世界坐标范围: minX = x * 16, maxX = x * 16 + 15
                    int worldMinX = chunkMinX * 16;
                    int worldMaxX = chunkMaxX * 16 + 15;
                    int worldMinZ = chunkMinZ * 16;
                    int worldMaxZ = chunkMaxZ * 16 + 15;

                    String world = String.valueOf(landData.get("world"));

                    logger.info("正在迁移领地: ID=" + landId + ", 名称=" + landName + ", 世界=" + world +
                               ", 所有者=" + ownerUuid +
                               ", 区块坐标=(" + chunkMinX + "," + chunkMinZ + ")-(" + chunkMaxX + "," + chunkMaxZ + ")" +
                               ", 世界坐标=(" + worldMinX + "," + worldMinZ + ")-(" + worldMaxX + "," + worldMaxZ + ")");

                    // 创建领地对象，使用转换后的世界坐标
                    Land land = new Land(landId, landName, world, worldMinX, worldMinZ, worldMaxX, worldMaxZ, ownerId);

                    // 插入领地数据（使用指定的ID）
                    insertLandWithId(conn, land);

                    // 处理信任玩家
                    Object trustedRaw = landData.get("trusted");
                    List<String> trustedPlayers = null;
                    if (trustedRaw instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<String> castedTrusted = (List<String>) trustedRaw;
                        trustedPlayers = castedTrusted;
                    }
                    if (trustedPlayers != null && !trustedPlayers.isEmpty()) {
                        logger.info("领地 " + landName + " 有 " + trustedPlayers.size() + " 个信任玩家");
                        for (String trustedUuidStr : trustedPlayers) {
                            try {
                                UUID trustedUuid = UUID.fromString(trustedUuidStr);
                                int trustedPlayerId = getOrCreatePlayer(conn, trustedUuid);

                                // 直接插入信任关系，使用当前连接
                                insertLandTrust(conn, landId, trustedPlayerId);
                            } catch (IllegalArgumentException e) {
                                logger.warning("领地 " + landName + " 的信任玩家UUID格式不正确: " + trustedUuidStr + "，跳过该玩家。");
                            }
                        }
                    }

                    // 处理保护规则
                    Object protectionRaw = landData.get("protection");
                    Map<String, Object> protection = null;
                    if (protectionRaw instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> castedProtection = (Map<String, Object>) protectionRaw;
                        protection = castedProtection;
                    }
                    
                    // 准备所有标志的默认值
                    java.util.Map<String, Boolean> finalFlags = new java.util.HashMap<>();
                    for (io.github.railgun19457.easyland.model.LandFlag flag : io.github.railgun19457.easyland.model.LandFlag.values()) {
                        finalFlags.put(flag.getName(), configManager.getDefaultRuleValue(flag.getName()));
                    }
                    
                    if (protection != null && !protection.isEmpty()) {
                        logger.info("领地 " + landName + " 有 " + protection.size() + " 个保护规则");
                        for (Map.Entry<String, Object> protectionEntry : protection.entrySet()) {
                            String flagName = protectionEntry.getKey();
                            boolean isProtected = (Boolean) protectionEntry.getValue();

                            // 将旧的保护规则名称映射到新的
                            List<String> newFlags = mapProtectionFlag(flagName);
                            if (!newFlags.isEmpty()) {
                                // 旧系统: true = 保护开启 = 禁止操作 (allow = false)
                                // 新系统: true = 允许操作
                                // 所以: new_value = !old_value
                                boolean allow = !isProtected;
                                
                                for (String newFlag : newFlags) {
                                    finalFlags.put(newFlag, allow);
                                    logger.info("为领地 " + landName + " 设置规则: " + newFlag + " = " + allow);
                                }
                            }
                        }
                    }
                    
                    // 插入所有标志
                    for (Map.Entry<String, Boolean> entry : finalFlags.entrySet()) {
                        insertLandFlag(conn, landId, entry.getKey(), entry.getValue());
                    }

                    migratedCount++;

                    // 每迁移10个领地报告一次进度
                    if (migratedCount % 10 == 0) {
                        sender.sendMessage(i18nManager.getMessage("migrate.lands-progress", String.valueOf(migratedCount)));
                    }

                } catch (Exception e) {
                    logger.warning("迁移领地索引 " + landIndex + " 时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 提交事务
            conn.commit();
            logger.info("领地数据迁移完成，共迁移 " + migratedCount + " 个领地。");
            return migratedCount;

        } catch (Exception e) {
            // 回滚事务
            try {
                conn.rollback();
            } catch (SQLException rollbackEx) {
                logger.severe("回滚事务时出错: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            // 恢复自动提交模式
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.warning("恢复自动提交模式时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取或创建玩家记录，并返回其数据库ID。
     * 使用提供的连接以支持事务。
     *
     * @param conn 数据库连接
     * @param uuid 玩家UUID
     * @return 玩家数据库ID
     */
    private int getOrCreatePlayer(Connection conn, UUID uuid) throws SQLException {
        // 先从缓存中查找
        if (playerIdCache.containsKey(uuid)) {
            return playerIdCache.get(uuid);
        }

        // 尝试获取真实名字
        String realName = null;
        org.bukkit.OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(uuid);
        if (offlinePlayer != null && offlinePlayer.getName() != null) {
            realName = offlinePlayer.getName();
        }

        // 从数据库中查找或创建
        String selectSql = "SELECT * FROM players WHERE uuid = ?";
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, uuid.toString());

            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int playerId = rs.getInt("id");
                    String currentName = rs.getString("name");
                    
                    // 如果当前名字是 Unknown 且我们获取到了真实名字，则更新
                    if (realName != null && ("Unknown".equals(currentName) || currentName == null)) {
                         String updateSql = "UPDATE players SET name = ? WHERE id = ?";
                         try (java.sql.PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                             updateStmt.setString(1, realName);
                             updateStmt.setInt(2, playerId);
                             updateStmt.executeUpdate();
                             logger.info("更新玩家 " + uuid + " 的名字为 " + realName);
                         }
                    }

                    playerIdCache.put(uuid, playerId);
                    return playerId;
                }
            }
        }

        // 玩家不存在，创建新玩家
        String playerName = realName != null ? realName : "Unknown";
        String insertSql = "INSERT INTO players (uuid, name) VALUES (?, ?)";
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerName);
            stmt.executeUpdate();

            try (java.sql.ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int playerId = generatedKeys.getInt(1);
                    playerIdCache.put(uuid, playerId);
                    return playerId;
                }
            }
        }

        throw new SQLException("创建玩家失败，无法获取生成的ID");
    }
    
    /**
     * 使用指定的ID插入领地记录。
     *
     * @param conn 数据库连接
     * @param land 领地对象
     */
    private void insertLandWithId(Connection conn, Land land) throws SQLException {
        String sql = "INSERT INTO lands (id, name, world, x1, z1, x2, z2, owner_id, parent_land_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, land.getId());
            stmt.setString(2, land.getName());
            stmt.setString(3, land.getWorld());
            stmt.setInt(4, land.getX1());
            stmt.setInt(5, land.getZ1());
            stmt.setInt(6, land.getX2());
            stmt.setInt(7, land.getZ2());
            stmt.setInt(8, land.getOwnerId());
            
            if (land.getParentLandId() != null) {
                stmt.setInt(9, land.getParentLandId());
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * 插入领地信任关系。
     *
     * @param conn 数据库连接
     * @param landId 领地ID
     * @param playerId 玩家ID
     */
    private void insertLandTrust(Connection conn, int landId, int playerId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO land_trusts (land_id, player_id, trust_level) VALUES (?, ?, ?)";

        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, landId);
            stmt.setInt(2, playerId);
            stmt.setInt(3, 1); // 默认信任等级为1(成员)
            stmt.executeUpdate();
        }
    }

    /**
     * 插入领地保护标记。
     *
     * @param conn 数据库连接
     * @param landId 领地ID
     * @param flagName 标记名称
     * @param value 标记值
     */
    private void insertLandFlag(Connection conn, int landId, String flagName, boolean value) throws SQLException {
        String sql = "INSERT OR IGNORE INTO land_flags (land_id, flag_name, flag_value) VALUES (?, ?, ?)";

        try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, landId);
            stmt.setString(2, flagName);
            stmt.setString(3, String.valueOf(value)); // 使用字符串存储标记值
            stmt.executeUpdate();
        }
    }
    
    /**
     * 将旧的保护规则名称映射到新的。
     *
     * @param oldFlagName 旧的保护规则名称
     * @return 新的保护规则名称列表
     */
    private List<String> mapProtectionFlag(String oldFlagName) {
        List<String> flags = new ArrayList<>();
        switch (oldFlagName) {
            case "block-protection":
                flags.add("build");
                flags.add("break");
                break;
            case "container-protection":
                flags.add("interact");
                flags.add("use");
                break;
            case "explosion-protection":
                flags.add("explosions");
                break;
            case "player-protection":
                flags.add("pvp");
                break;
            default:
                logger.warning("未知的保护规则: " + oldFlagName);
        }
        return flags;
    }
}