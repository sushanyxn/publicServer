package com.slg.game.bean.event;

import com.slg.common.event.model.IEvent;
import com.slg.sharedmodules.progress.model.IProgressEvent;
import com.slg.sharedmodules.progress.type.ProgressOwnerEnum;
import com.slg.game.base.player.model.Player;

/**
 * @author yangxunan
 * @date 2026/1/29
 */
public interface IPlayerProgressEvent extends IProgressEvent<Player> {

    default ProgressOwnerEnum getOwnerEnum(){
        return ProgressOwnerEnum.Player;
    }

}
