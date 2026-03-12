package com.slg.game.core.progress;

import com.slg.sharedmodules.progress.manager.IProgressTypeTransform;
import com.slg.sharedmodules.progress.type.ProgressTypeEnum;
import org.springframework.stereotype.Component;

/**
 * game进程接入进度管理类型转换
 *
 * @author yangxunan
 * @date 2026/1/29
 */
@Component
public class GameProgressTypeTransForm implements IProgressTypeTransform {
    @Override
    public ProgressTypeEnum getProgressType(int type){
        return GameProgressType.getProgressType(type);
    }
}
