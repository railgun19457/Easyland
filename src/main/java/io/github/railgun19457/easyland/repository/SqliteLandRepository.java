package io.github.railgun19457.easyland.repository;

import io.github.railgun19457.easyland.domain.Land;
import org.bukkit.Chunk;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SQLite领地数据访问层
 * 使用简单的连接池替代ThreadLocal，提高并发性能和资源管理
 */
public class SqliteLandRepository implements LandRepository {
    private final File databaseFile;
    private final String jdbcUrl;
    
    // 简单连接池
    private final BlockingQueue<Connection> connectionPool;
    private final int maxPoolSize;
    private final ReentrantLock poolLock = new ReentrantLock();
    
    // 用于保护事务操作的锁
    private final ReentrantLock transactionLock = new ReentrantLock();
    
    // 统计信息
    private volatile int activeConnections = 0;
    private volatile int totalConnectionsCreated = 0;

    public SqliteLandRepository(File databaseFile) {
        this(databaseFile, 10); // 默认连接池大小为10
    }
    
    public SqliteLandRepository(File databaseFile, int maxPoolSize) {
        this.databaseFile = databaseFile;
        this.maxPoolSize = maxPoolSize;
        this.jdbcUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        this.connectionPool = new LinkedBlockingQueue<>(maxPoolSize);
    }

    @Override
    public void initialize() {
        poolLock.lock();
        try {
            // 确保父目录存在
            if (!databaseFile.getParentFile().exists()) {
                databaseFile.getParentFile().mkdirs();
            }

            // 初始化连接池
            initializeConnectionPool();
            
            java.util.logging.Logger.getLogger("SqliteLandRepository")
                .info("SQLite database connection pool initialized: " + databaseFile.getAbsolutePath() + 
                      " (pool size: " + maxPoolSize + ")");

            // 创建表
            try (Connection conn = getConnection()) {
                createTables(conn);
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger("SqliteLandRepository")
                .severe("Failed to initialize SQLite database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize SQLite database", e);
        } finally {
            poolLock.unlock();
        }
    }
    
    /**
     * 初始化连接池
     */
    private void initializeConnectionPool() throws SQLException {
        // 预创建一些连接
        int initialSize = Math.min(3, maxPoolSize);
        for (int i = 0; i < initialSize; i++) {
            Connection conn = createNewConnection();
            connectionPool.offer(conn);
        }
    }
    
    /**
     * 创建新的数据库连接
     */
    private Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(jdbcUrl);
        
        // 设置连接参数
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA synchronous=NORMAL");
            stmt.execute("PRAGMA cache_size=10000");
            stmt.execute("PRAGMA temp_store=memory");
            stmt.execute("PRAGMA busy_timeout=30000"); // 30秒超时
        }
        
        totalConnectionsCreated++;
        return conn;
    }
    
    /**
     * 从连接池获取连接
     */
    private Connection getConnection() throws SQLException {
        try {
            // 尝试从池中获取连接
            Connection conn = connectionPool.poll(5, TimeUnit.SECONDS);
            
            if (conn == null || conn.isClosed()) {
                // 如果池中没有可用连接或连接已关闭，创建新连接
                if (activeConnections < maxPoolSize) {
                    conn = createNewConnection();
                } else {
                    // 等待可用连接
                    conn = connectionPool.take();
                    if (conn.isClosed()) {
                        conn = createNewConnection();
                    }
                }
            }
            
            activeConnections++;
            return conn;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("获取数据库连接被中断", e);
        }
    }
    
    /**
     * 归还连接到连接池
     */
    private void returnConnection(Connection conn) {
        if (conn != null) {
            try {
                // 重置连接状态
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
                
                // 如果连接有效，归还到池中
                if (!conn.isClosed()) {
                    connectionPool.offer(conn);
                } else {
                    // 如果连接已关闭，创建新连接补充
                    if (activeConnections < maxPoolSize) {
                        connectionPool.offer(createNewConnection());
                    }
                }
            } catch (SQLException e) {
                java.util.logging.Logger.getLogger("SqliteLandRepository")
                    .warning("归还连接时出错: " + e.getMessage());
            } finally {
                activeConnections--;
            }
        }
    }

    private void createTables(Connection conn) throws SQLException {
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

        try (Statement stmt = conn.createStatement()) {
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
        Connection conn = null;
        try {
            conn = getConnection();
            if (land.id() == null) {
                return insert(land, conn);
            } else {
                return update(land, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save land", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public List<Land> saveAll(List<Land> lands) {
        if (lands == null || lands.isEmpty()) {
            return new ArrayList<>();
        }

        List<Land> savedLands = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection();
            transactionLock.lock();
            
            // 开启事务以提高批量操作性能
            conn.setAutoCommit(false);

            for (Land land : lands) {
                if (land.id() == null) {
                    savedLands.add(insert(land, conn));
                } else {
                    savedLands.add(update(land, conn));
                }
            }

            conn.commit();
            return savedLands;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Failed to rollback transaction", rollbackEx);
            }
            throw new RuntimeException("Failed to save all lands", e);
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                java.util.logging.Logger.getLogger("SqliteLandRepository")
                    .warning("重置自动提交模式失败: " + e.getMessage());
            }
            if (conn != null) {
                returnConnection(conn);
            }
            transactionLock.unlock();
        }
    }

    private Land insert(Land land, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO lands (land_id, owner, world_name, min_x, max_x, min_z, max_z, min_y, max_y)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, land.landId());
            pstmt.setString(2, land.owner());
            pstmt.setString(3, land.worldName());
            pstmt.setInt(4, land.bounds().minX());
            pstmt.setInt(5, land.bounds().maxX());
            pstmt.setInt(6, land.bounds().minZ());
            pstmt.setInt(7, land.bounds().maxZ());

            // Y坐标可能为null
            if (land.bounds().minY() != null) {
                pstmt.setInt(8, land.bounds().minY());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            if (land.bounds().maxY() != null) {
                pstmt.setInt(9, land.bounds().maxY());
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }

            pstmt.executeUpdate();

            // 获取生成的ID
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    Land savedLand = land.withId(id);

                    // 保存信任列表和保护规则
                    saveTrustedPlayers(id, savedLand.trusted(), conn);
                    saveProtectionRules(id, savedLand.protectionRules(), conn);

                    return savedLand;
                }
            }
        }
        throw new SQLException("Failed to insert land - no generated key");
    }

    private Land update(Land land, Connection conn) throws SQLException {
        String sql = """
            UPDATE lands
            SET land_id = ?, owner = ?, world_name = ?, min_x = ?, max_x = ?, min_z = ?, max_z = ?,
                min_y = ?, max_y = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, land.landId());
            pstmt.setString(2, land.owner());
            pstmt.setString(3, land.worldName());
            pstmt.setInt(4, land.bounds().minX());
            pstmt.setInt(5, land.bounds().maxX());
            pstmt.setInt(6, land.bounds().minZ());
            pstmt.setInt(7, land.bounds().maxZ());

            // Y坐标可能为null
            if (land.bounds().minY() != null) {
                pstmt.setInt(8, land.bounds().minY());
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }
            if (land.bounds().maxY() != null) {
                pstmt.setInt(9, land.bounds().maxY());
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }

            pstmt.setLong(10, land.id());

            pstmt.executeUpdate();

            // 更新信任列表和保护规则
            deleteTrustedPlayers(land.id(), conn);
            deleteProtectionRules(land.id(), conn);
            saveTrustedPlayers(land.id(), land.trusted(), conn);
            saveProtectionRules(land.id(), land.protectionRules(), conn);

            return land;
        }
    }

    private void saveTrustedPlayers(Long landId, Set<String> trusted, Connection conn) throws SQLException {
        if (trusted.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO land_trusted (land_id, trusted_uuid) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String uuid : trusted) {
                pstmt.setLong(1, landId);
                pstmt.setString(2, uuid);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void saveProtectionRules(Long landId, Map<String, Boolean> rules, Connection conn) throws SQLException {
        if (rules.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO land_protection_rules (land_id, rule_name, enabled) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Boolean> entry : rules.entrySet()) {
                pstmt.setLong(1, landId);
                pstmt.setString(2, entry.getKey());
                pstmt.setInt(3, entry.getValue() ? 1 : 0);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
    }

    private void deleteTrustedPlayers(Long landId, Connection conn) throws SQLException {
        String sql = "DELETE FROM land_trusted WHERE land_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            pstmt.executeUpdate();
        }
    }

    private void deleteProtectionRules(Long landId, Connection conn) throws SQLException {
        String sql = "DELETE FROM land_protection_rules WHERE land_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            pstmt.executeUpdate();
        }
    }

    @Override
    public Optional<Land> findById(Long id) {
        String sql = "SELECT * FROM lands WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToLand(rs, conn));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find land by id", e);
        } finally {
            returnConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Land> findByLandId(String landId) {
        String sql = "SELECT * FROM lands WHERE land_id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, landId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToLand(rs, conn));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find land by landId", e);
        } finally {
            returnConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Land> findByOwner(String ownerUuid) {
        String sql = "SELECT * FROM lands WHERE owner = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ownerUuid);
                return executeQueryAndMapToList(pstmt, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find lands by owner", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public List<Land> findByWorld(String worldName) {
        String sql = "SELECT * FROM lands WHERE world_name = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, worldName);
                return executeQueryAndMapToList(pstmt, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find lands by world", e);
        } finally {
            returnConnection(conn);
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
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, chunk.getWorld().getName());
                pstmt.setInt(2, chunk.getX());
                pstmt.setInt(3, chunk.getZ());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSetToLand(rs, conn));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find land by chunk", e);
        } finally {
            returnConnection(conn);
        }
        return Optional.empty();
    }

    @Override
    public List<Land> findAllClaimed() {
        String sql = "SELECT * FROM lands WHERE owner IS NOT NULL";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                return executeQueryAndMapToList(pstmt, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all claimed lands", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public List<Land> findAllUnclaimed() {
        String sql = "SELECT * FROM lands WHERE owner IS NULL";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                return executeQueryAndMapToList(pstmt, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all unclaimed lands", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public List<Land> findAll() {
        String sql = "SELECT * FROM lands";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                return executeQueryAndMapToList(pstmt, conn);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all lands", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM lands WHERE id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete land by id", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public boolean deleteByLandId(String landId) {
        String sql = "DELETE FROM lands WHERE land_id = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, landId);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete land by landId", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public int countByOwner(String ownerUuid) {
        String sql = "SELECT COUNT(*) FROM lands WHERE owner = ?";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, ownerUuid);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count lands by owner", e);
        } finally {
            returnConnection(conn);
        }
        return 0;
    }

    @Override
    public boolean existsByLandId(String landId) {
        String sql = "SELECT 1 FROM lands WHERE land_id = ? LIMIT 1";
        Connection conn = null;
        try {
            conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, landId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if land exists", e);
        } finally {
            returnConnection(conn);
        }
    }

    @Override
    public void close() {
        poolLock.lock();
        try {
            java.util.logging.Logger.getLogger("SqliteLandRepository")
                .info("Closing SQLite database connection pool...");
            
            // 关闭所有连接
            int closedCount = 0;
            Connection conn;
            while ((conn = connectionPool.poll()) != null) {
                try {
                    if (!conn.isClosed()) {
                        // 确保连接处于自动提交模式
                        if (!conn.getAutoCommit()) {
                            conn.rollback();
                            conn.setAutoCommit(true);
                        }
                        conn.close();
                        closedCount++;
                    }
                } catch (SQLException e) {
                    java.util.logging.Logger.getLogger("SqliteLandRepository")
                        .warning("Failed to close connection: " + e.getMessage());
                }
            }
            
            java.util.logging.Logger.getLogger("SqliteLandRepository")
                .info("Closed " + closedCount + " database connections from pool. " +
                      "Total connections created: " + totalConnectionsCreated);
        } finally {
            poolLock.unlock();
        }
    }

    private List<Land> executeQueryAndMapToList(PreparedStatement pstmt, Connection conn) throws SQLException {
        List<Land> lands = new ArrayList<>();
        try (ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                lands.add(mapResultSetToLand(rs, conn));
            }
        }
        return lands;
    }

    private Land mapResultSetToLand(ResultSet rs, Connection conn) throws SQLException {
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
        Set<String> trusted = loadTrustedPlayers(id, conn);

        // 加载保护规则
        Map<String, Boolean> protectionRules = loadProtectionRules(id, conn);

        return new Land(id, landId, owner, worldName, minX, maxX, minZ, maxZ, minY, maxY, trusted, protectionRules);
    }

    private Set<String> loadTrustedPlayers(Long landId, Connection conn) throws SQLException {
        Set<String> trusted = new HashSet<>();
        String sql = "SELECT trusted_uuid FROM land_trusted WHERE land_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    trusted.add(rs.getString("trusted_uuid"));
                }
            }
        }
        return trusted;
    }

    private Map<String, Boolean> loadProtectionRules(Long landId, Connection conn) throws SQLException {
        Map<String, Boolean> rules = new HashMap<>();
        String sql = "SELECT rule_name, enabled FROM land_protection_rules WHERE land_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, landId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rules.put(rs.getString("rule_name"), rs.getInt("enabled") == 1);
                }
            }
        }
        return rules;
    }
    
    /**
     * 获取连接池统计信息
     */
    public PoolStats getPoolStats() {
        return new PoolStats(
            maxPoolSize,
            connectionPool.size(),
            activeConnections,
            totalConnectionsCreated
        );
    }
    
    /**
     * 连接池统计信息
     */
    public record PoolStats(int maxPoolSize, int availableConnections, int activeConnections, int totalConnectionsCreated) {
        @Override
        public String toString() {
            return String.format("PoolStats{max=%d, available=%d, active=%d, totalCreated=%d}", 
                               maxPoolSize, availableConnections, activeConnections, totalConnectionsCreated);
        }
    }
}
