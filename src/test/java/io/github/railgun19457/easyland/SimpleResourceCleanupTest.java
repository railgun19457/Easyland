package io.github.railgun19457.easyland;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.railgun19457.easyland.util.CacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 简单的资源清理测试
 * 验证修复后的资源清理功能是否正常工作
 */
public class SimpleResourceCleanupTest {
    
    private CacheManager<String, String> cacheManager;
    
    @BeforeEach
    void setUp() {
        // 创建测试缓存管理器
        cacheManager = new CacheManager<>("TestCache", 100, 5000);
    }
    
    @AfterEach
    void tearDown() {
        // 测试清理
        if (cacheManager != null) {
            cacheManager.shutdown();
        }
    }
    
    @Test
    void testCacheManagerCleanup() {
        // 添加一些测试数据
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        
        assertEquals(2, cacheManager.size());
        
        // 关闭缓存管理器
        cacheManager.shutdown();
        
        // 验证缓存已清理（这里我们只能验证方法不抛出异常）
        assertDoesNotThrow(() -> cacheManager.shutdown());
    }
    
    @Test
    void testCacheManagerStats() {
        // 添加一些测试数据
        cacheManager.put("key1", "value1");
        cacheManager.put("key2", "value2");
        
        // 获取统计信息
        CacheStats stats = cacheManager.getStats();
        assertNotNull(stats);
        assertEquals(2, cacheManager.size());
    }
    
    @Test
    void testCacheManagerCleanupExpiredEntries() {
        // 创建一个短过期时间的缓存
        CacheManager<String, String> shortLivedCache = new CacheManager<>("ShortLived", 100, 100);
        
        // 添加数据
        shortLivedCache.put("key1", "value1");
        assertEquals(1, shortLivedCache.size());
        
        // 等待过期
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 清理过期条目
        // With Caffeine, cleanup is automatic. We can't easily test the exact moment.
        // We can check that the value is gone after a while.
        // 显式触发清理
        // 显式触发清理
        shortLivedCache.cleanUp();
        // 由于Caffeine的异步性，我们不能保证size立即为0。
        // 但我们可以验证在清理后，通过getIfPresent无法获取到值。
        assertNull(shortLivedCache.getIfPresent("key1"));
        
        // 关闭缓存
        shortLivedCache.shutdown();
    }
}