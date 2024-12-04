package com.oneinstep.jupiter.threadpool.metrics;

import com.oneinstep.jupiter.threadpool.support.IpUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> registry.config().commonTags(
                TagKey.APPLICATION_NAME, applicationName,
                TagKey.SERVER_IP_NAME, IpUtil.getServerIp());
    }

}
