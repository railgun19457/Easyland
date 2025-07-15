package com.example.easyland;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 配置文件管理器
 * 负责检查、验证和修复配置文件
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, Object> standardConfig;
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.standardConfig = createStandardConfig();
    }
    
    /**
     * 创建标准配置模板
     */
    private Map<String, Object> createStandardConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 基础配置
        config.put("max-lands-per-player", 5);
        config.put("max-chunks-per-land", 256);
        config.put("land-boundary-particle", "firework");
        config.put("show-duration-seconds", 10);
        config.put("max-show-duration-seconds", 300);
        config.put("message-cooldown-seconds", 3);
        
        // 保护规则配置 - 默认为false，确保不影响原版体验
        Map<String, Object> protection = new HashMap<>();
        protection.put("block-protection", false);
        protection.put("explosion-protection", false);
        protection.put("container-protection", false);
        protection.put("player-protection", false);
        config.put("protection", protection);
        
        return config;
    }
    
    /**
     * 检查并修复配置文件
     * @return 是否进行了修复
     */
    public boolean checkAndFixConfig() {
        FileConfiguration config = plugin.getConfig();
        boolean hasChanges = false;
        
        // 删除多余的配置项
        hasChanges |= removeExtraConfigs(config);
        
        // 验证配置值的有效性
        hasChanges |= validateConfigValues(config);
        
        if (hasChanges) {
            plugin.saveConfig();
            plugin.getLogger().info("配置文件已修复并保存！");
        }
        
        return hasChanges;
    }
    
    /**
     * 删除多余的配置项
     */
    private boolean removeExtraConfigs(FileConfiguration config) {
        boolean hasChanges = false;
        
        Set<String> validKeys = getAllValidKeys();
        Set<String> currentKeys = config.getKeys(true);
        
        for (String key : currentKeys) {
            if (!validKeys.contains(key) && !isDeprecatedConfig(key)) {
                plugin.getLogger().info("删除多余的配置项: " + key);
                config.set(key, null);
                hasChanges = true;
            }
        }
        
        return hasChanges;
    }
    
    /**
     * 获取所有有效的配置键
     */
    private Set<String> getAllValidKeys() {
        Set<String> validKeys = new HashSet<>();
        
        for (Map.Entry<String, Object> entry : standardConfig.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            validKeys.add(key);
            
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> section = (Map<String, Object>) value;
                for (String subKey : section.keySet()) {
                    validKeys.add(key + "." + subKey);
                }
            }
        }
        
        return validKeys;
    }
    
    /**
     * 检查是否是已弃用的配置项
     */
    private boolean isDeprecatedConfig(String key) {
        Set<String> deprecatedKeys = new HashSet<>(Arrays.asList(
            "protect-from-mob-griefing"
        ));
        return deprecatedKeys.contains(key);
    }
    
    /**
     * 验证配置值的有效性
     */
    private boolean validateConfigValues(FileConfiguration config) {
        boolean hasChanges = false;
        
        // 验证数值范围
        hasChanges |= validateIntRange(config, "max-lands-per-player", 1, 100, 5);
        hasChanges |= validateIntRange(config, "max-chunks-per-land", 1, 10000, 256);
        hasChanges |= validateIntRange(config, "show-duration-seconds", 1, 60, 10);
        hasChanges |= validateIntRange(config, "max-show-duration-seconds", 10, 3600, 300);
        hasChanges |= validateIntRange(config, "message-cooldown-seconds", 0, 60, 3);
        
        // 验证粒子类型
        hasChanges |= validateParticleType(config);
        
        return hasChanges;
    }
    
    /**
     * 验证整数范围
     */
    private boolean validateIntRange(FileConfiguration config, String key, int min, int max, int defaultValue) {
        if (config.contains(key)) {
            int value = config.getInt(key);
            if (value < min || value > max) {
                plugin.getLogger().warning("配置项 " + key + " 的值 (" + value + ") 超出有效范围 [" + min + "-" + max + "]，已重置为默认值: " + defaultValue);
                config.set(key, defaultValue);
                return true;
            }
        }
        return false;
    }
    
    /**
     * 验证粒子类型
     */
    private boolean validateParticleType(FileConfiguration config) {
        String key = "land-boundary-particle";
        if (config.contains(key)) {
            String particle = config.getString(key);
            Set<String> validParticles = new HashSet<>(Arrays.asList(
                "flame", "smoke", "portal", "heart", "firework", "end_rod", 
                "dragon_breath", "damage_indicator", "sweep_attack"
            ));
            
            if (particle != null && !validParticles.contains(particle.toLowerCase())) {
                plugin.getLogger().warning("无效的粒子类型: " + particle + "，已重置为默认值: firework");
                config.set(key, "firework");
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取配置值，如果不存在则返回默认值
     */
    public <T> T getConfigValue(String key, T defaultValue) {
        FileConfiguration config = plugin.getConfig();
        if (config.contains(key)) {
            Object value = config.get(key);
            try {
                @SuppressWarnings("unchecked")
                T result = (T) value;
                return result;
            } catch (ClassCastException e) {
                plugin.getLogger().warning("配置项 " + key + " 的类型不正确，使用默认值: " + defaultValue);
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
