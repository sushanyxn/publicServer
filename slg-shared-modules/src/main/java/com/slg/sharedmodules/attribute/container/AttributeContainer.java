package com.slg.sharedmodules.attribute.container;

import com.slg.common.log.LoggerUtil;
import com.slg.sharedmodules.attribute.compute.AttributeFormula;
import com.slg.sharedmodules.attribute.compute.AttributeSchema;
import com.slg.sharedmodules.attribute.listener.AttributeChangeListener;
import com.slg.sharedmodules.attribute.manager.AttributeManager;
import com.slg.sharedmodules.attribute.model.AttributeChange;
import com.slg.sharedmodules.attribute.type.AttributeModule;
import com.slg.sharedmodules.attribute.type.AttributeModuleKey;
import com.slg.sharedmodules.attribute.type.AttributeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 属性容器。
 * <p>
 * 持有多个模块投放的属性，自动聚合并通过公式计算出最终属性值。
 * 支持增量重算：仅在变化属性的关联公式上进行重新计算，而非全量重算。
 * 可由玩家或战斗单位持有。
 * </p>
 * <p>
 * 使用 {@link AttributeManager} 提供的全局 {@link AttributeSchema}，
 * 无需为每个实例单独传入。
 * </p>
 * <p>
 * 每个模块通过 {@link AttributeModuleKey}（模块 + 子级 id）标识，
 * 允许同一模块下有多个属性来源（如装备模块的不同部位）。
 * </p>
 * <p>
 * 不保证线程安全，依赖项目执行器模型保证单实体串行访问。
 * </p>
 *
 * @author slgserver
 * @date 2026-03-12
 */
public class AttributeContainer {

    /** 各模块键投放的原始属性：moduleKey -> (属性类型 -> value)，内层 EnumMap 提升按类型查找性能 */
    private final Map<AttributeModuleKey, EnumMap<AttributeType, Long>> moduleAttributes = new HashMap<>();

    /** 所有模块汇总值（非公式属性的累加） */
    private final EnumMap<AttributeType, Long> aggregated = new EnumMap<>(AttributeType.class);

    /** 所有属性最终值（聚合 + 计算结果） */
    private final EnumMap<AttributeType, Long> finalValues = new EnumMap<>(AttributeType.class);

    private AttributeChangeListener listener;

    private AttributeSchema schema() {
        return AttributeManager.getSchema();
    }

    /**
     * 投放或替换指定模块键的属性，触发增量重算和变化通知。
     * <p>
     * 若该键之前已有属性，将先移除旧值再设置新值，最后增量重算受影响的公式。
     * 仅非公式属性可被模块投放，公式属性会被忽略并记录警告。
     * </p>
     *
     * @param moduleKey  属性来源模块键
     * @param attributes 该模块键投放的属性集合
     */
    public void putModule(AttributeModuleKey moduleKey, Map<AttributeType, Long> attributes) {
        EnumMap<AttributeType, Long> filtered = filterNonComputed(attributes);
        EnumMap<AttributeType, Long> oldModuleAttrs = moduleAttributes.put(moduleKey, filtered);

        Set<AttributeType> changedTypes = applyModuleDelta(oldModuleAttrs, filtered);
        recalculateAndNotify(changedTypes);
    }

    /**
     * 移除指定模块键的全部属性，触发增量重算和变化通知
     *
     * @param moduleKey 要移除的模块键
     */
    public void removeModule(AttributeModuleKey moduleKey) {
        EnumMap<AttributeType, Long> removed = moduleAttributes.remove(moduleKey);
        if (removed == null || removed.isEmpty()) {
            return;
        }

        Set<AttributeType> changedTypes = applyModuleDelta(removed, new EnumMap<>(AttributeType.class));
        recalculateAndNotify(changedTypes);
    }

    /**
     * 移除指定模块下所有子级的属性，触发增量重算和变化通知
     *
     * @param module 要移除的模块
     */
    public void removeModule(AttributeModule module) {
        Set<AttributeType> allChangedTypes = new HashSet<>();

        Iterator<Map.Entry<AttributeModuleKey, EnumMap<AttributeType, Long>>> it = moduleAttributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<AttributeModuleKey, EnumMap<AttributeType, Long>> entry = it.next();
            if (entry.getKey().module() == module) {
                allChangedTypes.addAll(applyModuleDelta(entry.getValue(), new EnumMap<>(AttributeType.class)));
                it.remove();
            }
        }

        if (!allChangedTypes.isEmpty()) {
            recalculateAndNotify(allChangedTypes);
        }
    }

    /**
     * 获取指定属性的最终值
     *
     * @param type 属性类型
     * @return 最终值，未设置时返回 0
     */
    public long get(AttributeType type) {
        return finalValues.getOrDefault(type, 0L);
    }

    /**
     * 获取指定模块键投放的原始属性（不可变视图）
     *
     * @param moduleKey 属性来源模块键
     * @return 该模块键的属性映射（属性类型 -> value），无投放时返回空 Map
     */
    public Map<AttributeType, Long> getModuleAttributes(AttributeModuleKey moduleKey) {
        Map<AttributeType, Long> attrs = moduleAttributes.get(moduleKey);
        return attrs != null ? Collections.unmodifiableMap(attrs) : Map.of();
    }

    /**
     * 获取指定模块下所有子级投放的属性汇总（不可变视图）。
     * <p>
     * 将该模块下所有 {@link AttributeModuleKey} 的属性值累加后返回。
     * </p>
     *
     * @param module 属性来源模块
     * @return 汇总后的属性映射（属性类型 -> value），无投放时返回空 Map
     */
    public Map<AttributeType, Long> getModuleAttributes(AttributeModule module) {
        Map<AttributeType, Long> result = new EnumMap<>(AttributeType.class);
        for (var entry : moduleAttributes.entrySet()) {
            if (entry.getKey().module() == module) {
                for (var attr : entry.getValue().entrySet()) {
                    result.merge(attr.getKey(), attr.getValue(), Long::sum);
                }
            }
        }
        return result.isEmpty() ? Map.of() : Collections.unmodifiableMap(result);
    }

    /**
     * 获取所有属性最终值（不可变视图）
     *
     * @return 属性类型 -> finalValue
     */
    public Map<AttributeType, Long> getAll() {
        return Collections.unmodifiableMap(finalValues);
    }

    /**
     * 设置属性变化监听器
     *
     * @param listener 监听器，传 null 可移除
     */
    public void setListener(AttributeChangeListener listener) {
        this.listener = listener;
    }

    /**
     * 基于全部已投放属性进行一次全量计算，用于初始化或数据恢复场景
     */
    public void recalculateAll() {
        aggregated.clear();
        finalValues.clear();

        for (EnumMap<AttributeType, Long> attrs : moduleAttributes.values()) {
            for (var entry : attrs.entrySet()) {
                aggregated.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }

        for (var entry : aggregated.entrySet()) {
            finalValues.put(entry.getKey(), entry.getValue());
        }

        for (AttributeFormula formula : schema().getAllFormulas()) {
            long computed = formula.compute(type -> finalValues.getOrDefault(type, 0L));
            finalValues.put(formula.getTarget(), computed);
        }
    }

    /**
     * 过滤掉公式属性，只保留可被模块投放的基础属性
     */
    private EnumMap<AttributeType, Long> filterNonComputed(Map<AttributeType, Long> attributes) {
        EnumMap<AttributeType, Long> result = new EnumMap<>(AttributeType.class);
        for (var entry : attributes.entrySet()) {
            AttributeType type = entry.getKey();
            if (schema().isComputedAttribute(type)) {
                LoggerUtil.warn("模块不可直接投放计算属性 {}，已忽略", type);
                continue;
            }
            result.put(type, entry.getValue());
        }
        return result;
    }

    /**
     * 将模块属性变更应用到聚合值，返回实际发生变化的属性类型集合
     *
     * @param oldAttrs 旧的模块属性（可能为 null）
     * @param newAttrs 新的模块属性
     * @return 聚合值发生变化的属性类型集合
     */
    private Set<AttributeType> applyModuleDelta(Map<AttributeType, Long> oldAttrs, Map<AttributeType, Long> newAttrs) {
        Set<AttributeType> allKeys = new HashSet<>();
        if (oldAttrs != null) {
            allKeys.addAll(oldAttrs.keySet());
        }
        allKeys.addAll(newAttrs.keySet());

        Set<AttributeType> changedTypes = new HashSet<>();
        for (AttributeType type : allKeys) {
            long oldVal = oldAttrs != null ? oldAttrs.getOrDefault(type, 0L) : 0L;
            long newVal = newAttrs.getOrDefault(type, 0L);
            long delta = newVal - oldVal;
            if (delta != 0) {
                aggregated.merge(type, delta, Long::sum);
                changedTypes.add(type);
            }
        }
        return changedTypes;
    }

    /**
     * 根据变化的属性类型集合，增量重算受影响的公式并触发变化通知
     */
    private void recalculateAndNotify(Set<AttributeType> changedTypes) {
        List<AttributeChange> changes = new ArrayList<>();

        for (AttributeType type : changedTypes) {
            long oldFinal = finalValues.getOrDefault(type, 0L);
            long newFinal = aggregated.getOrDefault(type, 0L);
            if (oldFinal != newFinal) {
                finalValues.put(type, newFinal);
                changes.add(new AttributeChange(type, oldFinal, newFinal));
            }
        }

        List<AttributeFormula> affected = schema().getAffectedFormulas(changedTypes);
        for (AttributeFormula formula : affected) {
            AttributeType target = formula.getTarget();
            long oldFinal = finalValues.getOrDefault(target, 0L);
            long newFinal = formula.compute(t -> finalValues.getOrDefault(t, 0L));
            if (oldFinal != newFinal) {
                finalValues.put(target, newFinal);
                changes.add(new AttributeChange(target, oldFinal, newFinal));
                changedTypes.add(target);
            }
        }

        if (!changes.isEmpty() && listener != null) {
            listener.onAttributesChanged(Collections.unmodifiableList(changes));
        }
    }
}
