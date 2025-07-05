package com.example.easyland;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;

public class LandShowUtil {
    public static void showLandBoundary(JavaPlugin plugin, Player player, List<int[]> chunkRanges, int durationSeconds) {
        World world = player.getWorld();
        int y = player.getLocation().getBlockY() + 1;
        // 读取配置中的粒子类型
        String particleName = plugin.getConfig().getString("land-boundary-particle", "FLAME");
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (Exception e) {
            particle = Particle.FLAME;
        }
        final Particle finalParticle = particle;
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > durationSeconds * 20) {
                    cancel();
                    return;
                }
                for (int[] range : chunkRanges) {
                    int minX = Math.min(range[0], range[2]) << 4;
                    int maxX = (Math.max(range[0], range[2]) << 4) + 15;
                    int minZ = Math.min(range[1], range[3]) << 4;
                    int maxZ = (Math.max(range[1], range[3]) << 4) + 15;
                    for (int x = minX; x <= maxX; x++) {
                        world.spawnParticle(finalParticle, x + 0.5, y, minZ + 0.5, 1, 0, 0, 0, 0);
                        world.spawnParticle(finalParticle, x + 0.5, y, maxZ + 0.5, 1, 0, 0, 0, 0);
                    }
                    for (int z = minZ; z <= maxZ; z++) {
                        world.spawnParticle(finalParticle, minX + 0.5, y, z + 0.5, 1, 0, 0, 0, 0);
                        world.spawnParticle(finalParticle, maxX + 0.5, y, z + 0.5, 1, 0, 0, 0, 0);
                    }
                }
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L); // 每0.5秒刷新一次
    }
}
