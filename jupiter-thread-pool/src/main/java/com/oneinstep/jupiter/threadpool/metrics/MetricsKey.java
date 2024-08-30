package com.oneinstep.jupiter.threadpool.metrics;

/**
 * 指标key
 */
public class MetricsKey {

    private MetricsKey() {
    }

    // 线程池-最大线程数
    public static final String THREAD_POOL_MAX_POOL_SIZE = "thread_pool_max_pool_size";

    // 线程池-核心线程数
    public static final String THREAD_POOL_CORE_POOL_SIZE = "thread_pool_core_pool_size";

    // 线程池-活跃线程数
    public static final String THREAD_POOL_ACTIVE_THREADS = "thread_pool_active_threads";

    // 线程池-队列大小
    public static final String THREAD_POOL_QUEUE_SIZE = "thread_pool_queue_size";

    // thread_pool_queue_remaining_capacity 线程池-队列剩余大小
    public static final String THREAD_POOL_QUEUE_REMAINING_CAPACITY = "thread_pool_queue_remaining_capacity";

    // thread_pool_delta_task_count 线程池-队列增加任务数
    public static final String THREAD_POOL_DELTA_TASK_COUNT = "thread_pool_delta_task_count";

    // thread_pool_delta_completed_task_count 线程池-队列完成任务数
    public static final String THREAD_POOL_DELTA_COMPLETED_TASK_COUNT = "thread_pool_delta_completed_task_count";

    // 任务-总数
    public static final String TASK_TOTAL_COUNT = "task_total_count_qps";

    // 任务-成功数
    public static final String TASK_SUCCESS_COUNT = "task_success_count_qps";

    // 任务-失败数
    public static final String TASK_FAILURE_COUNT = "task_failure_count_qps";

    // 任务-拒绝数
    public static final String TASK_REJECTED_COUNT = "task_rejected_count_qps";

    // 任务-平均执行时间
    public static final String TASK_RT = "task_rt";

    // 任务-等待时间
    public static final String TASK_WAIT_TIME = "task_wait_time";

}
