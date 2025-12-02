package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
import io.github.railgun19457.easyland.model.LandFlag;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Set;

/**
 * 监听玩家交互事件，实现领地容器保护。
 */
public class ContainerProtectionListener extends BaseProtectionListener {
    
    /**
     * 需要保护的容器类型集合。
     * 包含所有明确指定的方块类型。
     */
    private static final Set<Material> PROTECTED_CONTAINERS = Set.of(
        // 容器类
        Material.CHEST,
        Material.TRAPPED_CHEST,
        Material.ENDER_CHEST,
        Material.SHULKER_BOX,
        // 熔炉和工作台
        Material.FURNACE,
        Material.BLAST_FURNACE,
        Material.SMOKER,
        Material.CRAFTING_TABLE,
        Material.ANVIL,
        Material.CHIPPED_ANVIL,
        Material.DAMAGED_ANVIL,
        Material.SMITHING_TABLE,
        Material.FLETCHING_TABLE,
        Material.GRINDSTONE,
        Material.STONECUTTER,
        Material.LOOM,
        // 酿造台和其他功能性方块
        Material.BREWING_STAND,
        Material.ENCHANTING_TABLE,
        Material.BEACON,
        Material.HOPPER,
        Material.DROPPER,
        Material.DISPENSER,
        // 其他可交互方块
        Material.LEVER,
        Material.STONE_BUTTON,
        Material.OAK_BUTTON,
        Material.SWEET_BERRY_BUSH,
        Material.CAVE_VINES,
        Material.CAVE_VINES_PLANT
    );
    
    /**
     * 需要保护的方块后缀列表。
     * 用于匹配以这些后缀结尾的方块类型。
     */
    private static final List<String> PROTECTED_SUFFIXES = List.of(
        "_SHULKER_BOX",
        "_DOOR",
        "_GATE",
        "_TRAPDOOR",
        "_BUTTON",
        "_PRESSURE_PLATE",
        "_CROP",
        "_STEM"
    );

    /**
     * ContainerProtectionListener 构造函数。
     *
     * @param flagManager 标志管理器
     */
    public ContainerProtectionListener(FlagManager flagManager) {
        super(flagManager);
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
     * 使用常量集合和后缀列表进行高效匹配。
     *
     * @param material 要检查的方块类型
     * @return 如果是需要保护的方块返回 true，否则返回 false
     */
    private boolean isProtectedBlock(Material material) {
        // 首先检查是否在明确指定的保护容器集合中
        if (PROTECTED_CONTAINERS.contains(material)) {
            return true;
        }
        
        // 然后检查是否匹配任何保护后缀
        String materialName = material.name();
        return PROTECTED_SUFFIXES.stream().anyMatch(materialName::endsWith);
    }
}