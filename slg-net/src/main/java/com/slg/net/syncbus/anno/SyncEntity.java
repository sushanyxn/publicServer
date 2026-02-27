package com.slg.net.syncbus.anno;

import com.slg.net.syncbus.SyncModule;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 类级别注解，声明该类所属的 SyncModule
 * 标注在实现了 ISyncHolder 或 ISyncCache 的实体类上
 * 扫描时直接读取注解值，无需实例化
 *
 * @author yangxunan
 * @date 2026/02/12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SyncEntity {

    /**
     * 所属的同步模块
     *
     * @return SyncModule 枚举值
     */
    SyncModule value();
}
