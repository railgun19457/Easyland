package io.github.railgun19457.easyland.model;

/**
 * Enumeration of land protection flags.
 * Each flag represents a specific permission or setting for a land.
 */
public enum LandFlag {
    /**
     * Allows players to build/place blocks on the land.
     */
    BUILD("build", "允许建造/放置方块"),

    /**
     * Allows players to break blocks on the land.
     */
    BREAK("break", "允许破坏方块"),

    /**
     * Allows players to interact with blocks (chests, doors, buttons, etc.).
     */
    INTERACT("interact", "允许与方块交互"),

    /**
     * Allows players to use items on the land.
     */
    USE("use", "允许使用物品"),

    /**
     * Allows players to enter the land.
     */
    ENTER("enter", "允许进入领地"),

    /**
     * Allows mobs to spawn on the land.
     */
    MOB_SPAWNING("mob_spawning", "允许怪物生成"),

    /**
     * Allows PvP (Player vs Player) combat on the land.
     */
    PVP("pvp", "允许玩家对战"),

    /**
     * Allows explosions on the land.
     */
    EXPLOSIONS("explosions", "允许爆炸"),

    /**
     * Allows fire to spread on the land.
     */
    FIRE_SPREAD("fire_spread", "允许火焰蔓延");

    private final String name;
    private final String description;

    /**
     * Constructor for LandFlag enum.
     *
     * @param name        The flag name used in database and commands
     * @param description The description of what this flag controls
     */
    LandFlag(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the flag name.
     *
     * @return The flag name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the flag description.
     *
     * @return The flag description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Finds a LandFlag by its name.
     *
     * @param name The name to search for
     * @return The LandFlag if found, null otherwise
     */
    public static LandFlag fromName(String name) {
        for (LandFlag flag : values()) {
            if (flag.name.equalsIgnoreCase(name)) {
                return flag;
            }
        }
        return null;
    }

    /**
     * Gets all available flag names.
     *
     * @return Array of flag names
     */
    public static String[] getFlagNames() {
        LandFlag[] flags = values();
        String[] names = new String[flags.length];
        for (int i = 0; i < flags.length; i++) {
            names[i] = flags[i].name;
        }
        return names;
    }

    @Override
    public String toString() {
        return name;
    }
}