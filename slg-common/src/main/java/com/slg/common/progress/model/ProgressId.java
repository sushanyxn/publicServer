package com.slg.common.progress.model;

import com.slg.common.progress.type.ProgressTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * 进度唯一标识
 * 由进度类型和配置ID组成
 *
 * @author yangxunan
 * @date 2026/1/28
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressId {

    /**
     * 进度类型
     */
    private ProgressTypeEnum type;

    /**
     * 进度配置ID
     */
    private long id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgressId that = (ProgressId) o;
        return id == that.id && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, id);
    }

    @Override
    public String toString() {
        return "ProgressId{" +
                "type=" + type +
                ", id=" + id +
                '}';
    }
}
