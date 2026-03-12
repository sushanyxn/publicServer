package com.slg.scene.scene.node.component.impl.army;

import com.slg.sharedmodules.fight.wos.model.FightArmy;
import com.slg.sharedmodules.fight.wos.model.FightHero;
import com.slg.sharedmodules.fight.wos.model.FightTroop;
import com.slg.net.message.clientmessage.army.packet.ArmyVO;
import com.slg.net.message.clientmessage.army.packet.PlayerArmyVO;
import com.slg.net.message.clientmessage.army.packet.TroopVO;
import com.slg.net.message.clientmessage.hero.packet.HeroVO;
import com.slg.scene.scene.node.node.model.impl.PlayerArmy;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 玩家军队详情组件
 * 面向 {@link PlayerArmy}，描述军队中的英雄与士兵情况，并可生成 {@link PlayerArmyVO}。
 *
 * @author yangxunan
 * @date 2026/2/5
 */
@Getter
@Setter
public class PlayerArmyDetailComponent extends ArmyDetailComponent<PlayerArmy> {

    private List<HeroVO> heroes;
    private List<TroopVO> troops;

    public PlayerArmyDetailComponent(PlayerArmy belongNode) {
        super(belongNode);
    }

    @Override
    public ArmyVO toArmyVO() {
        PlayerArmyVO vo = new PlayerArmyVO();
        vo.setId(getBelongNode().getId());
        vo.setHeroes(heroes != null && !heroes.isEmpty() ? heroes.toArray(new HeroVO[0]) : null);
        vo.setTroops(troops != null && !troops.isEmpty() ? troops.toArray(new TroopVO[0]) : null);
        return vo;
    }

    @Override
    public FightArmy toFightArmy() {
        List<HeroVO> heroList = heroes != null ? heroes : Collections.emptyList();
        List<TroopVO> troopList = troops != null ? troops : Collections.emptyList();
        FightHero[] heroArr = heroList.isEmpty() ? null
                : heroList.stream().map(h -> FightHero.valueOf(h.getHeroId(), h.getHeroLv())).toArray(FightHero[]::new);
        FightTroop[] troopArr = troopList.isEmpty() ? null
                : troopList.stream()
                .map(t -> FightTroop.valueOf(t.getTroopId(), t.getNum(), t.getInitNum(), t.getHurtNum()))
                .toArray(FightTroop[]::new);
        return FightArmy.valueOf(heroArr, troopArr);
    }
}
