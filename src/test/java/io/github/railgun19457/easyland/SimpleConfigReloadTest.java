package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 简单的配置重载测试
 * 验证修复后的配置热重载功能是否正常工作
 */
public class SimpleConfigReloadTest {
    
    @Mock
    private JavaPlugin mockPlugin;
    
    private ConfigManager configManager;
    private LanguageManager languageManager;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 模拟插件行为
        when(mockPlugin.getDataFolder()).thenReturn(new File("target/test-data"));
        when(mockPlugin.getConfig()).thenReturn(createMockConfig());
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));
        
        // 创建管理器
        configManager = new ConfigManager(mockPlugin);
        languageManager = new LanguageManager(mockPlugin);
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试数据
        File testDataDir = new File("target/test-data");
        if (testDataDir.exists()) {
            deleteDirectory(testDataDir);
        }
    }
    
    @Test
    void testConfigManagerReload() {
        // 测试配置管理器重载
        ConfigManager.ReloadResult result = configManager.reload();
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertNull(result.getException());
    }
    
    @Test
    void testLanguageManagerReload() {
        // 测试语言管理器重载
        LanguageManager.ReloadResult result = languageManager.reload();
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertNull(result.getException());
    }
    
    @Test
    void testConfigValidation() {
        // 测试配置验证功能
        boolean hasChanges = configManager.checkAndFixConfig();
        
        // 验证不会抛出异常
        assertDoesNotThrow(() -> configManager.checkAndFixConfig());
    }
    
    @Test
    void testLanguageSupport() {
        // 测试语言支持检查
        assertTrue(languageManager.isLanguageSupported("zh_cn"));
        assertTrue(languageManager.isLanguageSupported("en_us"));
        assertTrue(languageManager.isLanguageSupported("ja_jp"));
        assertFalse(languageManager.isLanguageSupported("invalid_lang"));
    }
    
    /**
     * 创建模拟配置
     */
    private org.bukkit.configuration.file.FileConfiguration createMockConfig() {
        org.bukkit.configuration.file.FileConfiguration config =
            new org.bukkit.configuration.file.YamlConfiguration();
        
        // 设置基本配置
        config.set("language", "zh_cn");
        config.set("max-lands-per-player", 5);
        config.set("max-chunks-per-land", 256);
        config.set("message-cooldown-seconds", 3);
        
        // 设置保护规则
        config.set("protection.block-protection.enable", true);
        config.set("protection.block-protection.default", false);
        config.set("protection.explosion-protection.enable", true);
        config.set("protection.explosion-protection.default", false);
        config.set("protection.container-protection.enable", true);
        config.set("protection.container-protection.default", false);
        config.set("protection.player-protection.enable", true);
        config.set("protection.player-protection.default", false);
        
        return config;
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }
}