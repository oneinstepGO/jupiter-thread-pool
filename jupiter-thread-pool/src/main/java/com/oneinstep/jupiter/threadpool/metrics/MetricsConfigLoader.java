package com.oneinstep.jupiter.threadpool.metrics;

import com.oneinstep.jupiter.threadpool.config.DynamicThreadPoolProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@ConditionalOnClass(DynamicThreadPoolProperties.class)
public class MetricsConfigLoader implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String METRICS_YML = "application-metrics.yml";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        org.springframework.core.io.Resource resource = new ClassPathResource(METRICS_YML);
        if (resource.exists()) {
            log.info("application-metrics.yml found");
            try {
                YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
                List<PropertySource<?>> propertySource = yamlPropertySourceLoader.load(METRICS_YML, resource);

                if (propertySource.isEmpty()) {
                    log.warn("application-metrics.yml is empty");
                    return;
                }

                environment.getPropertySources().addLast(propertySource.get(0));
                log.info("application-metrics.yml loaded successfully");
            } catch (IOException e) {
                log.error("Failed to load application-metrics.yml", e);
                throw new IllegalStateException("Failed to load application-metrics.yml", e);
            }
        } else {
            log.warn("application-metrics.yml not found");
        }
    }
}
