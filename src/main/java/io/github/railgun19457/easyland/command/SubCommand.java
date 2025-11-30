package io.github.railgun19457.easyland.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * 子命令抽象基类
 */
public abstract class SubCommand {

    /**
     * 执行命令
     * @param sender 命令发送者
     * @param args 命令参数（不包含子命令名称）
     * @return 是否执行成功
     */
    public abstract boolean execute(CommandSender sender, String[] args);

    /**
     * Tab 补全
     * @param sender 命令发送者
     * @param args 命令参数（不包含子命令名称）
     * @return 补全列表
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * 获取命令名称
     */
    public abstract String getName();

    /**
     * 获取命令描述
     */
    public abstract String getDescription();

    /**
     * 获取命令用法
     */
    public abstract String getUsage();

    /**
     * 是否需要玩家执行
     */
    public boolean requiresPlayer() {
        return true;
    }

    /**
     * 获取所需权限
     */
    public String getPermission() {
        return "easyland." + getName();
    }

    /**
     * 检查是否为玩家
     */
    protected boolean checkPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * 获取玩家对象
     */
    protected Player getPlayer(CommandSender sender) {
        return (Player) sender;
    }
}
