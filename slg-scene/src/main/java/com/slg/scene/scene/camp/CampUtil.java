package com.slg.scene.scene.camp;

import com.slg.scene.scene.camp.strategy.*;
import com.slg.scene.scene.node.owner.NodeOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 阵营关系判断工具类
 * <p>基于策略模式实现多态的阵营关系判断</p>
 * 
 * <p><b>核心功能：</b></p>
 * <ul>
 *   <li>根据不同的 NodeOwner 类型组合，使用对应的策略判断阵营关系</li>
 *   <li>支持策略缓存，提升性能</li>
 *   <li>扩展新类型只需添加新策略，无需修改现有代码</li>
 * </ul>
 * 
 * <p><b>使用示例：</b></p>
 * <pre>
 * ScenePlayer player1 = ...;
 * ScenePlayer player2 = ...;
 * CampType relation = CampUtil.getCampRelation(player1, player2);
 * if (relation.canAttack()) {
 *     // 可以攻击
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class CampUtil {

    /** 策略列表（按注册顺序） */
    private static final List<ICampRelationStrategy> STRATEGIES = new ArrayList<>();

    /** 策略缓存（key=类型组合，value=策略） */
    private static final Map<String, ICampRelationStrategy> STRATEGY_CACHE = new ConcurrentHashMap<>();

    static {
        // 注册所有策略（注意顺序：越具体的策略越靠前，默认策略最后）
        registerStrategy(new PlayerVsPlayerStrategy());
        registerStrategy(new PlayerVsAllianceStrategy());
        registerStrategy(new PlayerVsNpcStrategy());
        registerStrategy(new AllianceVsAnyStrategy());
        registerStrategy(new NpcVsAnyStrategy());
        
        // 默认策略（兜底），必须放在最后
        registerStrategy(new DefaultStrategy());
    }

    /**
     * 注册阵营关系判断策略
     * <p>如果需要动态添加策略，可以调用此方法</p>
     *
     * @param strategy 策略实例
     */
    public static void registerStrategy(ICampRelationStrategy strategy) {
        STRATEGIES.add(strategy);
    }

    /**
     * 判断两个 Owner 的阵营关系
     * <p><b>注意：</b>关系判断可能不对称</p>
     * <ul>
     *   <li>getCampRelation(player, npc) 可能返回 NEUTRAL</li>
     *   <li>getCampRelation(npc, player) 也可能返回 NEUTRAL</li>
     *   <li>但含义不同：玩家可以攻击NPC，NPC不会主动攻击玩家</li>
     * </ul>
     * 
     * <p><b>策略匹配：</b></p>
     * <ul>
     *   <li>优先匹配具体的策略（PlayerVsPlayer、PlayerVsNpc等）</li>
     *   <li>如果没有匹配的策略，使用默认策略返回 NEUTRAL</li>
     *   <li>保证永远不会返回 null</li>
     * </ul>
     *
     * @param owner 主体（发起方）
     * @param other 目标（接收方）
     * @return 阵营关系（保证非空）
     */
    public static CampType getCampRelation(NodeOwner owner, NodeOwner other) {
        // 空值检查
        if (owner == null || other == null) {
            return CampType.NEUTRAL;
        }

        // 获取类型
        Class<? extends NodeOwner> ownerClass = owner.getClass();
        Class<? extends NodeOwner> otherClass = other.getClass();

        // 尝试从缓存获取策略，如果没有则查找（包括默认策略）
        String cacheKey = getCacheKey(ownerClass, otherClass);
        ICampRelationStrategy strategy = STRATEGY_CACHE.computeIfAbsent(cacheKey, s -> findStrategy(ownerClass, otherClass));

        // 使用策略判断关系
        // 注：因为有默认策略兜底，strategy 不会为 null
        return strategy.judgeRelation(owner, other);
    }

    /**
     * 查找支持指定类型组合的策略
     * <p>遍历所有已注册的策略，找到第一个匹配的</p>
     * <p><b>注意：</b>因为有默认策略兜底，此方法保证不会返回 null</p>
     *
     * @param ownerClass 主体类型
     * @param otherClass 目标类型
     * @return 匹配的策略（保证非空，至少会返回默认策略）
     */
    private static ICampRelationStrategy findStrategy(
            Class<? extends NodeOwner> ownerClass,
            Class<? extends NodeOwner> otherClass) {

        for (ICampRelationStrategy strategy : STRATEGIES) {
            if (strategy.support(ownerClass, otherClass)) {
                return strategy;
            }
        }
        // 因为有默认策略，理论上不会到这里
        // 但为了代码健壮性，仍然返回 null 让上层处理
        return null;
    }

    /**
     * 生成缓存Key
     * <p>格式：ownerClassName#otherClassName</p>
     *
     * @param ownerClass 主体类型
     * @param otherClass 目标类型
     * @return 缓存Key
     */
    private static String getCacheKey(Class<?> ownerClass, Class<?> otherClass) {
        return ownerClass.getName() + "#" + otherClass.getName();
    }

    /**
     * 清空策略缓存
     * <p>用于测试或运行时动态调整策略后刷新缓存</p>
     */
    public static void clearCache() {
        STRATEGY_CACHE.clear();
    }

}
