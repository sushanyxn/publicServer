package com.slg.frameworktest.hotreload;

import com.slg.common.script.GroovyScriptManager;
import com.slg.common.script.ScriptExecuteResult;
import com.slg.frameworktest.FrameworkTestRedisOnlyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Groovy 脚本引擎端到端集成测试
 *
 * @author framework-test
 */
@SpringBootTest(classes = FrameworkTestRedisOnlyApplication.class)
@ActiveProfiles("test")
class GroovyScriptE2EIntegrationTest {

    @Autowired
    private GroovyScriptManager groovyScriptManager;

    @Test
    @DisplayName("简单表达式求值")
    void simpleExpression() {
        ScriptExecuteResult result = groovyScriptManager.execute("1 + 1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo(2);
    }

    @Test
    @DisplayName("访问预置绑定变量 ctx")
    void accessSpringContext() {
        ScriptExecuteResult result = groovyScriptManager.execute(
                "ctx.getBeanDefinitionNames().length > 0");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo(true);
    }

    @Test
    @DisplayName("通过 ctx.getBean 获取 Bean 并调用方法")
    void getBeanAndInvoke() {
        ScriptExecuteResult result = groovyScriptManager.execute("""
                def mgr = ctx.getBean(com.slg.common.script.GroovyScriptManager)
                return mgr != null ? "found" : "missing"
                """);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo("found");
    }

    @Test
    @DisplayName("println 输出被正确捕获")
    void captureOutput() {
        ScriptExecuteResult result = groovyScriptManager.execute("""
                out.println "hello"
                out.println "world"
                return "done"
                """);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).contains("hello").contains("world");
        assertThat(result.getResult()).isEqualTo("done");
    }

    @Test
    @DisplayName("传入额外绑定变量")
    void extraBindings() {
        ScriptExecuteResult result = groovyScriptManager.execute(
                "return \"Hello, ${name}!\"",
                Map.of("name", "SLG"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().toString()).isEqualTo("Hello, SLG!");
    }

    @Test
    @DisplayName("脚本语法错误返回 error result")
    void compilationError() {
        ScriptExecuteResult result = groovyScriptManager.execute("def x = {{{");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).isNotBlank();
    }

    @Test
    @DisplayName("脚本运行时异常被正确捕获")
    void runtimeException() {
        ScriptExecuteResult result = groovyScriptManager.execute("""
                throw new RuntimeException("test error")
                """);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("test error");
    }

    @Test
    @DisplayName("空脚本返回错误")
    void emptyScript() {
        ScriptExecuteResult result = groovyScriptManager.execute("");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("为空");
    }

    @Test
    @DisplayName("脚本无返回值时 result 为 null")
    void noReturnValue() {
        ScriptExecuteResult result = groovyScriptManager.execute("def x = 42");

        assertThat(result.isSuccess()).isTrue();
    }
}
