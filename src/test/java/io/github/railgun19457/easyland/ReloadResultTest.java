package io.github.railgun19457.easyland;

import io.github.railgun19457.easyland.manager.ConfigManager;
import io.github.railgun19457.easyland.manager.LanguageManager;
import io.github.railgun19457.easyland.service.LandService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 重载结果类测试
 * 测试各个组件的 ReloadResult 类功能
 */
public class ReloadResultTest {

    @Test
    public void testConfigManagerReloadResult() {
        // 测试 ConfigManager.ReloadResult
        ConfigManager.ReloadResult successResult = new ConfigManager.ReloadResult(true, "配置重载成功", null);
        
        assertTrue(successResult.isSuccess(), "成功结果应该返回 true");
        assertEquals("配置重载成功", successResult.getMessage(), "消息应该正确");
        assertNull(successResult.getException(), "成功时异常应为空");
        
        Exception testException = new RuntimeException("配置重载失败");
        ConfigManager.ReloadResult failureResult = new ConfigManager.ReloadResult(false, "配置重载失败", testException);
        
        assertFalse(failureResult.isSuccess(), "失败结果应该返回 false");
        assertEquals("配置重载失败", failureResult.getMessage(), "消息应该正确");
        assertEquals(testException, failureResult.getException(), "异常应该正确");
    }

    @Test
    public void testLanguageManagerReloadResult() {
        // 测试 LanguageManager.ReloadResult
        LanguageManager.ReloadResult successResult = new LanguageManager.ReloadResult(true, "语言重载成功", null);
        
        assertTrue(successResult.isSuccess(), "成功结果应该返回 true");
        assertEquals("语言重载成功", successResult.getMessage(), "消息应该正确");
        assertNull(successResult.getException(), "成功时异常应为空");
        
        Exception testException = new RuntimeException("语言重载失败");
        LanguageManager.ReloadResult failureResult = new LanguageManager.ReloadResult(false, "语言重载失败", testException);
        
        assertFalse(failureResult.isSuccess(), "失败结果应该返回 false");
        assertEquals("语言重载失败", failureResult.getMessage(), "消息应该正确");
        assertEquals(testException, failureResult.getException(), "异常应该正确");
    }

    @Test
    public void testLandServiceReloadResult() {
        // 测试 LandService.ReloadResult
        LandService.ReloadResult successResult = new LandService.ReloadResult(true, "服务重载成功", null);
        
        assertTrue(successResult.isSuccess(), "成功结果应该返回 true");
        assertEquals("服务重载成功", successResult.getMessage(), "消息应该正确");
        assertNull(successResult.getException(), "成功时异常应为空");
        
        Exception testException = new RuntimeException("服务重载失败");
        LandService.ReloadResult failureResult = new LandService.ReloadResult(false, "服务重载失败", testException);
        
        assertFalse(failureResult.isSuccess(), "失败结果应该返回 false");
        assertEquals("服务重载失败", failureResult.getMessage(), "消息应该正确");
        assertEquals(testException, failureResult.getException(), "异常应该正确");
    }

    @Test
    public void testReloadResultConsistency() {
        // 测试所有 ReloadResult 类的一致性
        
        // 创建相同的结果
        Exception testException = new RuntimeException("测试异常");
        
        ConfigManager.ReloadResult configResult = new ConfigManager.ReloadResult(true, "成功消息", testException);
        LanguageManager.ReloadResult langResult = new LanguageManager.ReloadResult(true, "成功消息", testException);
        LandService.ReloadResult serviceResult = new LandService.ReloadResult(true, "成功消息", testException);
        
        // 验证所有结果类都有相同的接口
        assertTrue(configResult.isSuccess());
        assertTrue(langResult.isSuccess());
        assertTrue(serviceResult.isSuccess());
        
        assertEquals("成功消息", configResult.getMessage());
        assertEquals("成功消息", langResult.getMessage());
        assertEquals("成功消息", serviceResult.getMessage());
        
        assertEquals(testException, configResult.getException());
        assertEquals(testException, langResult.getException());
        assertEquals(testException, serviceResult.getException());
    }
}