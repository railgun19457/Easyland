package io.github.railgun19457.easyland.storage;

import io.github.railgun19457.easyland.model.LandTrust;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface for LandTrust Data Access Object.
 * Defines the contract for land trust data operations.
 */
public interface LandTrustDAO {
    /**
     * Creates a new land trust entry in the database.
     *
     * @param landTrust The LandTrust object to create
     * @throws SQLException if a database access error occurs
     */
    void createLandTrust(LandTrust landTrust) throws SQLException;

    /**
     * Retrieves a land trust by land ID and player ID.
     *
     * @param landId   The ID of the land
     * @param playerId The ID of the player
     * @return An Optional containing the land trust if found, otherwise empty
     * @throws SQLException if a database access error occurs
     */
    Optional<LandTrust> getLandTrust(int landId, int playerId) throws SQLException;

    /**
     * Retrieves all trusts for a specific land.
     *
     * @param landId The ID of the land
     * @return A list of land trusts
     * @throws SQLException if a database access error occurs
     */
    List<LandTrust> getTrustsByLand(int landId) throws SQLException;

    /**
     * Retrieves all lands trusted to a specific player.
     *
     * @param playerId The ID of the player
     * @return A list of land trusts
     * @throws SQLException if a database access error occurs
     */
    List<LandTrust> getTrustsByPlayer(int playerId) throws SQLException;

    /**
     * Deletes a land trust by land ID and player ID.
     *
     * @param landId   The ID of the land
     * @param playerId The ID of the player
     * @throws SQLException if a database access error occurs
     */
    void deleteLandTrust(int landId, int playerId) throws SQLException;

    /**
     * Deletes all trusts for a specific land.
     *
     * @param landId The ID of the land
     * @throws SQLException if a database access error occurs
     */
    void deleteTrustsByLand(int landId) throws SQLException;

    /**
     * Deletes all trusts for a specific player.
     *
     * @param playerId The ID of the player
     * @throws SQLException if a database access error occurs
     */
    void deleteTrustsByPlayer(int playerId) throws SQLException;

    /**
     * Checks if a player is trusted on a land.
     *
     * @param landId   The ID of the land
     * @param playerId The ID of the player
     * @return true if the player is trusted, false otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean isPlayerTrusted(int landId, int playerId) throws SQLException;

    /**
     * Gets the count of trusted players on a land.
     *
     * @param landId The ID of the land
     * @return The number of trusted players
     * @throws SQLException if a database access error occurs
     */
    int getTrustCountByLand(int landId) throws SQLException;
}