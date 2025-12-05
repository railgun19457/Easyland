package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 监听玩家伤害事件，实现领地 PvP 保护。
 */
public class PlayerProtectionListener extends BaseProtectionListener {

    /**
     * PlayerProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public PlayerProtectionListener(FlagManager flagManager) {
        super(flagManager);
    }

    /**
     * 处理实体伤害事件。
     * 如果是玩家之间的伤害且发生在领地内且 PVP 标志未启用，则阻止伤害。
     *
     * @param event 实体伤害事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // 如果事件已被取消，则不处理
        if (isEventCancelled(event)) {
            return;
        }

        // 获取受害者和攻击者
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        
        // 获取真实的攻击者（处理投射物）
        Entity realDamager = damager;
        if (damager instanceof org.bukkit.entity.Projectile) {
            org.bukkit.projectiles.ProjectileSource shooter = ((org.bukkit.entity.Projectile) damager).getShooter();
            if (shooter instanceof Entity) {
                realDamager = (Entity) shooter;
            }
        }

        // 1. 处理 PvP (玩家攻击玩家)
        if (victim instanceof Player && realDamager instanceof Player) {
            Player victimPlayer = (Player) victim;
            // 检查该位置是否允许 PvP
            if (!flagManager.isFlagEnabled(victimPlayer.getLocation(), LandFlag.PVP)) {
                event.setCancelled(true);
                sendDenyMessage((Player) realDamager, "permission.no-pvp");
            }
            return;
        }
        
        // 2. 处理 PvE (非玩家攻击玩家)
        if (victim instanceof Player && !(realDamager instanceof Player)) {
            Player victimPlayer = (Player) victim;
            // 检查该位置是否允许 PvE
            if (!flagManager.isFlagEnabled(victimPlayer.getLocation(), LandFlag.PVE)) {
                event.setCancelled(true);
            }
            return;
        }
    }

    /**
     * 处理玩家交互事件。
     * 如果玩家使用物品且 USE 标志未启用，则阻止使用。
     *
     * @param event 玩家交互事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isEventCancelled(event)) {
            return;
        }

        // 检查是否是使用物品
        if (event.hasItem()) {
            // 确定检查位置：如果是点击方块，则检查方块位置；否则检查玩家位置
            org.bukkit.Location location = event.getClickedBlock() != null ? 
                    event.getClickedBlock().getLocation() : event.getPlayer().getLocation();

            if (!flagManager.hasPermission(event.getPlayer(), location, LandFlag.USE)) {
                event.setCancelled(true);
                sendDenyMessage(event.getPlayer(), "permission.no-use");
            }
        }
    }
}