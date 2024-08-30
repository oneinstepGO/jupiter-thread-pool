package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;

import static com.oneinstep.jupiter.threadpool.config.DefaultConfigConstants.*;

@Data
public class AdaptiveConfig {
    private Boolean enabled = DEFAULT_ENABLE_ADAPTIVE; // 是否开启自适应，默认关闭
    private Boolean onlyIncrease = DEFAULT_ONLY_INCREASE; // 是否只增加线程数，默认关闭

    private Integer queueUsageThreshold = DEFAULT_ADAPTIVE_QUEUE_THRESHOLD; // 队列使用率阈值
    private Integer threadUsageThreshold = DEFAULT_ADAPTIVE_THREAD_THRESHOLD; // 队列使用率阈值
    private Integer waitTimeThresholdMs = DEFAULT_ADAPTIVE_TIME_THRESHOLD; // 任务执行时间阈值，单位毫秒

    public Integer getQueueUsageThreshold() {
        if (queueUsageThreshold == null) return null;
        if (queueUsageThreshold < 0 || queueUsageThreshold > 100) {
            throw new IllegalArgumentException("Illegal queueUsageThreshold: " + queueUsageThreshold);
        }
        return queueUsageThreshold;
    }

    public Integer getThreadUsageThreshold() {
        if (threadUsageThreshold == null) return null;
        if (threadUsageThreshold < 0 || threadUsageThreshold > 100) {
            throw new IllegalArgumentException("Illegal threadUsageThreshold: " + threadUsageThreshold);
        }
        return threadUsageThreshold;
    }

    public Integer getWaitTimeThresholdMs() {
        if (waitTimeThresholdMs == null) return null;
        if (waitTimeThresholdMs <= 0) {
            throw new IllegalArgumentException("Illegal waitTimeThresholdMs: " + waitTimeThresholdMs);
        }
        return waitTimeThresholdMs;
    }

}