package com.slg.table.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.table.anno.TableBean;
import com.slg.table.config.TableProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * TableBean 管理器
 * 在 Spring 启动时扫描所有被 @TableBean 标注的类并进行管理
 * 
 * <p>扫描包路径优先级：
 * <ol>
 *   <li>配置文件中的 table.scan-packages</li>
 *   <li>Spring Boot 主类的 @SpringBootApplication 扫描路径</li>
 *   <li>默认扫描 com.slg 包</li>
 * </ol>
 * 
 * <p>提供单例访问方式，方便非 Spring 管理的类使用
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
@Component
public class TableBeanManager {

    /**
     * 单例实例
     */
    private static TableBeanManager instance;

    @Autowired
    private TableProperties tableProperties;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 存储 TableBean 类：简单类名 -> Class 对象
     */
    private final Map<String, Class<?>> tableBeans = new HashMap<>();

    /**
     * 存储 TableBean 类：完整类名 -> Class 对象
     */
    private final Map<String, Class<?>> tableBeansByFullName = new HashMap<>();

    /**
     * Spring 容器初始化后自动扫描
     */
    @PostConstruct
    public void init() {
        // 设置单例实例
        instance = this;
        
        String[] basePackages = getBasePackages();
        LoggerUtil.debug("开始扫描 @TableBean 注解的类，扫描包路径: {}", Arrays.toString(basePackages));
        
        scanTableBeans();
        
        LoggerUtil.debug("@TableBean 扫描完成，共找到 {} 个类", tableBeans.size());
    }

    /**
     * 获取单例实例
     * 用于在非 Spring 管理的类中访问 TableBeanManager
     * 
     * @return TableBeanManager 实例，如果未初始化返回 null
     */
    public static TableBeanManager getInstance() {
        return instance;
    }

    /**
     * 扫描所有被 @TableBean 标注的类
     */
    private void scanTableBeans() {
        // 创建类路径扫描器
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(false);
        
        // 添加 @TableBean 注解过滤器
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableBean.class));
        
        // 扫描的基础包路径
        String[] basePackages = getBasePackages();
        
        for (String basePackage : basePackages) {
            try {
                Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
                
                for (BeanDefinition beanDefinition : candidateComponents) {
                    String className = beanDefinition.getBeanClassName();
                    if (className != null) {
                        try {
                            Class<?> clazz = Class.forName(className);
                            registerTableBean(clazz);
                        } catch (ClassNotFoundException e) {
                            LoggerUtil.error("无法加载类: {}", className, e);
                        }
                    }
                }
            } catch (Exception e) {
                LoggerUtil.error("扫描包 {} 时发生错误", basePackage, e);
            }
        }
    }

    /**
     * 注册 TableBean 类
     * 
     * @param clazz TableBean 类
     */
    private void registerTableBean(Class<?> clazz) {
        String simpleName = clazz.getSimpleName();
        String fullName = clazz.getName();
        
        // 检查简单类名是否重复
        if (tableBeans.containsKey(simpleName)) {
            LoggerUtil.warn("发现重复的 TableBean 简单类名: {}, 新类: {}, 已存在: {}", 
                simpleName, fullName, tableBeans.get(simpleName).getName());
        }
        
        tableBeans.put(simpleName, clazz);
        tableBeansByFullName.put(fullName, clazz);
    }

    /**
     * 智能获取需要扫描的基础包路径
     * 
     * <p>优先级：
     * <ol>
     *   <li>配置文件中的 table.scan-packages（如果已配置）</li>
     *   <li>Spring Boot 主类的 @SpringBootApplication 或 @ComponentScan 注解（自动检测）</li>
     *   <li>默认扫描 "com.slg" 包（保底方案）</li>
     * </ol>
     * 
     * @return 包路径数组
     */
    private String[] getBasePackages() {
        Set<String> packages = new LinkedHashSet<>();
        
        // 1. 优先使用配置文件中的设置
        List<String> configuredPackages = tableProperties.getScanPackages();
        if (configuredPackages != null && !configuredPackages.isEmpty()) {
            packages.addAll(configuredPackages);
            return packages.toArray(new String[0]);
        }
        
        // 2. 尝试从 Spring Boot 主类获取扫描包路径
        String[] autoDetectedPackages = detectSpringBootBasePackages();
        if (autoDetectedPackages.length > 0) {
            packages.addAll(Arrays.asList(autoDetectedPackages));
            return packages.toArray(new String[0]);
        }
        
        // 3. 默认扫描 com.slg 包（涵盖所有 slg-* 模块）
        packages.add("com.slg");

        return packages.toArray(new String[0]);
    }

    /**
     * 从 Spring Boot 主类自动检测扫描包路径
     * 
     * @return 检测到的包路径数组
     */
    private String[] detectSpringBootBasePackages() {
        try {
            // 获取所有带 @SpringBootApplication 注解的 Bean
            Map<String, Object> beansWithAnnotation = 
                applicationContext.getBeansWithAnnotation(SpringBootApplication.class);
            
            if (!beansWithAnnotation.isEmpty()) {
                // 获取第一个主类
                Object mainBean = beansWithAnnotation.values().iterator().next();
                Class<?> mainClass = mainBean.getClass();
                
                // 获取主类所在的包
                String mainPackage = mainClass.getPackage().getName();

                return new String[]{mainPackage};
            }
        } catch (Exception e) {
            LoggerUtil.warn("自动检测 Spring Boot 主类扫描路径失败", e);
        }
        
        return new String[0];
    }

    /**
     * 根据简单类名获取 TableBean 类
     * 
     * @param simpleName 简单类名
     * @return TableBean 类，不存在返回 null
     */
    public Class<?> getTableBean(String simpleName) {
        return tableBeans.get(simpleName);
    }

    /**
     * 根据完整类名获取 TableBean 类
     * 
     * @param fullName 完整类名
     * @return TableBean 类，不存在返回 null
     */
    public Class<?> getTableBeanByFullName(String fullName) {
        return tableBeansByFullName.get(fullName);
    }

    /**
     * 获取所有 TableBean 类
     * 
     * @return 所有 TableBean 类的集合
     */
    public Map<String, Class<?>> getAllTableBeans() {
        return new HashMap<>(tableBeans);
    }

    /**
     * 检查类是否是 TableBean
     * 
     * @param clazz 要检查的类
     * @return 如果是 TableBean 返回 true
     */
    public boolean isTableBean(Class<?> clazz) {
        return tableBeansByFullName.containsKey(clazz.getName());
    }

    /**
     * 获取 TableBean 的数量
     * 
     * @return TableBean 数量
     */
    public int getTableBeanCount() {
        return tableBeans.size();
    }
}
