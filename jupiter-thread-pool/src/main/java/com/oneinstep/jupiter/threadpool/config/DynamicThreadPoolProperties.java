package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.oneinstep.jupiter.threadpool.config.DefaultConfigConstants.*;

/**
 * Named thread pool properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "dynamic-thread-pool")
public class DynamicThreadPoolProperties implements Serializable {
    @Serial
    private static final long serialVersionUID = -1L;

    private final Map<String, ThreadPoolConfig> pools = new HashMap<>();

    private final GlobalMonitorConfig monitor = new GlobalMonitorConfig();

    private final GlobalAdaptiveConfig adaptive = new GlobalAdaptiveConfig();

    @Data
    public static class GlobalMonitorConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = -1L;
        private final MetricsExportConfig export = new MetricsExportConfig();
        private String baseMonitorUrl;
    }

    @Data
    public static class MetricsExportConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = -1L;
        private boolean enabled = DEFAULT_ENABLE_METRICS_EXPORT;
        private String step = DEFAULT_METRICS_EXPORT_STEP;
        private int port = DEFAULT_METRICS_EXPORT_PORT;
    }

    @Data
    public static class GlobalAdaptiveConfig implements Serializable {
        @Serial
        private static final long serialVersionUID = -1L;
        private boolean enabled = DEFAULT_ENABLE_ADAPTIVE;
        private long adjustmentIntervalMs = DEFAULT_ADJUSTMENT_INTERVAL_MS;
    }

}
