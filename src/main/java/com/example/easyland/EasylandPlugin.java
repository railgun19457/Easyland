package com.example.easyland;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class EasylandPlugin extends JavaPlugin {
    private LandManager landManager;
    private LandSelectListener landSelectListener;
    private LandProtectionListener landProtectionListener;
    private LandEnterListener landEnterListener;
    private int maxLandsPerPlayer;
    private int maxChunksPerLand;
    private int showDurationSeconds;
    private int maxShowDurationSeconds;
    private boolean protectFromMobGriefing;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("EasylandPlugin 已启用！");
        File dataFile = new File(getDataFolder(), "lands.yml");
        maxLandsPerPlayer = getConfig().getInt("max-lands-per-player", 1);
        maxChunksPerLand = getConfig().getInt("max-chunks-per-land", 4);
        showDurationSeconds = getConfig().getInt("show-duration-seconds", 10);
        maxShowDurationSeconds = getConfig().getInt("max-show-duration-seconds", 300);
        protectFromMobGriefing = getConfig().getBoolean("protect-from-mob-griefing", true);
        landManager = new LandManager(dataFile, maxLandsPerPlayer, maxChunksPerLand);
        landSelectListener = new LandSelectListener(landManager);
        landProtectionListener = new LandProtectionListener(landManager, protectFromMobGriefing);
        landEnterListener = new LandEnterListener(landManager);
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(landSelectListener, this);
        getServer().getPluginManager().registerEvents(landProtectionListener, this);
        getServer().getPluginManager().registerEvents(landEnterListener, this);
        // 注册 /easyland 指令（含别名）
        LandCommand landCommand = new LandCommand(this, landManager, landSelectListener, showDurationSeconds, maxShowDurationSeconds);
        this.getCommand("easyland").setExecutor(landCommand);
        this.getCommand("easyland").setTabCompleter(landCommand);
    }

    @Override
    public void onDisable() {
        if (landManager != null) landManager.saveLands();
        getLogger().info("EasylandPlugin 已禁用！");
    }
}
