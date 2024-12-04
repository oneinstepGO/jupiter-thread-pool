package com.oneinstep.jupiter.threadpool;

import lombok.Getter;

import java.util.concurrent.FutureTask;

/**
 * A named task.
 *
 * @param <V> the result type of method {@code call}
 */
@Getter
public class NamedFutureTask<V> extends FutureTask<V> {
    // The name of the task.
    private final NamedCallable<V> namedCallable;

    // Creates a {@code FutureTask} that will, upon running, execute the given {@code Callable}.
    public NamedFutureTask(NamedCallable<V> namedCallable) {
        super(namedCallable);
        this.namedCallable = namedCallable;
    }

}