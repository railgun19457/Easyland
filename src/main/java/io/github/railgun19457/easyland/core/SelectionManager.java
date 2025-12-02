package io.github.railgun19457.easyland.core;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理玩家的领地选区。
 * 存储玩家使用选择工具选择的两个位置点。
 */
public class SelectionManager {
    
    /**
     * 存储玩家选区信息的内部类。
     */
    public static class Selection {
        private Location pos1;
        private Location pos2;
        
        public Location getPos1() {
            return pos1;
        }
        
        public void setPos1(Location pos1) {
            this.pos1 = pos1;
        }
        
        public Location getPos2() {
            return pos2;
        }
        
        public void setPos2(Location pos2) {
            this.pos2 = pos2;
        }
        
        /**
         * 检查选区是否完整（两个位置都已设置）。
         */
        public boolean isComplete() {
            return pos1 != null && pos2 != null;
        }
        
        /**
         * 检查两个位置是否在同一个世界。
         */
        public boolean isInSameWorld() {
            if (!isComplete()) {
                return false;
            }
            return pos1.getWorld().equals(pos2.getWorld());
        }
        
        /**
         * 获取选区面积。
         */
        public int getArea() {
            if (!isComplete() || !isInSameWorld()) {
                return 0;
            }
            int x1 = Math.min(pos1.getBlockX(), pos2.getBlockX());
            int z1 = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
            int x2 = Math.max(pos1.getBlockX(), pos2.getBlockX());
            int z2 = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
            return (x2 - x1 + 1) * (z2 - z1 + 1);
        }
        
        /**
         * 清除选区。
         */
        public void clear() {
            pos1 = null;
            pos2 = null;
        }
    }
    
    // 玩家UUID到选区的映射
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();
    
    /**
     * 设置玩家的第一个选区位置。
     *
     * @param player   玩家
     * @param location 位置
     */
    public void setPos1(Player player, Location location) {
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
        selection.setPos1(location.clone());
    }
    
    /**
     * 设置玩家的第二个选区位置。
     *
     * @param player   玩家
     * @param location 位置
     */
    public void setPos2(Player player, Location location) {
        Selection selection = selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
        selection.setPos2(location.clone());
    }
    
    /**
     * 获取玩家的第一个选区位置。
     *
     * @param player 玩家
     * @return 第一个位置，如果未设置则返回 null
     */
    public Location getPos1(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection != null ? selection.getPos1() : null;
    }
    
    /**
     * 获取玩家的第二个选区位置。
     *
     * @param player 玩家
     * @return 第二个位置，如果未设置则返回 null
     */
    public Location getPos2(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection != null ? selection.getPos2() : null;
    }
    
    /**
     * 获取玩家的选区。
     *
     * @param player 玩家
     * @return 选区对象，如果未设置则返回 null
     */
    public Selection getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }
    
    /**
     * 检查玩家的选区是否完整。
     *
     * @param player 玩家
     * @return 如果选区完整返回 true，否则返回 false
     */
    public boolean hasCompleteSelection(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection != null && selection.isComplete();
    }
    
    /**
     * 检查玩家的选区是否在同一个世界。
     *
     * @param player 玩家
     * @return 如果在同一个世界返回 true，否则返回 false
     */
    public boolean isSelectionInSameWorld(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        return selection != null && selection.isInSameWorld();
    }
    
    /**
     * 清除玩家的选区。
     *
     * @param player 玩家
     */
    public void clearSelection(Player player) {
        Selection selection = selections.get(player.getUniqueId());
        if (selection != null) {
            selection.clear();
        }
    }
    
    /**
     * 移除玩家的选区（玩家退出时调用）。
     *
     * @param player 玩家
     */
    public void removeSelection(Player player) {
        selections.remove(player.getUniqueId());
    }
    
    /**
     * 移除玩家的选区（通过UUID）。
     *
     * @param uuid 玩家UUID
     */
    public void removeSelection(UUID uuid) {
        selections.remove(uuid);
    }
}
