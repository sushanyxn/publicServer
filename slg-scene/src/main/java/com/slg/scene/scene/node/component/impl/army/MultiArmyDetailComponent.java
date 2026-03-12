package com.slg.scene.scene.node.component.impl.army;

import com.slg.sharedmodules.fight.wos.model.FightArmy;
import com.slg.sharedmodules.fight.wos.model.MultiFightArmy;
import com.slg.net.message.clientmessage.army.packet.AssembleArmyVO;
import com.slg.net.message.clientmessage.army.packet.ArmyVO;
import com.slg.net.message.clientmessage.army.packet.PlayerArmyVO;
import com.slg.scene.scene.node.component.ComponentEnum;
import com.slg.scene.scene.node.node.model.impl.AssembleArmy;
import com.slg.scene.scene.node.node.model.impl.PlayerArmy;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 多军队详情组件
 * 面向 {@link AssembleArmy}，包含多个 {@link PlayerArmy}（按 playerId 索引），并可生成 {@link AssembleArmyVO}。
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Getter
public class MultiArmyDetailComponent extends ArmyDetailComponent<AssembleArmy> {

    /** 集结成员，key 为 playerId */
    private final Map<Long, PlayerArmy> memberArmies = new HashMap<>();

    /** 队长玩家 id（保存值，不依赖 node 的 owner） */
    private final long leaderId;

    public MultiArmyDetailComponent(AssembleArmy belongNode, long leaderId) {
        super(belongNode);
        this.leaderId = leaderId;
    }

    /**
     * 当前集结包含的玩家军队映射（只读视图，增删请使用 addMember/removeMember）
     *
     * @return playerId -> PlayerArmy，不可修改
     */
    public Map<Long, PlayerArmy> getMemberArmies() {
        return Collections.unmodifiableMap(memberArmies);
    }

    /**
     * 将一名玩家的军队加入集结
     *
     * @param playerArmy 玩家军队节点，非 null；以其 owner 的 id 作为 key
     */
    public void addMember(PlayerArmy playerArmy) {
        if (playerArmy != null && playerArmy.getOwner() != null) {
            memberArmies.put(playerArmy.getOwner().getId(), playerArmy);
        }
    }

    /**
     * 将一名玩家的军队移出集结
     *
     * @param playerArmy 玩家军队节点（按 owner id 移除）
     */
    public void removeMember(PlayerArmy playerArmy) {
        if (playerArmy != null && playerArmy.getOwner() != null) {
            memberArmies.remove(playerArmy.getOwner().getId());
        }
    }

    @Override
    public ArmyVO toArmyVO() {
        AssembleArmyVO vo = new AssembleArmyVO();
        vo.setId(getBelongNode().getId());
        vo.setLeaderId(leaderId);
        List<PlayerArmyVO> members = memberArmies.values().stream()
                .map(pa -> (PlayerArmyVO) pa.toArmyVO())
                .filter(Objects::nonNull)
                .toList();
        vo.setMembers(members.isEmpty() ? null : new ArrayList<>(members));
        return vo;
    }

    @Override
    public FightArmy toFightArmy() {
        if (memberArmies.isEmpty()) {
            return MultiFightArmy.valueOf(Collections.emptyMap(), leaderId);
        }
        Map<Long, FightArmy> memberFightArmies = new HashMap<>();
        for (Map.Entry<Long, PlayerArmy> e : memberArmies.entrySet()) {
            ArmyDetailComponent<?> comp = e.getValue().getComponent(ComponentEnum.ArmyDetail);
            if (comp != null) {
                FightArmy fa = comp.toFightArmy();
                memberFightArmies.put(e.getKey(), fa);
            }
        }
        return MultiFightArmy.valueOf(memberFightArmies, leaderId);
    }
}
