package io.github.railgun19457.easyland.visualization;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.core.ConfigManager;
import io.github.railgun19457.easyland.model.Land;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the visualization of land boundaries using particles.
 */
public class LandVisualizer {
    private final EasyLand plugin;
    private final ConfigManager configManager;
    private final Map<UUID, BukkitRunnable> activeVisualizations = new HashMap<>();

    public LandVisualizer(EasyLand plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    /**
     * Shows the land boundary for a player for a specified duration.
     *
     * @param player   The player to show the visualization to
     * @param land     The land to visualize
     * @param duration The duration in seconds
     */
    public void showLandBoundary(Player player, Land land, int duration) {
        // Cancel any existing visualization for this player
        cancelVisualization(player);

        int maxDuration = configManager.getMaxVisualizationDuration();
        final int finalDuration = Math.min(duration, maxDuration);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = finalDuration * 20; // 20 ticks per second

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    this.cancel();
                    activeVisualizations.remove(player.getUniqueId());
                    return;
                }

                // Only show particles every 5 ticks to reduce spam
                if (ticks % 5 == 0) {
                    drawLandBoundary(player, land);
                }

                ticks++;
            }
        };

        task.runTaskTimer(plugin, 0, 1);
        activeVisualizations.put(player.getUniqueId(), task);
    }

    /**
     * Cancels any active visualization for a player.
     *
     * @param player The player
     */
    public void cancelVisualization(Player player) {
        BukkitRunnable task = activeVisualizations.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Draws the land boundary using particles.
     *
     * @param player The player to show the particles to
     * @param land   The land to draw
     */
    private void drawLandBoundary(Player player, Land land) {
        World world = Bukkit.getWorld(land.getWorld());
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }

        int x1 = land.getX1();
        int z1 = land.getZ1();
        int x2 = land.getX2();
        int z2 = land.getZ2();

        // Find a safe Y level to display particles (player's Y level or 64 if underground)
        int y = Math.max(player.getLocation().getBlockY(), 64);

        // Draw the perimeter
        for (int x = x1; x <= x2; x++) {
            spawnParticle(player, world, x, y, z1);
            spawnParticle(player, world, x, y, z2);
        }

        for (int z = z1; z <= z2; z++) {
            spawnParticle(player, world, x1, y, z);
            spawnParticle(player, world, x2, y, z);
        }
    }

    /**
     * Spawns a single particle at a location for a player.
     *
     * @param player The player
     * @param world  The world
     * @param x      The X coordinate
     * @param y      The Y coordinate
     * @param z      The Z coordinate
     */
    private void spawnParticle(Player player, World world, int x, int y, int z) {
        Location location = new Location(world, x + 0.5, y + 0.5, z + 0.5);
        player.spawnParticle(Particle.DUST, location, 1, new Particle.DustOptions(Color.RED, 1.0f));
    }
}