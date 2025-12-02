package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;

/**
 * 监听玩家交互事件，实现领地容器保护。
 */
public class ContainerProtectionListener implements Listener {
    private final FlagManager flagManager;

    /**
     * ContainerProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public ContainerProtectionListener(FlagManager flagManager) {
        this.flagManager = flagManager;
    }

    /**
     * 处理玩家交互事件。
     * 如果玩家与容器交互且没有权限且 INTERACT 标志未启用，则阻止交互。
     *
     * @param event 玩家交互事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 如果事件已被取消，则不处理
        // 检查事件是否已经被其他插件以某种方式处理或取消
        if (event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
            return;
        }

        // 只处理右键点击方块的情况
        if (event.getClickedBlock() == null || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        Material blockType = clickedBlock.getType();

        // 检查是否是容器或其他需要保护的方块
        if (isProtectedBlock(blockType)) {
            // 检查玩家是否有权限与该方块交互
            if (!flagManager.hasPermission(event.getPlayer(), clickedBlock.getLocation(), LandFlag.INTERACT)) {
                event.setCancelled(true);
                // 可以在这里添加消息通知玩家
                // event.getPlayer().sendMessage("你没有权限与此方块交互！");
            }
        }
    }

    /**
     * 检查方块类型是否是需要保护的方块。
     *
     * @param material 要检查的方块类型
     * @return 如果是需要保护的方块返回 true，否则返回 false
     */
    private boolean isProtectedBlock(Material material) {
        // 容器类
        if (material == Material.CHEST || 
            material == Material.TRAPPED_CHEST || 
            material == Material.ENDER_CHEST ||
            material == Material.SHULKER_BOX ||
            material.name().endsWith("_SHULKER_BOX")) {
            return true;
        }

        // 熔炉和工作台
        if (material == Material.FURNACE || 
            material == Material.BLAST_FURNACE || 
            material == Material.SMOKER ||
            material == Material.CRAFTING_TABLE ||
            material == Material.ANVIL ||
            material == Material.CHIPPED_ANVIL ||
            material == Material.DAMAGED_ANVIL ||
            material == Material.SMITHING_TABLE ||
            material == Material.FLETCHING_TABLE ||
            material == Material.GRINDSTONE ||
            material == Material.STONECUTTER ||
            material == Material.LOOM) {
            return true;
        }

        // 酿造台和其他功能性方块
        if (material == Material.BREWING_STAND || 
            material == Material.ENCHANTING_TABLE ||
            material == Material.BEACON ||
            material == Material.HOPPER ||
            material == Material.DROPPER ||
            material == Material.DISPENSER) {
            return true;
        }

        // 门和其他可交互方块
        if (material.name().endsWith("_DOOR") || 
            material.name().endsWith("_GATE") ||
            material.name().endsWith("_TRAPDOOR") ||
            material == Material.LEVER ||
            material == Material.STONE_BUTTON ||
            material == Material.OAK_BUTTON ||
            material.name().endsWith("_BUTTON") ||
            material.name().endsWith("_PRESSURE_PLATE")) {
            return true;
        }

        // 农作物和其他可交互方块
        if (material == Material.SWEET_BERRY_BUSH ||
            material == Material.CAVE_VINES ||
            material == Material.CAVE_VINES_PLANT ||
            material.name().endsWith("_CROP") ||
            material.name().endsWith("_STEM")) {
            return true;
        }

        return false;
    }
}