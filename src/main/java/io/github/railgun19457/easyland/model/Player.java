package io.github.railgun19457.easyland.model;

import java.util.UUID;

/**
 * Represents a player in the Easyland system.
 */
public class Player {
    private int id;
    private UUID uuid;
    private String name;

    /**
     * Default constructor.
     */
    public Player() {}

    /**
     * Constructor with all fields.
     *
     * @param id   The player's database ID
     * @param uuid The player's UUID
     * @param name The player's name
     */
    public Player(int id, UUID uuid, String name) {
        this.id = id;
        this.uuid = uuid;
        this.name = name;
    }

    /**
     * Constructor without ID (for new players).
     *
     * @param uuid The player's UUID
     * @param name The player's name
     */
    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return uuid != null && uuid.equals(player.uuid);
    }

    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
}