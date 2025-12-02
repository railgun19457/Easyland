package io.github.railgun19457.easyland.listener;

import io.github.railgun19457.easyland.core.FlagManager;
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
     * 构造函数。
     *
     * @param flagManager 标志管理器
     */
    protected BaseProtectionListener(FlagManager flagManager) {
        this.flagManager = flagManager;
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
}
