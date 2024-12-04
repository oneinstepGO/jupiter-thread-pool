package com.oneinstep.jupiter.threadpool.config;

import com.oneinstep.jupiter.threadpool.DynamicThreadPool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.MapUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(DynamicThreadPoolProperties.class)
@ConditionalOnClass(DynamicThreadPoolProperties.class)
public class DynamicThreadPoolAutoConfiguration {

    @Resource
    private DynamicThreadPoolProperties dynamicThreadPoolProperties;
    @Resource
    private GenericApplicationContext applicationContext;

    @PostConstruct
    public void init() {
        Map<String, ThreadPoolConfig> pools = dynamicThreadPoolProperties.getPools();
        if (MapUtils.isEmpty(pools)) {
            return;
        }
        pools.forEach((name, config) -> {
            config.setPoolName(name);
            // copy the configuration to avoid the original configuration being modified
            ThreadPoolConfig copy = config.copy();
            applicationContext.registerBean(name, DynamicThreadPool.class, () -> new DynamicThreadPool(copy), bd -> bd.setAutowireCandidate(false));
        });
    }
}
