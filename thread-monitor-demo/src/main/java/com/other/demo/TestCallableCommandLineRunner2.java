package com.other.demo;

import com.oneinstep.jupiter.threadpool.DynamicThreadPool;
import com.oneinstep.jupiter.threadpool.DynamicThreadPoolManager;
import com.oneinstep.jupiter.threadpool.NamedCallable;
import jakarta.annotation.Resource;
import lombok.Getter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Component
public class TestCallableCommandLineRunner2 implements CommandLineRunner {

    @Resource
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    private static final String BIZ_THREAD_POOL = "bizThreadPool";

    @Getter
    private final Map<String, NamedCallableTestThread> threadMap = new ConcurrentHashMap<>();

    private static final boolean ENABLE = false;

    @Override
    public void run(String... args) throws Exception {
        if (!ENABLE) {
            return;
        }

        DynamicThreadPool dynamicThreadPool = dynamicThreadPoolManager.getDynamicThreadPool(BIZ_THREAD_POOL).orElseThrow(() -> new RuntimeException("No such thread pool: " + BIZ_THREAD_POOL));


        NamedCallable<String> task1 = new NamedCallable<>("Task1", () -> "Result1");
        NamedCallable<String> task2 = new NamedCallable<>("Task2", () -> "Result2");

        Future<String> future1 = dynamicThreadPool.submit(task1);
        Future<String> future2 = dynamicThreadPool.submit(task2);

        String result1 = future1.get();
        String result2 = future2.get();

        System.out.println("Result1: " + result1);
        System.out.println("Result2: " + result2);
        dynamicThreadPool.shutdown();

    }

}
