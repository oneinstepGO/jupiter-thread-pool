package com.oneinstep.jupiter.threadpool.support;

public class CallableNotSupportException extends RuntimeException {
    public CallableNotSupportException() {
        super("NamedCallable is required");
    }

    public CallableNotSupportException(String message) {
        super(message);
    }

    public CallableNotSupportException(String message, Throwable cause) {
        super(message, cause);
    }

    public CallableNotSupportException(Throwable cause) {
        super(cause);
    }

    protected CallableNotSupportException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
