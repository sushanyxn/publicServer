package com.slg.net.message.innermessage.socket.packet;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 内部链接注册返回
 *
 * @author yangxunan
 * @date 2026/1/26
 */
@Getter
@Setter
@NoArgsConstructor
public class IM_RegisterSessionResponce {

    /** 注册结果（0 成功，非 0 失败） */
    private int result;

    /** 是否需要重新初始化（首次连接或宽限期已过为 true，宽限期内重连为 false） */
    private boolean needReInit;

    /**
     * 单参数工厂方法（兼容原有调用方，如 Game 侧 socketRegisterRequest）
     *
     * @param result 注册结果
     * @return 响应对象
     */
    public static IM_RegisterSessionResponce valueOf(int result) {
        IM_RegisterSessionResponce resp = new IM_RegisterSessionResponce();
        resp.result = result;
        return resp;
    }

    /**
     * 双参数工厂方法（Scene 侧 socketRegisterRequest 使用，传入 needReInit）
     *
     * @param result     注册结果
     * @param needReInit 是否需要重新初始化
     * @return 响应对象
     */
    public static IM_RegisterSessionResponce valueOf(int result, boolean needReInit) {
        IM_RegisterSessionResponce resp = new IM_RegisterSessionResponce();
        resp.result = result;
        resp.needReInit = needReInit;
        return resp;
    }
}
