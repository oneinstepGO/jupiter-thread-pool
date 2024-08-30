package com.oneinstep.jupiter.threadpool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Named thread pool properties.
 */
@Data
@Component
@ConfigurationProperties(prefix = "dynamic-thread-pool")
public class DynamicThreadPoolProperties {

    private Map<String, ThreadPoolConfig> pools;

    private GlobalMonitorConfig monitor;

    private GlobalAdaptiveConfig adaptive;

    @Data
    public static class GlobalMonitorConfig {
        private MetricsExportConfig export;
        private String baseMonitorUrl;
    }

    @Data
    public static class MetricsExportConfig {
        private Boolean enabled;
        private String step;
        private Integer port;
    }

    @Data
    public static class GlobalAdaptiveConfig {
        private Boolean enabled = false; // 是否开启自适应，默认关闭
        private Long adjustmentIntervalMs = 30000L; // 调整间隔，默认30秒
    }

}
