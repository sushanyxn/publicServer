package com.slg.net.message.core.anno;

import java.lang.annotation.*;

/**
 * 消息处理器注解
 * 标注在方法上，表示该方法用于处理指定类型的消息
 * 
 * 方法签名要求：
 * - 第一个参数必须是 NetSession
 * - 第二个参数必须是已注册的协议类型
 * 
 * 示例：
 * <pre>
 * {@code
 * @MessageHandler
 * public void handleLogin(NetSession session, CM_LoginReq request) {
 *     // 处理登录逻辑
 * }
 * }
 * </pre>
 * 
 * @author yangxunan
 * @date 2026/01/22
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageHandler {
}

