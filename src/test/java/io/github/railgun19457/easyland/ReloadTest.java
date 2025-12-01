package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.service.LandService;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 配置重载功能测试
 */
public class ReloadTest {

    @Mock
    private JavaPlugin mockPlugin;
    
    @Mock
    private LandRepository mockRepository;

    private ConfigManager configManager;
    private LanguageManager languageManager;
    private LandService landService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 模拟Logger
        when(mockPlugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("TestLogger"));
        
        // 模拟配置
        org.bukkit.configuration.file.FileConfiguration mockConfig =
            new org.bukkit.configuration.file.YamlConfiguration();
        mockConfig.set("language", "zh_cn");
        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        
        // 模拟数据文件夹
        when(mockPlugin.getDataFolder()).thenReturn(new java.io.File("target/test-data"));
        
        // 模拟repository行为
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        when(mockRepository.findByLandId(anyString())).thenReturn(java.util.Optional.empty());
        when(mockRepository.findByOwner(anyString())).thenReturn(new ArrayList<>());
        when(mockRepository.findAllClaimed()).thenReturn(new ArrayList<>());
        when(mockRepository.findAllUnclaimed()).thenReturn(new ArrayList<>());
        when(mockRepository.countByOwner(anyString())).thenReturn(0);
        when(mockRepository.existsByLandId(anyString())).thenReturn(false);
        
        // 初始化配置管理器
        configManager = new ConfigManager(mockPlugin);
        
        // 初始化语言管理器
        languageManager = new LanguageManager(mockPlugin);
        
        // 初始化服务层
        Map<String, Boolean> defaultProtectionRules = new HashMap<>();
        landService = new LandService(mockRepository, 5, 256, defaultProtectionRules);
    }

    @Test
    public void testConfigManagerReload() {
        // 模拟配置重载
        when(mockPlugin.getConfig()).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration.class));
        doNothing().when(mockPlugin).reloadConfig();
        doNothing().when(mockPlugin).saveConfig();

        // 测试配置重载
        ConfigManager.ReloadResult result = configManager.reload();
        
        // 验证结果
        assertTrue(result.isSuccess(), "配置管理器重载应该成功");
        assertNotNull(result.getMessage(), "重载结果消息不应为空");
        assertNull(result.getException(), "重载成功时异常应为空");
    }

    @Test
    public void testLanguageManagerReload() {
        // 模拟语言文件重载
        when(mockPlugin.getDataFolder()).thenReturn(new java.io.File("test"));
        when(mockPlugin.getResource(anyString())).thenReturn(null);
        doNothing().when(mockPlugin).saveResource(anyString(), anyBoolean());

        // 测试语言管理器重载
        LanguageManager.ReloadResult result = languageManager.reload();
        
        // 验证结果
        assertTrue(result.isSuccess(), "语言管理器重载应该成功");
        assertNotNull(result.getMessage(), "重载结果消息不应为空");
        assertNull(result.getException(), "重载成功时异常应为空");
    }

    @Test
    public void testLandServiceConfigurationUpdate() {
        // 测试服务层配置更新
        Map<String, Boolean> newProtectionRules = new HashMap<>();
        newProtectionRules.put("block-protection", true);
        newProtectionRules.put("explosion-protection", false);
        
        LandService.ReloadResult result = landService.updateConfiguration(10, 512, newProtectionRules);
        
        // 验证结果
        assertTrue(result.isSuccess(), "服务层配置更新应该成功");
        assertNotNull(result.getMessage(), "更新结果消息不应为空");
        assertNull(result.getException(), "更新成功时异常应为空");
        
        // 验证配置值已更新
        assertEquals(10, landService.getMaxLandsPerPlayer(), "最大领地数应该已更新");
        assertEquals(512, landService.getMaxChunksPerLand(), "最大区块数应该已更新");
    }

    @Test
    public void testReloadErrorHandling() {
        // 模拟配置重载错误
        doThrow(new RuntimeException("模拟错误")).when(mockPlugin).reloadConfig();

        // 测试错误处理
        ConfigManager.ReloadResult result = configManager.reload();
        
        // 验证错误处理
        assertFalse(result.isSuccess(), "配置重载失败时应该返回失败");
        assertNotNull(result.getMessage(), "错误消息不应为空");
        assertNotNull(result.getException(), "异常不应为空");
        assertTrue(result.getMessage().contains("模拟错误"), "错误消息应包含异常信息");
    }
}