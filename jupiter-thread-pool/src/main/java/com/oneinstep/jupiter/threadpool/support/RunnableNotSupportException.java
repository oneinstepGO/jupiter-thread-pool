package com.oneinstep.jupiter.threadpool.support;

public class RunnableNotSupportException extends RuntimeException {
    public RunnableNotSupportException() {
        super("NamedRunnable is required");
    }

    public RunnableNotSupportException(String message) {
        super(message);
    }

    public RunnableNotSupportException(String message, Throwable cause) {
        super(message, cause);
    }

    public RunnableNotSupportException(Throwable cause) {
        super(cause);
    }

    protected RunnableNotSupportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
