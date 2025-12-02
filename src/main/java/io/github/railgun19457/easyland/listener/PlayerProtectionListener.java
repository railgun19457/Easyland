package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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

        // 检查是否是玩家之间的伤害
        if (!(victim instanceof Player) || !(damager instanceof Player)) {
            return;
        }

        Player victimPlayer = (Player) victim;

        // 检查该位置是否允许 PvP
        if (!flagManager.isFlagEnabled(victimPlayer.getLocation(), LandFlag.PVP)) {
            event.setCancelled(true);
            // 可以在这里添加消息通知玩家
            // ((Player) damager).sendMessage("此区域不允许 PvP！");
        }
    }
}