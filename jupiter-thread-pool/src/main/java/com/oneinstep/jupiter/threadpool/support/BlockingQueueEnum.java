package com.oneinstep.jupiter.threadpool.support;

import lombok.Getter;

import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 阻塞队列枚举
 */
public enum BlockingQueueEnum {

    LINKED_BLOCKING_QUEUE("LinkedBlockingQueue", LinkedBlockingQueue::new),
    ARRAY_BLOCKING_QUEUE("ArrayBlockingQueue", ArrayBlockingQueue::new),
    SYNCHRONOUS_QUEUE("SynchronousQueue", cap -> new SynchronousQueue<>()),
    PRIORITY_BLOCKING_QUEUE("PriorityBlockingQueue", PriorityBlockingQueue::new);

    @Getter
    private final String queueType;

    private final Function<Integer, BlockingQueue<Runnable>> queueFactory;

    BlockingQueueEnum(String queueType, Function<Integer, BlockingQueue<Runnable>> queueFactory) {
        this.queueType = queueType;
        this.queueFactory = queueFactory;
    }

    public BlockingQueue<Runnable> createQueue(Integer capacity) {
        return queueFactory.apply(capacity);
    }

    public static BlockingQueueEnum getQueueByName(String queueName) {
        return Stream.of(BlockingQueueEnum.values()).filter(queue -> queue.queueType.equals(queueName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported queue type: " + queueName));
    }

}
