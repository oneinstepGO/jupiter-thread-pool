package com.oneinstep.jupiter.threadpool.metrics;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 * Wrapper around Counter like Long and Integer.
 * <p>copy from dubbo-metrics</p>
 */
@Slf4j
public class TimeWindowCounter {

    private final LongAdderSlidingWindow slidingWindow;

    public TimeWindowCounter(int bucketNum, long timeWindowSeconds) {
        this.slidingWindow = new LongAdderSlidingWindow(bucketNum, TimeUnit.SECONDS.toMillis(timeWindowSeconds));
    }

    // 获取当前窗口的总和
    public double get() {
        double result = 0.0;
        List<LongAdder> windows = this.slidingWindow.values();
        for (LongAdder window : windows) {
            result += window.sum();
        }
        return result;
    }

    public long bucketLivedSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(
                this.slidingWindow.values().size() * this.slidingWindow.getPaneIntervalInMs());
    }

    public long bucketLivedMillSeconds() {
        return this.slidingWindow.getIntervalInMs()
                - (System.currentTimeMillis() - this.slidingWindow.currentPane().getEndInMs());
    }

    public void increment() {
        this.increment(1L);
    }

    public void increment(Long step) {
        this.slidingWindow.currentPane().getValue().add(step);
    }

    public void decrement() {
        this.decrement(1L);
    }

    public void decrement(Long step) {
        this.slidingWindow.currentPane().getValue().add(-step);
    }

    /**
     * Sliding window of type LongAdder.
     */
    private static class LongAdderSlidingWindow extends SlidingWindow<LongAdder> {

        public LongAdderSlidingWindow(int sampleCount, long intervalInMs) {
            super(sampleCount, intervalInMs);
        }

        @Override
        public LongAdder newEmptyValue(long timeMillis) {
            return new LongAdder();
        }

        @Override
        protected Pane<LongAdder> resetPaneTo(final Pane<LongAdder> pane, long startTime) {
            pane.setStartInMs(startTime);
            pane.getValue().reset();
            return pane;
        }
    }
}
