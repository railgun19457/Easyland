package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.EasyLand;
import io.github.railgun19457.easyland.I18nManager;
import io.github.railgun19457.easyland.core.FlagManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Listener;

/**
 * 保护监听器的抽象基类。
 * 提供所有保护监听器共用的功能和字段。
 */
public abstract class BaseProtectionListener implements Listener {
    
    /**
     * 标志管理器实例，用于检查权限和标志状态。
     */
    protected final FlagManager flagManager;
    
    /**
     * 国际化管理器实例，用于获取消息。
     */
    protected final I18nManager i18nManager;
    
    /**
     * 构造函数。
     *
     * @param flagManager 标志管理器
     */
    protected BaseProtectionListener(FlagManager flagManager) {
        this.flagManager = flagManager;
        this.i18nManager = org.bukkit.plugin.java.JavaPlugin.getPlugin(EasyLand.class).getI18nManager();
    }
    
    /**
     * 检查事件是否已被取消。
     * 用于在处理事件之前快速检查是否需要继续处理。
     *
     * @param event 可取消的事件
     * @return 如果事件已被取消返回true，否则返回false
     */
    protected boolean isEventCancelled(Cancellable event) {
        return event.isCancelled();
    }
    
    /**
     * 获取标志管理器。
     *
     * @return 标志管理器实例
     */
    protected FlagManager getFlagManager() {
        return flagManager;
    }

    /**
     * 发送拒绝消息给玩家（ActionBar）。
     *
     * @param player 玩家
     * @param messageKey 消息键
     */
    protected void sendDenyMessage(Player player, String messageKey) {
        String message = i18nManager.getMessage(messageKey);
        // 如果消息键不存在，使用默认消息
        if (message.equals(messageKey)) {
            switch (messageKey) {
                case "permission.no-break":
                    message = "§c你没有权限在此处破坏方块！";
                    break;
                case "permission.no-build":
                    message = "§c你没有权限在此处建造！";
                    break;
                case "permission.no-interact":
                    message = "§c你没有权限在此处交互！";
                    break;
                case "permission.no-use":
                    message = "§c你没有权限在此处使用物品！";
                    break;
                case "permission.no-pvp":
                    message = "§c此处禁止 PvP！";
                    break;
                default:
                    message = "§c你没有权限执行此操作！";
            }
        }
        player.sendActionBar(Component.text(message));
    }
}
