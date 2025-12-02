package io.github.railgun19457.easyland.storage;

import io.github.railgun19457.easyland.model.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for Player Data Access Object.
 * Defines the contract for player data operations.
 */
public interface PlayerDAO {
    /**
     * Creates a new player entry in the database.
     *
     * @param player The Player object to create
     * @throws SQLException if a database access error occurs
     */
    void createPlayer(Player player) throws SQLException;

    /**
     * Retrieves a player by their database ID.
     *
     * @param id The ID of the player
     * @return An Optional containing the player if found, otherwise empty
     * @throws SQLException if a database access error occurs
     */
    Optional<Player> getPlayerById(int id) throws SQLException;

    /**
     * Retrieves a player by their UUID.
     *
     * @param uuid The UUID of the player
     * @return An Optional containing the player if found, otherwise empty
     * @throws SQLException if a database access error occurs
     */
    Optional<Player> getPlayerByUuid(UUID uuid) throws SQLException;

    /**
     * Retrieves a player by their name.
     *
     * @param name The name of the player
     * @return An Optional containing the player if found, otherwise empty
     * @throws SQLException if a database access error occurs
     */
    Optional<Player> getPlayerByName(String name) throws SQLException;

    /**
     * Updates an existing player's details.
     *
     * @param player The player object with updated information
     * @throws SQLException if a database access error occurs
     */
    void updatePlayer(Player player) throws SQLException;

    /**
     * Deletes a player from the database by their ID.
     *
     * @param id The ID of the player to delete
     * @throws SQLException if a database access error occurs
     */
    void deletePlayer(int id) throws SQLException;

    /**
     * Gets all players in the database.
     *
     * @return A list of all players
     * @throws SQLException if a database access error occurs
     */
    List<Player> getAllPlayers() throws SQLException;

    /**
     * Checks if a player exists in the database by UUID.
     *
     * @param uuid The UUID of the player
     * @return true if the player exists, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean playerExists(UUID uuid) throws SQLException;

    /**
     * Gets or creates a player in the database.
     * If the player doesn't exist, it will be created.
     *
     * @param uuid The UUID of the player
     * @param name The name of the player
     * @return The player object
     * @throws SQLException if a database access error occurs
     */
    Player getOrCreatePlayer(UUID uuid, String name) throws SQLException;
}