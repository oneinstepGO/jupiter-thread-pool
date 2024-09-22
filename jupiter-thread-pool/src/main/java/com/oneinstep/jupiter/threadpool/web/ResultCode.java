package com.oneinstep.jupiter.threadpool.web;

import lombok.Getter;

@Getter
public enum ResultCode {
    FAIL(500, "Fail"), SUCCESS(200, "Success");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
