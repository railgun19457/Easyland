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
     * @throws SQLException if a database access error occurs
     */
    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Create players table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS players (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL UNIQUE," +
                "name TEXT NOT NULL" +
                ")"
            );

            // Create lands table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS lands (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "world TEXT NOT NULL," +
                "x1 INTEGER NOT NULL," +
                "z1 INTEGER NOT NULL," +
                "x2 INTEGER NOT NULL," +
                "z2 INTEGER NOT NULL," +
                "owner_id INTEGER NOT NULL," +
                "parent_land_id INTEGER," +
                "FOREIGN KEY (owner_id) REFERENCES players (id)," +
                "FOREIGN KEY (parent_land_id) REFERENCES lands (id)" +
                ")"
            );

            // Check if the name column exists and add it if it doesn't (for existing databases)
            try {
                statement.executeQuery("SELECT name FROM lands LIMIT 1");
            } catch (SQLException e) {
                // Column doesn't exist, add it
                statement.execute("ALTER TABLE lands ADD COLUMN name TEXT");
                logger.info("Added 'name' column to lands table for existing database.");
            }

            // Check if the parent_land_id column exists and add it if it doesn't (for existing databases)
            try {
                statement.executeQuery("SELECT parent_land_id FROM lands LIMIT 1");
            } catch (SQLException e) {
                // Column doesn't exist, add it
                statement.execute("ALTER TABLE lands ADD COLUMN parent_land_id INTEGER");
                logger.info("Added 'parent_land_id' column to lands table for existing database.");
            }

            // Create land_flags table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS land_flags (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "land_id INTEGER NOT NULL," +
                "flag_name TEXT NOT NULL," +
                "is_enabled BOOLEAN NOT NULL," +
                "FOREIGN KEY (land_id) REFERENCES lands (id)," +
                "UNIQUE (land_id, flag_name)" +
                ")"
            );

            // Create land_trusts table
            statement.execute(
                "CREATE TABLE IF NOT EXISTS land_trusts (" +
                "land_id INTEGER NOT NULL," +
                "player_id INTEGER NOT NULL," +
                "PRIMARY KEY (land_id, player_id)," +
                "FOREIGN KEY (land_id) REFERENCES lands (id)," +
                "FOREIGN KEY (player_id) REFERENCES players (id)" +
                ")"
            );

            // Create indexes for better performance
            statement.execute("CREATE INDEX IF NOT EXISTS idx_players_uuid ON players (uuid)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_owner ON lands (owner_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_lands_world ON lands (world)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_land_flags_land ON land_flags (land_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_land_trusts_land ON land_trusts (land_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_land_trusts_player ON land_trusts (player_id)");
        }
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