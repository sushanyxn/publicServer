package com.slg.net.message.core.inject;

import com.slg.common.log.LoggerUtil;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 消息包扫描器
 * 使用 ClassGraph 扫描所有 packet 包下的消息类，并验证类名唯一性
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageScanner {
    
    /**
     * 消息包的基础路径
     */
    private static final String BASE_PACKAGE = "com.slg.net.message";
    
    /**
     * 扫描所有 packet 包下的类
     * 
     * @return 类名到 Class 的映射
     * @throws IllegalStateException 如果发现重名类
     */
    public Map<String, Class<?>> scanPacketClasses() {
        
        // 使用 ClassGraph 扫描类路径（安静模式，不输出日志）
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages(BASE_PACKAGE)  // 只扫描指定包
                .enableClassInfo()              // 启用类信息
                .scan()) {                      // 执行扫描
            
            // 收集类名和对应的 Class 对象
            Map<String, List<Class<?>>> classNameMap = new HashMap<>();
            
            // 获取所有类（包括抽象类和接口）
            for (ClassInfo classInfo : scanResult.getAllClasses()) {
                String fullName = classInfo.getName();
                
                // 检查是否在 packet 包下（支持任意层级）
                if (!fullName.contains(".packet.")) {
                    continue;
                }
                
                try {
                    Class<?> clazz = classInfo.loadClass();
                    String simpleName = clazz.getSimpleName();
                    
                    classNameMap.computeIfAbsent(simpleName, k -> new ArrayList<>()).add(clazz);
                    
                } catch (Throwable e) {
                    LoggerUtil.error("无法加载类: {}", fullName, e);
                    throw new IllegalStateException("无法加载消息类: " + fullName, e);
                }
            }
            
            return validateAndBuildResult(classNameMap);
        }
    }
    
    /**
     * 验证类名唯一性并构建结果
     */
    private Map<String, Class<?>> validateAndBuildResult(Map<String, List<Class<?>>> classNameMap) {
        
        // 验证类名唯一性
        List<String> duplicateNames = classNameMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (!duplicateNames.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("发现重名的消息类，所有 packet 包下的类名必须全局唯一:\n");
            for (String name : duplicateNames) {
                List<Class<?>> classes = classNameMap.get(name);
                errorMsg.append("  类名: ").append(name).append("\n");
                for (Class<?> clazz : classes) {
                    errorMsg.append("    - ").append(clazz.getName()).append("\n");
                }
            }
            LoggerUtil.error(errorMsg.toString());
            throw new IllegalStateException(errorMsg.toString());
        }
        
        // 构建结果映射
        Map<String, Class<?>> result = new HashMap<>();
        for (Map.Entry<String, List<Class<?>>> entry : classNameMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get(0));
        }
        
        LoggerUtil.debug("消息类扫描完成，共扫描到 {} 个类", result.size());
        return result;
    }
}

