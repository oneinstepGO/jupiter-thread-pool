package com.other.demo;

import com.oneinstep.jupiter.threadpool.DynamicThreadPoolManager;
import com.oneinstep.jupiter.threadpool.NamedRunnable;
import com.oneinstep.jupiter.threadpool.support.NoSuchNamedThreadPoolException;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class TestThread extends Thread {

    private final DynamicThreadPoolManager dynamicThreadPoolManager;
    private final String poolName;
    private final String taskName;
    private final Random random = new Random();
    private final Long taskExecuteTime;
    private Long taskSleepTime;
    private final Integer errorRate;

    public TestThread(DynamicThreadPoolManager dynamicThreadPoolManager, String poolName, String taskName, Long taskExecuteTime, Long taskSleepTime, Integer errorRate) {
        this.dynamicThreadPoolManager = dynamicThreadPoolManager;
        this.poolName = poolName;
        this.taskName = taskName;
        this.taskExecuteTime = taskExecuteTime;
        this.taskSleepTime = taskSleepTime;
        if (errorRate == null || errorRate < 0 || errorRate > 100) {
            this.errorRate = 0;
        } else {
            this.errorRate = errorRate;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                dynamicThreadPoolManager.getDynamicThreadPool(poolName).orElseThrow(() -> new NoSuchNamedThreadPoolException(poolName))
                        .execute(new NamedRunnable(taskName, () -> {

                            // 模拟任务执行时间
                            try {
                                Thread.sleep(taskExecuteTime + random.nextLong(taskExecuteTime / 3));
                            } catch (InterruptedException e) {
                                // ignore
                            }
                            int i = random.nextInt(100);
                            if (i < errorRate) {
                                throw new RuntimeException(taskName + " failed");
                            }
                        }));
                try {
                    Thread.sleep(taskSleepTime + random.nextLong(taskSleepTime / 3));
                } catch (InterruptedException e) {
                    // ignore
                }
            } catch (Exception e) {
//                log.error("Failed to execute task", e);
            }
        }
    }


    public void addPressure() {
        // reduce taskSleepTime but not less than 5
        taskSleepTime = Math.max(5, taskSleepTime - 5);
    }

    public void reducePressure() {
        // increase taskSleepTime but not more than 1000
        taskSleepTime = Math.min(1000, taskSleepTime + 5);
    }

}
