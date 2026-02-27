package com.slg.scene.scene.node.component.impl.report.handler;

/**
 * 战报模块类型枚举。
 * 与协议中定义的各战报模块 VO 一一对应，用于组装战报时按需构建与获取模块。
 * {@link #requireBoth} 声明构建必要性：true = 双方都需要才需要构建，false = 只需单方需要即需构建。
 *
 * @author yangxunan
 * @date 2026/2/6
 */
public enum ReportModuleTypeEnum {

    /** 战报基础模块：进攻/防守方、胜负、战斗地点等 */
    BASE(1, false),

    /** 战报英雄模块：双方参战英雄简要信息 */
    HERO(2, false),

    /** 战报兵种模块：双方兵种数量与伤亡 */
    TROOP(3, false),

    /** 战报属性模块：双方战斗属性 */
    ATTRIBUTE(4, false),

    /** 战报成员模块：集结战等场景下双方成员与兵种 */
    MEMBER(5, false),

    /** 战报录像模块：关联录像 id */
    VIDEO(6, false),

    /** 战报科技模块 */
    TECHNOLOGY(7, true),

    ;

    private final int typeId;

    /** true = 双方都需要才需要构建，false = 只需单方需要即需构建 */
    private final boolean requireBoth;

    ReportModuleTypeEnum(int typeId, boolean requireBoth) {
        this.typeId = typeId;
        this.requireBoth = requireBoth;
    }

    public int getTypeId() {
        return typeId;
    }

    /**
     * 是否要求双方都需要才构建本模块。
     *
     * @return true 表示双方都需要才构建，false 表示单方需要即需构建
     */
    public boolean isRequireBoth() {
        return requireBoth;
    }

    /**
     * 根据类型 id 获取枚举
     *
     * @param typeId 模块类型 id
     * @return 对应枚举，未找到返回 null
     */
    public static ReportModuleTypeEnum getByTypeId(int typeId) {
        for (ReportModuleTypeEnum e : values()) {
            if (e.typeId == typeId) {
                return e;
            }
        }
        return null;
    }
}
