package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.core.PermissionManager;
import io.github.railgun19457.easyland.core.SelectionManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 监听玩家使用领地选择工具的事件。
 */
public class SelectionToolListener implements Listener {
    
    private static final String TOOL_NAME_INDICATOR = "EasyLand";
    
    private final SelectionManager selectionManager;
    private final I18nManager i18nManager;
    private final PermissionManager permissionManager;
    
    public SelectionToolListener(SelectionManager selectionManager, 
                                  I18nManager i18nManager, PermissionManager permissionManager) {
        this.selectionManager = selectionManager;
        this.i18nManager = i18nManager;
        this.permissionManager = permissionManager;
    }
    
    /**
     * 检查物品是否是领地选择工具。
     */
    @SuppressWarnings("deprecation")
    private boolean isSelectionTool(ItemStack item) {
        if (item == null || item.getType() != Material.WOODEN_HOE) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        
        // 检查物品名称中是否包含工具标识
        String displayName = meta.getDisplayName();
        // 使用 Adventure API 或检查原始名称
        return displayName != null && displayName.contains(TOOL_NAME_INDICATOR);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // 检查是否手持选择工具
        if (!isSelectionTool(item)) {
            return;
        }
        
        // 检查权限
        if (!permissionManager.hasPermission(player, "easyland.select")) {
            player.sendMessage(i18nManager.getMessage("permission.no-select"));
            return;
        }
        
        Action action = event.getAction();
        Location clickedLocation = null;
        
        // 获取点击的位置
        if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock() != null) {
                clickedLocation = event.getClickedBlock().getLocation();
            }
        }
        
        if (clickedLocation == null) {
            return;
        }
        
        // 取消事件以防止破坏方块或其他交互
        event.setCancelled(true);
        
        // 根据动作设置位置
        if (action == Action.LEFT_CLICK_BLOCK) {
            // 左键设置第一个位置
            selectionManager.setPos1(player, clickedLocation);
            player.sendMessage(i18nManager.getMessage("select.pos1-set", 
                String.valueOf(clickedLocation.getBlockX()),
                String.valueOf(clickedLocation.getBlockY()),
                String.valueOf(clickedLocation.getBlockZ())));
            
            // 如果选区完成，显示面积信息
            showSelectionInfo(player);
            
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            // 右键设置第二个位置
            selectionManager.setPos2(player, clickedLocation);
            player.sendMessage(i18nManager.getMessage("select.pos2-set",
                String.valueOf(clickedLocation.getBlockX()),
                String.valueOf(clickedLocation.getBlockY()),
                String.valueOf(clickedLocation.getBlockZ())));
            
            // 如果选区完成，显示面积信息
            showSelectionInfo(player);
        }
    }
    
    /**
     * 如果选区完成，显示选区信息。
     */
    private void showSelectionInfo(Player player) {
        if (selectionManager.hasCompleteSelection(player)) {
            if (selectionManager.isSelectionInSameWorld(player)) {
                SelectionManager.Selection selection = selectionManager.getSelection(player);
                int area = selection.getArea();
                player.sendMessage(i18nManager.getMessage("select.selection-complete", String.valueOf(area)));
            } else {
                player.sendMessage(i18nManager.getMessage("select.different-worlds"));
            }
        }
    }
    
    /**
     * 玩家退出时清理选区数据。
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        selectionManager.removeSelection(event.getPlayer());
    }
}
