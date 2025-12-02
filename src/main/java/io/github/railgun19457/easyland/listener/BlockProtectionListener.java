package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
//import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * 监听方块破坏和放置事件，实现领地保护。
 */
public class BlockProtectionListener implements Listener {
    private final FlagManager flagManager;

    /**
     * BlockProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public BlockProtectionListener(FlagManager flagManager) {
        this.flagManager = flagManager;
    }

    /**
     * 处理方块破坏事件。
     * 如果玩家没有权限且 BREAK 标志未启用，则阻止破坏。
     *
     * @param event 方块破坏事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        // 如果事件已被取消，则不处理
        if (event.isCancelled()) {
            return;
        }

        // 检查玩家是否有权限破坏该方块
        if (!flagManager.hasPermission(event.getPlayer(), event.getBlock().getLocation(), LandFlag.BREAK)) {
            event.setCancelled(true);
            // 可以在这里添加消息通知玩家
            // event.getPlayer().sendMessage("你没有权限在此处破坏方块！");
        }
    }

    /**
     * 处理方块放置事件。
     * 如果玩家没有权限且 BUILD 标志未启用，则阻止放置。
     *
     * @param event 方块放置事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        // 如果事件已被取消，则不处理
        if (event.isCancelled()) {
            return;
        }

        // 检查玩家是否有权限在该位置放置方块
        if (!flagManager.hasPermission(event.getPlayer(), event.getBlock().getLocation(), LandFlag.BUILD)) {
            event.setCancelled(true);
            // 可以在这里添加消息通知玩家
            // event.getPlayer().sendMessage("你没有权限在此处建造！");
        }
    }
}