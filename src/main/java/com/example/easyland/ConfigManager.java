package com.example.easyland;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * 配置文件管理器
 * 负责检查、验证和修复配置文件
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, Object> standardConfig;
    
    // 常量定义
    private static final String[] PROTECTION_RULES = {
        "block-protection", "explosion-protection", "container-protection", "player-protection"
    };
    
    private static final Set<String> VALID_PARTICLES = Set.of(
        "flame", "smoke", "portal", "heart", "firework", "end_rod", 
        "dragon_breath", "damage_indicator", "sweep_attack"
    );
    
    private static final Set<String> DEPRECATED_KEYS = Set.of(
        "protect-from-mob-griefing"
    );
    
    // 配置键常量
    private static final String KEY_MAX_LANDS = "max-lands-per-player";
    private static final String KEY_MAX_CHUNKS = "max-chunks-per-land";
    private static final String KEY_PARTICLE = "land-boundary-particle";
    private static final String KEY_SHOW_DURATION = "show-duration-seconds";
    private static final String KEY_MAX_SHOW_DURATION = "max-show-duration-seconds";
    private static final String KEY_MESSAGE_COOLDOWN = "message-cooldown-seconds";
    private static final String KEY_PROTECTION = "protection";
    
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
        config.put(KEY_MAX_LANDS, 5);
        config.put(KEY_MAX_CHUNKS, 256);
        config.put(KEY_PARTICLE, "firework");
        config.put(KEY_SHOW_DURATION, 10);
        config.put(KEY_MAX_SHOW_DURATION, 300);
        config.put(KEY_MESSAGE_COOLDOWN, 3);
        
        // 保护规则配置
        config.put(KEY_PROTECTION, createProtectionRulesConfig());
        
        return config;
    }
    
    /**
     * 创建保护规则配置
     */
    private Map<String, Object> createProtectionRulesConfig() {
        Map<String, Object> protection = new HashMap<>();
        
        for (String ruleName : PROTECTION_RULES) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("enable", true);
            rule.put("default", false);
            protection.put(ruleName, rule);
        }
        
        return protection;
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
            if (!validKeys.contains(key) && !DEPRECATED_KEYS.contains(key)) {
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
        
        collectValidKeys(standardConfig, "", validKeys);
        
        return validKeys;
    }
    
    /**
     * 递归收集有效配置键
     */
    private void collectValidKeys(Map<String, Object> configMap, String prefix, Set<String> validKeys) {
        for (Map.Entry<String, Object> entry : configMap.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            validKeys.add(key);
            
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) entry.getValue();
                collectValidKeys(subMap, key, validKeys);
            }
        }
    }
    
    /**
     * 验证配置值的有效性
     */
    private boolean validateConfigValues(FileConfiguration config) {
        boolean hasChanges = false;
        
        // 验证数值范围
        hasChanges |= validateIntRange(config, KEY_MAX_LANDS, 1, 100, 5);
        hasChanges |= validateIntRange(config, KEY_MAX_CHUNKS, 1, 10000, 256);
        hasChanges |= validateIntRange(config, KEY_SHOW_DURATION, 1, 60, 10);
        hasChanges |= validateIntRange(config, KEY_MAX_SHOW_DURATION, 10, 3600, 300);
        hasChanges |= validateIntRange(config, KEY_MESSAGE_COOLDOWN, 0, 60, 3);
        
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
                plugin.getLogger().warning(String.format(
                    "配置项 %s 的值 (%d) 超出有效范围 [%d-%d]，已重置为默认值: %d", 
                    key, value, min, max, defaultValue));
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
        if (config.contains(KEY_PARTICLE)) {
            String particle = config.getString(KEY_PARTICLE);
            if (particle != null && !VALID_PARTICLES.contains(particle.toLowerCase())) {
                plugin.getLogger().warning("无效的粒子类型: " + particle + "，已重置为默认值: firework");
                config.set(KEY_PARTICLE, "firework");
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
    
    /**
     * 获取默认保护规则
     */
    public Map<String, Boolean> getDefaultProtectionRules() {
        Map<String, Boolean> defaultRules = new HashMap<>();
        
        for (String ruleName : PROTECTION_RULES) {
            boolean defaultValue = getConfigValue(KEY_PROTECTION + "." + ruleName + ".default", false);
            defaultRules.put(ruleName, defaultValue);
        }
        
        return defaultRules;
    }
    
    /**
     * 检查保护规则是否被服务器允许启用
     */
    public boolean isProtectionRuleEnabled(String ruleName) {
        return getConfigValue(KEY_PROTECTION + "." + ruleName + ".enable", true);
    }
    
    /**
     * 获取所有保护规则名称
     */
    public static String[] getProtectionRules() {
        return PROTECTION_RULES.clone();
    }
}
