package com.slg.common.constant;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录平台类型枚举
 * 定义客户端支持的各类登录平台
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public enum PlatformType {

    DEV(1, "开发模式"),
    FACEBOOK(5, "Facebook"),
    APPSTORE(6, "Apple AppStore"),
    GOOGLE(7, "Google"),
    VISITOR(11, "游客"),
    UNKNOW(99, "未知平台"),
    ;

    private final int id;
    private final String desc;

    private static final Map<Integer, PlatformType> ID_MAP = new HashMap<>();

    static {
        for (PlatformType type : values()) {
            ID_MAP.put(type.id, type);
        }
    }

    PlatformType(int id, String desc) {
        this.id = id;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 ID 查找平台类型，未找到返回 UNKNOW
     */
    public static PlatformType findById(int id) {
        return ID_MAP.getOrDefault(id, UNKNOW);
    }
}
