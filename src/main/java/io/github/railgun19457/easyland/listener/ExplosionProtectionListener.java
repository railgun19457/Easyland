package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
import org.bukkit.Location;
//import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;
import java.util.List;

/**
 * 监听爆炸事件，实现领地爆炸保护。
 */
public class ExplosionProtectionListener extends BaseProtectionListener {

    /**
     * ExplosionProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public ExplosionProtectionListener(FlagManager flagManager) {
        super(flagManager);
    }

    /**
     * 处理实体爆炸事件。
     * 如果爆炸发生在领地内且 EXPLOSIONS 标志未启用，则阻止爆炸破坏方块。
     *
     * @param event 实体爆炸事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        // 如果事件已被取消，则不处理
        if (isEventCancelled(event)) {
            return;
        }

        // 获取爆炸影响的方块列表
        List<org.bukkit.block.Block> blockList = event.blockList();
        
        // 使用迭代器安全地遍历和修改列表
        Iterator<org.bukkit.block.Block> iterator = blockList.iterator();
        while (iterator.hasNext()) {
            org.bukkit.block.Block block = iterator.next();
            Location location = block.getLocation();
            
            // 检查该位置是否允许爆炸
            if (!flagManager.isFlagEnabled(location, LandFlag.EXPLOSIONS)) {
                // 如果不允许爆炸，则从受影响的方块列表中移除该方块
                iterator.remove();
            }
        }
        
        // 如果所有方块都被移除了，可以考虑取消整个爆炸事件
        // 但这里我们保留爆炸效果，只是不破坏方块
    }
}