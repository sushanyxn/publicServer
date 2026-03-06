package com.slg.game.develop.army.facade;

import com.slg.common.event.anno.EventListener;
import com.slg.net.message.innermessage.event.packet.FightSettleEvent;
import org.springframework.stereotype.Component;

/**
 * 军队模块 Facade
 * 处理军队相关协议与战斗结算事件（FightSettleEvent）
 *
 * @author yangxunan
 * @date 2026/2/13
 */
@Component
public class ArmyFacade {


    @EventListener
    public void onFightSettleEvent(FightSettleEvent event){
        // 处理士兵，战力，医院，杀敌积分等业务
        // 。。。
    }

}
