package com.slg.game.core.progress;

import com.slg.sharedmodules.progress.table.IProgressTable;
import com.slg.sharedmodules.progress.type.ProgressTypeEnum;
import com.slg.game.SpringContext;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

/**
 * GAME的进度类型
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Getter
public enum GameProgressType implements ProgressTypeEnum {

    /**
     * 主线任务
     */
    Main(1){
        @Override
        public IProgressTable getTable(int id){
            return SpringContext.getTaskManager().getMainTaskTable(id);
        }
    },

    ;

    private int type;

    GameProgressType(int type){
        this.type = type;
    }

    private static Int2ObjectMap<GameProgressType> progressTypes = new Int2ObjectOpenHashMap<>();

    static {
        for (GameProgressType type : GameProgressType.values()) {
            progressTypes.put(type.getType(), type);
        }
    }

    public static GameProgressType getProgressType(int type){
        return progressTypes.get(type);
    }

}
