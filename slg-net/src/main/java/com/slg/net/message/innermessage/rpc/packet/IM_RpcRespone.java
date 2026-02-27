package com.slg.net.message.innermessage.rpc.packet;

import lombok.Data;

/**
 * RPC 响应消息
 *
 * @author yangxunan
 * @date 2026/1/23
 */
@Data
public class IM_RpcRespone {
    
    /**
     * 请求ID（用于响应匹配）
     */
    private long id;
    
    /**
     * 响应结果
     */
    private Object result;
    
    /**
     * 异常信息（如果有）
     */
    private String error;
    
}
