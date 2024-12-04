package com.oneinstep.jupiter.threadpool.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * TimeWindowExecutionTimeCounter is a sliding window counter for execution time.
 */
@Slf4j
public class TimeWindowExecutionTimeCounter {

    private final LongAdderSlidingWindow slidingWindow;

    public TimeWindowExecutionTimeCounter(int bucketNum, long timeWindowSeconds) {
        this.slidingWindow = new LongAdderSlidingWindow(bucketNum, TimeUnit.SECONDS.toMillis(timeWindowSeconds));
    }

    // 获取当前窗口的平均值
    public double getAverage() {
        double totalExecutionTime = 0.0;
        double totalCount = 0.0;
        List<ExecutionTimeCounter> windows = this.slidingWindow.values();
        for (ExecutionTimeCounter window : windows) {
            double windowTotalExecutionTime = window.getTotalExecutionTime();
            double windowCount = window.getCount();

            // 检查是否会溢出
            if (Double.MAX_VALUE - totalExecutionTime < windowTotalExecutionTime) {
                // 记录警告日志并重置计数器
                log.error("Warning: totalExecutionTime is approaching the maximum value of double. Resetting counters.");
                resetCounters();
                return 0;
            }

            totalExecutionTime += windowTotalExecutionTime;
            totalCount += windowCount;
        }
        return totalCount == 0 ? 0 : totalExecutionTime / totalCount;
    }

    public long bucketLivedSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(
                this.slidingWindow.values().size() * this.slidingWindow.getPaneIntervalInMs());
    }

    public long bucketLivedMillSeconds() {
        return this.slidingWindow.getIntervalInMs()
                - (System.currentTimeMillis() - this.slidingWindow.currentPane().getEndInMs());
    }

    public void addExecutionTime(long executionTime) {
        this.slidingWindow.currentPane().getValue().addExecutionTime(executionTime);
    }

    private void resetCounters() {
        for (ExecutionTimeCounter counter : this.slidingWindow.values()) {
            counter.reset();
        }
    }

    /**
     * Sliding window of type ExecutionTimeCounter.
     */
    private static class LongAdderSlidingWindow extends SlidingWindow<ExecutionTimeCounter> {

        public LongAdderSlidingWindow(int sampleCount, long intervalInMs) {
            super(sampleCount, intervalInMs);
        }

        @Override
        public ExecutionTimeCounter newEmptyValue(long timeMillis) {
            return new ExecutionTimeCounter();
        }

        @Override
        protected Pane<ExecutionTimeCounter> resetPaneTo(final Pane<ExecutionTimeCounter> pane, long startTime) {
            pane.setStartInMs(startTime);
            pane.getValue().reset();
            return pane;
        }
    }

    /**
     * ExecutionTimeCounter to hold total execution time and count.
     */
    private static class ExecutionTimeCounter {
        private final LongAdder totalExecutionTime = new LongAdder();
        private final LongAdder count = new LongAdder();

        public void addExecutionTime(long executionTime) {
            totalExecutionTime.add(executionTime);
            count.increment();
        }

        public double getTotalExecutionTime() {
            return totalExecutionTime.sum();
        }

        public double getCount() {
            return count.sum();
        }

        public void reset() {
            totalExecutionTime.reset();
            count.reset();
        }
    }

}
