package com.slg.web.response;

import lombok.Getter;
import lombok.Setter;

/**
 * 统一响应封装
 *
 * @author yangxunan
 * @date 2026-02-25
 */
@Getter
@Setter
public class Response<T> {

    private int code;
    private String message;
    private T data;

    private Response() {
    }

    public static <T> Response<T> success() {
        return success(null);
    }

    public static <T> Response<T> success(T data) {
        Response<T> response = new Response<>();
        response.code = ErrorCode.SUCCESS.getCode();
        response.message = ErrorCode.SUCCESS.getMessage();
        response.data = data;
        return response;
    }

    public static <T> Response<T> error(ErrorCode errorCode) {
        Response<T> response = new Response<>();
        response.code = errorCode.getCode();
        response.message = errorCode.getMessage();
        return response;
    }

    public static <T> Response<T> error(ErrorCode errorCode, String message) {
        Response<T> response = new Response<>();
        response.code = errorCode.getCode();
        response.message = message;
        return response;
    }

    public static <T> Response<T> error(int code, String message) {
        Response<T> response = new Response<>();
        response.code = code;
        response.message = message;
        return response;
    }
}
