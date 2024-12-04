package com.oneinstep.jupiter.threadpool.support;

/**
 * 线程池不存在异常
 */
public class NoSuchNamedThreadPoolException extends Exception {
    public NoSuchNamedThreadPoolException(String message) {
        super(message);
    }
}
