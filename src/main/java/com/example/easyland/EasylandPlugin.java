package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class EasylandPlugin extends JavaPlugin {
    private LandManager landManager;
    private LandSelectListener landSelectListener;
    private LandProtectionListener landProtectionListener;
    private LandEnterListener landEnterListener;

    @Override
    public void onEnable() {
        getLogger().info("EasylandPlugin 已启用！");
        File dataFile = new File(getDataFolder(), "lands.yml");
        landManager = new LandManager(dataFile);
        landSelectListener = new LandSelectListener();
        landProtectionListener = new LandProtectionListener(landManager);
        landEnterListener = new LandEnterListener(landManager);
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);
        // 注册 /easyland 指令（含别名）
        this.getCommand("easyland").setExecutor(new LandCommand(landManager, landSelectListener));
    }

    @Override
    public void onDisable() {
        if (landManager != null) landManager.saveLands();
        getLogger().info("EasylandPlugin 已禁用！");
    }
}
