package com.oneinstep.jupiter.threadpool;

import com.google.common.util.concurrent.Striped;
import com.oneinstep.jupiter.threadpool.config.*;
import com.oneinstep.jupiter.threadpool.support.BlockingQueueEnum;
import com.oneinstep.jupiter.threadpool.support.IpUtil;
import com.oneinstep.jupiter.threadpool.support.NoSuchNamedThreadPoolException;
import com.oneinstep.jupiter.threadpool.support.RejectPolicyEnum;
import com.oneinstep.jupiter.threadpool.web.SwitchAdaptiveParam;
import com.oneinstep.jupiter.threadpool.web.SwitchMonitorParam;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

/**
 * 可以动态修改线程池参数的管理器
 */
@Component
@Primary
@Slf4j
@DependsOn("dynamicThreadPoolAutoConfiguration")
public class DynamicThreadPoolManager {

    private static final int STRIPE_SIZE = 100;

    @Resource
    private ApplicationContext applicationContext;
    @Resource
    private DynamicThreadPoolProperties dynamicThreadPoolProperties;
    @Value("${spring.application.name}")
    private String applicationName;

    // 线程池名称 -> 读写锁
    private static final Map<String, Striped<Lock>> LOCK_MAP = new HashMap<>();

    // 自适应调整线程池的步长
    private static final int THREAD_ADJUST_STEP = 2;

    // 允许的最大线程数
    private static final int MAX_THREAD_NUMS_ALLOW = 500;

    private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);

    @PostConstruct
    public void init() {

        // 初始化 LOCK_MAP
        for (String poolName : getAllPoolNames()) {
            LOCK_MAP.put(poolName, Striped.lock(STRIPE_SIZE));
        }

        if (dynamicThreadPoolProperties != null && dynamicThreadPoolProperties.getAdaptive() != null && dynamicThreadPoolProperties.getAdaptive().isEnabled()) {
            scheduledExecutorService.scheduleAtFixedRate(this::monitorAndAdjustThreadPools,
                    60000, dynamicThreadPoolProperties.getAdaptive().getAdjustmentIntervalMs(), TimeUnit.MILLISECONDS);
        }
    }

    public List<String> getAllPoolNames() {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(DynamicThreadPool.class);
        return Arrays.asList(beanNamesForType);
    }

    public void monitorAndAdjustThreadPools() {
        String[] poolNames = applicationContext.getBeanNamesForType(DynamicThreadPool.class);
        for (String poolName : poolNames) {
            try {
                DynamicThreadPool threadPool = applicationContext.getBean(poolName, DynamicThreadPool.class);
                adjustThreadPoolParameters(threadPool);
            } catch (Exception e) {
                log.error("Monitor and adjust thread pool [{}] failed", poolName, e);
            }
        }
    }

    /**
     * 自动调整线程池参数
     *
     * @param threadPool 线程池
     */
    private void adjustThreadPoolParameters(DynamicThreadPool threadPool) {
        String poolName = threadPool.getPoolName();
        ThreadPoolConfig threadPoolConfig = threadPool.getUnmodifyThreadPoolConfig();
        AdaptiveConfig adaptiveConfig = threadPoolConfig.getAdaptive();
        if (adaptiveConfig == null || !adaptiveConfig.isEnabled()) {
            log.debug("Adaptive config is disabled for pool {}", poolName);
            return;
        }

        MonitorConfig monitor = threadPoolConfig.getMonitor();
        if (monitor == null || !monitor.isEnabled()) {
            log.debug("Monitor is disabled for pool {}", poolName);
            return;
        }

        int queueSize = threadPool.getQueueSize();
        int activeThreads = threadPool.getActiveCount();
        int corePoolSize = threadPool.getCorePoolSize();
        int maxPoolSize = threadPool.getMaximumPoolSize();
        double avgWaitTime = threadPool.getAverageWaitTime();

        boolean onlyIncrease = adaptiveConfig.isOnlyIncrease();
        double queueUsageThreshold = adaptiveConfig.getQueueUsageThreshold() / 100.00;
        double threadUsageThreshold = adaptiveConfig.getThreadUsageThreshold() / 100.00;
        int waitTimeThresholdMs = adaptiveConfig.getWaitTimeThresholdMs();

        double threadUsageRate = (double) activeThreads / maxPoolSize;
        double queueUsageRate;

        // Check if the queue is SynchronousQueue
        if (threadPool.getQueue() instanceof SynchronousQueue) {
            queueUsageRate = threadUsageRate > 0 ? 1.0 : 0.0; // If there are active threads, consider the queue as "full"
        } else {
            queueUsageRate = (double) queueSize / (queueSize + threadPool.getQueue().remainingCapacity());
        }

        boolean needIncreaseCoreThreads = false;
        boolean needIncreaseThreads = false;
        boolean needDecreaseThreads = false;

        if (queueUsageRate > queueUsageThreshold) {
            if (threadUsageRate < threadUsageThreshold) {
                needIncreaseCoreThreads = true;
            } else {
                needIncreaseThreads = true;
            }
        } else if (avgWaitTime > waitTimeThresholdMs) {
            needIncreaseThreads = true;
        } else if (threadUsageRate < threadUsageThreshold && activeThreads < corePoolSize && !onlyIncrease) {
            needDecreaseThreads = true;
        }

        Striped<Lock> lock = LOCK_MAP.computeIfAbsent(poolName, k -> Striped.lock(STRIPE_SIZE));
        Lock writeLock = lock.get(poolName);
        writeLock.lock();
        try {
            doAutoAdjust(threadPool, needIncreaseCoreThreads, corePoolSize, maxPoolSize, needIncreaseThreads, poolName, needDecreaseThreads);
        } finally {
            writeLock.unlock();
        }

    }

    private static void doAutoAdjust(DynamicThreadPool threadPool, boolean needIncreaseCoreThreads, int corePoolSize, int maxPoolSize, boolean needIncreaseThreads, String poolName, boolean needDecreaseThreads) {
        if (needIncreaseCoreThreads && corePoolSize < maxPoolSize) {
            int newCorePoolSize = Math.min(corePoolSize + THREAD_ADJUST_STEP, maxPoolSize);
            threadPool.setCorePoolSize(newCorePoolSize);
            log.info("Increased core pool size to {} for pool {}", newCorePoolSize, threadPool.getPoolName());
        } else if (needIncreaseThreads && maxPoolSize < MAX_THREAD_NUMS_ALLOW) {
            int newCorePoolSize = Math.min(corePoolSize + THREAD_ADJUST_STEP, maxPoolSize);
            int newMaxPoolSize = Math.min(maxPoolSize + THREAD_ADJUST_STEP, maxPoolSize * 2); // Ensure it doesn't exceed the system's maximum value
            threadPool.setMaximumPoolSize(newMaxPoolSize);
            threadPool.setCorePoolSize(newCorePoolSize);
            log.info("Increased thread pool size: {} (core: {}, max: {})", poolName, newCorePoolSize, newMaxPoolSize);
        } else if (needDecreaseThreads && corePoolSize > THREAD_ADJUST_STEP) {
            int newCorePoolSize = Math.max(corePoolSize - THREAD_ADJUST_STEP, THREAD_ADJUST_STEP);
            int newMaxPoolSize = Math.max(maxPoolSize - THREAD_ADJUST_STEP, THREAD_ADJUST_STEP * 2);
            threadPool.setMaximumPoolSize(newMaxPoolSize);
            threadPool.setCorePoolSize(newCorePoolSize);
            log.info("Decreased thread pool size: {} (core: {}, max: {})", poolName, newCorePoolSize, newMaxPoolSize);
        }
    }


    /**
     * 修改线程池参数
     *
     * @param newConfig 新的线程池配置
     */
    public synchronized void modifyThreadPool(ThreadPoolConfig newConfig) throws NoSuchNamedThreadPoolException {
        log.info("Modify thread pool: {}", newConfig);
        String poolName = newConfig.getPoolName();
        checkParams(newConfig);

        Striped<Lock> lock = LOCK_MAP.computeIfAbsent(poolName, k -> Striped.lock(STRIPE_SIZE));
        Lock writeLock = lock.get(poolName);
        writeLock.lock();
        try {
            resetIfChanged(poolName, newConfig);
            log.info("Modify thread pool [{}] success", poolName);
        } finally {
            writeLock.unlock();
        }
    }

    private void resetIfChanged(String poolName, ThreadPoolConfig newConfig) {

        DynamicThreadPool threadPoolToChange = applicationContext.getBean(poolName, DynamicThreadPool.class);

        ThreadPoolConfig oldConfig = threadPoolToChange.getUnmodifyThreadPoolConfig();
        if (newConfig.getWorkQueue() != null) {
            Integer oldCapacity = oldConfig.getWorkQueue().getCapacity();
            Integer newCapacity = newConfig.getWorkQueue().getCapacity();
            // 如果队列类型不同或者容量不同，需要重新设置队列
            if (!oldConfig.getWorkQueue().getType().equals(newConfig.getWorkQueue().getType()) || !Objects.equals(oldCapacity, newCapacity)) {
                mayUpdateNewConfig(oldConfig, newConfig);
                // 重新设置线程池
                resetThreadPool(poolName, newConfig);
                return;
            }
        }

        // 以下情况不需要重新设置线程池
        // 如果线程池参数有变化，需要重新设置
        // 需要先设置最大线程数，再设置核心线程数
        boolean needChangeMax = false;
        boolean needChangeCore = false;
        boolean needChangeKeepAlive = false;
        boolean needChangeHandler = false;
        boolean needChangeMonitor = false;

        final int oldMax = oldConfig.getMaxPoolSize();
        if (newConfig.getMaxPoolSize() != oldMax) {
            needChangeMax = true;
        }
        final int oldCore = oldConfig.getCorePoolSize();
        if (newConfig.getCorePoolSize() != oldCore) {
            needChangeCore = true;
        }
        long oldKeepAliveTimeMs = oldConfig.getKeepAliveTimeMs();
        if (!Objects.equals(newConfig.getKeepAliveTimeMs(), oldKeepAliveTimeMs)) {
            needChangeKeepAlive = true;
        }
        if (StringUtils.isNotBlank(newConfig.getPolicy()) && !newConfig.getPolicy().equals(oldConfig.getPolicy())) {
            needChangeHandler = true;
        }
        if (newConfig.getMonitor() != null && (!Objects.equals(newConfig.getMonitor().isEnabled(), oldConfig.getMonitor().isEnabled())
                || !Objects.equals(newConfig.getMonitor().getTimeWindowSeconds(), oldConfig.getMonitor().getTimeWindowSeconds())
                || !Objects.equals(newConfig.getMonitor().getMonitorUrl(), oldConfig.getMonitor().getMonitorUrl()))) {
            needChangeMonitor = true;
        }

        if (needChangeMax) {
            threadPoolToChange.setMaximumPoolSize(newConfig.getMaxPoolSize());
        }
        if (needChangeCore) {
            threadPoolToChange.setCorePoolSize(newConfig.getCorePoolSize());
        }
        if (needChangeKeepAlive) {
            threadPoolToChange.setKeepAliveTime(newConfig.getKeepAliveTimeMs(), TimeUnit.MILLISECONDS);
        }
        if (needChangeHandler) {
            threadPoolToChange.setRejectedExecutionHandler(RejectPolicyEnum.createRejectedExecutionHandler(newConfig.getPolicy()));
        }
        if (needChangeMonitor) {
            log.info("Modify thread pool [{}] monitor: {}", poolName, newConfig.getMonitor());
            threadPoolToChange.updateMonitor(newConfig.getMonitor());
        }

        threadPoolToChange.updateAdaptive(newConfig.getAdaptive());

    }

    private void mayUpdateNewConfig(ThreadPoolConfig oldConfig, ThreadPoolConfig newConfig) {
        if (newConfig.getWorkQueue() == null) {
            newConfig.setWorkQueue(oldConfig.getWorkQueue());
        }
        if (StringUtils.isBlank(newConfig.getPolicy())) {
            newConfig.setPolicy(oldConfig.getPolicy());
        }
        if (newConfig.getMonitor() == null) {
            newConfig.setMonitor(oldConfig.getMonitor());
        }
    }

    private void checkParams(ThreadPoolConfig newConfig) throws NoSuchNamedThreadPoolException {
        check(newConfig);
        checkQueue(newConfig);
    }

    private static void checkQueue(ThreadPoolConfig newConfig) {
        WorkQueueConfig workQueue = newConfig.getWorkQueue();
        String poolName = newConfig.getPoolName();
        if (workQueue != null && StringUtils.isNotBlank(workQueue.getType())) {
            String queueType = workQueue.getType();
            BlockingQueueEnum queueEnum = BlockingQueueEnum.getQueueByName(queueType);
            int queueCapacity = workQueue.getCapacity();
            if (queueCapacity < 0) {
                log.error("Modify thread pool [{}] failed, queue capacity < 0", poolName);
                throw new IllegalArgumentException("queue capacity < 0");
            }

            if (BlockingQueueEnum.SYNCHRONOUS_QUEUE.equals(queueEnum)) {
                newConfig.getWorkQueue().setCapacity(0);
            }
        }
    }

    private void check(ThreadPoolConfig newConfig) throws NoSuchNamedThreadPoolException {
        String poolName = newConfig.getPoolName();
        if (StringUtils.isBlank(poolName) || isThreadPoolNotExist(poolName)) {
            log.error("Modify thread pool [{}] failed, pool not found", newConfig);
            throw new NoSuchNamedThreadPoolException("pool not found");
        }

        if (newConfig.getCorePoolSize() < 0 || newConfig.getMaxPoolSize() < 0 || newConfig.getCorePoolSize() > newConfig.getMaxPoolSize()) {
            log.error("Modify thread pool [{}] failed, coreSize or maxSize < 0 or coreSize > maxSize", poolName);
            throw new IllegalArgumentException("coreSize or maxSize < 0 or coreSize > maxSize");
        }

        if (newConfig.getMonitor() != null
                && (newConfig.getMonitor().isEnabled() && (newConfig.getMonitor().getTimeWindowSeconds() <= 0))) {
            log.error("Modify thread pool [{}] failed, monitor intervalMs <= 0", poolName);
            throw new IllegalArgumentException("monitor intervalMs <= 0");
        }

        RejectPolicyEnum.getPolicyByName(newConfig.getPolicy());

        if (newConfig.getAdaptive() != null) {
            checkAdaptive(newConfig, poolName);
        }
    }

    private static void checkAdaptive(ThreadPoolConfig newConfig, String poolName) {
        if (newConfig.getAdaptive().getQueueUsageThreshold() < 1 || newConfig.getAdaptive().getQueueUsageThreshold() > 100) {
            log.error("Modify thread pool [{}] failed, queueUsageThreshold < 1 or queueUsageThreshold > 100", poolName);
            throw new IllegalArgumentException("queueUsageThreshold < 1 or queueUsageThreshold > 100");
        }
        if (newConfig.getAdaptive().getThreadUsageThreshold() < 1 || newConfig.getAdaptive().getThreadUsageThreshold() > 100) {
            log.error("Modify thread pool [{}] failed, threadUsageThreshold < 1 or threadUsageThreshold > 100", poolName);
            throw new IllegalArgumentException("threadUsageThreshold < 1 or threadUsageThreshold > 100");
        }
        if (newConfig.getAdaptive().getWaitTimeThresholdMs() < 10 || newConfig.getAdaptive().getWaitTimeThresholdMs() > 10000) {
            log.error("Modify thread pool [{}] failed, executionTimeThresholdMs < 10 or executionTimeThresholdMs > 10000", poolName);
            throw new IllegalArgumentException("executionTimeThresholdMs < 10 or executionTimeThresholdMs > 10000");
        }

        boolean enabled = newConfig.getAdaptive().isEnabled();
        if (enabled && newConfig.getMonitor() != null && !newConfig.getMonitor().isEnabled()) {
            log.error("Modify thread pool [{}] failed, monitor is disabled", poolName);
            throw new IllegalArgumentException("monitor is disabled, please enable monitor first");
        }
    }


    private boolean isThreadPoolNotExist(String poolName) {
        return !applicationContext.containsBean(poolName) || !(applicationContext.getBean(poolName) instanceof DynamicThreadPool);
    }

    public ThreadPoolConfig getPoolConfig(String poolName) {
        if (isThreadPoolNotExist(poolName)) {
            return null;
        }
        DynamicThreadPool threadPool = applicationContext.getBean(poolName, DynamicThreadPool.class);

        ThreadPoolConfig threadPoolConfig = threadPool.getUnmodifyThreadPoolConfig();

        String monitorUrl = threadPoolConfig.getMonitor().getMonitorUrl();

        DynamicThreadPoolProperties.GlobalMonitorConfig monitor = dynamicThreadPoolProperties.getMonitor();
        if (StringUtils.isBlank(monitorUrl) && monitor != null && StringUtils.isNotBlank(monitor.getBaseMonitorUrl())) {
            // var-task_name=All&var-application=thread-monitor-demo&var-server_ip=172.20.0.2&var-thread_pool=otherThreadPool
            monitorUrl = monitor.getBaseMonitorUrl() + "&var-application=" + applicationName + "&var-server_ip=" + IpUtil.getServerIp() + "&var-thread_pool=" + poolName;
            threadPoolConfig.getMonitor().setMonitorUrl(monitorUrl);
        }

        return threadPoolConfig;
    }

    public List<ThreadPoolConfig> getAllPoolConfig() {
        String[] beanNamesForType = applicationContext.getBeanNamesForType(DynamicThreadPool.class);
        List<ThreadPoolConfig> collect = Stream.of(beanNamesForType).map(this::getPoolConfig)
                .filter(Objects::nonNull).sorted(Comparator.comparingInt(ThreadPoolConfig::getCorePoolSize).reversed())
                .toList();
        return new ArrayList<>(collect);
    }

    public void resetThreadPool(String poolName) {
        // use the default config from yml config
        Map<String, ThreadPoolConfig> pools = dynamicThreadPoolProperties.getPools();
        if (pools == null || pools.isEmpty()) {
            log.error("Reset thread pool failed, no pool found in config");
            return;
        }

        ThreadPoolConfig newConfig = pools.get(poolName);
        // copy the configuration to avoid the original configuration being modified
        if (newConfig != null) {
            ThreadPoolConfig copy = newConfig.copy();
            resetThreadPool(poolName, copy);
        } else {
            log.error("Reset thread pool failed, pool not found: {}", poolName);
        }
    }

    public void resetThreadPool(@Nonnull String poolName, ThreadPoolConfig newConfig) {
        log.info("Reset thread pool: {}", poolName);

        Striped<Lock> lock = LOCK_MAP.computeIfAbsent(poolName, k -> Striped.lock(STRIPE_SIZE));
        Lock writeLock = lock.get(poolName);
        writeLock.lock();
        try {
            if (isThreadPoolNotExist(poolName)) {
                return;
            }
            DynamicThreadPool oldPool = applicationContext.getBean(poolName, DynamicThreadPool.class);

            // 创建新的线程池
            newConfig.setPoolName(poolName);
            DynamicThreadPool newPool = new DynamicThreadPool(newConfig);

            BlockingQueue<Runnable> oldPoolQueue = oldPool.getQueue();

            BlockingQueue<Runnable> tmpQueue;
            // 将旧线程池的任务转移到新线程池中 必须稍后再执行，否则 指标会在调用 shutdown 之后被清除
            if (!oldPoolQueue.isEmpty()) {
                tmpQueue = new LinkedBlockingQueue<>(oldPoolQueue.size());
                oldPoolQueue.drainTo(tmpQueue);
            } else {
                tmpQueue = null;
            }

            // 关闭旧的线程池
            oldPool.shutdown();

            // 获取BeanDefinitionRegistry
            ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
            BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) configurableApplicationContext.getBeanFactory();

            // 创建新的BeanDefinition
            BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(DynamicThreadPool.class, () -> newPool).getBeanDefinition();

            // 重新注册Bean
            beanDefinitionRegistry.removeBeanDefinition(poolName);
            beanDefinitionRegistry.registerBeanDefinition(poolName, beanDefinition);

            // 在新线程中执行任务转移，尽快释放锁
            if (tmpQueue != null) {
                new Thread(() -> {
                    while (!tmpQueue.isEmpty()) {
                        try {
                            Runnable task = tmpQueue.poll();
                            if (task != null) {
                                newPool.execute(task);
                            }
                        } catch (Exception e) {
                            log.error("Transfer tasks from old thread pool [{}] to new thread pool [{}] failed", poolName, newConfig.getPoolName(), e);
                        }
                    }
                    log.info("Transfer tasks from old thread pool [{}] to new thread pool [{}] success", poolName, newConfig.getPoolName());

                }).start();
            }

            log.info("Reset thread pool [{}] success", poolName);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * DynamicThreadPool 只能通过该方法 或者 ApplicationContext.getBean(poolName, DynamicThreadPool.class) 获取
     *
     * @param poolName 线程池名称
     * @return DynamicThreadPool
     */
    public Optional<DynamicThreadPool> getDynamicThreadPool(@Nonnull String poolName) {
        Striped<Lock> lock = LOCK_MAP.computeIfAbsent(poolName, k -> Striped.lock(10));
        Lock readLock = lock.get(poolName);
        readLock.lock();
        try {
            if (isThreadPoolNotExist(poolName)) {
                return Optional.empty();
            }
            return Optional.of(applicationContext.getBean(poolName, DynamicThreadPool.class));
        } finally {
            readLock.unlock();
        }
    }

    public void switchMonitor(SwitchMonitorParam param) {
        String poolName = param.poolName();
        if (isThreadPoolNotExist(poolName)) {
            return;
        }
        boolean enable = param.enableMonitor();
        DynamicThreadPool dynamicThreadPool = applicationContext.getBean(poolName, DynamicThreadPool.class);
        final MonitorConfig oldMonitor = dynamicThreadPool.getUnmodifyThreadPoolConfig().getMonitor();
        long ms = oldMonitor.getTimeWindowSeconds();
        dynamicThreadPool.updateMonitor(new MonitorConfig(enable,
                enable ? ms : DefaultConfigConstants.DEFAULT_TIME_WINDOW_SECONDS,
                oldMonitor.getMonitorUrl()));
    }

    public synchronized void switchAdaptive(SwitchAdaptiveParam param) throws NoSuchNamedThreadPoolException {
        String poolName = param.poolName();
        boolean enabled = param.enableAdaptive();
        if (isThreadPoolNotExist(poolName)) {
            log.error("Switch adaptive failed, pool not found: {}", poolName);
            throw new NoSuchNamedThreadPoolException("pool not found");
        }
        // 检查全局开关
        if (dynamicThreadPoolProperties != null && dynamicThreadPoolProperties.getAdaptive() != null && dynamicThreadPoolProperties.getAdaptive().isEnabled()) {
            DynamicThreadPool dynamicThreadPool = applicationContext.getBean(poolName, DynamicThreadPool.class);

            // 只有开启了监控，才能开启自适应
            if (dynamicThreadPool.getUnmodifyThreadPoolConfig().getMonitor() == null || !dynamicThreadPool.getUnmodifyThreadPoolConfig().getMonitor().isEnabled()) {
                log.error("Switch adaptive failed, monitor is disabled");
                throw new IllegalArgumentException("monitor is disabled, please enable monitor first");
            }
            AdaptiveConfig newAdaptiveConfig = new AdaptiveConfig(enabled);
            dynamicThreadPool.updateAdaptive(newAdaptiveConfig);
            log.info("Switch adaptive success, pool: {}, enable: {}", poolName, param.enableAdaptive());
        } else {
            log.error("Switch adaptive failed, global adaptive is disabled");
        }

    }

}
