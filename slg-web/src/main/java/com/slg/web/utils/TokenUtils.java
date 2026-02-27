package com.slg.web.utils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Token 生成工具类
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class TokenUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private TokenUtils() {
    }

    /**
     * 生成安全随机 Token（URL-safe Base64 编码，44 字符）
     *
     * @return 随机 Token 字符串
     */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
