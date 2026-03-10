package com.slg.frameworktest.hotreload;

import com.slg.common.hotreload.HotReloadManager;
import com.slg.common.hotreload.HotReloadResult;
import com.slg.common.hotreload.InstrumentationHolder;
import com.slg.common.hotreload.ReloadStatus;
import com.slg.frameworktest.FrameworkTestRedisOnlyApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agent 热更框架端到端集成测试。
 * <p>
 * 需要 -javaagent:slg-common.jar 启动参数（已在 pom.xml surefire 中配置）。
 * 使用 javax.tools.JavaCompiler 动态编译测试类，验证热更流程。
 *
 * @author framework-test
 */
@SpringBootTest(classes = FrameworkTestRedisOnlyApplication.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.TestMethodOrder(org.junit.jupiter.api.MethodOrderer.OrderAnnotation.class)
class HotReloadE2EIntegrationTest {

    @Autowired
    private HotReloadManager hotReloadManager;

    @Test
    @DisplayName("Instrumentation 已通过 -javaagent 注入")
    @org.junit.jupiter.api.Order(1)
    void instrumentationAvailable() {
        assertThat(InstrumentationHolder.isAvailable())
                .as("启动参数中应包含 -javaagent:slg-common.jar")
                .isTrue();
    }

    @Test
    @DisplayName("重定义已有类：方法体变更生效")
    @org.junit.jupiter.api.Order(2)
    void redefineExistingClass(@TempDir Path tempDir) throws Exception {

        String modifiedSource = """
                package com.slg.frameworktest.hotreload;
                public class HotReloadTestTarget {
                    public static String getValue() {
                        return "hot-reloaded";
                    }
                }
                """;

        compileToDir(tempDir, "com.slg.frameworktest.hotreload.HotReloadTestTarget", modifiedSource);

        HotReloadResult result = hotReloadManager.reload(tempDir.toString());

        assertThat(result.isAllSuccess()).isTrue();
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getDetails().getFirst().getStatus()).isEqualTo(ReloadStatus.SUCCESS_REDEFINED);
        assertThat(HotReloadTestTarget.getValue()).isEqualTo("hot-reloaded");
    }

    @Test
    @DisplayName("加载全新类：defineClass 成功后可反射调用")
    @org.junit.jupiter.api.Order(3)
    void loadNewClass(@TempDir Path tempDir) throws Exception {
        String newClassSource = """
                package com.slg.frameworktest.hotreload.generated;
                public class BrandNewClass {
                    public static String hello() {
                        return "I am brand new!";
                    }
                }
                """;

        compileToDir(tempDir, "com.slg.frameworktest.hotreload.generated.BrandNewClass", newClassSource);

        HotReloadResult result = hotReloadManager.reload(tempDir.toString());

        assertThat(result.isAllSuccess()).isTrue();
        assertThat(result.getDetails().getFirst().getStatus()).isEqualTo(ReloadStatus.SUCCESS_NEW_LOADED);

        Class<?> newClass = Class.forName("com.slg.frameworktest.hotreload.generated.BrandNewClass");
        Object value = newClass.getMethod("hello").invoke(null);
        assertThat(value).isEqualTo("I am brand new!");
    }

    @Test
    @DisplayName("混合热更：新类 + 已有类同时热更")
    @org.junit.jupiter.api.Order(4)
    void mixedReload(@TempDir Path tempDir) throws Exception {
        String helperSource = """
                package com.slg.frameworktest.hotreload.generated;
                public class MixedHelper {
                    public static String tag() { return "mixed"; }
                }
                """;

        String modifiedTarget = """
                package com.slg.frameworktest.hotreload;
                public class HotReloadTestTarget {
                    public static String getValue() {
                        return "mixed-reload";
                    }
                }
                """;

        compileToDir(tempDir, "com.slg.frameworktest.hotreload.generated.MixedHelper", helperSource);
        compileToDir(tempDir, "com.slg.frameworktest.hotreload.HotReloadTestTarget", modifiedTarget);

        HotReloadResult result = hotReloadManager.reload(tempDir.toString());

        assertThat(result.getSuccessCount()).isEqualTo(2);
        assertThat(result.getFailCount()).isZero();
    }

    @Test
    @DisplayName("空目录返回错误信息")
    @org.junit.jupiter.api.Order(10)
    void emptyDirectory(@TempDir Path tempDir) {
        HotReloadResult result = hotReloadManager.reload(tempDir.toString());

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorMessage()).contains("未找到 .class 文件");
    }

    @Test
    @DisplayName("不存在的目录返回错误信息")
    @org.junit.jupiter.api.Order(11)
    void nonExistentDirectory() {
        HotReloadResult result = hotReloadManager.reload("/non/existent/path");

        assertThat(result.hasError()).isTrue();
        assertThat(result.getErrorMessage()).contains("目录不存在");
    }

    @Test
    @DisplayName("重复热更同一目录具有幂等性")
    @org.junit.jupiter.api.Order(5)
    void idempotentReload(@TempDir Path tempDir) throws Exception {
        String source = """
                package com.slg.frameworktest.hotreload;
                public class HotReloadTestTarget {
                    public static String getValue() {
                        return "idempotent";
                    }
                }
                """;

        compileToDir(tempDir, "com.slg.frameworktest.hotreload.HotReloadTestTarget", source);

        HotReloadResult result1 = hotReloadManager.reload(tempDir.toString());
        HotReloadResult result2 = hotReloadManager.reload(tempDir.toString());

        assertThat(result1.isAllSuccess()).isTrue();
        assertThat(result2.isAllSuccess()).isTrue();
        assertThat(HotReloadTestTarget.getValue()).isEqualTo("idempotent");
    }

    /**
     * 使用 javax.tools.JavaCompiler 将源码编译到指定目录（按包结构生成 .class 文件）
     */
    private void compileToDir(Path outputDir, String className, String source) throws IOException {
        String relativePath = className.replace('.', '/') + ".java";
        Path sourceFile = outputDir.resolve("src").resolve(relativePath);
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, source);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).as("需要 JDK（非 JRE）才能运行动态编译测试").isNotNull();

        int exitCode = compiler.run(null, null, null,
                "-d", outputDir.toString(),
                sourceFile.toString());
        assertThat(exitCode).as("编译 %s 应成功", className).isZero();
    }
}
