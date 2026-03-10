package com.slg.common.hotreload;

import java.lang.instrument.Instrumentation;
import java.util.Map;
import java.util.Set;

/**
 * Java Agent 入口，支持 premain（启动时 -javaagent）和 agentmain（运行时 Attach）。
 * <p>
 * 职责：
 * <ul>
 *   <li>存储 {@link Instrumentation} 到 {@link InstrumentationHolder}</li>
 *   <li>打开 java.lang 包的反射权限，用于后续 defineClass 加载全新类</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/03/10
 */
public class HotReloadAgent {

    /**
     * JVM 启动时通过 -javaagent 调用
     */
    public static void premain(String args, Instrumentation inst) {
        init(inst);
    }

    /**
     * 运行时通过 Attach API 调用
     */
    public static void agentmain(String args, Instrumentation inst) {
        init(inst);
    }

    private static void init(Instrumentation inst) {
        InstrumentationHolder.set(inst);
        openJavaLangForReflection(inst);
    }

    /**
     * 利用 Instrumentation.redefineModule 打开 java.lang 包的反射权限，
     * 使得 {@link HotReloadManager} 可以反射调用 ClassLoader.defineClass() 加载全新类。
     * 这样无需在启动参数中添加 --add-opens java.base/java.lang=ALL-UNNAMED。
     */
    private static void openJavaLangForReflection(Instrumentation inst) {
        try {
            Module javaBase = Object.class.getModule();
            Module ourModule = HotReloadAgent.class.getModule();
            inst.redefineModule(
                    javaBase,
                    Set.of(),
                    Map.of(),
                    Map.of("java.lang", Set.of(ourModule)),
                    Set.of(),
                    Map.of()
            );
        } catch (Exception e) {
            System.err.println("[HotReloadAgent] 打开 java.lang 反射权限失败: " + e.getMessage());
        }
    }
}
