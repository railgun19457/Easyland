package io.github.railgun19457.easyland.storage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Manages the SQLite database connection for the Easyland plugin.
 * Handles database initialization, connection management, and table creation.
 */
public class DatabaseManager {
    private final File dataFolder;
    private Connection connection;
    private final Logger logger;

    /**
     * Constructor for DatabaseManager.
     *
     * @param dataFolder The plugin's data folder where the database will be stored
     */
    public DatabaseManager(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
    }

    /**
     * Initializes the database connection and creates necessary tables.
     *
     * @throws SQLException if a database access error occurs
     * @throws IOException  if the database file cannot be created
     */
    public void initialize() throws SQLException, IOException {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File databaseFile = new File(dataFolder, "easyland.db");
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

        connection = DriverManager.getConnection(url);
        createTables();
    }

    /**
     * Gets the database connection.
     *
     * @return The database connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            File databaseFile = new File(dataFolder, "easyland.db");
            String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }
        return connection;
    }

    /**
     * Creates a new, independent database connection for explicit transaction management.
     * This method is useful for operations that require their own connection lifecycle,
     * such as data migration operations.
     *
     * @return A new database connection
     * @throws SQLException if a database access error occurs
     */
    public Connection createNewConnection() throws SQLException {
        File databaseFile = new File(dataFolder, "easyland.db");
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }

    /**
     * Closes the database connection.
     *
     * @throws SQLException if a database access error occurs
     */
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Creates all necessary database tables.
     * 
     * 数据表结构设计:
     * 
     * 1. players (玩家表)
     *    - id: 主键
     *    - uuid: 玩家UUID (唯一)
     *    - name: 玩家名称
     *    - created_at: 首次记录时间
     *    - last_seen: 最后在线时间
     * 
     * 2. lands (领地表)
     *    - id: 主键
     *    - name: 领地名称 (支持中文, UTF-8)
     *    - world: 世界名称
     *    - x1, z1: 第一对角坐标
     *    - x2, z2: 第二对角坐标
     *    - owner_id: 领地主人 (外键关联players)
     *    - parent_land_id: 父领地ID (支持子领地嵌套)
     *    - priority: 优先级 (解决重叠领地权限判断, 数值越大优先级越高)
     *    - created_at: 创建时间
     *    - updated_at: 最后修改时间
     * 
     * 3. land_flags (领地规则表)
     *    - id: 主键
     *    - land_id: 领地ID (外键)
     *    - flag_name: 规则名称 (如: build, break, interact等)
     *    - flag_value: 规则值 (TEXT类型, 支持更复杂的配置)
     *    - 联合唯一约束: (land_id, flag_name)
     * 
     * 4. land_trusts (领地信任表)
     *    - land_id: 领地ID (外键)
     *    - player_id: 玩家ID (外键)
     *    - trust_level: 信任等级 (0=访客, 1=成员, 2=管理员)
     *    - granted_at: 授权时间
     *    - granted_by: 授权人ID
     *    - 联合主键: (land_id, player_id)
     *
     * @throws SQLException if a database access error occurs
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // ========================================
            // 1. 玩家表 (players)
            // ========================================
            statement.execute(
                "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL UNIQUE," +
                "name TEXT NOT NULL," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "last_seen DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // ========================================
            // 2. 领地表 (lands)
            // ========================================
            statement.execute(
                "CREATE TABLE IF NOT EXISTS lands (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +                           // 领地名称 (支持中文)
                "world TEXT NOT NULL," +                 // 世界名称
                "x1 INTEGER NOT NULL," +                 // 第一对角 X 坐标
                "z1 INTEGER NOT NULL," +                 // 第一对角 Z 坐标
                "x2 INTEGER NOT NULL," +                 // 第二对角 X 坐标
                "z2 INTEGER NOT NULL," +                 // 第二对角 Z 坐标
                "owner_id INTEGER NOT NULL," +           // 领地主人ID
                "parent_land_id INTEGER," +              // 父领地ID (用于子领地)
                "priority INTEGER DEFAULT 0," +          // 优先级 (数值越大优先级越高)
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +  // 创建时间
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP," +  // 修改时间
                "FOREIGN KEY (owner_id) REFERENCES players (id) ON DELETE CASCADE," +
                "FOREIGN KEY (parent_land_id) REFERENCES lands (id) ON DELETE SET NULL" +
                ")"
            );

            // ========================================
            // 3. 领地规则表 (land_flags)
            // ========================================
            statement.execute(
                "CREATE TABLE IF NOT EXISTS land_flags (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "land_id INTEGER NOT NULL," +
                "flag_name TEXT NOT NULL," +             // 规则名称
                "flag_value TEXT NOT NULL DEFAULT 'true'," +  // 规则值 (支持更复杂配置)
                "FOREIGN KEY (land_id) REFERENCES lands (id) ON DELETE CASCADE," +
                "UNIQUE (land_id, flag_name)" +
                ")"
            );

            // ========================================
            // 4. 领地信任表 (land_trusts)
            // ========================================
            statement.execute(
                "CREATE TABLE IF NOT EXISTS land_trusts (" +
                "land_id INTEGER NOT NULL," +
                "player_id INTEGER NOT NULL," +
                "trust_level INTEGER NOT NULL DEFAULT 1," +  // 0=访客, 1=成员, 2=管理员
                "granted_at DATETIME DEFAULT CURRENT_TIMESTAMP," +  // 授权时间
                "granted_by INTEGER," +                  // 授权人ID
                "PRIMARY KEY (land_id, player_id)," +
                "FOREIGN KEY (land_id) REFERENCES lands (id) ON DELETE CASCADE," +
                "FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE," +
                "FOREIGN KEY (granted_by) REFERENCES players (id) ON DELETE SET NULL" +
                ")"
            );

            // ========================================
            // 数据库迁移: 为现有数据库添加新列
            // ========================================
            migrateExistingTables(statement);

            // ========================================
            // 创建索引以提升查询性能
            // ========================================
            createIndexes(statement);
        }
    }

    /**
     * Migrates existing tables by adding new columns if they don't exist.
     * This ensures backward compatibility with older database versions.
     *
     * @param statement The SQL statement to use
     */
    private void migrateExistingTables(Statement statement) {
        // 迁移 players 表
        addColumnIfNotExists(statement, "players", "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");
        addColumnIfNotExists(statement, "players", "last_seen", "DATETIME DEFAULT CURRENT_TIMESTAMP");

        // 迁移 lands 表
        addColumnIfNotExists(statement, "lands", "name", "TEXT");
        addColumnIfNotExists(statement, "lands", "parent_land_id", "INTEGER");
        addColumnIfNotExists(statement, "lands", "priority", "INTEGER DEFAULT 0");
        addColumnIfNotExists(statement, "lands", "created_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");
        addColumnIfNotExists(statement, "lands", "updated_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");

        // 迁移 land_flags 表 (将 is_enabled 改为 flag_value)
        addColumnIfNotExists(statement, "land_flags", "flag_value", "TEXT NOT NULL DEFAULT 'true'");

        // 迁移 land_trusts 表
        addColumnIfNotExists(statement, "land_trusts", "trust_level", "INTEGER NOT NULL DEFAULT 1");
        addColumnIfNotExists(statement, "land_trusts", "granted_at", "DATETIME DEFAULT CURRENT_TIMESTAMP");
        addColumnIfNotExists(statement, "land_trusts", "granted_by", "INTEGER");
    }

    /**
     * Adds a column to a table if it doesn't already exist.
     *
     * @param statement  The SQL statement to use
     * @param tableName  The name of the table
     * @param columnName The name of the column to add
     * @param columnDef  The column definition (type and constraints)
     */
    private void addColumnIfNotExists(Statement statement, String tableName, String columnName, String columnDef) {
        try {
            statement.executeQuery("SELECT " + columnName + " FROM " + tableName + " LIMIT 1");
        } catch (SQLException e) {
            // Column doesn't exist, add it
            try {
                statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
                logger.info("Added '" + columnName + "' column to " + tableName + " table.");
            } catch (SQLException ex) {
                logger.warning("Failed to add column '" + columnName + "' to " + tableName + ": " + ex.getMessage());
            }
        }
    }

    /**
     * Creates indexes for better query performance.
     *
     * @param statement The SQL statement to use
     * @throws SQLException if a database access error occurs
     */
    private void createIndexes(Statement statement) throws SQLException {
        // 玩家表索引
        statement.execute("CREATE INDEX IF NOT EXISTS idx_players_uuid ON players (uuid)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_players_name ON players (name)");

        // 领地表索引
        statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_owner ON lands (owner_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_world ON lands (world)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_parent ON lands (parent_land_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_priority ON lands (priority DESC)");
        // 空间查询优化索引 (用于快速查找某坐标所在的领地)
        statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_coords ON lands (world, x1, z1, x2, z2)");

        // 领地规则表索引
        statement.execute("CREATE INDEX IF NOT EXISTS idx_land_flags_land ON land_flags (land_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_land_flags_name ON land_flags (flag_name)");

        // 领地信任表索引
        statement.execute("CREATE INDEX IF NOT EXISTS idx_land_trusts_land ON land_trusts (land_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_land_trusts_player ON land_trusts (player_id)");
        statement.execute("CREATE INDEX IF NOT EXISTS idx_land_trusts_level ON land_trusts (trust_level)");
    }

    /**
     * Checks if the database connection is valid.
     *
     * @return true if the connection is valid, false otherwise
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Executes a database update and returns the number of affected rows.
     *
     * @param sql The SQL statement to execute
     * @return The number of affected rows
     * @throws SQLException if a database access error occurs
     */
    public int executeUpdate(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeUpdate(sql);
        }
    }

    /**
     * Executes a database query and returns true if it returns any results.
     *
     * @param sql The SQL query to execute
     * @return true if the query returns any results, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean executeQuery(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            return statement.executeQuery(sql).next();
        }
    }
}