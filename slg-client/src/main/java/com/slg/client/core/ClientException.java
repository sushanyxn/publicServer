package com.slg.client.core;

import lombok.Getter;

/**
 * 客户端业务异常
 * 用于客户端操作中的可预期错误（如重复登录、账号不存在等），调用方应捕获并向用户展示提示信息
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
public class ClientException extends RuntimeException {

    public ClientException(String message) {
        super(message);
    }
}
