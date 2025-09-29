package com.example.easyland;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * 多语言管理器
 * 支持中文(zh_cn)、英文(en_us)、日文(ja_jp)
 */
public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languages;
    private String defaultLanguage;

    // 支持的语言列表
    private static final String[] SUPPORTED_LANGUAGES = { "zh_cn", "en_us", "ja_jp" };

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.languages = new HashMap<>();
        this.defaultLanguage = "zh_cn"; // 默认语言

        loadLanguages();
    }

    /**
     * 加载所有语言文件
     */
    private void loadLanguages() {
        // 确保 lang 目录存在
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        // 加载每种支持的语言
        for (String language : SUPPORTED_LANGUAGES) {
            loadLanguage(language);
        }

        // 从配置文件读取默认语言设置
        String configLanguage = plugin.getConfig().getString("language", "zh_cn");
        if (isLanguageSupported(configLanguage)) {
            this.defaultLanguage = configLanguage;
        }
    }

    /**
     * 加载单个语言文件
     */
    private void loadLanguage(String language) {
        File languageFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");

        // 如果文件不存在，从资源中复制
        if (!languageFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(languageFile);

            // 加载默认配置（从jar中的资源文件）
            InputStream defConfigStream = plugin.getResource("lang/" + language + ".yml");
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
                config.setDefaults(defConfig);
            }

            languages.put(language, config);
            plugin.getLogger().info("Loaded language file: " + language + ".yml");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load language file: " + language + ".yml", e);
        }
    }

    /**
     * 获取消息文本
     * 
     * @param key  消息键
     * @param args 格式化参数
     * @return 格式化后的消息
     */
    public String getMessage(String key, Object... args) {
        return getMessage(defaultLanguage, key, args);
    }

    /**
     * 获取指定语言的消息文本
     * 
     * @param language 语言代码
     * @param key      消息键
     * @param args     格式化参数
     * @return 格式化后的消息
     */
    public String getMessage(String language, String key, Object... args) {
        FileConfiguration config = languages.get(language);
        if (config == null) {
            config = languages.get(defaultLanguage);
        }

        if (config == null) {
            return "Missing language config: " + key;
        }

        String message = config.getString(key);
        if (message == null) {
            // 尝试使用默认语言
            if (!language.equals(defaultLanguage)) {
                return getMessage(defaultLanguage, key, args);
            }
            return "Missing message: " + key;
        }

        // 格式化消息
        if (args.length > 0) {
            try {
                message = String.format(message, args);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Message formatting failed: " + key, e);
            }
        }

        return message;
    }

    /**
     * 向玩家发送消息
     * 
     * @param player 玩家
     * @param key    消息键
     * @param args   格式化参数
     */
    public void sendMessage(Player player, String key, Object... args) {
        String message = getMessage(key, args);
        player.sendMessage(message);
    }

    /**
     * 获取玩家的语言偏好（可扩展为从数据库或玩家配置读取）
     * 
     * @param player 玩家
     * @return 语言代码
     */
    public String getPlayerLanguage(Player player) {
        // 目前使用服务器默认语言，后续可以扩展为玩家个人设置
        return defaultLanguage;
    }

    /**
     * 检查语言是否受支持
     * 
     * @param language 语言代码
     * @return 是否支持
     */
    public boolean isLanguageSupported(String language) {
        for (String supported : SUPPORTED_LANGUAGES) {
            if (supported.equals(language)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取默认语言
     * 
     * @return 默认语言代码
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * 设置默认语言
     * 
     * @param language 语言代码
     */
    public void setDefaultLanguage(String language) {
        if (isLanguageSupported(language)) {
            this.defaultLanguage = language;
            // 更新配置文件
            plugin.getConfig().set("language", language);
            plugin.saveConfig();
        }
    }

    /**
     * 重新加载所有语言文件
     */
    public void reload() {
        languages.clear();
        loadLanguages();
    }

    /**
     * 获取支持的语言列表
     * 
     * @return 支持的语言数组
     */
    public String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES.clone();
    }
}