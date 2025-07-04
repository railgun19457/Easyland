package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;

public class EasylandPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("EasylandPlugin 已启用！");
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("EasylandPlugin 已禁用！");
    }
}
