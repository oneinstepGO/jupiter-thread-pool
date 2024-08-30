package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;

/**
 * Monitor configuration.
 */
@Data
public class MonitorConfig {
    // Whether to enable monitor.
    private Boolean enabled = DefaultConfigConstants.DEFAULT_ENABLE_MONITOR;
    // The time window for monitor.
    private Long timeWindowSeconds = DefaultConfigConstants.DEFAULT_TIME_WINDOW_SECONDS;
    // The monitor URL.
    private String monitorUrl;

    public MonitorConfig() {
    }

    public MonitorConfig(boolean enabled, Long timeWindowSeconds, String monitorUrl) {
        this.enabled = enabled;
        this.timeWindowSeconds = timeWindowSeconds;
        this.monitorUrl = monitorUrl;
    }

}