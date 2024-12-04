package com.oneinstep.jupiter.threadpool.config;

/**
 * 默认线程池配置常量
 */
public class DefaultConfigConstants {

    private DefaultConfigConstants() {
    }

    // 默认的滑动窗口大小
    public static final int DEFAULT_BUCKET_SIZE = 10;

    // 默认的时间窗口大小
    public static final long DEFAULT_TIME_WINDOW_SECONDS = 3;

    // 默认的最大线程数
    public static final int DEFAULT_MAX_POOL_SIZE = 1;

    // 默认的核心线程数
    public static final int DEFAULT_CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;

    // 默认的线程存活时间
    public static final long DEFAULT_KEEP_ALIVE_TIME_MS = 60000;

    // 默认的队列容量
    public static final int DEFAULT_QUEUE_CAPACITY = 64;

    // 默认开启监控
    public static final boolean DEFAULT_ENABLE_MONITOR = true;

    // 默认的队列类型
    public static final String DEFAULT_QUEUE_TYPE = "LinkedBlockingQueue";

    // 默认的拒绝策略
    public static final String DEFAULT_POLICY = "AbortPolicy";

    // 默认的是否开启自适应
    public static final boolean DEFAULT_ENABLE_ADAPTIVE = false;

    // 自动调整-默认的是否只增加线程数
    public static final boolean DEFAULT_ONLY_INCREASE = false;

    // 自动调整-默认的任务等待时间阈值
    public static final int DEFAULT_ADAPTIVE_TIME_THRESHOLD = 1000;

    // 自动调整-默认的队列使用率阈值
    public static final int DEFAULT_ADAPTIVE_QUEUE_THRESHOLD = 80;

    // 自动调整-默认的线程使用率阈值
    public static final int DEFAULT_ADAPTIVE_THREAD_THRESHOLD = 80;

    // 是否开启监控指标导出
    public static final boolean DEFAULT_ENABLE_METRICS_EXPORT = true;

    // 默认的监控指标采集间隔
    public static final String DEFAULT_METRICS_EXPORT_STEP = "1s";

    // 默认的监控端口
    public static final int DEFAULT_METRICS_EXPORT_PORT = 18081;

    // 默认的调整间隔
    public static final long DEFAULT_ADJUSTMENT_INTERVAL_MS = 15000;
}
