package com.slg.net.message.innermessage.event.packet;

import com.slg.common.event.model.IEvent;
import com.slg.net.crossevent.ICrossServerEvent;
import com.slg.net.crossevent.anno.RoutePlayerGame;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 战斗结算事件
 *
 * @author yangxunan
 * @date 2026/2/13
 */
@Getter
@Setter
public class FightSettleEvent implements IEvent, ICrossServerEvent {

    @RoutePlayerGame
    private long playerId;

    private boolean win;

    private Map<Integer, Long> killNum;

    @Override
    public FightSettleEvent toCrossEvent(){
        // 简单数据结构，可以被传递
        return this;
    }
}
