package io.github.railgun19457.easyland.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a land claim in the Easyland system.
 */
public class Land {
    private int id;
    private String name;
    private String world;
    private int x1;
    private int z1;
    private int x2;
    private int z2;
    private int ownerId;
    private Integer parentLandId;
    private Double teleportX;
    private Double teleportY;
    private Double teleportZ;
    private Float teleportYaw;
    private Float teleportPitch;
    private Player owner;
    private Set<LandFlag> flags;
    private List<Player> trustedPlayers;

    /**
     * Default constructor.
     */
    public Land() {}

    /**
     * Constructor with basic fields.
     *
     * @param world  The world name
     * @param x1     The first X coordinate
     * @param z1     The first Z coordinate
     * @param x2     The second X coordinate
     * @param z2     The second Z coordinate
     * @param ownerId The owner's database ID
     */
    public Land(String world, int x1, int z1, int x2, int z2, int ownerId) {
        this.name = null; // Default name is null
        this.world = world;
        this.x1 = Math.min(x1, x2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.z2 = Math.max(z1, z2);
        this.ownerId = ownerId;
        this.parentLandId = null; // Default is null (not a sub-land)
    }

    /**
     * Constructor with all fields.
     *
     * @param id     The land's database ID
     * @param world  The world name
     * @param x1     The first X coordinate
     * @param z1     The first Z coordinate
     * @param x2     The second X coordinate
     * @param z2     The second Z coordinate
     * @param ownerId The owner's database ID
     */
    public Land(int id, String world, int x1, int z1, int x2, int z2, int ownerId) {
        this(world, x1, z1, x2, z2, ownerId);
        this.id = id;
    }

    public Land(int id, String name, String world, int x1, int z1, int x2, int z2, int ownerId) {
        this(id, world, x1, z1, x2, z2, ownerId);
        this.name = name;
    }

    public Land(int id, String name, String world, int x1, int z1, int x2, int z2, int ownerId, Integer parentLandId) {
        this(id, name, world, x1, z1, x2, z2, ownerId);
        this.parentLandId = parentLandId;
    }
    
    /**
     * Private constructor for Builder pattern.
     */
    private Land(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.world = builder.world;
        this.x1 = builder.x1;
        this.z1 = builder.z1;
        this.x2 = builder.x2;
        this.z2 = builder.z2;
        this.ownerId = builder.ownerId;
        this.parentLandId = builder.parentLandId;
        this.teleportX = builder.teleportX;
        this.teleportY = builder.teleportY;
        this.teleportZ = builder.teleportZ;
        this.teleportYaw = builder.teleportYaw;
        this.teleportPitch = builder.teleportPitch;
        this.owner = builder.owner;
        this.flags = builder.flags;
        this.trustedPlayers = builder.trustedPlayers;
    }
    
    /**
     * Builder class for Land objects.
     * Provides a fluent API for constructing Land instances.
     */
    public static class Builder {
        private int id;
        private String name;
        private String world;
        private int x1;
        private int z1;
        private int x2;
        private int z2;
        private int ownerId;
        private Integer parentLandId;
        private Double teleportX;
        private Double teleportY;
        private Double teleportZ;
        private Float teleportYaw;
        private Float teleportPitch;
        private Player owner;
        private Set<LandFlag> flags;
        private List<Player> trustedPlayers;
        
        public Builder() {}
        
        public Builder id(int id) {
            this.id = id;
            return this;
        }
        
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder world(String world) {
            this.world = world;
            return this;
        }
        
        public Builder x1(int x1) {
            this.x1 = x1;
            return this;
        }
        
        public Builder z1(int z1) {
            this.z1 = z1;
            return this;
        }
        
        public Builder x2(int x2) {
            this.x2 = x2;
            return this;
        }
        
        public Builder z2(int z2) {
            this.z2 = z2;
            return this;
        }

        public Builder teleportX(Double teleportX) {
            this.teleportX = teleportX;
            return this;
        }

        public Builder teleportY(Double teleportY) {
            this.teleportY = teleportY;
            return this;
        }

        public Builder teleportZ(Double teleportZ) {
            this.teleportZ = teleportZ;
            return this;
        }

        public Builder teleportYaw(Float teleportYaw) {
            this.teleportYaw = teleportYaw;
            return this;
        }

        public Builder teleportPitch(Float teleportPitch) {
            this.teleportPitch = teleportPitch;
            return this;
        }
        
        /**
         * Sets the coordinates, automatically normalizing min/max values.
         */
        public Builder coordinates(int x1, int z1, int x2, int z2) {
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);
            return this;
        }
        
        public Builder ownerId(int ownerId) {
            this.ownerId = ownerId;
            return this;
        }
        
        public Builder parentLandId(Integer parentLandId) {
            this.parentLandId = parentLandId;
            return this;
        }
        
        public Builder owner(Player owner) {
            this.owner = owner;
            return this;
        }
        
        public Builder flags(Set<LandFlag> flags) {
            this.flags = flags;
            return this;
        }
        
        public Builder trustedPlayers(List<Player> trustedPlayers) {
            this.trustedPlayers = trustedPlayers;
            return this;
        }
        
        public Land build() {
            return new Land(this);
        }
    }
    
    /**
     * Creates a new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWorld() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getZ1() {
        return z1;
    }

    public void setZ1(int z1) {
        this.z1 = z1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getZ2() {
        return z2;
    }

    public void setZ2(int z2) {
        this.z2 = z2;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getParentLandId() {
        return parentLandId;
    }

    public void setParentLandId(Integer parentLandId) {
        this.parentLandId = parentLandId;
    }

    public Double getTeleportX() {
        return teleportX;
    }

    public void setTeleportX(Double teleportX) {
        this.teleportX = teleportX;
    }

    public Double getTeleportY() {
        return teleportY;
    }

    public void setTeleportY(Double teleportY) {
        this.teleportY = teleportY;
    }

    public Double getTeleportZ() {
        return teleportZ;
    }

    public void setTeleportZ(Double teleportZ) {
        this.teleportZ = teleportZ;
    }

    public Float getTeleportYaw() {
        return teleportYaw;
    }

    public void setTeleportYaw(Float teleportYaw) {
        this.teleportYaw = teleportYaw;
    }

    public Float getTeleportPitch() {
        return teleportPitch;
    }

    public void setTeleportPitch(Float teleportPitch) {
        this.teleportPitch = teleportPitch;
    }

    public boolean isSubLand() {
        return parentLandId != null;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Set<LandFlag> getFlags() {
        return flags;
    }

    public void setFlags(Set<LandFlag> flags) {
        this.flags = flags;
    }

    public List<Player> getTrustedPlayers() {
        return trustedPlayers;
    }

    public void setTrustedPlayers(List<Player> trustedPlayers) {
        this.trustedPlayers = trustedPlayers;
    }

    /**
     * Checks if the given coordinates are within this land.
     *
     * @param x The X coordinate to check
     * @param z The Z coordinate to check
     * @return true if the coordinates are within the land, false otherwise
     */
    public boolean contains(int x, int z) {
        return x >= x1 && x <= x2 && z >= z1 && z <= z2;
    }

    /**
     * Gets the area of this land in blocks.
     *
     * @return The area in blocks
     */
    public int getArea() {
        return (x2 - x1 + 1) * (z2 - z1 + 1);
    }

    /**
     * Checks if a specific flag is enabled for this land.
     *
     * @param flag The flag to check
     * @return true if the flag is enabled, false otherwise
     */
    public boolean hasFlag(LandFlag flag) {
        return flags != null && flags.contains(flag);
    }

    /**
     * Checks if a player is trusted on this land.
     *
     * @param player The player to check
     * @return true if the player is trusted, false otherwise
     */
    public boolean isTrusted(Player player) {
        return trustedPlayers != null && trustedPlayers.contains(player);
    }

    @Override
    public String toString() {
        return "Land{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", world='" + world + '\'' +
                ", x1=" + x1 +
                ", z1=" + z1 +
                ", x2=" + x2 +
                ", z2=" + z2 +
                ", ownerId=" + ownerId +
                ", parentLandId=" + parentLandId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Land land = (Land) o;
        return id == land.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}