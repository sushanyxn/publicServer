package com.slg.game.gm.service;

import com.slg.common.log.LoggerUtil;
import com.slg.game.base.player.model.Player;
import com.slg.game.gm.model.GMMethodMeta;
import com.slg.game.gm.model.IGMCommand;
import com.slg.game.net.ToClientPacketUtil;
import com.slg.net.message.clientmessage.gm.packet.SM_GMResult;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * GM 服务
 * 自动扫描所有 {@link IGMCommand} 实现类中的实例方法（排除构造方法和静态方法），
 * 要求第一个参数为 Player，按「方法名（不区分大小写）+ 参数数量」注册。
 * GM 方法返回 int 错误码：0 成功，非 0 失败。
 *
 * @author yangxunan
 * @date 2026/03/13
 */
@Component
public class GMService {

    public static final int SUCCESS = 0;
    public static final int FAIL = 1;

    @Autowired(required = false)
    private List<IGMCommand> gmCommands;

    /** key: "methodname_paramcount"（methodname 全小写，paramcount 不含 Player） */
    private final Map<String, GMMethodMeta> methodMap = new HashMap<>();

    @PostConstruct
    public void init() {
        if (gmCommands == null || gmCommands.isEmpty()) {
            LoggerUtil.info("未发现 GM 指令实现类");
            return;
        }

        for (IGMCommand cmd : gmCommands) {
            registerGMMethods(cmd);
        }

        LoggerUtil.info("注册 {} 个 GM 方法: {}", methodMap.size(), methodMap.keySet());
    }

    private void registerGMMethods(IGMCommand cmd) {
        for (Method method : cmd.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.isSynthetic() || method.isBridge()) {
                continue;
            }

            Class<?>[] allParams = method.getParameterTypes();
            if (allParams.length == 0 || allParams[0] != Player.class) {
                continue;
            }

            Class<?>[] userParams = Arrays.copyOfRange(allParams, 1, allParams.length);
            String key = method.getName().toLowerCase() + "_" + userParams.length;

            if (methodMap.containsKey(key)) {
                LoggerUtil.warn("GM 方法 key={} 重复注册（已有 {}.{}()），跳过",
                        key,
                        methodMap.get(key).getMethod().getDeclaringClass().getSimpleName(),
                        methodMap.get(key).getMethod().getName());
                continue;
            }

            method.setAccessible(true);
            methodMap.put(key, new GMMethodMeta(cmd, method, userParams));
        }
    }

    /**
     * 执行 GM 指令并推送结果
     *
     * @param player  执行者
     * @param command 指令字符串，如 "gainHero 1001"
     */
    public void executeCommand(Player player, String command) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            ToClientPacketUtil.send(player, SM_GMResult.valueOf(command, FAIL, "空指令"));
            return;
        }

        String methodName = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        String key = methodName.toLowerCase() + "_" + args.length;
        GMMethodMeta meta = methodMap.get(key);
        if (meta == null) {
            ToClientPacketUtil.send(player, SM_GMResult.valueOf(command, FAIL,
                    "未找到 GM 方法: " + methodName + "（参数数量: " + args.length + "）"));
            return;
        }

        try {
            Object[] invokeArgs = new Object[args.length + 1];
            invokeArgs[0] = player;
            for (int i = 0; i < args.length; i++) {
                invokeArgs[i + 1] = convertParam(args[i], meta.getParamTypes()[i]);
            }

            Object result = meta.getMethod().invoke(meta.getBean(), invokeArgs);
            int code = (result instanceof Number) ? ((Number) result).intValue() : SUCCESS;
            String msg = code == SUCCESS ? "执行成功" : "执行失败";
            ToClientPacketUtil.send(player, SM_GMResult.valueOf(command, code, msg));
        } catch (Exception e) {
            LoggerUtil.error("GM 指令执行异常: {}", command, e);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            ToClientPacketUtil.send(player, SM_GMResult.valueOf(command, FAIL, "异常: " + cause.getMessage()));
        }
    }

    /**
     * 获取所有已注册的 GM 方法 key 列表
     */
    public Collection<String> getRegisteredMethods() {
        return Collections.unmodifiableCollection(methodMap.keySet());
    }

    private Object convertParam(String value, Class<?> type) {
        if (type == String.class) return value;
        if (type == int.class || type == Integer.class) return Integer.parseInt(value);
        if (type == long.class || type == Long.class) return Long.parseLong(value);
        if (type == double.class || type == Double.class) return Double.parseDouble(value);
        if (type == float.class || type == Float.class) return Float.parseFloat(value);
        if (type == boolean.class || type == Boolean.class) return Boolean.parseBoolean(value);
        if (type == short.class || type == Short.class) return Short.parseShort(value);
        throw new IllegalArgumentException("不支持的参数类型: " + type.getSimpleName());
    }

}
