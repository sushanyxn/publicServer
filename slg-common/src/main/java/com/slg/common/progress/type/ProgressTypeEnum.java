package com.slg.common.progress.type;

import com.slg.common.progress.table.IProgressTable;

/**
 * 进度类型接口
 * 序列化时只保存类型ID，反序列化后由业务层重新设置
 *
 * @author yangxunan
 * @date 2026/1/28
 */
public interface ProgressTypeEnum {

    int getType();

    IProgressTable getTable(int id);
}
