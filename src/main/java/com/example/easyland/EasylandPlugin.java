package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;

public class EasylandPlugin extends JavaPlugin {
    private LandManager landManager;

    @Override
    public void onEnable() {
        getLogger().info("EasylandPlugin 已启用！");
        landManager = new LandManager();
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(), this);
        // 注册 /easyland 指令（含别名）
        this.getCommand("easyland").setExecutor(new LandCommand(landManager));
    }

    @Override
    public void onDisable() {
        getLogger().info("EasylandPlugin 已禁用！");
    }
}
