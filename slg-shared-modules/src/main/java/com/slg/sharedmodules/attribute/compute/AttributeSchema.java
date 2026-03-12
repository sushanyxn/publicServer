package com.slg.sharedmodules.attribute.compute;

import com.slg.sharedmodules.attribute.type.AttributeType;

import java.util.*;

/**
 * 属性计算图谱。
 * <p>
 * 管理所有属性公式，构建反向依赖图并进行拓扑排序。
 * 当某些属性值变化时，能够高效查询出需要重算的公式列表（按拓扑序排列）。
 * 调用 {@link #build()} 后不可再注册新公式。
 * </p>
 *
 * @author yangxunan
 * @date 2026-03-12
 */
public class AttributeSchema {

    /** 目标属性 -> 公式（EnumMap 提升按属性类型查找性能） */
    private final EnumMap<AttributeType, AttributeFormula> formulaMap = new EnumMap<>(AttributeType.class);

    /** 反向依赖：依赖属性 -> 依赖它的公式目标属性集合 */
    private EnumMap<AttributeType, Set<AttributeType>> reverseDependencies;

    /** 拓扑排序后的公式执行顺序 */
    private List<AttributeFormula> topologicalOrder;

    /** 是否已构建 */
    private boolean built;

    /**
     * 注册属性公式
     *
     * @param formula 属性公式
     * @throws IllegalStateException    图谱已构建后不可注册
     * @throws IllegalArgumentException 目标属性重复注册
     */
    public void register(AttributeFormula formula) {
        if (built) {
            throw new IllegalStateException("AttributeSchema 已构建，不可再注册公式");
        }
        AttributeType target = formula.getTarget();
        if (formulaMap.containsKey(target)) {
            throw new IllegalArgumentException("属性 " + target + " 的公式已存在，不可重复注册");
        }
        formulaMap.put(target, formula);
    }

    /**
     * 构建依赖图与拓扑排序。构建后不可再注册新公式。
     *
     * @throws IllegalStateException 检测到循环依赖
     */
    public void build() {
        if (built) {
            return;
        }
        this.reverseDependencies = buildReverseDependencies();
        this.topologicalOrder = buildTopologicalOrder();
        this.built = true;
    }

    /**
     * 判断指定属性是否为计算属性（即存在对应公式）
     *
     * @param type 属性类型
     * @return 是计算属性返回 true
     */
    public boolean isComputedAttribute(AttributeType type) {
        return formulaMap.containsKey(type);
    }

    /**
     * 获取受影响的公式列表（按拓扑序排列）。
     * <p>
     * 给定一组值发生变化的属性，通过反向依赖图进行传递查找，
     * 返回所有直接或间接受影响的公式，按拓扑序排列以保证计算顺序正确。
     * </p>
     *
     * @param changedTypes 值发生变化的属性类型集合
     * @return 需要重算的公式列表（按拓扑序）
     */
    public List<AttributeFormula> getAffectedFormulas(Set<AttributeType> changedTypes) {
        ensureBuilt();
        if (changedTypes.isEmpty()) {
            return Collections.emptyList();
        }

        Set<AttributeType> affected = new HashSet<>();
        Deque<AttributeType> queue = new ArrayDeque<>(changedTypes);
        while (!queue.isEmpty()) {
            AttributeType current = queue.poll();
            Set<AttributeType> dependents = reverseDependencies.getOrDefault(current, Collections.emptySet());
            for (AttributeType dependent : dependents) {
                if (affected.add(dependent)) {
                    queue.add(dependent);
                }
            }
        }

        if (affected.isEmpty()) {
            return Collections.emptyList();
        }

        List<AttributeFormula> result = new ArrayList<>(affected.size());
        for (AttributeFormula formula : topologicalOrder) {
            if (affected.contains(formula.getTarget())) {
                result.add(formula);
            }
        }
        return result;
    }

    /**
     * 获取所有公式（按拓扑序），用于全量计算
     *
     * @return 拓扑序排列的公式列表
     */
    public List<AttributeFormula> getAllFormulas() {
        ensureBuilt();
        return topologicalOrder;
    }

    private EnumMap<AttributeType, Set<AttributeType>> buildReverseDependencies() {
        EnumMap<AttributeType, Set<AttributeType>> reverse = new EnumMap<>(AttributeType.class);
        for (AttributeFormula formula : formulaMap.values()) {
            AttributeType target = formula.getTarget();
            for (AttributeType dep : formula.getDependencies()) {
                reverse.computeIfAbsent(dep, k -> new HashSet<>()).add(target);
            }
        }
        return reverse;
    }

    /**
     * Kahn 算法拓扑排序，检测循环依赖
     */
    private List<AttributeFormula> buildTopologicalOrder() {
        EnumMap<AttributeType, Integer> inDegree = new EnumMap<>(AttributeType.class);
        EnumMap<AttributeType, Set<AttributeType>> forwardDeps = new EnumMap<>(AttributeType.class);

        for (AttributeFormula formula : formulaMap.values()) {
            AttributeType target = formula.getTarget();
            inDegree.putIfAbsent(target, 0);
            for (AttributeType dep : formula.getDependencies()) {
                if (formulaMap.containsKey(dep)) {
                    forwardDeps.computeIfAbsent(dep, k -> new HashSet<>()).add(target);
                    inDegree.merge(target, 1, Integer::sum);
                }
            }
        }

        Deque<AttributeType> queue = new ArrayDeque<>();
        for (var entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<AttributeFormula> sorted = new ArrayList<>(formulaMap.size());
        while (!queue.isEmpty()) {
            AttributeType current = queue.poll();
            sorted.add(formulaMap.get(current));
            Set<AttributeType> successors = forwardDeps.getOrDefault(current, Collections.emptySet());
            for (AttributeType successor : successors) {
                int newDegree = inDegree.merge(successor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(successor);
                }
            }
        }

        if (sorted.size() != formulaMap.size()) {
            throw new IllegalStateException("属性公式存在循环依赖，无法完成拓扑排序");
        }
        return Collections.unmodifiableList(sorted);
    }

    private void ensureBuilt() {
        if (!built) {
            throw new IllegalStateException("AttributeSchema 尚未构建，请先调用 build()");
        }
    }
}
