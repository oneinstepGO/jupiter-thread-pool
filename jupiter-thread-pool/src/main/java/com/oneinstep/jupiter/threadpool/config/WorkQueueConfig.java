package com.oneinstep.jupiter.threadpool.config;

import com.oneinstep.jupiter.threadpool.support.BlockingQueueEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.concurrent.BlockingQueue;

/**
 * Work queue configuration.
 */
@Data
public class WorkQueueConfig implements Serializable {
    @Serial
    private static final long serialVersionUID = -1L;
    // Queue type
    private String type = DefaultConfigConstants.DEFAULT_QUEUE_TYPE;
    // Queue capacity
    private int capacity = DefaultConfigConstants.DEFAULT_QUEUE_CAPACITY;

    public BlockingQueue<Runnable> createQueue() {
        if (StringUtils.isBlank(type) ||
                (!BlockingQueueEnum.SYNCHRONOUS_QUEUE.getQueueType().equals(this.type) && (capacity <= 0))) {
            throw new IllegalArgumentException("Illegal work queue configuration: type=" + type + ", capacity=" + capacity);
        }
        return BlockingQueueEnum.getQueueByName(this.getType()).createQueue(this.getCapacity());
    }

    public WorkQueueConfig copy() {
        WorkQueueConfig copy = new WorkQueueConfig();
        copy.setType(this.getType());
        copy.setCapacity(this.getCapacity());
        return copy;
    }

}