package io.github.railgun19457.easyland.storage;

import io.github.railgun19457.easyland.model.Player;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite implementation of the PlayerDAO interface.
 * Provides CRUD operations for player data using SQLite database.
 */
public class SqlitePlayerDAO implements PlayerDAO {
    private final DatabaseManager databaseManager;

    /**
     * Constructor for SqlitePlayerDAO.
     *
     * @param databaseManager The database manager to use for connections
     */
    public SqlitePlayerDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public void createPlayer(Player player) throws SQLException {
        String sql = "INSERT INTO players (uuid, name) VALUES (?, ?)";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, player.getUuid().toString());
            stmt.setString(2, player.getName());
            
            stmt.executeUpdate();
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    player.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    @Override
    public Optional<Player> getPlayerById(int id) throws SQLException {
        String sql = "SELECT * FROM players WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPlayer(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<Player> getPlayerByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT * FROM players WHERE uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPlayer(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public Optional<Player> getPlayerByName(String name) throws SQLException {
        String sql = "SELECT * FROM players WHERE name = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, name);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPlayer(rs));
                }
            }
        }
        
        return Optional.empty();
    }

    @Override
    public void updatePlayer(Player player) throws SQLException {
        String sql = "UPDATE players SET uuid = ?, name = ? WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, player.getUuid().toString());
            stmt.setString(2, player.getName());
            stmt.setInt(3, player.getId());
            
            stmt.executeUpdate();
        }
    }

    @Override
    public void deletePlayer(int id) throws SQLException {
        // Delete related records first
        deletePlayerTrusts(id);
        
        // Update lands owned by this player to have no owner
        updateLandsOwner(id, 0);
        
        // Delete the player
        String sql = "DELETE FROM players WHERE id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Player> getAllPlayers() throws SQLException {
        String sql = "SELECT * FROM players ORDER BY id";
        List<Player> players = new ArrayList<>();
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    players.add(mapResultSetToPlayer(rs));
                }
            }
        }
        
        return players;
    }

    @Override
    public boolean playerExists(UUID uuid) throws SQLException {
        String sql = "SELECT COUNT(*) FROM players WHERE uuid = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, uuid.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        
        return false;
    }

    @Override
    public Player getOrCreatePlayer(UUID uuid, String name) throws SQLException {
        Optional<Player> existingPlayer = getPlayerByUuid(uuid);
        
        if (existingPlayer.isPresent()) {
            Player player = existingPlayer.get();
            // Update name if it has changed
            if (!player.getName().equals(name)) {
                player.setName(name);
                updatePlayer(player);
            }
            return player;
        } else {
            Player newPlayer = new Player(uuid, name);
            createPlayer(newPlayer);
            return newPlayer;
        }
    }

    /**
     * Maps a ResultSet to a Player object.
     *
     * @param rs The ResultSet to map
     * @return The mapped Player object
     * @throws SQLException if a database access error occurs
     */
    private Player mapResultSetToPlayer(ResultSet rs) throws SQLException {
        Player player = new Player();
        player.setId(rs.getInt("id"));
        player.setUuid(UUID.fromString(rs.getString("uuid")));
        player.setName(rs.getString("name"));
        return player;
    }

    /**
     * Deletes all trusts for a player.
     *
     * @param playerId The player ID
     * @throws SQLException if a database access error occurs
     */
    private void deletePlayerTrusts(int playerId) throws SQLException {
        String sql = "DELETE FROM land_trusts WHERE player_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
        }
    }

    /**
     * Updates the owner of lands owned by a player.
     *
     * @param oldOwnerId The old owner ID
     * @param newOwnerId The new owner ID (0 for no owner)
     * @throws SQLException if a database access error occurs
     */
    private void updateLandsOwner(int oldOwnerId, int newOwnerId) throws SQLException {
        String sql = "UPDATE lands SET owner_id = ? WHERE owner_id = ?";
        
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, newOwnerId);
            stmt.setInt(2, oldOwnerId);
            
            stmt.executeUpdate();
        }
    }
}