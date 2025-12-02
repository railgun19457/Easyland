package io.github.railgun19457.easyland.storage;

import io.github.railgun19457.easyland.model.Land;
//import io.github.railgun19457.easyland.model.LandFlag;
//import io.github.railgun19457.easyland.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of the LandDAO interface.
 * Provides CRUD operations for land data using SQLite database.
 */
public class SqliteLandDAO implements LandDAO {
    private final DatabaseManager databaseManager;

    /**
     * Constructor for SqliteLandDAO.
     *
     * @param databaseManager The database manager to use for connections
     */
    public SqliteLandDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void createLand(Land land) throws SQLException {
        String sql = "INSERT INTO lands (name, world, x1, z1, x2, z2, owner_id, parent_land_id, teleport_x, teleport_y, teleport_z, teleport_yaw, teleport_pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, land.getName());
            stmt.setString(2, land.getWorld());
            stmt.setInt(3, land.getX1());
            stmt.setInt(4, land.getZ1());
            stmt.setInt(5, land.getX2());
            stmt.setInt(6, land.getZ2());
            stmt.setInt(7, land.getOwnerId());
            if (land.getParentLandId() != null) {
                stmt.setInt(8, land.getParentLandId());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }
            
            if (land.getTeleportX() != null) {
                stmt.setDouble(9, land.getTeleportX());
                stmt.setDouble(10, land.getTeleportY());
                stmt.setDouble(11, land.getTeleportZ());
                stmt.setFloat(12, land.getTeleportYaw());
                stmt.setFloat(13, land.getTeleportPitch());
            } else {
                stmt.setNull(9, Types.DOUBLE);
                stmt.setNull(10, Types.DOUBLE);
                stmt.setNull(11, Types.DOUBLE);
                stmt.setNull(12, Types.FLOAT);
                stmt.setNull(13, Types.FLOAT);
            }
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    land.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<Land> getLandById(int id) throws SQLException {
        String sql = "SELECT * FROM lands WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLand(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<Land> getLandByName(String name) throws SQLException {
        String sql = "SELECT * FROM lands WHERE name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLand(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<Land> getNearestLand(String world, int x, int z) throws SQLException {
        // Calculate distance using squared Euclidean distance to avoid square roots in SQL if possible,
        // but SQLite doesn't have a simple distance function.
        // We'll select lands in the same world and order by distance.
        // Distance = (center_x - x)^2 + (center_z - z)^2
        // center_x = (x1 + x2) / 2, center_z = (z1 + z2) / 2
        // To simplify, we can just use the distance to the center of the land.
        
        String sql = "SELECT *, " +
                     "((x1 + x2) / 2 - ?) * ((x1 + x2) / 2 - ?) + " +
                     "((z1 + z2) / 2 - ?) * ((z1 + z2) / 2 - ?) AS distance_sq " +
                     "FROM lands WHERE world = ? " +
                     "ORDER BY distance_sq ASC LIMIT 1";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, x);
            stmt.setInt(2, x);
            stmt.setInt(3, z);
            stmt.setInt(4, z);
            stmt.setString(5, world);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLand(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public List<Land> getLandsByOwner(int ownerId) throws SQLException {
        String sql = "SELECT * FROM lands WHERE owner_id = ? ORDER BY id";
        List<Land> lands = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, ownerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lands.add(mapResultSetToLand(rs));
                }
            }
        }
        
        return lands;
    }

    @Override
    public List<Land> getLandsByWorld(String world) throws SQLException {
        String sql = "SELECT * FROM lands WHERE world = ? ORDER BY id";
        List<Land> lands = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, world);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lands.add(mapResultSetToLand(rs));
                }
            }
        }
        
        return lands;
    }

    @Override
    public List<Land> getLandsAtLocation(String world, int x, int z) throws SQLException {
        String sql = "SELECT * FROM lands WHERE world = ? AND x1 <= ? AND x2 >= ? AND z1 <= ? AND z2 >= ?";
        List<Land> lands = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, world);
            stmt.setInt(2, x);
            stmt.setInt(3, x);
            stmt.setInt(4, z);
            stmt.setInt(5, z);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lands.add(mapResultSetToLand(rs));
                }
            }
        }
        
        return lands;
    }

    @Override
    public void updateLand(Land land) throws SQLException {
        String sql = "UPDATE lands SET name = ?, world = ?, x1 = ?, z1 = ?, x2 = ?, z2 = ?, owner_id = ?, parent_land_id = ?, teleport_x = ?, teleport_y = ?, teleport_z = ?, teleport_yaw = ?, teleport_pitch = ? WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, land.getName());
            stmt.setString(2, land.getWorld());
            stmt.setInt(3, land.getX1());
            stmt.setInt(4, land.getZ1());
            stmt.setInt(5, land.getX2());
            stmt.setInt(6, land.getZ2());
            stmt.setInt(7, land.getOwnerId());
            if (land.getParentLandId() != null) {
                stmt.setInt(8, land.getParentLandId());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }
            
            if (land.getTeleportX() != null) {
                stmt.setDouble(9, land.getTeleportX());
                stmt.setDouble(10, land.getTeleportY());
                stmt.setDouble(11, land.getTeleportZ());
                stmt.setFloat(12, land.getTeleportYaw());
                stmt.setFloat(13, land.getTeleportPitch());
            } else {
                stmt.setNull(9, Types.DOUBLE);
                stmt.setNull(10, Types.DOUBLE);
                stmt.setNull(11, Types.DOUBLE);
                stmt.setNull(12, Types.FLOAT);
                stmt.setNull(13, Types.FLOAT);
            }
            
            stmt.setInt(14, land.getId());
            
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteLand(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            // 禁用自动提交，开启事务
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            
            try {
                // Delete related records first
                deleteLandFlags(conn, id);
                deleteLandTrusts(conn, id);
                
                // Delete the land
                String sql = "DELETE FROM lands WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    stmt.executeUpdate();
                }
                
                // 所有操作成功，提交事务
                conn.commit();
            } catch (SQLException e) {
                // 发生异常，回滚事务
                conn.rollback();
                throw e;
            } finally {
                // 恢复原始的自动提交设置
                conn.setAutoCommit(originalAutoCommit);
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @Override
    public List<Land> getOverlappingLands(String world, int x1, int z1, int x2, int z2) throws SQLException {
        String sql = "SELECT * FROM lands WHERE world = ? AND " +
                    "NOT (x2 < ? OR x1 > ? OR z2 < ? OR z1 > ?)";
        List<Land> lands = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, world);
            stmt.setInt(2, x1);
            stmt.setInt(3, x2);
            stmt.setInt(4, z1);
            stmt.setInt(5, z2);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lands.add(mapResultSetToLand(rs));
                }
            }
        }
        
        return lands;
    }

    @Override
    public int getLandCountByOwner(int ownerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM lands WHERE owner_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, ownerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }

    @Override
    public List<Land> getAllLands() throws SQLException {
        String sql = "SELECT * FROM lands ORDER BY id";
        List<Land> lands = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lands.add(mapResultSetToLand(rs));
                }
            }
        }
        
        return lands;
    }

    @Override
    public List<Land> getSubLands(int parentLandId) throws SQLException {
        String sql = "SELECT * FROM lands WHERE parent_land_id = ? ORDER BY id";
        List<Land> lands = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, parentLandId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lands.add(mapResultSetToLand(rs));
                }
            }
        }
        
        return lands;
    }

    /**
     * Maps a ResultSet to a Land object.
     *
     * @param rs The ResultSet to map
     * @return The mapped Land object
     * @throws SQLException if a database access error occurs
     */
    private Land mapResultSetToLand(ResultSet rs) throws SQLException {
        Land land = new Land();
        land.setId(rs.getInt("id"));
        land.setName(rs.getString("name"));
        land.setWorld(rs.getString("world"));
        land.setX1(rs.getInt("x1"));
        land.setZ1(rs.getInt("z1"));
        land.setX2(rs.getInt("x2"));
        land.setZ2(rs.getInt("z2"));
        land.setOwnerId(rs.getInt("owner_id"));
        
        // 安全地获取 parent_land_id，处理 NULL 值
        int parentLandId = rs.getInt("parent_land_id");
        if (rs.wasNull()) {
            land.setParentLandId(null);
        } else {
            land.setParentLandId(parentLandId);
        }
        
        // 获取传送点信息
        double teleportX = rs.getDouble("teleport_x");
        if (!rs.wasNull()) {
            land.setTeleportX(teleportX);
            land.setTeleportY(rs.getDouble("teleport_y"));
            land.setTeleportZ(rs.getDouble("teleport_z"));
            land.setTeleportYaw(rs.getFloat("teleport_yaw"));
            land.setTeleportPitch(rs.getFloat("teleport_pitch"));
        }
        
        return land;
    }

    /**
     * Deletes all flags for a land.
     *
     * @param conn The database connection to use
     * @param landId The land ID
     * @throws SQLException if a database access error occurs
     */
    private void deleteLandFlags(Connection conn, int landId) throws SQLException {
        String sql = "DELETE FROM land_flags WHERE land_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, landId);
            stmt.executeUpdate();
        }
    }

    /**
     * Deletes all trusts for a land.
     *
     * @param conn The database connection to use
     * @param landId The land ID
     * @throws SQLException if a database access error occurs
     */
    private void deleteLandTrusts(Connection conn, int landId) throws SQLException {
        String sql = "DELETE FROM land_trusts WHERE land_id = ?";
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, landId);
            stmt.executeUpdate();
        }
    }
}