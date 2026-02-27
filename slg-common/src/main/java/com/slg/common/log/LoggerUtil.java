package com.slg.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志工具类
 * 提供统一的日志输出方法和便捷的日志格式化
 * 自动在日志消息前添加调用者的类名和行号
 * 
 * @author yangxunan
 * @date 2025-12-22
 */
public class LoggerUtil {

    private static final Logger logger = LoggerFactory.getLogger(LoggerUtil.class);

    /**
     * 获取调用者信息（类名和行号）
     * 
     * @return 格式化的调用者信息，例如：[PlayerService:45]
     */
    private static String getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        if (stackTrace.length > 4) {
            StackTraceElement caller = stackTrace[4];
            String className = caller.getClassName();
            String methodName = caller.getMethodName();
            String fileName = caller.getFileName();
            int lineNumber = caller.getLineNumber();

            // 获取简单类名（不带包名）
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

            return String.format("(%s:%d)", fileName, lineNumber);
        }

        return "Unknown";
    }
    
    /**
     * 在消息前添加调用者信息
     * 
     * @param message 原始消息
     * @return 添加了调用者信息的消息
     */
    private static String formatMessageWithCaller(String message) {
        return getCallerInfo() + " " + message;
    }

    /**
     * 输出调试日志
     *
     * @param message 日志消息
     * @param args 参数
     */
    public static void debug(String message, Object... args) {
        logger.debug(formatMessageWithCaller(message), args);
    }

    /**
     * 输出信息日志
     *
     * @param message 日志消息
     * @param args    参数
     */
    public static void info(String message, Object... args) {
        logger.info(formatMessageWithCaller(message), args);
    }

    /**
     * 输出警告日志
     *
     * @param message 日志消息
     * @param args    参数
     */
    public static void warn(String message, Object... args) {
        logger.warn(formatMessageWithCaller(message), args);
    }

    /**
     * 输出错误日志
     *
     * @param message 日志消息
     * @param args 参数
     */
    public static void error(String message, Object... args) {
        logger.error(formatMessageWithCaller(message), args);
    }

    /**
     * 输出错误日志（带异常）
     *
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void error(String message, Throwable throwable) {
        logger.error(formatMessageWithCaller(message), throwable);
    }
}

