package com.slg.common.hotreload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 热更核心管理器，提供基于文件夹的批量类热更能力。
 * <p>
 * 支持两种场景：
 * <ul>
 *   <li>已有类：通过 {@link Instrumentation#redefineClasses} 替换字节码（仅支持方法体变更）</li>
 *   <li>全新类：通过反射调用 ClassLoader.defineClass 注入到应用 ClassLoader</li>
 * </ul>
 * <p>
 * 传入目录作为 classpath 根目录，例如目录下有 com/slg/game/service/PlayerService.class，
 * 则类名为 com.slg.game.service.PlayerService。
 *
 * @author yangxunan
 * @date 2026/03/10
 */
@Component
public class HotReloadManager {

    private static final Logger log = LoggerFactory.getLogger(HotReloadManager.class);
    private static final String CLASS_SUFFIX = ".class";

    private final ReentrantLock reloadLock = new ReentrantLock();

    /**
     * 热更指定目录下的所有 .class 文件
     *
     * @param dirPath 目录路径，作为 classpath 根目录
     * @return 热更结果
     */
    public HotReloadResult reload(String dirPath) {
        if (!InstrumentationHolder.isAvailable()) {
            return HotReloadResult.error(dirPath, "Instrumentation 不可用，请确认启动时添加了 -javaagent:lib/slg-common-*.jar 参数");
        }

        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return HotReloadResult.error(dirPath, "目录不存在或不是有效目录: " + dirPath);
        }

        if (!reloadLock.tryLock()) {
            return HotReloadResult.error(dirPath, "有其他热更正在执行，请稍后重试");
        }

        long startTime = System.currentTimeMillis();
        try {
            List<Path> classFiles = scanClassFiles(dir);
            if (classFiles.isEmpty()) {
                return HotReloadResult.error(dirPath, "目录下未找到 .class 文件");
            }

            log.info("热更开始 [{}]: 扫描到 {} 个 .class 文件", dirPath, classFiles.size());

            List<ClassCandidate> newClasses = new ArrayList<>();
            List<ClassCandidate> existingClasses = new ArrayList<>();
            List<ClassReloadDetail> details = new ArrayList<>();

            classifyClasses(dir, classFiles, newClasses, existingClasses, details);

            if (!newClasses.isEmpty()) {
                defineNewClasses(newClasses, details);
            }

            if (!existingClasses.isEmpty()) {
                redefineExistingClasses(existingClasses, details);
            }

            long costMs = System.currentTimeMillis() - startTime;
            HotReloadResult result = HotReloadResult.valueOf(dirPath, details, costMs);
            log.info(result.getSummary());
            return result;

        } catch (Exception e) {
            log.error("热更过程发生未预期异常 [{}]", dirPath, e);
            return HotReloadResult.error(dirPath, "热更过程异常: " + e.getMessage());
        } finally {
            reloadLock.unlock();
        }
    }

    private List<Path> scanClassFiles(Path dir) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(CLASS_SUFFIX)) {
                    result.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return result;
    }

    private String resolveClassName(Path classFile, Path baseDir) {
        Path relative = baseDir.relativize(classFile);
        String path = relative.toString();
        return path.substring(0, path.length() - CLASS_SUFFIX.length())
                .replace('/', '.')
                .replace('\\', '.');
    }

    /**
     * 将扫描到的 class 文件分为"已加载"和"未加载"两组
     */
    private void classifyClasses(Path baseDir, List<Path> classFiles,
                                 List<ClassCandidate> newClasses,
                                 List<ClassCandidate> existingClasses,
                                 List<ClassReloadDetail> details) {
        for (Path classFile : classFiles) {
            String className = resolveClassName(classFile, baseDir);
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(classFile);
            } catch (IOException e) {
                details.add(ClassReloadDetail.valueOf(className, ReloadStatus.FAILED_IO,
                        "读取文件失败: " + e.getMessage()));
                continue;
            }

            Class<?> loadedClass = findLoadedClass(className);
            ClassCandidate candidate = new ClassCandidate(className, bytes, loadedClass);
            if (loadedClass != null) {
                existingClasses.add(candidate);
            } else {
                newClasses.add(candidate);
            }
        }
    }

    private Class<?> findLoadedClass(String className) {
        try {
            return Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            return findFromInstrumentation(className);
        }
    }

    private Class<?> findFromInstrumentation(String className) {
        Instrumentation inst = InstrumentationHolder.get();
        for (Class<?> clazz : inst.getAllLoadedClasses()) {
            if (clazz.getName().equals(className)) {
                return clazz;
            }
        }
        return null;
    }

    /**
     * 通过反射 ClassLoader.defineClass 加载全新类
     */
    private void defineNewClasses(List<ClassCandidate> newClasses, List<ClassReloadDetail> details) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        for (ClassCandidate candidate : newClasses) {
            try {
                Method defineClass = ClassLoader.class.getDeclaredMethod(
                        "defineClass", String.class, byte[].class, int.class, int.class);
                defineClass.setAccessible(true);
                defineClass.invoke(classLoader, candidate.className, candidate.bytes, 0, candidate.bytes.length);

                details.add(ClassReloadDetail.success(candidate.className, ReloadStatus.SUCCESS_NEW_LOADED));
                log.info("热更-新类加载成功: {}", candidate.className);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                details.add(ClassReloadDetail.valueOf(candidate.className, ReloadStatus.FAILED_DEFINE_ERROR,
                        cause.getClass().getSimpleName() + ": " + cause.getMessage()));
                log.warn("热更-新类加载失败: {} - {}", candidate.className, cause.getMessage());
            }
        }
    }

    /**
     * 通过 Instrumentation.redefineClasses 重定义已有类。
     * 先尝试批量，失败则降级为逐个重定义。
     */
    private void redefineExistingClasses(List<ClassCandidate> existingClasses, List<ClassReloadDetail> details) {
        Instrumentation inst = InstrumentationHolder.get();

        List<ClassDefinition> definitions = existingClasses.stream()
                .map(c -> new ClassDefinition(c.loadedClass, c.bytes))
                .collect(Collectors.toList());

        try {
            inst.redefineClasses(definitions.toArray(new ClassDefinition[0]));
            for (ClassCandidate candidate : existingClasses) {
                details.add(ClassReloadDetail.success(candidate.className, ReloadStatus.SUCCESS_REDEFINED));
                log.info("热更-类重定义成功: {}", candidate.className);
            }
        } catch (Exception e) {
            log.warn("批量热更失败，降级为逐个热更: {}", e.getMessage());
            redefineOneByOne(inst, existingClasses, details);
        }
    }

    private void redefineOneByOne(Instrumentation inst, List<ClassCandidate> classes, List<ClassReloadDetail> details) {
        for (ClassCandidate candidate : classes) {
            try {
                inst.redefineClasses(new ClassDefinition(candidate.loadedClass, candidate.bytes));
                details.add(ClassReloadDetail.success(candidate.className, ReloadStatus.SUCCESS_REDEFINED));
                log.info("热更-类重定义成功: {}", candidate.className);
            } catch (UnsupportedOperationException e) {
                details.add(ClassReloadDetail.valueOf(candidate.className, ReloadStatus.FAILED_STRUCTURAL_CHANGE,
                        "不支持的结构性变更(新增/删除方法或字段): " + e.getMessage()));
                log.warn("热更-结构性变更失败: {} - {}", candidate.className, e.getMessage());
            } catch (ClassFormatError e) {
                details.add(ClassReloadDetail.valueOf(candidate.className, ReloadStatus.FAILED_CLASS_FORMAT,
                        "类文件格式错误: " + e.getMessage()));
                log.warn("热更-类格式错误: {} - {}", candidate.className, e.getMessage());
            } catch (Exception e) {
                details.add(ClassReloadDetail.valueOf(candidate.className, ReloadStatus.FAILED_OTHER,
                        e.getClass().getSimpleName() + ": " + e.getMessage()));
                log.warn("热更-未知错误: {} - {}", candidate.className, e.getMessage());
            }
        }
    }

    /**
     * 热更候选类，封装类名、字节码和已加载的 Class 对象（新类为 null）
     */
    private record ClassCandidate(String className, byte[] bytes, Class<?> loadedClass) {
    }
}
