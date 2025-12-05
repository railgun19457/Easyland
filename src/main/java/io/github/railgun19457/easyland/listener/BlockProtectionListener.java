package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
//import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * 监听方块破坏和放置事件，实现领地保护。
 */
public class BlockProtectionListener extends BaseProtectionListener {

    /**
     * BlockProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public BlockProtectionListener(FlagManager flagManager) {
        super(flagManager);
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
        if (isEventCancelled(event)) {
            return;
        }

        // 检查玩家是否有权限破坏该方块
        if (!flagManager.hasPermission(event.getPlayer(), event.getBlock().getLocation(), LandFlag.BREAK)) {
            event.setCancelled(true);
            sendDenyMessage(event.getPlayer(), "permission.no-break");
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
        if (isEventCancelled(event)) {
            return;
        }

        // 检查玩家是否有权限在该位置放置方块
        if (!flagManager.hasPermission(event.getPlayer(), event.getBlock().getLocation(), LandFlag.BUILD)) {
            event.setCancelled(true);
            sendDenyMessage(event.getPlayer(), "permission.no-build");
        }
    }

    /**
     * 处理方块点燃事件。
     * 如果是火焰蔓延且 FIRE_SPREAD 标志未启用，则阻止点燃。
     *
     * @param event 方块点燃事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        if (isEventCancelled(event)) {
            return;
        }

        // 检查是否是火焰蔓延
        if (event.getCause() == org.bukkit.event.block.BlockIgniteEvent.IgniteCause.SPREAD) {
            if (!flagManager.isFlagEnabled(event.getBlock().getLocation(), LandFlag.FIRE_SPREAD)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * 处理方块燃烧事件。
     * 如果 FIRE_SPREAD 标志未启用，则阻止方块燃烧。
     *
     * @param event 方块燃烧事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        if (isEventCancelled(event)) {
            return;
        }

        if (!flagManager.isFlagEnabled(event.getBlock().getLocation(), LandFlag.FIRE_SPREAD)) {
            event.setCancelled(true);
        }
    }
}