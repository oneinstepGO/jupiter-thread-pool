package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

import static com.oneinstep.jupiter.threadpool.config.DefaultConfigConstants.*;

@Data
public class AdaptiveConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = -1L;
    private boolean enabled = DEFAULT_ENABLE_ADAPTIVE; // 是否开启自适应，默认关闭
    private boolean onlyIncrease = DEFAULT_ONLY_INCREASE; // 是否只增加线程数，默认关闭

    private int queueUsageThreshold = DEFAULT_ADAPTIVE_QUEUE_THRESHOLD; // 队列使用率阈值
    private int threadUsageThreshold = DEFAULT_ADAPTIVE_THREAD_THRESHOLD; // 队列使用率阈值
    private int waitTimeThresholdMs = DEFAULT_ADAPTIVE_TIME_THRESHOLD; // 任务执行时间阈值，单位毫秒

    public int getQueueUsageThreshold() {
        if (queueUsageThreshold < 0 || queueUsageThreshold > 100) {
            throw new IllegalArgumentException("Illegal queueUsageThreshold: " + queueUsageThreshold);
        }
        return queueUsageThreshold;
    }

    public int getThreadUsageThreshold() {
        if (threadUsageThreshold < 0 || threadUsageThreshold > 100) {
            throw new IllegalArgumentException("Illegal threadUsageThreshold: " + threadUsageThreshold);
        }
        return threadUsageThreshold;
    }

    public int getWaitTimeThresholdMs() {
        if (waitTimeThresholdMs <= 0) {
            throw new IllegalArgumentException("Illegal waitTimeThresholdMs: " + waitTimeThresholdMs);
        }
        return waitTimeThresholdMs;
    }

    public AdaptiveConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public AdaptiveConfig() {
    }

    public AdaptiveConfig copy() {
        AdaptiveConfig copy = new AdaptiveConfig();
        copy.setEnabled(this.isEnabled());
        copy.setOnlyIncrease(this.isOnlyIncrease());
        copy.setQueueUsageThreshold(this.getQueueUsageThreshold());
        copy.setThreadUsageThreshold(this.getThreadUsageThreshold());
        copy.setWaitTimeThresholdMs(this.getWaitTimeThresholdMs());
        return copy;
    }

}