package com.other.demo;

import com.oneinstep.jupiter.threadpool.DynamicThreadPoolManager;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TestCommandLineRunner implements CommandLineRunner {

    @Resource
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    private static final String BIZ_THREAD_POOL = "bizThreadPool";

    private static final String OTHER_THREAD_POOL = "otherThreadPool";

    @Getter
    private final Map<String, TestThread> threadMap = new ConcurrentHashMap<>();

    @Override
    public void run(String... args) throws Exception {
        TestThread queryProductListTask = new TestThread(dynamicThreadPoolManager, BIZ_THREAD_POOL, "QueryProductListTask", 300L, 20L, 10);
        threadMap.put("QueryProductListTask", queryProductListTask);
        queryProductListTask.start();

        TestThread queryPromotionTask = new TestThread(dynamicThreadPoolManager, BIZ_THREAD_POOL, "QueryPromotionTask", 500L, 50L, 20);
        threadMap.put("QueryPromotionTask", queryPromotionTask);
        queryPromotionTask.start();

        TestThread queryUserInfoTask = new TestThread(dynamicThreadPoolManager, BIZ_THREAD_POOL, "QueryUserInfoTask", 100L, 10L, 5);
        threadMap.put("QueryUserInfoTask", queryUserInfoTask);
        queryUserInfoTask.start();

        TestThread clearCacheTask = new TestThread(dynamicThreadPoolManager, OTHER_THREAD_POOL, "ClearCacheTask", 800L, 200L, 5);
        threadMap.put("ClearCacheTask", clearCacheTask);
        clearCacheTask.start();

    }

}
