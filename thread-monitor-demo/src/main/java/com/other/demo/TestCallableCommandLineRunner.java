package com.other.demo;

import com.oneinstep.jupiter.threadpool.DynamicThreadPoolManager;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TestCallableCommandLineRunner implements CommandLineRunner {

    @Resource
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    private static final String BIZ_THREAD_POOL = "bizThreadPool";

    private static final String OTHER_THREAD_POOL = "otherThreadPool";

    @Getter
    private final Map<String, NamedCallableTestThread> threadMap = new ConcurrentHashMap<>();

    private static final boolean ENABLE = false;

    @Override
    public void run(String... args) throws Exception {
        if (!ENABLE) {
            return;
        }
        NamedCallableTestThread queryProductListTask = new NamedCallableTestThread(dynamicThreadPoolManager, BIZ_THREAD_POOL, "QueryProductListTask", 300L, 20L, 10);
        threadMap.put("QueryProductListTask", queryProductListTask);
        queryProductListTask.start();

        NamedCallableTestThread queryPromotionTask = new NamedCallableTestThread(dynamicThreadPoolManager, BIZ_THREAD_POOL, "QueryPromotionTask", 500L, 50L, 20);
        threadMap.put("QueryPromotionTask", queryPromotionTask);
        queryPromotionTask.start();

        NamedCallableTestThread queryUserInfoTask = new NamedCallableTestThread(dynamicThreadPoolManager, BIZ_THREAD_POOL, "QueryUserInfoTask", 100L, 10L, 5);
        threadMap.put("QueryUserInfoTask", queryUserInfoTask);
        queryUserInfoTask.start();

        NamedCallableTestThread clearCacheTask = new NamedCallableTestThread(dynamicThreadPoolManager, OTHER_THREAD_POOL, "ClearCacheTask", 800L, 200L, 5);
        threadMap.put("ClearCacheTask", clearCacheTask);
        clearCacheTask.start();

    }

}
