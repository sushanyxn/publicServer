package com.slg.sharedmodules.progress.manager;

import com.slg.sharedmodules.progress.type.ProgressTypeEnum;

/**
 * 业务模块需要自己实现类型枚举，用来做type的类型转换，以及获取配置表等具体的业务数据（必须）
 *
 * @author yangxunan
 * @date 2026/1/29
 */
public interface IProgressTypeTransform {

    ProgressTypeEnum getProgressType(int type);
}
