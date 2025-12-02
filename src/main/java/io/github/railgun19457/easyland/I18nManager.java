package io.github.railgun19457.easyland;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Internationalization manager for Easyland plugin.
 * Handles loading and providing translated strings from language files.
 */
public class I18nManager {
    private final Logger logger;
    private final File dataFolder;
    private final EasyLand plugin;
    private final Map<String, FileConfiguration> languages;
    private FileConfiguration currentLanguage;

    /**
     * Constructor for I18nManager.
     *
     * @param logger         The plugin logger
     * @param dataFolder     The plugin's data folder
     * @param defaultLanguage The default language code (e.g., "zh_cn")
     */
    public I18nManager(Logger logger, File dataFolder, EasyLand plugin) {
        this.logger = logger;
        this.dataFolder = dataFolder;
        this.plugin = plugin;
        this.languages = new HashMap<>();
    }

    /**
     * Initializes the I18nManager and loads all language files.
     */
    public void initialize() {
        loadLanguages();
        String defaultLanguage = plugin.getConfig().getString("language.default", "zh_cn");
        setLanguage(defaultLanguage);
    }

    /**
     * Loads all language files from the lang directory.
     */
    private void loadLanguages() {
        File langFolder = new File(dataFolder, "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // 保存默认语言文件到磁盘（如果不存在）
        saveDefaultLanguageFile("zh_cn");
        saveDefaultLanguageFile("en_us");
        saveDefaultLanguageFile("ja_jp");

        // 从磁盘加载语言文件（优先使用磁盘文件）
        File[] langFiles = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File langFile : langFiles) {
                String languageCode = langFile.getName().replace(".yml", "");
                loadLanguageFromFile(langFile, languageCode);
            }
        }

        // 如果磁盘上没有文件，从资源加载
        if (languages.isEmpty()) {
            logger.warning("No language files found on disk, loading from resources...");
            loadLanguageFromResource("zh_cn");
            loadLanguageFromResource("en_us");
            loadLanguageFromResource("ja_jp");
        }

        logger.info("Loaded " + languages.size() + " language files");
    }
    
    /**
     * 保存默认语言文件到磁盘。
     *
     * @param languageCode 语言代码
     */
    private void saveDefaultLanguageFile(String languageCode) {
        File langFile = new File(dataFolder, "lang/" + languageCode + ".yml");
        if (!langFile.exists()) {
            String resourcePath = "lang/" + languageCode + ".yml";
            try (InputStream inputStream = plugin.getResource(resourcePath)) {
                if (inputStream != null) {
                    java.nio.file.Files.copy(inputStream, langFile.toPath());
                    logger.info("Saved default language file: " + languageCode);
                }
            } catch (IOException e) {
                logger.warning("Failed to save default language file: " + languageCode + " - " + e.getMessage());
            }
        }
    }

    /**
     * Loads a language file from the plugin's resources.
     *
     * @param languageCode The language code (e.g., "zh_cn")
     */
    private void loadLanguageFromResource(String languageCode) {
        String resourcePath = "lang/" + languageCode + ".yml";
        // 使用插件的资源加载方法，更可靠
        try (InputStream inputStream = plugin.getResource(resourcePath)) {
            if (inputStream != null) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                languages.put(languageCode, config);
                logger.info("Loaded language file from resources: " + languageCode + " with " + config.getKeys(true).size() + " keys");
            } else {
                logger.warning("Language resource not found: " + resourcePath);
            }
        } catch (IOException e) {
            logger.warning("Failed to load language file from resources: " + languageCode + " - " + e.getMessage());
        }
    }

    /**
     * Loads a language file from disk.
     *
     * @param file         The language file
     * @param languageCode The language code
     */
    private void loadLanguageFromFile(File file, String languageCode) {
        try {
            // 使用 UTF-8 编码加载文件
            FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(new java.io.FileInputStream(file), StandardCharsets.UTF_8));
            languages.put(languageCode, config);
            
            // 调试：列出所有加载的键
            //logger.info("Loaded language file from disk: " + languageCode + " (" + file.getAbsolutePath() + ")");
            //logger.info("Loaded keys count: " + config.getKeys(true).size());
            
            // 检查 list 相关的键是否存在
            if (config.contains("list")) {
                logger.info("'list' section found, keys: " + config.getConfigurationSection("list").getKeys(false));
            } else {
                logger.warning("'list' section NOT found in " + languageCode);
            }
        } catch (Exception e) {
            logger.warning("Failed to load language file from disk: " + languageCode + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sets the current language.
     *
     * @param languageCode The language code to set
     */
    public void setLanguage(String languageCode) {
        FileConfiguration language = languages.get(languageCode);
        if (language != null) {
            currentLanguage = language;
            logger.info("Language set to: " + languageCode);
        } else {
            String defaultLang = plugin.getConfig().getString("language.default", "zh_cn");
            logger.warning("Language not found: " + languageCode + ", using default: " + defaultLang);
            currentLanguage = languages.get(defaultLang);
            if (currentLanguage == null) {
                logger.severe("Default language not found: " + defaultLang);
                currentLanguage = new YamlConfiguration();
            }
        }
    }

    /**
     * Gets a translated string for the current language.
     *
     * @param key The message key (e.g., "general.only-players")
     * @return The translated string, or the key if not found
     */
    public String getMessage(String key) {
        return getMessage(key, (Object[]) null);
    }

    /**
     * Gets a translated string with formatting arguments for the current language.
     *
     * @param key  The message key
     * @param args The formatting arguments
     * @return The formatted translated string, or the key if not found
     */
    public String getMessage(String key, Object... args) {
        if (currentLanguage == null) {
            logger.warning("currentLanguage is null when getting key: " + key);
            return key;
        }

        String message = currentLanguage.getString(key);
        if (message == null) {
            // 调试：显示当前语言中有哪些键
            logger.warning("Key '" + key + "' not found. Available top-level keys: " + currentLanguage.getKeys(false));
            
            // Try to get from default language
            String defaultLangCode = plugin.getConfig().getString("language.default", "zh_cn");
            FileConfiguration defaultLang = languages.get(defaultLangCode);
            if (defaultLang != null) {
                message = defaultLang.getString(key);
            }
            
            if (message == null) {
                logger.warning("Language not found: " + key);
                return key;
            }
        }

        return args != null && args.length > 0 ? String.format(message, args) : message;
    }

    /**
     * Gets a translated string for a specific language.
     *
     * @param languageCode The language code
     * @param key          The message key
     * @return The translated string, or the key if not found
     */
    public String getLanguageMessage(String languageCode, String key) {
        return getLanguageMessage(languageCode, key, (Object[]) null);
    }

    /**
     * Gets a translated string with formatting arguments for a specific language.
     *
     * @param languageCode The language code
     * @param key          The message key
     * @param args         The formatting arguments
     * @return The formatted translated string, or the key if not found
     */
    public String getLanguageMessage(String languageCode, String key, Object... args) {
        FileConfiguration language = languages.get(languageCode);
        if (language == null) {
            logger.warning("Language not found: " + languageCode);
            return key;
        }

        String message = language.getString(key);
        if (message == null) {
            logger.warning("Message not found: " + key + " for language: " + languageCode);
            return key;
        }

        return args != null && args.length > 0 ? String.format(message, args) : message;
    }

    /**
     * Gets the current language code.
     *
     * @return The current language code
     */
    public String getCurrentLanguage() {
        for (Map.Entry<String, FileConfiguration> entry : languages.entrySet()) {
            if (entry.getValue() == currentLanguage) {
                return entry.getKey();
            }
        }
        return plugin.getConfig().getString("language.default", "zh_cn");
    }

    /**
     * Gets all available language codes.
     *
     * @return Array of available language codes
     */
    public String[] getAvailableLanguages() {
        return languages.keySet().toArray(new String[0]);
    }

    /**
     * Checks if a language is available.
     *
     * @param languageCode The language code to check
     * @return true if the language is available, false otherwise
     */
    public boolean isLanguageAvailable(String languageCode) {
        return languages.containsKey(languageCode);
    }

    /**
     * Reloads all language files.
     */
    public void reload() {
        languages.clear();
        loadLanguages();
        setLanguage(getCurrentLanguage());
        logger.info("Language files reloaded");
    }

    /**
     * Gets the default language code.
     *
     * @return The default language code
     */
    public String getDefaultLanguage() {
        return plugin.getConfig().getString("language.default", "zh_cn");
    }
}