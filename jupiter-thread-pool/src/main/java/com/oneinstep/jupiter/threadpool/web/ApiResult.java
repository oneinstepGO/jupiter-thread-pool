package com.oneinstep.jupiter.threadpool.web;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class ApiResult<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public ApiResult(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(ResultCode.FAIL.getCode(), message);
    }

}
