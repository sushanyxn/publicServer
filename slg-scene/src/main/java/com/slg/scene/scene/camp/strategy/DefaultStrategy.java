package com.slg.scene.scene.camp.strategy;

import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.NodeOwner;

/**
 * 默认阵营关系判断策略（兜底策略）
 * <p>当没有其他策略匹配时，使用此策略返回默认关系</p>
 * 
 * <p><b>设计说明：</b></p>
 * <ul>
 *   <li>支持所有类型组合（support 总是返回 true）</li>
 *   <li>始终返回 NEUTRAL（中立关系）</li>
 *   <li>必须注册在策略列表的最后，作为兜底</li>
 *   <li>避免因缺少策略而返回 null</li>
 * </ul>
 * 
 * <p><b>使用场景：</b></p>
 * <ul>
 *   <li>新增 NodeOwner 子类但尚未实现具体策略</li>
 *   <li>特殊类型组合未覆盖到的情况</li>
 *   <li>保证系统健壮性，避免空指针</li>
 * </ul>
 * 
 * <p><b>注意：</b></p>
 * <ul>
 *   <li>此策略应该是最后注册的策略</li>
 *   <li>如果此策略被触发，说明可能需要添加更具体的策略</li>
 *   <li>可以添加日志记录，监控是否有未覆盖的类型组合</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public class DefaultStrategy implements ICampRelationStrategy {

    @Override
    public CampType judgeRelation(NodeOwner owner, NodeOwner other) {
        // 默认返回中立关系
        // 可以在这里添加日志，监控是否有未覆盖的类型组合
        // LoggerUtil.debug("使用默认策略判断阵营关系: {} vs {}", 
        //     owner.getClass().getSimpleName(), 
        //     other.getClass().getSimpleName());
        
        return CampType.NEUTRAL;
    }

    @Override
    public boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass) {
        // 支持所有类型组合（兜底策略）
        return true;
    }

}
