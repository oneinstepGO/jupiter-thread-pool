package com.oneinstep.jupiter.threadpool;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.Callable;

/**
 * A named task.
 *
 * @param <V> the result type of method {@code call}
 */
@Getter
public class NamedCallable<V> implements Callable<V> {

    // Task name
    private final String name;
    // Callable task
    private final Callable<V> callable;

    @Setter
    private long startTime;

    @Setter
    private long submitTime;

    @Setter
    private long endTime;

    public NamedCallable(String name, Callable<V> callable) {
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        if (callable == null) {
            throw new IllegalArgumentException("callable cannot be null");
        }
        this.name = name;
        this.callable = callable;
    }

    @Override
    public V call() throws Exception {
        return callable.call();
    }
}