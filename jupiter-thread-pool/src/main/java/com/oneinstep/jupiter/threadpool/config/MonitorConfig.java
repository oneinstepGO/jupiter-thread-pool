package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Monitor configuration.
 */
@Data
public class MonitorConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = -1L;
    // Whether to enable monitor.
    private boolean enabled = DefaultConfigConstants.DEFAULT_ENABLE_MONITOR;
    // The time window for monitor.
    private long timeWindowSeconds = DefaultConfigConstants.DEFAULT_TIME_WINDOW_SECONDS;
    // The monitor URL.
    private String monitorUrl;

    public MonitorConfig() {
    }

    public MonitorConfig(boolean enabled, long timeWindowSeconds, String monitorUrl) {
        this.enabled = enabled;
        this.timeWindowSeconds = timeWindowSeconds;
        this.monitorUrl = monitorUrl;
    }

    public MonitorConfig copy() {
        MonitorConfig copy = new MonitorConfig();
        copy.setEnabled(this.isEnabled());
        copy.setTimeWindowSeconds(this.getTimeWindowSeconds());
        copy.setMonitorUrl(this.getMonitorUrl());
        return copy;
    }

}