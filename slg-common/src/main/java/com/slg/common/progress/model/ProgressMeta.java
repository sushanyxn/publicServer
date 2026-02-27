package com.slg.common.progress.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.slg.common.log.LoggerUtil;
import com.slg.common.progress.bean.IProgressCondition;
import com.slg.common.progress.manager.ProgressManager;
import com.slg.common.progress.table.IProgressTable;
import com.slg.common.progress.type.ProgressTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

/**
 * 进度元数据
 * 封装进度的配置信息和当前进度值
 * 
 * @author yangxunan
 * @date 2026/1/28
 */
@Getter
@Setter
public class ProgressMeta {


    /**
     * 进度配置ID
     */
    private int id;

    /**
     * 进度类型（不参与序列化，通过 typeId 间接序列化）
     */
    @JsonIgnore
    private ProgressTypeEnum type;

    /**
     * 当前进度值
     */
    private long progress;

    /**
     * 进度更新时的回调（与whenFinish互斥，不参与序列化）
     */
    @JsonIgnore
    private Consumer<ProgressMeta> whenUpdate;

    /**
     * 进度完成时的回调（与whenUpdate互斥，不参与序列化）
     */
    @JsonIgnore
    private Consumer<ProgressMeta> whenFinish;

    /**
     * 获取进度类型ID（用于 JSON 序列化）
     */
    @JsonProperty("typeId")
    public int getTypeId() {
        return type != null ? type.getType() : 0;
    }

    /**
     * 设置进度类型ID（用于 JSON 反序列化）
     */
    @JsonProperty("typeId")
    public void setTypeId(int typeId) {
        ProgressTypeEnum progressType = ProgressManager.getInstance().getProgressType(typeId);
        if (progressType == null) {
            LoggerUtil.warn("反序列化进度失败: 找不到对应的进度类型, typeId={}, progressId={}", typeId, id);
        }
        this.type = progressType;
    }

    /**
     * 更新进度值
     * 
     * @param delta 增量值
     */
    public void addProgress(long delta) {
        this.progress += delta;
    }

    /**
     * 触发更新回调
     */
    public void triggerUpdate() {
        if (whenUpdate != null) {
            whenUpdate.accept(this);
        }
    }

    /**
     * 触发完成回调
     */
    public void triggerFinish() {
        if (whenFinish != null) {
            whenFinish.accept(this);
        }
    }

    /**
     * 获取进度条件
     * 
     * @return 进度条件，如果类型或表不存在则返回 null
     */
    public IProgressCondition<?, ?> getProgressCondition() {
        if (type == null) {
            LoggerUtil.warn("获取进度条件失败: 进度类型为空, progressId={}", id);
            return null;
        }
        IProgressTable table = type.getTable(id);
        if (table == null) {
            LoggerUtil.warn("获取进度条件失败: 找不到进度表, type={}, progressId={}", type.getType(), id);
            return null;
        }
        return table.getProgressCondition();
    }
}
