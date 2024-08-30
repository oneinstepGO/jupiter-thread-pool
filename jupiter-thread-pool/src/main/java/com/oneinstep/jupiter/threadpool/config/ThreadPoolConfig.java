package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;

/**
 * Named thread pool properties.
 */
@Data
public class ThreadPoolConfig {
    // The name of the thread pool.
    private String poolName;
    // The core number of threads.
    private Integer corePoolSize = DefaultConfigConstants.DEFAULT_CORE_POOL_SIZE;
    // The maximum allowed number of threads.
    private Integer maxPoolSize = DefaultConfigConstants.DEFAULT_MAX_POOL_SIZE;
    // The time to keep the thread alive.
    private Long keepAliveTimeMs = DefaultConfigConstants.DEFAULT_KEEP_ALIVE_TIME_MS;
    // The work queue configuration.
    private WorkQueueConfig workQueue = new WorkQueueConfig();
    // The policy for the thread pool.
    private String policy = DefaultConfigConstants.DEFAULT_POLICY;
    // The monitor configuration.
    private MonitorConfig monitor = new MonitorConfig();
    // The adaptive configuration.
    private AdaptiveConfig adaptive = new AdaptiveConfig();

    // deep copy
    public ThreadPoolConfig copy() {
        ThreadPoolConfig copy = new ThreadPoolConfig();
        copy.setPoolName(this.getPoolName());
        copy.setCorePoolSize(this.getCorePoolSize());
        copy.setMaxPoolSize(this.getMaxPoolSize());
        copy.setKeepAliveTimeMs(this.getKeepAliveTimeMs());
        copy.setPolicy(this.getPolicy());
        copy.getWorkQueue().setType(this.getWorkQueue().getType());
        copy.getWorkQueue().setCapacity(this.getWorkQueue().getCapacity());
        copy.getMonitor().setEnabled(this.getMonitor().getEnabled());
        copy.getMonitor().setTimeWindowSeconds(this.getMonitor().getTimeWindowSeconds());
        copy.getMonitor().setMonitorUrl(this.getMonitor().getMonitorUrl());
        copy.getAdaptive().setEnabled(this.getAdaptive().getEnabled());
        copy.getAdaptive().setOnlyIncrease(this.getAdaptive().getOnlyIncrease());
        copy.getAdaptive().setQueueUsageThreshold(this.getAdaptive().getQueueUsageThreshold());
        copy.getAdaptive().setThreadUsageThreshold(this.getAdaptive().getThreadUsageThreshold());
        copy.getAdaptive().setWaitTimeThresholdMs(this.getAdaptive().getWaitTimeThresholdMs());
        return copy;
    }

}