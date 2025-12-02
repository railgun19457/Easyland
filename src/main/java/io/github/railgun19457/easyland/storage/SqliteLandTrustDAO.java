package io.github.railgun19457.easyland.storage;

import io.github.railgun19457.easyland.model.LandTrust;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of the LandTrustDAO interface.
 * Provides CRUD operations for land trust data using SQLite database.
 */
public class SqliteLandTrustDAO implements LandTrustDAO {
    private final DatabaseManager databaseManager;

    /**
     * Constructor for SqliteLandTrustDAO.
     *
     * @param databaseManager The database manager to use for connections
     */
    public SqliteLandTrustDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void createLandTrust(LandTrust landTrust) throws SQLException {
        String sql = "INSERT OR IGNORE INTO land_trusts (land_id, player_id) VALUES (?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landTrust.getLandId());
            stmt.setInt(2, landTrust.getPlayerId());
            
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<LandTrust> getLandTrust(int landId, int playerId) throws SQLException {
        String sql = "SELECT * FROM land_trusts WHERE land_id = ? AND player_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landId);
            stmt.setInt(2, playerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLandTrust(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public List<LandTrust> getTrustsByLand(int landId) throws SQLException {
        String sql = "SELECT * FROM land_trusts WHERE land_id = ?";
        List<LandTrust> trusts = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trusts.add(mapResultSetToLandTrust(rs));
                }
            }
        }
        
        return trusts;
    }

    @Override
    public List<LandTrust> getTrustsByPlayer(int playerId) throws SQLException {
        String sql = "SELECT * FROM land_trusts WHERE player_id = ?";
        List<LandTrust> trusts = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    trusts.add(mapResultSetToLandTrust(rs));
                }
            }
        }
        
        return trusts;
    }

    @Override
    public void deleteLandTrust(int landId, int playerId) throws SQLException {
        String sql = "DELETE FROM land_trusts WHERE land_id = ? AND player_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landId);
            stmt.setInt(2, playerId);
            
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteTrustsByLand(int landId) throws SQLException {
        String sql = "DELETE FROM land_trusts WHERE land_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landId);
            stmt.executeUpdate();
        }
    }

    @Override
    public void deleteTrustsByPlayer(int playerId) throws SQLException {
        String sql = "DELETE FROM land_trusts WHERE player_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
        }
    }

    @Override
    public boolean isPlayerTrusted(int landId, int playerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM land_trusts WHERE land_id = ? AND player_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landId);
            stmt.setInt(2, playerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }

    @Override
    public int getTrustCountByLand(int landId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM land_trusts WHERE land_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, landId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }

    /**
     * Maps a ResultSet to a LandTrust object.
     *
     * @param rs The ResultSet to map
     * @return The mapped LandTrust object
     * @throws SQLException if a database access error occurs
     */
    private LandTrust mapResultSetToLandTrust(ResultSet rs) throws SQLException {
        LandTrust landTrust = new LandTrust();
        landTrust.setLandId(rs.getInt("land_id"));
        landTrust.setPlayerId(rs.getInt("player_id"));
        return landTrust;
    }
}