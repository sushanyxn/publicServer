package com.slg.scene.core.progress;

import com.slg.sharedmodules.progress.table.IProgressTable;
import com.slg.sharedmodules.progress.type.ProgressTypeEnum;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

/**
 * SCENE的进度类型
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Getter
public enum SceneProgressType implements ProgressTypeEnum {

    /**
     * 场景进度占位
     */
    NONE(999){
        @Override
        public IProgressTable getTable(int id){
            return null;
        }
    },

    ;

    private final int type;

    SceneProgressType(int type){
        this.type = type;
    }

    private static Int2ObjectMap<SceneProgressType> progressTypes = new Int2ObjectOpenHashMap<>();

    static {
        for (SceneProgressType type : SceneProgressType.values()) {
            progressTypes.put(type.getType(), type);
        }
    }

    public static SceneProgressType getProgressType(int type){
        return progressTypes.get(type);
    }

}
