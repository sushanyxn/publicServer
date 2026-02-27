package com.slg.net.message.core.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.net.message.core.ProtocolIds;
import com.slg.net.message.core.inject.MessageScanner;
import com.slg.net.message.core.model.MessageMeta;
import com.slg.net.message.core.model.MessageMetaFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * 消息注册中心
 * 管理所有消息类型的协议号、元信息映射
 * 单例模式，启动时完成所有初始化和验证
 * 
 * @author yangxunan
 * @date 2026/01/21
 */
public class MessageRegistry {
    
    /**
     * 单例实例
     */
    private static volatile MessageRegistry instance;
    
    /**
     * 协议号 → 类型
     */
    private final Map<Integer, Class<?>> idToType = new HashMap<>();
    
    /**
     * 类型 → 协议号
     */
    private final Map<Class<?>, Integer> typeToId = new HashMap<>();
    
    /**
     * 类型 → Meta
     */
    private final Map<Class<?>, MessageMeta> classToMeta = new HashMap<>();
    
    /**
     * 消息扫描器
     */
    private final MessageScanner scanner = new MessageScanner();
    
    /**
     * Meta 工厂
     */
    private final MessageMetaFactory metaFactory = new MessageMetaFactory();
    
    /**
     * 私有构造函数
     */
    private MessageRegistry() {
        initialize();
    }
    
    /**
     * 获取单例实例
     */
    public static MessageRegistry getInstance() {
        if (instance == null) {
            synchronized (MessageRegistry.class) {
                if (instance == null) {
                    instance = new MessageRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化注册中心
     */
    private void initialize() {
        LoggerUtil.info("开始初始化消息注册中心");

        try {
            // 1. 注册基础类型
            registerBasicTypes();
            
            // 2. 扫描 packet 包
            Map<String, Class<?>> scannedClasses = scanner.scanPacketClasses();

            // 3. 加载 message.yml
            Map<Integer, String> messageConfig = loadMessageConfig();

            // 4. 验证配置有效性
            validateConfiguration(messageConfig, scannedClasses);

            // 5. 验证配置完整性
            validateCompleteness(messageConfig, scannedClasses);

            // 6. 注册用户消息
            registerUserMessages(messageConfig, scannedClasses);
            
            LoggerUtil.info("消息注册中心初始化完成");
        } catch (Exception e) {
            LoggerUtil.error("消息注册中心初始化失败！");
            LoggerUtil.error("错误详情：", e);
            throw new IllegalStateException("消息注册中心初始化失败", e);
        }
    }
    
    /**
     * 注册基础类型
     */
    private void registerBasicTypes() {

        // 基础类型和包装类型使用同一个协议号
        registerType(ProtocolIds.BYTE, Byte.class);
        registerType(ProtocolIds.BYTE, byte.class);

        registerType(ProtocolIds.SHORT, Short.class);
        registerType(ProtocolIds.SHORT, short.class);

        registerType(ProtocolIds.INT, Integer.class);
        registerType(ProtocolIds.INT, int.class);

        registerType(ProtocolIds.LONG, Long.class);
        registerType(ProtocolIds.LONG, long.class);

        registerType(ProtocolIds.FLOAT, Float.class);
        registerType(ProtocolIds.FLOAT, float.class);

        registerType(ProtocolIds.DOUBLE, Double.class);
        registerType(ProtocolIds.DOUBLE, double.class);

        registerType(ProtocolIds.BOOLEAN, Boolean.class);
        registerType(ProtocolIds.BOOLEAN, boolean.class);

        registerType(ProtocolIds.STRING, String.class);
        registerType(ProtocolIds.BYTE_ARRAY, byte[].class);
        registerType(ProtocolIds.LIST, List.class);
        registerType(ProtocolIds.SET, Set.class);
        registerType(ProtocolIds.MAP, Map.class);
        registerType(ProtocolIds.ENUM, Enum.class);
        registerType(ProtocolIds.ARRAY, Object[].class);
        
    }
    
    /**
     * 加载 message.yml 配置
     */
    private Map<Integer, String> loadMessageConfig() {

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("message.yml");
        if (inputStream == null) {
            LoggerUtil.error("找不到 message.yml 文件");
            throw new IllegalStateException("message.yml 文件必须存在");
        }
        
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);
            
            if (config == null || !config.containsKey("messages")) {
                LoggerUtil.error("message.yml 格式错误：缺少 messages 节点");
                throw new IllegalStateException("message.yml 格式错误");
            }
            
            @SuppressWarnings("unchecked")
            Map<String, List<String>> messages = (Map<String, List<String>>) config.get("messages");
            
            Map<Integer, String> result = new HashMap<>();
            for (List<String> strings : messages.values()) {
                if (strings == null || strings.isEmpty()) {
                    continue;
                }
                for (String line : strings) {
                    // 解析格式：协议号, 类名[, 参数...]
                    String[] parts = line.split(",");
                    if (parts.length < 2) {
                        LoggerUtil.error("message.yml 配置格式错误：{}", line);
                        throw new IllegalStateException("message.yml 配置格式错误: " + line);
                    }

                    int protocolId = Integer.parseInt(parts[0].trim());
                    String className = parts[1].trim();

                    // 检查协议号是否重复
                    if (result.containsKey(protocolId)) {
                        LoggerUtil.error("协议号 {} 重复", protocolId);
                        throw new IllegalStateException("协议号重复: " + protocolId);
                    }

                    result.put(protocolId, className);
                }
            }

            return result;
        } catch (Exception e) {
            LoggerUtil.error("加载 message.yml 失败", e);
            throw new IllegalStateException("加载 message.yml 失败", e);
        }
    }
    
    /**
     * 验证配置有效性
     */
    private void validateConfiguration(Map<Integer, String> config, Map<String, Class<?>> scannedClasses) {

        List<String> errors = new ArrayList<>();
        
        for (Map.Entry<Integer, String> entry : config.entrySet()) {
            int protocolId = entry.getKey();
            String className = entry.getValue();
            
            // 检查类是否存在
            if (!scannedClasses.containsKey(className)) {
                errors.add(String.format("类名 '%s' (协议号: %d) 不存在或不在 packet 包下", className, protocolId));
            }
            
            // 检查协议号范围
            if (protocolId < ProtocolIds.USER_MESSAGE_START) {
                errors.add(String.format("协议号 %d 小于用户消息起始 ID %d", protocolId, ProtocolIds.USER_MESSAGE_START));
            }
            
            // 检查协议号是否与基础类型冲突
            if (idToType.containsKey(protocolId)) {
                errors.add(String.format("协议号 %d 与基础类型冲突", protocolId));
            }
        }
        
        if (!errors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("message.yml 配置验证失败:\n");
            for (String error : errors) {
                errorMsg.append("  - ").append(error).append("\n");
            }
            LoggerUtil.error(errorMsg.toString());
            throw new IllegalStateException(errorMsg.toString());
        }
        
    }
    
    /**
     * 验证配置完整性
     */
    private void validateCompleteness(Map<Integer, String> config, Map<String, Class<?>> scannedClasses) {

        Set<String> configuredClasses = new HashSet<>(config.values());
        Set<String> unconfiguredClasses = new HashSet<>();
        
        for (String className : scannedClasses.keySet()) {
            if (!configuredClasses.contains(className)) {
                unconfiguredClasses.add(className);
            }
        }
        
        if (!unconfiguredClasses.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("发现未配置的消息类，所有 packet 包下的类都必须在 message.yml 中配置:\n");
            for (String className : unconfiguredClasses) {
                Class<?> clazz = scannedClasses.get(className);
                errorMsg.append("  - ").append(className).append(" (").append(clazz.getName()).append(")\n");
            }
            LoggerUtil.error(errorMsg.toString());
            throw new IllegalStateException(errorMsg.toString());
        }
        
    }
    
    /**
     * 注册用户消息
     */
    private void registerUserMessages(Map<Integer, String> config, Map<String, Class<?>> scannedClasses) {

        for (Map.Entry<Integer, String> entry : config.entrySet()) {
            int protocolId = entry.getKey();
            String className = entry.getValue();
            Class<?> clazz = scannedClasses.get(className);
            
            // 注册类型映射
            registerType(protocolId, clazz);
            
            // 生成并缓存 Meta
            MessageMeta meta = metaFactory.createMeta(protocolId, clazz);
            classToMeta.put(clazz, meta);
            
        }
        
    }
    
    /**
     * 注册类型映射
     */
    private void registerType(int protocolId, Class<?> clazz) {
        // 注意：基础类型和包装类型共用协议号，所以这里可能会覆盖
        if (!idToType.containsKey(protocolId)) {
            idToType.put(protocolId, clazz);
        }
        typeToId.put(clazz, protocolId);
    }
    
    /**
     * 根据协议号获取类型
     */
    public Class<?> getClass(int protocolId) {
        return idToType.get(protocolId);
    }
    
    /**
     * 根据类型获取协议号
     */
    public Integer getProtocolId(Class<?> clazz) {
        return typeToId.get(clazz);
    }
    
    /**
     * 根据类型获取 Meta
     */
    public MessageMeta getMeta(Class<?> clazz) {
        return classToMeta.get(clazz);
    }
    
    /**
     * 判断类型是否可实例化
     */
    public boolean isInstantiable(Class<?> clazz) {
        MessageMeta meta = classToMeta.get(clazz);
        return meta != null && meta.isInstantiable();
    }
}

