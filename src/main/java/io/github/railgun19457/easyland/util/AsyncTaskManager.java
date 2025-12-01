package io.github.railgun19457.easyland.util;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * 异步任务管理器
 * 用于处理耗时的数据库操作和其他异步任务
 */
public class AsyncTaskManager {
    private static final Logger logger = Logger.getLogger(AsyncTaskManager.class.getName());
    
    private final JavaPlugin plugin;
    private final BukkitScheduler scheduler;
    private final ExecutorService asyncExecutor;
    
    public AsyncTaskManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getServer().getScheduler();
        
        // 创建自定义线程池
        this.asyncExecutor = Executors.newVirtualThreadPerTaskExecutor();
        
        logger.info("AsyncTaskManager initialized with Java 21 Virtual Threads");
    }
    
    /**
     * 异步执行任务，然后在主线程执行回调
     * @param asyncTask 异步任务
     * @param syncCallback 主线程回调
     * @param <T> 返回值类型
     */
    public <T> void runAsync(Supplier<T> asyncTask, Consumer<T> syncCallback) {
        CompletableFuture.supplyAsync(asyncTask, asyncExecutor)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Async task failed: " + throwable.getMessage());
                    throwable.printStackTrace();
                    return;
                }
                
                // 在主线程执行回调
                scheduler.runTask(plugin, () -> {
                    try {
                        syncCallback.accept(result);
                    } catch (Exception e) {
                        logger.severe("Sync callback failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            });
    }
    
    /**
     * 异步执行任务，无返回值
     * @param asyncTask 异步任务
     */
    public void runAsync(Runnable asyncTask) {
        CompletableFuture.runAsync(asyncTask, asyncExecutor)
            .whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Async task failed: " + throwable.getMessage());
                    throwable.printStackTrace();
                }
            });
    }
    
    /**
     * 延迟异步执行任务
     * @param asyncTask 异步任务
     * @param delayTicks 延迟时间（tick）
     */
    public void runAsyncLater(Runnable asyncTask, long delayTicks) {
        scheduler.runTaskLaterAsynchronously(plugin, () -> {
            try {
                asyncTask.run();
            } catch (Exception e) {
                logger.severe("Delayed async task failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, delayTicks);
    }
    
    /**
     * 定期异步执行任务
     * @param asyncTask 异步任务
     * @param initialDelayTicks 初始延迟（tick）
     * @param periodTicks 执行周期（tick）
     * @return 任务ID，可用于取消任务
     */
    public int runAsyncTimer(Runnable asyncTask, long initialDelayTicks, long periodTicks) {
        return scheduler.runTaskTimerAsynchronously(plugin, () -> {
            try {
                asyncTask.run();
            } catch (Exception e) {
                logger.severe("Timer async task failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, initialDelayTicks, periodTicks).getTaskId();
    }
    
    /**
     * 在主线程执行任务
     * @param syncTask 同步任务
     */
    public void runSync(Runnable syncTask) {
        scheduler.runTask(plugin, () -> {
            try {
                syncTask.run();
            } catch (Exception e) {
                logger.severe("Sync task failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 延迟在主线程执行任务
     * @param syncTask 同步任务
     * @param delayTicks 延迟时间（tick）
     */
    public void runSyncLater(Runnable syncTask, long delayTicks) {
        scheduler.runTaskLater(plugin, () -> {
            try {
                syncTask.run();
            } catch (Exception e) {
                logger.severe("Delayed sync task failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, delayTicks);
    }
    
    /**
     * 取消任务
     * @param taskId 任务ID
     */
    public void cancelTask(int taskId) {
        scheduler.cancelTask(taskId);
    }
    
    /**
     * 检查任务是否在主线程
     * @return 是否在主线程
     */
    public boolean isMainThread() {
        return scheduler.isCurrentlyRunning(plugin.getServer().getScheduler().runTask(plugin, () -> {}).getTaskId());
    }
    
    /**
     * 关闭异步任务管理器
     */
    public void shutdown() {
        logger.info("Shutting down AsyncTaskManager...");
        
        try {
            // 1. 停止接受新任务
            asyncExecutor.shutdown();
            logger.info("AsyncTaskManager stopped accepting new tasks");
            
            // 2. 等待正在执行的任务完成
            if (!asyncExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warning("AsyncTaskManager did not terminate gracefully within 10 seconds, forcing shutdown");
                
                // 3. 强制关闭
                java.util.List<Runnable> pendingTasks = asyncExecutor.shutdownNow();
                if (!pendingTasks.isEmpty()) {
                    logger.warning("Cancelled " + pendingTasks.size() + " pending tasks");
                }
                
                // 4. 再次等待
                if (!asyncExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.severe("AsyncTaskManager failed to terminate completely");
                } else {
                    logger.info("AsyncTaskManager terminated forcefully");
                }
            } else {
                logger.info("AsyncTaskManager terminated gracefully");
            }
        } catch (InterruptedException e) {
            logger.warning("AsyncTaskManager shutdown interrupted");
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.severe("Unexpected error during AsyncTaskManager shutdown: " + e.getMessage());
            e.printStackTrace();
            try {
                asyncExecutor.shutdownNow();
            } catch (Exception ex) {
                logger.severe("Failed to force shutdown AsyncTaskManager: " + ex.getMessage());
            }
        }
        
        logger.info("AsyncTaskManager shutdown completed");
    }
    
    /**
     * 立即强制关闭异步任务管理器
     */
    public void forceShutdown() {
        logger.info("Force shutting down AsyncTaskManager...");
        
        try {
            java.util.List<Runnable> pendingTasks = asyncExecutor.shutdownNow();
            if (!pendingTasks.isEmpty()) {
                logger.warning("Force shutdown cancelled " + pendingTasks.size() + " pending tasks");
            }
            
            if (!asyncExecutor.awaitTermination(3, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.severe("AsyncTaskManager failed to terminate even with force shutdown");
            } else {
                logger.info("AsyncTaskManager force shutdown completed");
            }
        } catch (InterruptedException e) {
            logger.warning("AsyncTaskManager force shutdown interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.severe("Unexpected error during AsyncTaskManager force shutdown: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查线程池状态
     */
    public boolean isShutdown() {
        return asyncExecutor.isShutdown();
    }
    
    /**
     * 检查线程池是否已终止
     */
    public boolean isTerminated() {
        return asyncExecutor.isTerminated();
    }
    
    /**
     * 获取待执行任务数量
     */
    public int getPendingTaskCount() {
        if (asyncExecutor instanceof java.util.concurrent.ThreadPoolExecutor) {
            java.util.concurrent.ThreadPoolExecutor executor = (java.util.concurrent.ThreadPoolExecutor) asyncExecutor;
            return executor.getQueue().size();
        }
        return -1;
    }
    
    /**
     * 获取活跃线程数量
     */
    public int getActiveThreadCount() {
        if (asyncExecutor instanceof java.util.concurrent.ThreadPoolExecutor) {
            java.util.concurrent.ThreadPoolExecutor executor = (java.util.concurrent.ThreadPoolExecutor) asyncExecutor;
            return executor.getActiveCount();
        }
        return -1;
    }
    
    /**
     * 获取线程池状态信息
     */
    public String getThreadPoolStatus() {
        StringBuilder status = new StringBuilder();
        
        if (isTerminated()) {
            status.append("ThreadPool: TERMINATED");
        } else if (isShutdown()) {
            status.append("ThreadPool: SHUTTING_DOWN");
        } else {
            status.append("ThreadPool: RUNNING");
        }
        
        // 添加详细信息
        int activeThreads = getActiveThreadCount();
        int pendingTasks = getPendingTaskCount();
        
        if (activeThreads >= 0 && pendingTasks >= 0) {
            status.append(String.format(" (Active: %d, Pending: %d)", activeThreads, pendingTasks));
        }
        
        return status.toString();
    }
}