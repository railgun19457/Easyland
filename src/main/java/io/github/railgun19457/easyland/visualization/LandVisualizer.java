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

        // Get player's Y coordinate as base
        double playerY = player.getLocation().getY();
        
        // Define colors
        // Corners: Gold/Orange (Prominent)
        Particle.DustOptions cornerColor = new Particle.DustOptions(Color.fromRGB(255, 170, 0), 1.5f);
        // Edges: Aqua (Visible and friendly)
        Particle.DustOptions edgeColor = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.0f);

        // Define height range (Feet and Head level)
        double yBottom = playerY + 0.2;
        double yTop = playerY + 2.2;

        // 1. Draw Corners
        drawCorner(player, world, x1, z1, yBottom, yTop, cornerColor);
        drawCorner(player, world, x1, z2, yBottom, yTop, cornerColor);
        drawCorner(player, world, x2, z1, yBottom, yTop, cornerColor);
        drawCorner(player, world, x2, z2, yBottom, yTop, cornerColor);

        // 2. Draw Edges

        // Draw along X axis
        for (int x = x1; x <= x2; x++) {
            // Skip corners to avoid overlapping (optional, but cleaner)
            if (x == x1 || x == x2) continue;
            
            spawnParticle(player, world, x + 0.5, yBottom, z1 + 0.5, edgeColor);
            spawnParticle(player, world, x + 0.5, yTop, z1 + 0.5, edgeColor);
            
            spawnParticle(player, world, x + 0.5, yBottom, z2 + 0.5, edgeColor);
            spawnParticle(player, world, x + 0.5, yTop, z2 + 0.5, edgeColor);
        }

        // Draw along Z axis
        for (int z = z1; z <= z2; z++) {
            // Skip corners
            if (z == z1 || z == z2) continue;

            spawnParticle(player, world, x1 + 0.5, yBottom, z + 0.5, edgeColor);
            spawnParticle(player, world, x1 + 0.5, yTop, z + 0.5, edgeColor);
            
            spawnParticle(player, world, x2 + 0.5, yBottom, z + 0.5, edgeColor);
            spawnParticle(player, world, x2 + 0.5, yTop, z + 0.5, edgeColor);
        }
    }

    /**
     * Draws a vertical pillar at a corner.
     */
    private void drawCorner(Player player, World world, int x, int z, double minY, double maxY, Particle.DustOptions options) {
        for (double y = minY; y <= maxY; y += 0.2) {
            spawnParticle(player, world, x + 0.5, y, z + 0.5, options);
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
     * @param options The particle options (color, size)
     */
    private void spawnParticle(Player player, World world, double x, double y, double z, Particle.DustOptions options) {
        Location location = new Location(world, x, y, z);
        player.spawnParticle(Particle.DUST, location, 1, options);
    }
}