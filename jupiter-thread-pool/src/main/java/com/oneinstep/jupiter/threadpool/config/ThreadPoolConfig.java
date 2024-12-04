package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Named thread pool properties.
 */
@Data
public class ThreadPoolConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = -1L;
    // The name of the thread pool.
    private String poolName;
    // The core number of threads.
    private int corePoolSize = DefaultConfigConstants.DEFAULT_CORE_POOL_SIZE;
    // The maximum allowed number of threads.
    private int maxPoolSize = DefaultConfigConstants.DEFAULT_MAX_POOL_SIZE;
    // The time to keep the thread alive.
    private long keepAliveTimeMs = DefaultConfigConstants.DEFAULT_KEEP_ALIVE_TIME_MS;
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
        copy.setWorkQueue(this.getWorkQueue().copy());
        copy.setMonitor(this.getMonitor().copy());
        copy.setAdaptive(this.getAdaptive().copy());
        return copy;
    }

}