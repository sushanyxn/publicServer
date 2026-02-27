package com.slg.scene.scene.node.component.impl.report.handler;

import com.slg.fight.wos.model.FightContext;
import com.slg.net.message.clientmessage.report.packet.ReportModuleVO;
import com.slg.scene.scene.node.component.impl.army.ArmyDetailComponent;
import com.slg.scene.scene.node.node.model.SceneNode;
import jakarta.annotation.PostConstruct;

import java.util.EnumMap;
import java.util.Map;

/**
 * 战报模块构造 Handler 基类。
 * 子类在 Spring 初始化时通过 {@link #init()} 注册到静态 EnumMap 中，由父类按 {@link ReportModuleTypeEnum} 统一管理。
 * 仅保留构建模块方法：根据战斗结束的 FightContext 及双方节点与军队详情生成模块 VO。
 * 是否构建某模块由枚举 {@link ReportModuleTypeEnum#isRequireBoth()} 与调用方根据双方需要共同决定。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public abstract class AbstractReportModuleHandler {

    private static final Map<ReportModuleTypeEnum, AbstractReportModuleHandler> HANDLERS = new EnumMap<>(ReportModuleTypeEnum.class);

    @PostConstruct
    public void init() {
        HANDLERS.put(getModuleType(), this);
    }

    /**
     * 根据模块类型获取对应 Handler
     *
     * @param moduleType 战报模块类型
     * @return 对应 Handler，未注册则返回 null
     */
    public static AbstractReportModuleHandler getHandler(ReportModuleTypeEnum moduleType) {
        return HANDLERS.get(moduleType);
    }

    /**
     * 本 Handler 负责的战报模块类型
     *
     * @return 模块类型枚举
     */
    public abstract ReportModuleTypeEnum getModuleType();

    /**
     * 根据战斗结束生成的战斗上下文及双方节点与军队详情，构建本模块的模块 VO。
     *
     * @param attackerNode       进攻方场景节点
     * @param defenderNode       防守方场景节点
     * @param attackerArmyDetail 进攻方军队详情组件
     * @param defenderArmyDetail 防守方军队详情组件
     * @param fightContext       战斗结束后的上下文（含攻守方军队、胜负、战报等）
     * @return 本模块对应的 ReportModuleVO，不应为 null（若无需数据可返回空 VO）
     */
    public abstract ReportModuleVO buildModule(SceneNode<?> attackerNode, SceneNode<?> defenderNode,
                                              ArmyDetailComponent<?> attackerArmyDetail, ArmyDetailComponent<?> defenderArmyDetail,
                                              FightContext fightContext);
}
