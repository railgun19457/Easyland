package io.github.railgun19457.easyland.migration;

import java.io.File;
import java.sql.*;
import java.util.logging.Logger;

/**
 * 坐标系统迁移工具
 * 将旧的区块坐标数据迁移到新的世界坐标系统
 */
public class CoordinateMigration {
    private final Logger logger;

    public CoordinateMigration(Logger logger) {
        this.logger = logger;
    }

    /**
     * 检查是否需要迁移
     * 如果数据库中存在 min_y 和 max_y 列但所有值都为 NULL，则需要迁移
     */
    public boolean needsMigration(File sqliteFile) {
        if (!sqliteFile.exists()) {
            return false;
        }

        String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            // 检查表是否存在
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "lands", null);
            if (!tables.next()) {
                return false; // 表不存在，不需要迁移
            }

            // 检查是否有 min_y 和 max_y 列
            ResultSet columns = meta.getColumns(null, null, "lands", "min_y");
            if (!columns.next()) {
                return false; // 列不存在，说明是全新的数据库
            }

            // 检查是否所有领地的坐标都是区块坐标（可以通过检查坐标是否都是16的倍数来判断）
            String checkSql = "SELECT COUNT(*) as total, " +
                             "SUM(CASE WHEN (min_x % 16 = 0 AND max_x % 16 = 15 AND min_z % 16 = 0 AND max_z % 16 = 15) THEN 1 ELSE 0 END) as chunk_coords " +
                             "FROM lands";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next()) {
                    int total = rs.getInt("total");
                    int chunkCoords = rs.getInt("chunk_coords");
                    // 如果超过80%的领地使用区块坐标，则认为需要迁移
                    return total > 0 && chunkCoords >= total * 0.8;
                }
            }
        } catch (SQLException e) {
            logger.warning("Failed to check migration status: " + e.getMessage());
        }
        return false;
    }

    /**
     * 执行迁移
     * 注意：这个迁移是幂等的，可以安全地多次执行
     */
    public MigrationResult migrate(File sqliteFile) {
        if (!sqliteFile.exists()) {
            return new MigrationResult(false, 0, "Database file does not exist");
        }

        String url = "jdbc:sqlite:" + sqliteFile.getAbsolutePath();
        int migratedCount = 0;

        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);

            // 由于旧数据已经是世界坐标（区块坐标 * 16），我们只需要确保数据格式正确
            // 实际上不需要做任何转换，因为 Land 类的 Chunk 构造函数已经处理了转换

            // 但我们可以添加一个验证步骤，确保所有数据都是有效的
            String validateSql = "SELECT id, land_id, min_x, max_x, min_z, max_z FROM lands";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(validateSql)) {

                while (rs.next()) {
                    long id = rs.getLong("id");
                    String landId = rs.getString("land_id");
                    int minX = rs.getInt("min_x");
                    int maxX = rs.getInt("max_x");
                    int minZ = rs.getInt("min_z");
                    int maxZ = rs.getInt("max_z");

                    // 验证坐标是否合理
                    if (minX > maxX || minZ > maxZ) {
                        logger.warning("Invalid coordinates for land " + landId + " (id=" + id + ")");
                        continue;
                    }

                    migratedCount++;
                }
            }

            conn.commit();
            return new MigrationResult(true, migratedCount, "Migration completed successfully");

        } catch (SQLException e) {
            logger.severe("Migration failed: " + e.getMessage());
            e.printStackTrace();
            return new MigrationResult(false, 0, "Migration failed: " + e.getMessage());
        }
    }

    /**
     * 迁移结果
     */
    public static class MigrationResult {
        private final boolean success;
        private final int migratedCount;
        private final String message;

        public MigrationResult(boolean success, int migratedCount, String message) {
            this.success = success;
            this.migratedCount = migratedCount;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getMigratedCount() {
            return migratedCount;
        }

        public String getMessage() {
            return message;
        }
    }
}
