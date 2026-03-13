package com.slg.common.exception;

import lombok.Getter;

/**
 * 客户端请求异常
 * 业务逻辑中校验不通过时抛出，框架层自动捕获并向客户端推送提示消息
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Getter
public class ClientRequestException extends RuntimeException {

    /** 消息 ID，对应 InfoId 常量与 MessageTable 配置 */
    private final int infoId;

    public ClientRequestException(int infoId) {
        super("infoId=" + infoId);
        this.infoId = infoId;
    }

}
