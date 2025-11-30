package io.github.railgun19457.easyland.repository;

import io.github.railgun19457.easyland.domain.Land;
import org.bukkit.Chunk;

import java.io.File;
import java.sql.*;
import java.util.*;

/**
 * SQLite 实现的领地数据访问层
 */
public class SqliteLandRepository implements LandRepository {
    private final File databaseFile;
    private Connection connection;

    public SqliteLandRepository(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    @Override
    public void initialize() {
        try {
            // 确保父目录存在
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }

            // 建立连接
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);

            // 创建表
            createTables();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite database", e);
        }
    }

    private void createTables() throws SQLException {
        String createLandsTable = """
            CREATE TABLE IF NOT EXISTS lands (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                land_id TEXT,
                owner TEXT,
                world_name TEXT NOT NULL,
                min_x INTEGER NOT NULL,
                max_x INTEGER NOT NULL,
                min_z INTEGER NOT NULL,
                max_z INTEGER NOT NULL,
                min_y INTEGER,
                max_y INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;

        String createTrustedTable = """
            CREATE TABLE IF NOT EXISTS land_trusted (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                land_id INTEGER NOT NULL,
                trusted_uuid TEXT NOT NULL,
                FOREIGN KEY (land_id) REFERENCES lands(id) ON DELETE CASCADE,
                UNIQUE(land_id, trusted_uuid)
            )
            """;

        String createProtectionRulesTable = """
            CREATE TABLE IF NOT EXISTS land_protection_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                land_id INTEGER NOT NULL,
                rule_name TEXT NOT NULL,
                enabled INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (land_id) REFERENCES lands(id) ON DELETE CASCADE,
                UNIQUE(land_id, rule_name)
            )
            """;

        // 创建索引以提高查询性能
        String createOwnerIndex = "CREATE INDEX IF NOT EXISTS idx_lands_owner ON lands(owner)";
        String createWorldIndex = "CREATE INDEX IF NOT EXISTS idx_lands_world ON lands(world_name)";
        String createLandIdIndex = "CREATE INDEX IF NOT EXISTS idx_lands_land_id ON lands(land_id)";
        String createChunkIndex = "CREATE INDEX IF NOT EXISTS idx_lands_chunks ON lands(world_name, min_x, max_x, min_z, max_z)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createLandsTable);
            stmt.execute(createTrustedTable);
            stmt.execute(createProtectionRulesTable);
            stmt.execute(createOwnerIndex);
            stmt.execute(createWorldIndex);
            stmt.execute(createLandIdIndex);
            stmt.execute(createChunkIndex);
        }
    }

    @Override
    public Land save(Land land) {
        if (land.getId() == null) {
            return insert(land);
        } else {
            return update(land);
        }
    }

    @Override
    public List<Land> saveAll(List<Land> lands) {
        if (lands == null || lands.isEmpty()) {
            return new ArrayList<>();
        }

        List<Land> savedLands = new ArrayList<>();
        try {
            // 开启事务以提高批量操作性能
            connection.setAutoCommit(false);

            for (Land land : lands) {
                savedLands.add(save(land));
            }

            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Failed to rollback transaction", rollbackEx);
            }
            throw new RuntimeException("Failed to save all lands", e);
        }

        return savedLands;
    }

    private Land insert(Land land) {
        String sql = """
            INSERT INTO lands (land_id, owner, world_name, min_x, max_x, min_z, max_z, min_y, max_y)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, land.getLandId());
            pstmt.setString(2, land.getOwner());
            pstmt.setString(3, land.getWorldName());
            pstmt.setInt(4, land.getMinX());
            pstmt.setInt(5, land.getMaxX());
            pstmt.setInt(6, land.getMinZ());
            pstmt.setInt(7, land.getMaxZ());

            // Y坐标可能为null
            if (land.getMinY() != null) {
                pstmt.setInt(8, land.getMinY());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            if (land.getMaxY() != null) {
                pstmt.setInt(9, land.getMaxY());
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }

            pstmt.executeUpdate();

            // 获取生成的ID
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    land.setId(id);

                    // 保存信任列表和保护规则
                    saveTrustedPlayers(id, land.getTrusted());
                    saveProtectionRules(id, land.getProtectionRules());
                }
            }

            return land;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert land", e);
        }
    }

    private Land update(Land land) {
        String sql = """
            UPDATE lands
            SET land_id = ?, owner = ?, world_name = ?, min_x = ?, max_x = ?, min_z = ?, max_z = ?,
                min_y = ?, max_y = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, land.getLandId());
            pstmt.setString(2, land.getOwner());
            pstmt.setString(3, land.getWorldName());
            pstmt.setInt(4, land.getMinX());
            pstmt.setInt(5, land.getMaxX());
            pstmt.setInt(6, land.getMinZ());
            pstmt.setInt(7, land.getMaxZ());

            // Y坐标可能为null
            if (land.getMinY() != null) {
                pstmt.setInt(8, land.getMinY());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            if (land.getMaxY() != null) {
                pstmt.setInt(9, land.getMaxY());
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }

            pstmt.setLong(10, land.getId());

            pstmt.executeUpdate();

            // 更新信任列表和保护规则
            deleteTrustedPlayers(land.getId());
            deleteProtectionRules(land.getId());
            saveTrustedPlayers(land.getId(), land.getTrusted());
            saveProtectionRules(land.getId(), land.getProtectionRules());

            return land;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update land", e);
        }
    }

    private void saveTrustedPlayers(Long landId, Set<String> trusted) throws SQLException {
        if (trusted.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO land_trusted (land_id, trusted_uuid) VALUES (?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (String uuid : trusted) {
                pstmt.setLong(1, landId);
                pstmt.setString(2, uuid);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void saveProtectionRules(Long landId, Map<String, Boolean> rules) throws SQLException {
        if (rules.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO land_protection_rules (land_id, rule_name, enabled) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Boolean> entry : rules.entrySet()) {
                pstmt.setLong(1, landId);
                pstmt.setString(2, entry.getKey());
                pstmt.setInt(3, entry.getValue() ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void deleteTrustedPlayers(Long landId) throws SQLException {
        String sql = "DELETE FROM land_trusted WHERE land_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            pstmt.executeUpdate();
        }
    }

    private void deleteProtectionRules(Long landId) throws SQLException {
        String sql = "DELETE FROM land_protection_rules WHERE land_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<Land> findById(Long id) {
        String sql = "SELECT * FROM lands WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLand(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find land by id", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Land> findByLandId(String landId) {
        String sql = "SELECT * FROM lands WHERE land_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, landId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLand(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find land by landId", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Land> findByOwner(String ownerUuid) {
        String sql = "SELECT * FROM lands WHERE owner = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUuid);
            return executeQueryAndMapToList(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find lands by owner", e);
        }
    }

    @Override
    public List<Land> findByWorld(String worldName) {
        String sql = "SELECT * FROM lands WHERE world_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, worldName);
            return executeQueryAndMapToList(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find lands by world", e);
        }
    }

    @Override
    public Optional<Land> findByChunk(Chunk chunk) {
        String sql = """
            SELECT * FROM lands
            WHERE world_name = ?
            AND ? BETWEEN min_x AND max_x
            AND ? BETWEEN min_z AND max_z
            LIMIT 1
            """;
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, chunk.getWorld().getName());
            pstmt.setInt(2, chunk.getX());
            pstmt.setInt(3, chunk.getZ());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLand(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find land by chunk", e);
        }
        return Optional.empty();
    }

    @Override
    public List<Land> findAllClaimed() {
        String sql = "SELECT * FROM lands WHERE owner IS NOT NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            return executeQueryAndMapToList(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all claimed lands", e);
        }
    }

    @Override
    public List<Land> findAllUnclaimed() {
        String sql = "SELECT * FROM lands WHERE owner IS NULL";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            return executeQueryAndMapToList(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all unclaimed lands", e);
        }
    }

    @Override
    public List<Land> findAll() {
        String sql = "SELECT * FROM lands";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            return executeQueryAndMapToList(pstmt);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all lands", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM lands WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete land by id", e);
        }
    }

    @Override
    public boolean deleteByLandId(String landId) {
        String sql = "DELETE FROM lands WHERE land_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, landId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete land by landId", e);
        }
    }

    @Override
    public int countByOwner(String ownerUuid) {
        String sql = "SELECT COUNT(*) FROM lands WHERE owner = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, ownerUuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count lands by owner", e);
        }
        return 0;
    }

    @Override
    public boolean existsByLandId(String landId) {
        String sql = "SELECT 1 FROM lands WHERE land_id = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, landId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if land exists", e);
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Land> executeQueryAndMapToList(PreparedStatement pstmt) throws SQLException {
        List<Land> lands = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                lands.add(mapResultSetToLand(rs));
            }
        }
        return lands;
    }

    private Land mapResultSetToLand(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String landId = rs.getString("land_id");
        String owner = rs.getString("owner");
        String worldName = rs.getString("world_name");
        int minX = rs.getInt("min_x");
        int maxX = rs.getInt("max_x");
        int minZ = rs.getInt("min_z");
        int maxZ = rs.getInt("max_z");

        // 读取Y坐标（可能为null）
        Integer minY = rs.getObject("min_y") != null ? rs.getInt("min_y") : null;
        Integer maxY = rs.getObject("max_y") != null ? rs.getInt("max_y") : null;

        // 加载信任列表
        Set<String> trusted = loadTrustedPlayers(id);

        // 加载保护规则
        Map<String, Boolean> protectionRules = loadProtectionRules(id);

        return new Land(id, landId, owner, worldName, minX, maxX, minZ, maxZ, minY, maxY, trusted, protectionRules);
    }

    private Set<String> loadTrustedPlayers(Long landId) throws SQLException {
        Set<String> trusted = new HashSet<>();
        String sql = "SELECT trusted_uuid FROM land_trusted WHERE land_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    trusted.add(rs.getString("trusted_uuid"));
                }
            }
        }
        return trusted;
    }

    private Map<String, Boolean> loadProtectionRules(Long landId) throws SQLException {
        Map<String, Boolean> rules = new HashMap<>();
        String sql = "SELECT rule_name, enabled FROM land_protection_rules WHERE land_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rules.put(rs.getString("rule_name"), rs.getInt("enabled") == 1);
                }
            }
        }
        return rules;
    }
}
