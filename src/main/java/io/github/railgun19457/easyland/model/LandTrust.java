package io.github.railgun19457.easyland.model;

/**
 * Represents a trust relationship between a land and a player.
 * This class is used to manage which players have access to specific lands.
 */
public class LandTrust {
    private int landId;
    private int playerId;
    private Land land;
    private Player player;

    /**
     * Default constructor.
     */
    public LandTrust() {}

    /**
     * Constructor with IDs.
     *
     * @param landId   The land's database ID
     * @param playerId The player's database ID
     */
    public LandTrust(int landId, int playerId) {
        this.landId = landId;
        this.playerId = playerId;
    }

    /**
     * Constructor with objects.
     *
     * @param land   The land object
     * @param player The player object
     */
    public LandTrust(Land land, Player player) {
        this.land = land;
        this.player = player;
        if (land != null) {
            this.landId = land.getId();
        }
        if (player != null) {
            this.playerId = player.getId();
        }
    }

    /**
     * Constructor with all fields.
     *
     * @param landId   The land's database ID
     * @param playerId The player's database ID
     * @param land     The land object
     * @param player   The player object
     */
    public LandTrust(int landId, int playerId, Land land, Player player) {
        this.landId = landId;
        this.playerId = playerId;
        this.land = land;
        this.player = player;
    }

    public int getLandId() {
        return landId;
    }

    public void setLandId(int landId) {
        this.landId = landId;
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public Land getLand() {
        return land;
    }

    public void setLand(Land land) {
        this.land = land;
        if (land != null) {
            this.landId = land.getId();
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
        if (player != null) {
            this.playerId = player.getId();
        }
    }

    @Override
    public String toString() {
        return "LandTrust{" +
                "landId=" + landId +
                ", playerId=" + playerId +
                ", land=" + (land != null ? land.getId() : "null") +
                ", player=" + (player != null ? player.getName() : "null") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LandTrust that = (LandTrust) o;
        return landId == that.landId && playerId == that.playerId;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(landId);
        result = 31 * result + Integer.hashCode(playerId);
        return result;
    }
}