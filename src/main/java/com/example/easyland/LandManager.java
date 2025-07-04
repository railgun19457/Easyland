package com.example.easyland;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public class LandManager {
    private final Map<String, Land> lands = new HashMap<>();

    public boolean createLand(Player player, Location pos1, Location pos2) {
        if (lands.containsKey(player.getUniqueId().toString())) return false;
        lands.put(player.getUniqueId().toString(), new Land(player.getUniqueId().toString(), pos1, pos2));
        return true;
    }

    public Land getLand(Player player) {
        return lands.get(player.getUniqueId().toString());
    }

    public boolean isInLand(Location loc) {
        for (Land land : lands.values()) {
            if (land.contains(loc)) return true;
        }
        return false;
    }
}

class Land {
    private final String owner;
    private final Location pos1;
    private final Location pos2;

    public Land(String owner, Location pos1, Location pos2) {
        this.owner = owner;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public boolean contains(Location loc) {
        if (!loc.getWorld().equals(pos1.getWorld())) return false;
        double x1 = Math.min(pos1.getX(), pos2.getX());
        double x2 = Math.max(pos1.getX(), pos2.getX());
        double y1 = Math.min(pos1.getY(), pos2.getY());
        double y2 = Math.max(pos1.getY(), pos2.getY());
        double z1 = Math.min(pos1.getZ(), pos2.getZ());
        double z2 = Math.max(pos1.getZ(), pos2.getZ());
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    public String getOwner() { return owner; }
}
