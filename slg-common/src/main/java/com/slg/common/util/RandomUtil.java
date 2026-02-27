package com.slg.common.util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 随机数工具类
 * <p>基于 {@link ThreadLocalRandom} 提供线程安全的随机方法，供各模块统一使用。</p>
 *
 * @author yangxunan
 * @date 2026/2/7
 */
public final class RandomUtil {

    private RandomUtil() {
    }

    /**
     * 返回 [0, bound) 范围内的随机 int
     *
     * @param bound 上界（不包含）
     * @return 随机整数
     */
    public static int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * 返回 [origin, bound) 范围内的随机 int
     *
     * @param origin 下界（包含）
     * @param bound  上界（不包含）
     * @return 随机整数
     */
    public static int nextInt(int origin, int bound) {
        return ThreadLocalRandom.current().nextInt(origin, bound);
    }

    /**
     * 返回 [0, bound) 范围内的随机 long
     *
     * @param bound 上界（不包含）
     * @return 随机长整数
     */
    public static long nextLong(long bound) {
        return ThreadLocalRandom.current().nextLong(bound);
    }

    /**
     * 返回 [origin, bound) 范围内的随机 long
     *
     * @param origin 下界（包含）
     * @param bound  上界（不包含）
     * @return 随机长整数
     */
    public static long nextLong(long origin, long bound) {
        return ThreadLocalRandom.current().nextLong(origin, bound);
    }

    /**
     * 返回 [0.0, 1.0) 范围内的随机 double
     *
     * @return 随机双精度浮点数
     */
    public static double nextDouble() {
        return ThreadLocalRandom.current().nextDouble();
    }

    /**
     * 返回 [0.0, bound) 范围内的随机 double
     *
     * @param bound 上界（不包含）
     * @return 随机双精度浮点数
     */
    public static double nextDouble(double bound) {
        return ThreadLocalRandom.current().nextDouble(bound);
    }

    /**
     * 返回 [origin, bound) 范围内的随机 double
     *
     * @param origin 下界（包含）
     * @param bound  上界（不包含）
     * @return 随机双精度浮点数
     */
    public static double nextDouble(double origin, double bound) {
        return ThreadLocalRandom.current().nextDouble(origin, bound);
    }

    /**
     * 返回随机 boolean
     *
     * @return true 或 false 等概率
     */
    public static boolean nextBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    /**
     * 在 [0, size) 中随机一个下标，用于从列表等按索引取值
     *
     * @param size 集合大小（上界不包含）
     * @return 随机下标
     */
    public static int nextIndex(int size) {
        return size <= 0 ? 0 : ThreadLocalRandom.current().nextInt(size);
    }

    /**
     * 打乱列表顺序（原地修改）
     *
     * @param list 待打乱列表，可为空
     */
    public static void shuffle(List<?> list) {
        if (list != null && !list.isEmpty()) {
            Collections.shuffle(list, ThreadLocalRandom.current());
        }
    }
}
