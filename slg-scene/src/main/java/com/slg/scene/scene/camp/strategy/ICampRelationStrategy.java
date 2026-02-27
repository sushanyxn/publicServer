package com.slg.scene.scene.camp.strategy;

import com.slg.scene.scene.camp.CampType;
import com.slg.scene.scene.node.owner.NodeOwner;

/**
 * 阵营关系判断策略接口
 * <p>不同类型的 NodeOwner 组合使用不同的策略来判断阵营关系</p>
 * 
 * <p><b>策略模式优势：</b></p>
 * <ul>
 *   <li>每种组合的判断逻辑独立，便于维护和测试</li>
 *   <li>新增类型组合只需添加新策略，无需修改现有代码</li>
 *   <li>支持复杂的判断逻辑（外交关系、仇恨值等）</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/2/4
 */
public interface ICampRelationStrategy {

    /**
     * 判断两个 Owner 的阵营关系
     * <p><b>注意：</b>关系可能不对称，例如：</p>
     * <ul>
     *   <li>玩家可以攻击中立NPC，但中立NPC不会攻击玩家</li>
     *   <li>需要根据 owner（主体）的视角来判断</li>
     * </ul>
     *
     * @param owner 主体（发起方）
     * @param other 目标（接收方）
     * @return 阵营关系
     */
    CampType judgeRelation(NodeOwner owner, NodeOwner other);

    /**
     * 是否支持此类型组合
     * <p>用于策略匹配，判断该策略是否能处理给定的类型组合</p>
     *
     * @param ownerClass 主体类型
     * @param otherClass 目标类型
     * @return true=支持，false=不支持
     */
    boolean support(Class<? extends NodeOwner> ownerClass, Class<? extends NodeOwner> otherClass);

}
