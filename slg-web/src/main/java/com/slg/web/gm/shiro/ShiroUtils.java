package com.slg.web.gm.shiro;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Shiro 密码工具类
 * 提供密码加盐哈希和校验功能
 *
 * @author yangxunan
 * @date 2026-02-25
 */
public class ShiroUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_LENGTH = 16;
    private static final int HASH_ITERATIONS = 2;

    private ShiroUtils() {
    }

    /**
     * 生成随机盐值
     *
     * @return Base64 编码的盐值
     */
    public static String generateSalt() {
        byte[] bytes = new byte[SALT_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 使用 SHA-256 对密码加盐哈希
     *
     * @param password 明文密码
     * @param salt     盐值
     * @return 哈希后的密码（Hex 编码）
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = password + salt;
            byte[] hash = digest.digest(input.getBytes());
            for (int i = 1; i < HASH_ITERATIONS; i++) {
                hash = digest.digest(hash);
            }
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
    }

    /**
     * 校验密码是否匹配
     *
     * @param password       明文密码
     * @param salt           盐值
     * @param hashedPassword 数据库中的哈希密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String salt, String hashedPassword) {
        return hashPassword(password, salt).equals(hashedPassword);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
