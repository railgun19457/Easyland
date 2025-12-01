package io.github.railgun19457.easyland.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 缓存管理工具类，基于 Caffeine 实现
 * 提供高性能的缓存、统计和自动清理功能
 */
public class CacheManager<K, V> {
    private static final Logger logger = Logger.getLogger(CacheManager.class.getName());

    private final Cache<K, V> cache;
    private final String cacheName;

    /**
     * 创建缓存管理器
     * @param cacheName 缓存名称，用于日志
     * @param maxSize 最大缓存大小
     * @param expireTimeMillis 过期时间（毫秒），0表示永不过期
     */
    public CacheManager(String cacheName, int maxSize, long expireTimeMillis) {
        this.cacheName = cacheName;

        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .recordStats(); // 开启统计功能

        if (expireTimeMillis > 0) {
            builder.expireAfterWrite(expireTimeMillis, TimeUnit.MILLISECONDS);
        }

        this.cache = builder.build();

        logger.info("Cache '" + cacheName + "' initialized with Caffeine. MaxSize=" + maxSize +
                ", expireTime=" + expireTimeMillis + "ms");
    }

    /**
     * 获取缓存值
     */
    public V get(K key) {
        return cache.getIfPresent(key);
    }
    
    /**
     * 获取缓存值，如果不存在则返回null
     */
    public V getIfPresent(K key) {
        return cache.getIfPresent(key);
    }

    /**
     * 添加或更新缓存值
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * 移除缓存值
     */
    public void remove(K key) {
        cache.invalidate(key);
    }

    /**
     * 检查是否包含指定键
     */
    public boolean containsKey(K key) {
        return cache.asMap().containsKey(key);
    }

    /**
     * 获取缓存大小
     */
    public long size() {
        return cache.estimatedSize();
    }

    /**
     * 清空缓存
     */
    public void clear() {
        cache.invalidateAll();
        logger.info("Cache '" + cacheName + "' cleared.");
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return cache.stats();
    }
    
    /**
     * 打印缓存统计信息
     */
    public void logStats() {
        CacheStats stats = getStats();
        logger.info("Cache '" + cacheName + "' stats: " + stats.toString());
    }

    /**
     * 手动触发一次缓存清理
     */
    public void cleanUp() {
        cache.cleanUp();
    }

    /**
     * 关闭缓存管理器（在 Caffeine 中通常不需要手动关闭）
     */
    public void shutdown() {
        // Caffeine managed caches do not require explicit shutdown
        logger.info("Cache '" + cacheName + "' shutdown. Final stats: " + getStats().toString());
    }
}