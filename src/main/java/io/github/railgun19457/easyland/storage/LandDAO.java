package io.github.railgun19457.easyland.storage;

import io.github.railgun19457.easyland.model.Land;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Interface for Land Data Access Object.
 * Defines the contract for land data operations.
 */
public interface LandDAO {
    /**
     * Creates a new land entry in the database.
     *
     * @param land The Land object to create
     * @throws SQLException if a database access error occurs
     */
    void createLand(Land land) throws SQLException;

    /**
     * Retrieves a land by its ID.
     *
     * @param id The ID of the land
     * @return An Optional containing the land if found, otherwise empty
     * @throws SQLException if a database access error occurs
     */
    Optional<Land> getLandById(int id) throws SQLException;

    /**
     * Retrieves all lands owned by a specific player.
     *
     * @param ownerId The ID of the owner
     * @return A list of lands
     * @throws SQLException if a database access error occurs
     */
    List<Land> getLandsByOwner(int ownerId) throws SQLException;

    /**
     * Retrieves all lands in a specific world.
     *
     * @param world The world name
     * @return A list of lands in the specified world
     * @throws SQLException if a database access error occurs
     */
    List<Land> getLandsByWorld(String world) throws SQLException;

    /**
     * Retrieves lands that contain the specified coordinates.
     *
     * @param world The world name
     * @param x     The X coordinate
     * @param z     The Z coordinate
     * @return A list of lands containing the coordinates
     * @throws SQLException if a database access error occurs
     */
    List<Land> getLandsAtLocation(String world, int x, int z) throws SQLException;

    /**
     * Updates an existing land's details.
     *
     * @param land The land object with updated information
     * @throws SQLException if a database access error occurs
     */
    void updateLand(Land land) throws SQLException;

    /**
     * Deletes a land from the database by its ID.
     *
     * @param id The ID of the land to delete
     * @throws SQLException if a database access error occurs
     */
    void deleteLand(int id) throws SQLException;

    /**
     * Checks if a land overlaps with existing lands in the same world.
     *
     * @param world The world name
     * @param x1    The first X coordinate
     * @param z1    The first Z coordinate
     * @param x2    The second X coordinate
     * @param z2    The second Z coordinate
     * @return A list of overlapping lands
     * @throws SQLException if a database access error occurs
     */
    List<Land> getOverlappingLands(String world, int x1, int z1, int x2, int z2) throws SQLException;

    /**
     * Gets the total number of lands owned by a player.
     *
     * @param ownerId The ID of the owner
     * @return The number of lands
     * @throws SQLException if a database access error occurs
     */
    int getLandCountByOwner(int ownerId) throws SQLException;

    /**
     * Gets all lands in the database.
     *
     * @return A list of all lands
     * @throws SQLException if a database access error occurs
     */
    List<Land> getAllLands() throws SQLException;

    /**
     * Retrieves all sub-lands of a parent land.
     *
     * @param parentLandId The ID of the parent land
     * @return A list of sub-lands
     * @throws SQLException if a database access error occurs
     */
    List<Land> getSubLands(int parentLandId) throws SQLException;
}