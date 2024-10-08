package com.oneinstep.jupiter.threadpool;

import com.oneinstep.jupiter.threadpool.config.AdaptiveConfig;
import com.oneinstep.jupiter.threadpool.config.MonitorConfig;
import com.oneinstep.jupiter.threadpool.config.ThreadPoolConfig;
import com.oneinstep.jupiter.threadpool.metrics.ThreadPoolMetricsCollector;
import com.oneinstep.jupiter.threadpool.support.CallableNotSupportException;
import com.oneinstep.jupiter.threadpool.support.RejectPolicyEnum;
import com.oneinstep.jupiter.threadpool.support.RunnableNotSupportException;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A named thread pool.
 * 可以实时监控线程池的任务执行情况
 * {{@link ThreadPoolMetricsCollector}}
 * 需要使用 namedThreadPool.execute(new NamedTask("taskName", runnable)) 来提交任务，而非 submit
 * 提交callable任务时，需要使用 namedThreadPool.submit(new NamedCallable("taskName", callable)) 来提交任务，而非 execute
 * 唯一获取DynamicThreadPool线程池实例的方式是通过 {{@link DynamicThreadPoolManager#getDynamicThreadPool(String)}}
 * <p>
 * or <code>ApplicationContext.getBean(poolName, DynamicThreadPool.class)</code>
 */
@Slf4j
public class DynamicThreadPool extends ThreadPoolExecutor {

    private final ThreadPoolConfig threadPoolConfig;

    // 通过反射获取FutureTask的callable字段
    private static final VarHandle CALLABLE_HANDLE;

    // 初始化
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(FutureTask.class, MethodHandles.lookup());
            CALLABLE_HANDLE = lookup.findVarHandle(FutureTask.class, "callable", Callable.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    // 线程池名称
    @Getter
    private final String poolName;

    private final AtomicLong lastTaskCount = new AtomicLong(0);
    private final AtomicLong lastCompletedTaskCount = new AtomicLong(0);

    // 线程池监控收集器
    private volatile ThreadPoolMetricsCollector collector;

    public DynamicThreadPool(final ThreadPoolConfig threadPoolConfig) {
        super(threadPoolConfig.getCorePoolSize(), threadPoolConfig.getMaxPoolSize(),
                threadPoolConfig.getKeepAliveTimeMs(), TimeUnit.MILLISECONDS,
                threadPoolConfig.getWorkQueue().createQueue(), new NamedThreadFactory(threadPoolConfig.getPoolName()),
                new RejectionHandlerWrapperWithCounting(RejectPolicyEnum.createRejectedExecutionHandler(threadPoolConfig.getPolicy())));
        this.threadPoolConfig = threadPoolConfig;
        this.poolName = threadPoolConfig.getPoolName();
    }

    // 添加任务等待时间的记录
    @Override
    public void execute(@Nonnull Runnable r) {
        if (r instanceof NamedRunnable namedRunnable) {
            namedRunnable.setSubmitTime(System.currentTimeMillis());
            super.execute(namedRunnable);
        } else if (r instanceof FutureTask<?> futureTask) {
            Callable<?> callable = (Callable<?>) CALLABLE_HANDLE.get(futureTask);
            if (callable instanceof NamedCallable<?> namedCallable) {
                namedCallable.setSubmitTime(System.currentTimeMillis());
                super.execute(futureTask);
            } else {
                throw new RunnableNotSupportException();
            }
        } else {
            throw new RunnableNotSupportException();
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        if (r instanceof NamedRunnable namedRunnable) {
            namedRunnable.setStartTime(System.currentTimeMillis());
            if (this.threadPoolConfig.getMonitor().isEnabled()) {
                String taskName = namedRunnable.getName();
                long waitTime = namedRunnable.getStartTime() - namedRunnable.getSubmitTime();
                log.debug("Task {} waited {} ms before execution", taskName, waitTime);

                registerTaskWaitTime(taskName, waitTime);
            }
        } else if (r instanceof FutureTask<?> futureTask) {
            try {
                Callable<?> callable = (Callable<?>) CALLABLE_HANDLE.get(futureTask);
                if (callable instanceof NamedCallable<?> namedCallable) {
                    log.info("beforeExecute namedCallable, namedCallable:{}", namedCallable);
                    namedCallable.setStartTime(System.currentTimeMillis());
                    if (this.threadPoolConfig.getMonitor().isEnabled()) {
                        String taskName = namedCallable.getName();
                        long waitTime = namedCallable.getStartTime() - namedCallable.getSubmitTime();
                        log.debug("Task {} waited {} ms before execution", taskName, waitTime);
                        registerTaskWaitTime(taskName, waitTime);
                    }
                } else {
                    throw new CallableNotSupportException();
                }
            } catch (CallableNotSupportException callableNotSupportException) {
                throw callableNotSupportException;
            } catch (Exception e) {
                log.error("Failed to get callable field from FutureTask", e);
            }
        } else {
            throw new RunnableNotSupportException();
        }

    }

    private void registerTaskWaitTime(String taskName, long waitTime) {
        getCollector().ifPresent(metricsCollector -> {
            boolean taskRegistered = metricsCollector.isTaskRegistered(taskName);
            if (!taskRegistered) {
                synchronized (this) {
                    if (!metricsCollector.isTaskRegistered(taskName)) {
                        metricsCollector.registerTaskMetrics(taskName);
                    }
                }
            }
            metricsCollector.addTaskWaitTime(taskName, waitTime);
        });
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (r instanceof NamedRunnable namedRunnable) {

            namedRunnable.setEndTime(System.currentTimeMillis());
            if (this.threadPoolConfig.getMonitor().isEnabled()) {
                String taskName = namedRunnable.getName();
                long executionTime = namedRunnable.getEndTime() - namedRunnable.getStartTime();
                log.debug("Task {} executed in {} ms", taskName, executionTime);

                getCollector().ifPresent(metricsCollector -> recordTaskAfterFinish(metricsCollector, t, taskName, executionTime));
            }
        } else if (r instanceof FutureTask<?> futureTask) {
            Callable<?> callable = (Callable<?>) CALLABLE_HANDLE.get(futureTask);
            if (callable instanceof NamedCallable<?> namedCallable) {
                log.info("afterExecute namedCallable, namedCallable:{}", namedCallable);
                namedCallable.setEndTime(System.currentTimeMillis());
                if (this.threadPoolConfig.getMonitor().isEnabled()) {
                    String taskName = namedCallable.getName();
                    long executionTime = namedCallable.getEndTime() - namedCallable.getStartTime();
                    log.debug("Task {} executed in {} ms", taskName, executionTime);
                    getCollector().ifPresent(metricsCollector -> recordTaskAfterFinish(metricsCollector, t, taskName, executionTime));
                }
            } else {
                throw new CallableNotSupportException();
            }
        } else {
            throw new RunnableNotSupportException();
        }
    }

    // 获取监控收集器
    private Optional<ThreadPoolMetricsCollector> getCollector() {
        if (Boolean.FALSE.equals(this.threadPoolConfig.getMonitor().isEnabled())) {
            return Optional.empty();
        }
        if (this.collector == null) {
            synchronized (this) {
                if (this.collector == null && this.threadPoolConfig.getMonitor().isEnabled()) {
                    log.info("Creating ThreadPoolMetricsCollector for {}", this.threadPoolConfig.getPoolName());
                    this.collector = new ThreadPoolMetricsCollector(this);
                }
            }
        }
        return Optional.of(this.collector);
    }

    // 记录任务执行情况
    private void recordTaskAfterFinish(ThreadPoolMetricsCollector metricsCollector, Throwable t, String name, long executionTime) {
        metricsCollector.increaseTaskTotalCount(name);
        metricsCollector.addTaskExecutionTime(name, executionTime);
        if (t == null) {
            metricsCollector.increaseTaskSuccessCount(name);
        } else {
            metricsCollector.increaseTaskFailureCount(name);
        }
    }

    // 更新监控配置
    public synchronized void updateMonitor(final MonitorConfig newMonitorConfig) {
        final boolean enabled = newMonitorConfig.isEnabled();
        final Long newTimeWindowSeconds = enabled ? newMonitorConfig.getTimeWindowSeconds() : null;
        final MonitorConfig oldMonitorConfig = this.threadPoolConfig.getMonitor();
        final String newMonitorUrl = newMonitorConfig.getMonitorUrl() == null ? oldMonitorConfig.getMonitorUrl() : newMonitorConfig.getMonitorUrl();
        this.threadPoolConfig.getMonitor().setMonitorUrl(newMonitorUrl);
        if (enabled) {
            if (!oldMonitorConfig.isEnabled()) {
                // 旧配置未启用，直接启用新配置
                this.threadPoolConfig.getMonitor().setEnabled(true);
                this.threadPoolConfig.getMonitor().setTimeWindowSeconds(newTimeWindowSeconds);
                this.collector = new ThreadPoolMetricsCollector(this);
            } else {
                // 旧配置已启用，检查时间窗口是否变化
                if (!Objects.equals(oldMonitorConfig.getTimeWindowSeconds(), newTimeWindowSeconds)) {
                    // 更新监控配置
                    this.threadPoolConfig.getMonitor().setEnabled(true);
                    this.threadPoolConfig.getMonitor().setTimeWindowSeconds(newTimeWindowSeconds);

                    ThreadPoolMetricsCollector oldCollector = this.collector;

                    try {
                        // 创建新的收集器
                        this.collector = new ThreadPoolMetricsCollector(this);
                        // 停止旧的收集器
                        oldCollector.stop();

                    } catch (Exception e) {
                        log.error("Failed to create new ThreadPoolMetricsCollector", e);
                        // 恢复旧的收集器和监控配置
                        this.collector = oldCollector;
                        this.threadPoolConfig.getMonitor().setEnabled(oldMonitorConfig.isEnabled());
                        this.threadPoolConfig.getMonitor().setTimeWindowSeconds(oldMonitorConfig.getTimeWindowSeconds());
                    }
                } else {
                    log.warn("Monitor intervalMs is not changed, ignore update");
                }
            }
        } else {
            disableMonitor(oldMonitorConfig);
            // 关闭监控时，自适应配置也要关闭
            this.threadPoolConfig.getAdaptive().setEnabled(false);
        }
    }

    private void disableMonitor(final MonitorConfig oldMonitorConfig) {
        if (oldMonitorConfig.isEnabled()) {
            synchronized (this) {
                this.threadPoolConfig.getMonitor().setEnabled(false);

                log.info("Disabling monitor for {}, monitorConfig: {}", this.poolName, threadPoolConfig.getMonitor());
                if (this.collector != null) {
                    this.collector.stop();
                    this.collector = null;
                }
            }
        } else {
            log.warn("Monitor is not enabled, ignore update");
        }
    }

    public synchronized void updateAdaptive(@Nonnull AdaptiveConfig newAdaptiveConfig) {
        // 如果AdaptiveConfig配置有变化，更新配置

        this.threadPoolConfig.getAdaptive().setEnabled(newAdaptiveConfig.isEnabled());
        this.threadPoolConfig.getAdaptive().setOnlyIncrease(newAdaptiveConfig.isOnlyIncrease());
        this.threadPoolConfig.getAdaptive().setQueueUsageThreshold(newAdaptiveConfig.getQueueUsageThreshold());
        this.threadPoolConfig.getAdaptive().setThreadUsageThreshold(newAdaptiveConfig.getThreadUsageThreshold());
        this.threadPoolConfig.getAdaptive().setWaitTimeThresholdMs(newAdaptiveConfig.getWaitTimeThresholdMs());
    }


    // 拒绝策略
    @Getter
    public static class RejectionHandlerWrapperWithCounting implements RejectedExecutionHandler {

        private final RejectedExecutionHandler realHandler;

        public RejectionHandlerWrapperWithCounting(RejectedExecutionHandler realHandler) {
            this.realHandler = realHandler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {

            if (executor instanceof DynamicThreadPool dynamicThreadPool && r instanceof NamedRunnable task && dynamicThreadPool.threadPoolConfig.getMonitor().isEnabled()) {
                dynamicThreadPool.getCollector().ifPresent(metricsCollector -> metricsCollector.increaseTaskRejectedCount(task.getName()));
            }

            realHandler.rejectedExecution(r, executor);
        }

    }

    public int getQueueSize() {
        return getQueue().size();
    }

    public int getRemainingQueueCapacity() {
        return getQueue().remainingCapacity();
    }

    public long getDeltaTaskCount() {
        long currentTaskCount = super.getTaskCount();
        return Math.max(currentTaskCount - lastTaskCount.getAndSet(currentTaskCount), 0);
    }

    public long getDeltaCompletedTaskCount() {
        long currentCompletedTaskCount = super.getCompletedTaskCount();
        return Math.max(currentCompletedTaskCount - lastCompletedTaskCount.getAndSet(currentCompletedTaskCount), 0);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        try {
            try {
                if (!this.awaitTermination(30, TimeUnit.SECONDS)) {
                    this.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } finally {
            if (this.collector != null) {
                this.collector.stop();
            }
            this.collector = null;
        }


    }

    @Override
    public @Nonnull List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        try {
            tasks = super.shutdownNow();
        } finally {
            if (this.collector != null) {
                this.collector.stop();
            }
            this.collector = null;
        }

        return tasks;
    }

    @Override
    public synchronized void setMaximumPoolSize(int maximumPoolSize) {
        super.setMaximumPoolSize(maximumPoolSize);
        this.threadPoolConfig.setMaxPoolSize(maximumPoolSize);
    }

    @Override
    public synchronized void setCorePoolSize(int corePoolSize) {
        super.setCorePoolSize(corePoolSize);
        this.threadPoolConfig.setCorePoolSize(corePoolSize);
    }

    @Override
    public synchronized void setKeepAliveTime(long time, TimeUnit unit) {
        super.setKeepAliveTime(time, unit);
        this.threadPoolConfig.setKeepAliveTimeMs(unit.toMillis(time));
    }

    @Override
    public synchronized void setRejectedExecutionHandler(@Nonnull RejectedExecutionHandler newHandler) {
        super.setRejectedExecutionHandler(new RejectionHandlerWrapperWithCounting(newHandler));
        this.threadPoolConfig.setPolicy(RejectPolicyEnum.getPolicyByClass(newHandler.getClass()).policyName());
    }

    public ThreadPoolConfig getUnmodifyThreadPoolConfig() {
        // 为了防止外部修改配置，返回一个新的对象
        return this.threadPoolConfig.copy();
    }

    public double getAverageWaitTime() {
        if (collector != null) {
            return collector.getAverageWaitTime();
        }
        return 0;
    }

}
