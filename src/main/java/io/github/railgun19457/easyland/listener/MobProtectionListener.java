package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
import org.bukkit.entity.Enemy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;

/**
 * 监听怪物生成事件，实现领地保护。
 */
public class MobProtectionListener extends BaseProtectionListener {

    /**
     * MobProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public MobProtectionListener(FlagManager flagManager) {
        super(flagManager);
    }

    /**
     * 处理生物生成事件。
     * 如果 MOB_SPAWNING 标志未启用，则阻止敌对生物生成。
     *
     * @param event 生物生成事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // 如果事件已被取消，则不处理
        if (isEventCancelled(event)) {
            return;
        }

        // 1. 只拦截敌对生物 (实现了 Enemy 接口的实体，如僵尸、骷髅、史莱姆、幻翼等)
        // 这样可以放行 动物(Animals)、村民(Villager) 等
        if (!(event.getEntity() instanceof Enemy)) {
            return;
        }

        // 2. 忽略自定义生成（插件行为）
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }

        // 3. 忽略刷怪笼生成 (允许玩家在领地内使用刷怪笼)
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
            return;
        }

        // 4. 检查领地标志
        if (!flagManager.isFlagEnabled(event.getLocation(), LandFlag.MOB_SPAWNING)) {
            // 再次确认排除列表（虽然上面已经排除了部分，这里可以处理更细致的逻辑）
            
            // 排除刷怪蛋 (通常由 interact 权限控制，这里放行以便玩家手动放置)
            if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                return;
            }

            // 拦截其他所有生成原因 (自然生成 NATURAL, 黑暗生成, 增援 REINFORCEMENTS 等)
            event.setCancelled(true);
        }
    }
}
