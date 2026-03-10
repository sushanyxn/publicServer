package com.slg.common.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Groovy 脚本执行引擎。
 * <p>
 * 脚本内可使用以下预置变量：
 * <ul>
 *   <li>{@code ctx} / {@code applicationContext} -- Spring ApplicationContext，可通过 ctx.getBean(...) 获取任意 Bean</li>
 *   <li>{@code log} -- SLF4J Logger</li>
 *   <li>{@code out} -- PrintWriter，脚本内 println 的输出会被捕获到结果中</li>
 * </ul>
 * <p>
 * <b>安全警告：</b>Groovy 脚本可执行任意代码，仅限内部 GM 工具或运维通道调用，禁止对外暴露。
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Component
public class GroovyScriptManager {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptManager.class);
    private static final int MAX_OUTPUT_LENGTH = 64 * 1024;

    private final ApplicationContext applicationContext;
    private final ReentrantLock scriptLock = new ReentrantLock();

    public GroovyScriptManager(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 执行 Groovy 脚本
     *
     * @param script 脚本内容
     * @return 执行结果
     */
    public ScriptExecuteResult execute(String script) {
        return execute(script, Map.of());
    }

    /**
     * 执行 Groovy 脚本，可传入额外绑定变量
     *
     * @param script        脚本内容
     * @param extraBindings 额外绑定变量（会覆盖同名预置变量）
     * @return 执行结果
     */
    public ScriptExecuteResult execute(String script, Map<String, Object> extraBindings) {
        if (script == null || script.isBlank()) {
            return ScriptExecuteResult.error("脚本内容为空", null, 0);
        }

        if (!scriptLock.tryLock()) {
            return ScriptExecuteResult.error("有其他脚本正在执行，请稍后重试", null, 0);
        }

        long startTime = System.currentTimeMillis();
        StringWriter outputWriter = new StringWriter();
        try {
            Binding binding = buildBindings(outputWriter, extraBindings);
            GroovyShell shell = new GroovyShell(Thread.currentThread().getContextClassLoader(), binding);

            log.info("开始执行 Groovy 脚本, 长度: {} 字符", script.length());
            Object result = shell.evaluate(script);

            long costMs = System.currentTimeMillis() - startTime;
            String output = truncateOutput(outputWriter.toString());
            log.info("Groovy 脚本执行成功, 耗时 {}ms, 返回类型: {}",
                    costMs, result != null ? result.getClass().getSimpleName() : "null");

            return ScriptExecuteResult.valueOf(result, output, costMs);

        } catch (Exception e) {
            long costMs = System.currentTimeMillis() - startTime;
            String output = truncateOutput(outputWriter.toString());
            String errorDetail = formatError(e);

            log.warn("Groovy 脚本执行失败, 耗时 {}ms: {}", costMs, e.getMessage());
            return ScriptExecuteResult.error(errorDetail, output, costMs);

        } finally {
            scriptLock.unlock();
        }
    }

    private Binding buildBindings(StringWriter outputWriter, Map<String, Object> extraBindings) {
        Binding binding = new Binding();
        binding.setVariable("ctx", applicationContext);
        binding.setVariable("applicationContext", applicationContext);
        binding.setVariable("log", log);
        binding.setVariable("out", new PrintWriter(outputWriter, true));

        if (extraBindings != null) {
            extraBindings.forEach(binding::setVariable);
        }
        return binding;
    }

    private String truncateOutput(String output) {
        if (output == null || output.isEmpty()) {
            return output;
        }
        if (output.length() > MAX_OUTPUT_LENGTH) {
            return output.substring(0, MAX_OUTPUT_LENGTH) + "\n... (输出过长，已截断)";
        }
        return output;
    }

    private String formatError(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stackTrace = sw.toString();
        if (stackTrace.length() > 4096) {
            stackTrace = stackTrace.substring(0, 4096) + "\n... (堆栈过长，已截断)";
        }
        return stackTrace;
    }
}
