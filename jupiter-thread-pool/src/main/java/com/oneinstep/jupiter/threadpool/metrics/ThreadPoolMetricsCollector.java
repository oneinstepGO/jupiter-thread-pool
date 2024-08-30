package com.oneinstep.jupiter.threadpool.metrics;

import com.oneinstep.jupiter.threadpool.DynamicThreadPool;
import com.oneinstep.jupiter.threadpool.support.SpringBeanUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToDoubleFunction;

import static com.oneinstep.jupiter.threadpool.config.DefaultConfigConstants.DEFAULT_BUCKET_SIZE;
import static com.oneinstep.jupiter.threadpool.config.DefaultConfigConstants.DEFAULT_TIME_WINDOW_SECONDS;
import static com.oneinstep.jupiter.threadpool.metrics.MetricsKey.*;
import static com.oneinstep.jupiter.threadpool.metrics.TagKey.POOL_NAME;
import static com.oneinstep.jupiter.threadpool.metrics.TagKey.TASK_NAME;

/**
 * 线程池指标收集器
 */
@Slf4j
public class ThreadPoolMetricsCollector {

    private final DynamicThreadPool threadPool;
    private final MeterRegistry meterRegistry;
    private final Long timeWindowSeconds;
    private volatile boolean running;
    private final Map<Gauge, Boolean> registeredGauges = new ConcurrentHashMap<>();

    public ThreadPoolMetricsCollector(DynamicThreadPool threadPool) {
        this.threadPool = threadPool;
        this.meterRegistry = SpringBeanUtil.getBean(MeterRegistry.class);
        this.timeWindowSeconds = threadPool.getUnmodifyThreadPoolConfig().getMonitor().getTimeWindowSeconds();
        this.running = true; // 开始时设置为运行状态
        collectPool();
    }

    // key: taskName, value: (key: metricName, value: TimeWindowCounter)
    private final Map<String, Map<String, TimeWindowCounter>> taskStats = new ConcurrentHashMap<>();
    // key: taskName, value: TimeWindowExecutionTimeCounter
    private final Map<String, TimeWindowExecutionTimeCounter> taskRTStats = new ConcurrentHashMap<>();
    // key: taskName, value: 是否已经注册
    private final Map<String, Boolean> taskRegistered = new ConcurrentHashMap<>();
    // key: taskName, value: TimeWindowExecutionTimeCounter
    private final Map<String, TimeWindowExecutionTimeCounter> taskWaitTimeStats = new ConcurrentHashMap<>();

    public void collectPool() {
        if (!running) return; // 如果不在运行状态，直接返回
        String poolName = threadPool.getPoolName();
        Tags tags = Tags.of(POOL_NAME, poolName);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_MAX_POOL_SIZE, threadPool, DynamicThreadPool::getMaximumPoolSize).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_CORE_POOL_SIZE, threadPool, DynamicThreadPool::getCorePoolSize).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_ACTIVE_THREADS, threadPool, DynamicThreadPool::getActiveCount).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_QUEUE_SIZE, threadPool, DynamicThreadPool::getQueueSize).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_QUEUE_REMAINING_CAPACITY, threadPool, DynamicThreadPool::getRemainingQueueCapacity).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_DELTA_TASK_COUNT, threadPool, DynamicThreadPool::getDeltaTaskCount).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(THREAD_POOL_DELTA_COMPLETED_TASK_COUNT, threadPool, DynamicThreadPool::getDeltaCompletedTaskCount).tags(tags).register(meterRegistry), true);
    }

    public synchronized void stop() {
        this.running = false; // 设置为停止状态
        // 这里可以添加停止定时任务的逻辑
        this.taskStats.clear();
        this.taskRTStats.clear();
        this.taskRegistered.clear();
        this.taskWaitTimeStats.clear();

        // 移除所有注册的指标
        registeredGauges.keySet().forEach(g -> meterRegistry.remove(g.getId()));
        registeredGauges.clear();

    }


    // 任务是否已经注册
    public boolean isTaskRegistered(String taskName) {
        return taskRegistered.computeIfAbsent(taskName, k -> false);
    }

    // 增加任务总数
    public void increaseTaskTotalCount(String taskName) {
        if (!running) return;
        taskStats.computeIfAbsent(taskName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(TASK_TOTAL_COUNT, task -> createCounter(this.timeWindowSeconds))
                .increment();
    }

    // 增加任务成功数
    public void increaseTaskSuccessCount(String taskName) {
        if (!running) return;
        taskStats.computeIfAbsent(taskName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(TASK_SUCCESS_COUNT, task -> createCounter(this.timeWindowSeconds))
                .increment();
    }

    // 增加任务失败数
    public void increaseTaskFailureCount(String taskName) {
        if (!running) return;
        taskStats.computeIfAbsent(taskName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(TASK_FAILURE_COUNT, task -> createCounter(this.timeWindowSeconds))
                .increment();
    }

    // 增加任务拒绝数
    public void increaseTaskRejectedCount(String taskName) {
        if (!running) return;
        taskStats.computeIfAbsent(taskName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(TASK_REJECTED_COUNT, task -> createCounter(this.timeWindowSeconds))
                .increment();
    }

    // 增加任务执行时间
    public void addTaskExecutionTime(String taskName, long executionTime) {
        if (!running) return;
        taskRTStats.computeIfAbsent(taskName, task -> createExecutionTimeCounter(this.timeWindowSeconds))
                .addExecutionTime(executionTime);
    }

    // 增加任务等待时间
    public void addTaskWaitTime(String taskName, long waitTime) {
        if (!running) return;
        taskWaitTimeStats.computeIfAbsent(taskName, task -> createExecutionTimeCounter(this.timeWindowSeconds))
                .addExecutionTime(waitTime);
    }


    // 注册任务指标
    public synchronized void registerTaskMetrics(String taskName) {
        if (!running) return;
        log.info("Registering task metrics for task: {}", taskName);
        Map<String, TimeWindowCounter> taskStat = this.taskStats.computeIfAbsent(taskName, k -> initTaskStatMap(this.timeWindowSeconds));

        TimeWindowCounter totalCounter = taskStat.computeIfAbsent(TASK_TOTAL_COUNT, task -> createCounter(this.timeWindowSeconds));
        TimeWindowCounter successCounter = taskStat.computeIfAbsent(TASK_SUCCESS_COUNT, task -> createCounter(this.timeWindowSeconds));
        TimeWindowCounter failureCounter = taskStat.computeIfAbsent(TASK_FAILURE_COUNT, task -> createCounter(this.timeWindowSeconds));
        TimeWindowCounter rejectedCounter = taskStat.computeIfAbsent(TASK_REJECTED_COUNT, task -> createCounter(this.timeWindowSeconds));

        TimeWindowExecutionTimeCounter rtCounter = taskRTStats.computeIfAbsent(taskName, task -> createExecutionTimeCounter(this.timeWindowSeconds));
        TimeWindowExecutionTimeCounter waitTimeCounter = taskWaitTimeStats.computeIfAbsent(taskName, task -> createExecutionTimeCounter(this.timeWindowSeconds));

        Tags tags = Tags.of(POOL_NAME, threadPool.getPoolName(), TASK_NAME, taskName);

        registeredGauges.putIfAbsent(Gauge.builder(TASK_TOTAL_COUNT, totalCounter, calcQps()).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(TASK_SUCCESS_COUNT, successCounter, calcQps()).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(TASK_FAILURE_COUNT, failureCounter, calcQps()).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(TASK_REJECTED_COUNT, rejectedCounter, calcQps()).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(TASK_RT, rtCounter, TimeWindowExecutionTimeCounter::getAverage).tags(tags).register(meterRegistry), true);
        registeredGauges.putIfAbsent(Gauge.builder(TASK_WAIT_TIME, waitTimeCounter, TimeWindowExecutionTimeCounter::getAverage).tags(tags).register(meterRegistry), true);

        taskRegistered.put(taskName, true);
    }

    // 计算QPS
    private static ToDoubleFunction<TimeWindowCounter> calcQps() {
        return counter -> {
            double total = counter.get();
            long millSeconds = counter.bucketLivedMillSeconds();
            return total / millSeconds * 1000;
        };
    }

    private static Map<String, TimeWindowCounter> initTaskStatMap(Long timeWindowSeconds) {
        Map<String, TimeWindowCounter> map = new ConcurrentHashMap<>();
        map.put(TASK_TOTAL_COUNT, new TimeWindowCounter(DEFAULT_BUCKET_SIZE, timeWindowSeconds));
        map.put(TASK_SUCCESS_COUNT, new TimeWindowCounter(DEFAULT_BUCKET_SIZE, timeWindowSeconds));
        map.put(TASK_FAILURE_COUNT, new TimeWindowCounter(DEFAULT_BUCKET_SIZE, timeWindowSeconds));
        map.put(TASK_REJECTED_COUNT, new TimeWindowCounter(DEFAULT_BUCKET_SIZE, timeWindowSeconds));
        return map;
    }

    private static TimeWindowCounter createCounter(Long timeWindowSeconds) {
        return new TimeWindowCounter(DEFAULT_BUCKET_SIZE, timeWindowSeconds == null ? DEFAULT_TIME_WINDOW_SECONDS : timeWindowSeconds);
    }

    private static TimeWindowExecutionTimeCounter createExecutionTimeCounter(Long timeWindowSeconds) {
        return new TimeWindowExecutionTimeCounter(DEFAULT_BUCKET_SIZE, timeWindowSeconds == null ? DEFAULT_TIME_WINDOW_SECONDS : timeWindowSeconds);
    }

    public double getAverageExecutionTime() {
        return taskRTStats.values().stream()
                .mapToLong(counter -> (long) counter.getAverage())
                .average()
                .orElse(0);
    }

    public double getAverageWaitTime() {
        return taskWaitTimeStats.values().stream()
                .mapToLong(counter -> (long) counter.getAverage())
                .average()
                .orElse(0);
    }
}
