package com.oneinstep.jupiter.threadpool.config;

import com.oneinstep.jupiter.threadpool.support.BlockingQueueEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.BlockingQueue;

/**
 * Work queue configuration.
 */
@Data
public class WorkQueueConfig {
    // Queue type
    private String type = DefaultConfigConstants.DEFAULT_QUEUE_TYPE;
    // Queue capacity
    private Integer capacity = DefaultConfigConstants.DEFAULT_QUEUE_CAPACITY;

    public BlockingQueue<Runnable> createQueue() {
        if (StringUtils.isBlank(type) ||
                (!BlockingQueueEnum.SYNCHRONOUS_QUEUE.getQueueType().equals(this.type) && (capacity == null || capacity <= 0))) {
            throw new IllegalArgumentException("Illegal work queue configuration: type=" + type + ", capacity=" + capacity);
        }
        return BlockingQueueEnum.getQueueByName(this.getType()).createQueue(this.getCapacity());
    }

    public void setCapacity(Integer capacity) {
        if (BlockingQueueEnum.SYNCHRONOUS_QUEUE.getQueueType().equals(this.type)) {
            this.capacity = null;
        } else {
            this.capacity = capacity;
        }
    }

    public Integer getCapacity() {
        return BlockingQueueEnum.SYNCHRONOUS_QUEUE.getQueueType().equals(this.type) ? null : capacity;
    }

}