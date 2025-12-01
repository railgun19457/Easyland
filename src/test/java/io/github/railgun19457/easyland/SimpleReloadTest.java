package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.repository.LandRepository;
import io.github.railgun19457.easyland.service.LandService;
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
 * 简单的配置重载功能测试
 * 不依赖 Mockito，专注于测试核心逻辑
 */
public class SimpleReloadTest {

    @Mock
    private LandRepository mockRepository;
    
    private LandService landService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 模拟repository行为
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());
        when(mockRepository.findByLandId(anyString())).thenReturn(java.util.Optional.empty());
        when(mockRepository.findByOwner(anyString())).thenReturn(new ArrayList<>());
        when(mockRepository.findAllClaimed()).thenReturn(new ArrayList<>());
        when(mockRepository.findAllUnclaimed()).thenReturn(new ArrayList<>());
        when(mockRepository.countByOwner(anyString())).thenReturn(0);
        when(mockRepository.existsByLandId(anyString())).thenReturn(false);
        
        // 初始化服务层
        Map<String, Boolean> defaultProtectionRules = new HashMap<>();
        landService = new LandService(mockRepository, 5, 256, defaultProtectionRules);
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
    public void testLandServiceConfigurationUpdateWithEmptyRules() {
        // 测试空保护规则的情况
        LandService.ReloadResult result = landService.updateConfiguration(3, 128, null);
        
        // 验证结果
        assertTrue(result.isSuccess(), "服务层配置更新应该成功");
        assertNotNull(result.getMessage(), "更新结果消息不应为空");
        assertNull(result.getException(), "更新成功时异常应为空");
        
        // 验证配置值已更新
        assertEquals(3, landService.getMaxLandsPerPlayer(), "最大领地数应该已更新");
        assertEquals(128, landService.getMaxChunksPerLand(), "最大区块数应该已更新");
    }

    @Test
    public void testReloadResultClass() {
        // 测试 ReloadResult 类的功能
        LandService.ReloadResult successResult = new LandService.ReloadResult(true, "成功", null);
        
        assertTrue(successResult.isSuccess(), "成功结果应该返回 true");
        assertEquals("成功", successResult.getMessage(), "消息应该正确");
        assertNull(successResult.getException(), "成功时异常应为空");
        
        Exception testException = new RuntimeException("测试异常");
        LandService.ReloadResult failureResult = new LandService.ReloadResult(false, "失败", testException);
        
        assertFalse(failureResult.isSuccess(), "失败结果应该返回 false");
        assertEquals("失败", failureResult.getMessage(), "消息应该正确");
        assertEquals(testException, failureResult.getException(), "异常应该正确");
    }
}