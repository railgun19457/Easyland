package com.example.easyland;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.List;

public class LandShowUtil {
    
    private static final int PARTICLE_SPREAD = 2;
    private static final double PARTICLE_OFFSET = 0.2;
    private static final int HEIGHT_LAYERS = 3; // 显示3层高度
    
    public static void showLandBoundary(JavaPlugin plugin, Player player, List<int[]> chunkRanges, int durationSeconds) {
        World world = player.getWorld();
        int baseY = player.getLocation().getBlockY() + 1;
        
        // 读取配置中的粒子类型，优化异常处理
        Particle particle = getParticleFromConfig(plugin);
        
        new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = durationSeconds * 20;
            
            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    cancel();
                    return;
                }
                
                // 预计算边界坐标，避免重复计算
                for (int[] range : chunkRanges) {
                    BoundaryCoords coords = new BoundaryCoords(range);
                    drawBoundary(world, coords, baseY, particle);
                }
                
                ticks += 10; // 每10tick（0.5秒）更新一次
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    /**
     * 从配置获取粒子类型，优化错误处理
     */
    private static Particle getParticleFromConfig(JavaPlugin plugin) {
        String particleName = plugin.getConfig().getString("land-boundary-particle", "FIREWORK");
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的粒子类型: " + particleName + "，使用默认值: FIREWORK");
            return Particle.FIREWORK;
        }
    }
    
    /**
     * 绘制边界线
     */
    private static void drawBoundary(World world, BoundaryCoords coords, int baseY, Particle particle) {
        // 绘制水平线（上下边界）
        for (int x = coords.minX - 1; x <= coords.maxX + 1; x++) {
            for (int dy = 0; dy < HEIGHT_LAYERS; dy++) {
                // 上边界
                world.spawnParticle(particle, x + 0.5, baseY + dy, coords.minZ - 0.5, 
                    PARTICLE_SPREAD, PARTICLE_OFFSET, 0, PARTICLE_OFFSET, 0);
                // 下边界
                world.spawnParticle(particle, x + 0.5, baseY + dy, coords.maxZ + 1.5, 
                    PARTICLE_SPREAD, PARTICLE_OFFSET, 0, PARTICLE_OFFSET, 0);
            }
        }
        
        // 绘制垂直线（左右边界）
        for (int z = coords.minZ - 1; z <= coords.maxZ + 1; z++) {
            for (int dy = 0; dy < HEIGHT_LAYERS; dy++) {
                // 左边界
                world.spawnParticle(particle, coords.minX - 0.5, baseY + dy, z + 0.5, 
                    PARTICLE_SPREAD, 0, PARTICLE_OFFSET, PARTICLE_OFFSET, 0);
                // 右边界
                world.spawnParticle(particle, coords.maxX + 1.5, baseY + dy, z + 0.5, 
                    PARTICLE_SPREAD, 0, PARTICLE_OFFSET, PARTICLE_OFFSET, 0);
            }
        }
    }
    
    /**
     * 边界坐标类，预计算边界值避免重复计算
     */
    private static class BoundaryCoords {
        final int minX, maxX, minZ, maxZ;
        
        BoundaryCoords(int[] range) {
            int chunkMinX = Math.min(range[0], range[2]);
            int chunkMaxX = Math.max(range[0], range[2]);
            int chunkMinZ = Math.min(range[1], range[3]);
            int chunkMaxZ = Math.max(range[1], range[3]);
            
            this.minX = chunkMinX << 4;
            this.maxX = (chunkMaxX << 4) + 15;
            this.minZ = chunkMinZ << 4;
            this.maxZ = (chunkMaxZ << 4) + 15;
        }
    }
}
