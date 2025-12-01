package io.github.railgun19457.easyland.manager;

import io.github.railgun19457.easyland.config.PluginConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 配置文件管理器
 * 负责检查、验证和修复配置文件
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, Object> standardConfig;
    private volatile PluginConfig pluginConfig;
    
    // 常量定义
    private static final String[] PROTECTION_RULES = {
            "block-protection", "explosion-protection", "container-protection", "player-protection"
    };

    private static final Set<String> VALID_PARTICLES = Set.of(
            "flame", "smoke", "portal", "heart", "firework", "end_rod",
            "dragon_breath", "damage_indicator", "sweep_attack");

    private static final Set<String> DEPRECATED_KEYS = Set.of(
            "protect-from-mob-griefing");

    // 配置键常量
    private static final String KEY_LANGUAGE = "language";
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
        // 初始化时加载一次配置
        loadConfigIntoRecord();
    }

    /**
     * 创建标准配置模板
     */
    private Map<String, Object> createStandardConfig() {
        Map<String, Object> config = new HashMap<>();

        // 基础配置
        config.put(KEY_LANGUAGE, "zh_cn");
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
     * 
     * @return 是否进行了修复
     */
    public boolean checkAndFixConfig() {
        FileConfiguration config = plugin.getConfig();
        boolean hasChanges = false;

        // 添加缺失的配置项
        hasChanges |= addMissingConfigs(config);

        // 删除多余的配置项
        hasChanges |= removeExtraConfigs(config);

        // 验证配置值的有效性
        hasChanges |= validateConfigValues(config);

        // 调试：输出当前已加载的顶级配置键
        if (plugin.getConfig() != null) {
            plugin.getLogger().info("Current config top-level keys: " + plugin.getConfig().getKeys(false));
        }

        // 额外保证：如果语言键仍不存在，强制写入
        if (!config.contains(KEY_LANGUAGE)) {
            plugin.getLogger().warning("Language config not detected, force adding zh_cn");
            config.set(KEY_LANGUAGE, "zh_cn");
            hasChanges = true;
        }

        if (hasChanges) {
            plugin.saveConfig();
            plugin.getLogger().info("Configuration file fixed and saved!");
        }

        return hasChanges;
    }

    /**
     * 添加缺失的配置项
     */
    private boolean addMissingConfigs(FileConfiguration config) {
        boolean hasChanges = false;

        // 添加缺失的配置项
        hasChanges |= addMissingConfigRecursively(config, standardConfig, "");

        return hasChanges;
    }

    /**
     * 递归添加缺失的配置项
     */
    private boolean addMissingConfigRecursively(FileConfiguration config, Map<String, Object> standardMap,
            String prefix) {
        boolean hasChanges = false;

        for (Map.Entry<String, Object> entry : standardMap.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object standardValue = entry.getValue();

            if (!config.contains(key)) {
                plugin.getLogger().info("Adding missing config item: " + key + " = " + standardValue);
                config.set(key, standardValue);
                hasChanges = true;
            } else if (standardValue instanceof Map) {
                // 递归处理嵌套配置
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedStandardMap = (Map<String, Object>) standardValue;
                hasChanges |= addMissingConfigRecursively(config, nestedStandardMap, key);
            }
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
                plugin.getLogger().info("Removing extra config item: " + key);
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

        // 验证语言设置
        hasChanges |= validateLanguage(config);

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
                        "Config item %s value (%d) out of valid range [%d-%d], reset to default: %d",
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
                plugin.getLogger().warning("Invalid particle type: " + particle + ", reset to default: firework");
                config.set(KEY_PARTICLE, "firework");
                return true;
            }
        }
        return false;
    }

    /**
     * 验证语言设置
     */
    private boolean validateLanguage(FileConfiguration config) {
        if (config.contains(KEY_LANGUAGE)) {
            String language = config.getString(KEY_LANGUAGE);
            Set<String> validLanguages = Set.of("zh_cn", "en_us", "ja_jp");
            if (language != null && !validLanguages.contains(language.toLowerCase())) {
                plugin.getLogger().warning("Invalid language setting: " + language + ", reset to default: zh_cn");
                config.set(KEY_LANGUAGE, "zh_cn");
                return true;
            }
        }
        return false;
    }

    /**
     * 获取当前的配置快照
     * @return 不可变的 PluginConfig record
     */
    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }
    
    /**
     * 获取默认保护规则
     */
    public Map<String, Boolean> getDefaultProtectionRules() {
        Map<String, Boolean> defaultRules = new HashMap<>();
        if (pluginConfig == null || pluginConfig.protectionRules() == null) {
            return defaultRules;
        }
        
        for (Map.Entry<String, PluginConfig.ProtectionRule> entry : pluginConfig.protectionRules().entrySet()) {
            defaultRules.put(entry.getKey(), entry.getValue().defaultValue());
        }
        
        return defaultRules;
    }

    /**
     * 检查保护规则是否被服务器允许启用
     */
    public boolean isProtectionRuleEnabled(String ruleName) {
        if (pluginConfig == null || pluginConfig.protectionRules() == null) {
            return true; // 默认为 true
        }
        PluginConfig.ProtectionRule rule = pluginConfig.protectionRules().get(ruleName);
        return rule == null || rule.enable();
    }

    /**
     * 获取所有保护规则名称
     */
    public static String[] getProtectionRules() {
        return PROTECTION_RULES.clone();
    }
    
    /**
     * 重载配置
     * 重新加载配置文件并执行验证和修复
     *
     * @return 重载结果信息
     */
    public ReloadResult reload() {
        plugin.getLogger().info("开始重载配置...");
        
        try {
            // 重新加载配置文件
            plugin.reloadConfig();
            
            // 检查并修复配置
            boolean hasChanges = checkAndFixConfig();
            
            // 将加载的配置载入到 Record 中
            loadConfigIntoRecord();
            
            String message = hasChanges ? "配置已重载并修复" : "配置已重载";
            plugin.getLogger().info(message);
            
            return new ReloadResult(true, message, null);
        } catch (Exception e) {
            String errorMessage = "重载配置时出错: " + e.getMessage();
            plugin.getLogger().severe(errorMessage);
            e.printStackTrace();
            
            return new ReloadResult(false, errorMessage, e);
        }
    }
    
    /**
     * 从 FileConfiguration 加载配置到 PluginConfig record 中
     */
    private void loadConfigIntoRecord() {
        FileConfiguration config = plugin.getConfig();
        
        String language = config.getString(KEY_LANGUAGE, "zh_cn");
        int maxLands = config.getInt(KEY_MAX_LANDS, 5);
        int maxChunks = config.getInt(KEY_MAX_CHUNKS, 256);
        String particle = config.getString(KEY_PARTICLE, "firework");
        int showDuration = config.getInt(KEY_SHOW_DURATION, 10);
        int maxShowDuration = config.getInt(KEY_MAX_SHOW_DURATION, 300);
        int messageCooldown = config.getInt(KEY_MESSAGE_COOLDOWN, 3);
        
        Map<String, PluginConfig.ProtectionRule> protectionRules = loadProtectionRules(config);
        
        this.pluginConfig = new PluginConfig(
            language,
            maxLands,
            maxChunks,
            particle,
            showDuration,
            maxShowDuration,
            messageCooldown,
            protectionRules
        );
        plugin.getLogger().info("Configuration loaded into immutable record.");
    }
    
    /**
     * 从配置文件加载保护规则
     */
    private Map<String, PluginConfig.ProtectionRule> loadProtectionRules(FileConfiguration config) {
        ConfigurationSection protectionSection = config.getConfigurationSection(KEY_PROTECTION);
        if (protectionSection == null) {
            plugin.getLogger().warning("Protection configuration section is missing!");
            return Collections.emptyMap();
        }
        
        Map<String, PluginConfig.ProtectionRule> rules = new HashMap<>();
        for (String ruleName : PROTECTION_RULES) {
            boolean enable = protectionSection.getBoolean(ruleName + ".enable", true);
            boolean defaultValue = protectionSection.getBoolean(ruleName + ".default", false);
            rules.put(ruleName, new PluginConfig.ProtectionRule(enable, defaultValue));
        }
        return Collections.unmodifiableMap(rules);
    }
    
    /**
     * 重载结果类
     */
    public static class ReloadResult {
        private final boolean success;
        private final String message;
        private final Exception exception;
        
        public ReloadResult(boolean success, String message, Exception exception) {
            this.success = success;
            this.message = message;
            this.exception = exception;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Exception getException() {
            return exception;
        }
    }
}
