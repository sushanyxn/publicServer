package com.slg.common.hotreload;

import java.lang.instrument.Instrumentation;

/**
 * 静态持有 JVM Instrumentation 实例，由 {@link HotReloadAgent} 在 premain/agentmain 阶段写入
 *
 * @author yangxunan
 * @date 2026/03/10
 */
public final class InstrumentationHolder {

    private static volatile Instrumentation instrumentation;

    private InstrumentationHolder() {
    }

    static void set(Instrumentation inst) {
        instrumentation = inst;
    }

    public static Instrumentation get() {
        return instrumentation;
    }

    public static boolean isAvailable() {
        return instrumentation != null;
    }
}
