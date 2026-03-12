package com.slg.sharedmodules.attribute.compute;

import com.slg.sharedmodules.attribute.type.AttributeType;

import java.util.List;
import java.util.function.ToLongFunction;

/**
 * 属性计算公式。
 * <p>
 * 描述一个计算属性如何由其依赖属性推导而来。
 * 依赖可以是基础属性，也可以是其他计算属性（支持多层级依赖）。
 * </p>
 *
 * @author slgserver
 * @date 2026-03-12
 */
public class AttributeFormula {

    /** 计算产出的目标属性 */
    private final AttributeType target;

    /** 本公式依赖的属性列表 */
    private final List<AttributeType> dependencies;

    /** 计算函数，入参为属性值查询器，返回计算结果 */
    private final Calculator calculator;

    private AttributeFormula(AttributeType target, List<AttributeType> dependencies, Calculator calculator) {
        this.target = target;
        this.dependencies = List.copyOf(dependencies);
        this.calculator = calculator;
    }

    /**
     * 创建属性公式
     *
     * @param target       目标计算属性
     * @param dependencies 依赖的属性列表
     * @param calculator   计算函数
     * @return 公式实例
     */
    public static AttributeFormula of(AttributeType target, List<AttributeType> dependencies, Calculator calculator) {
        return new AttributeFormula(target, dependencies, calculator);
    }

    /**
     * 执行计算
     *
     * @param valueGetter 属性值查询器，根据属性类型返回其当前最终值
     * @return 计算结果
     */
    public long compute(ToLongFunction<AttributeType> valueGetter) {
        return calculator.calculate(valueGetter);
    }

    public AttributeType getTarget() {
        return target;
    }

    public List<AttributeType> getDependencies() {
        return dependencies;
    }

    /**
     * 属性计算函数接口
     */
    @FunctionalInterface
    public interface Calculator {

        /**
         * 根据属性值查询器计算目标属性值
         *
         * @param values 属性值查询器，传入属性类型返回其当前值
         * @return 计算结果
         */
        long calculate(ToLongFunction<AttributeType> values);
    }
}
