package com.slg.common.hotreload;

/**
 * 热更单个类的结果状态
 *
 * @author yangxunan
 * @date 2026/03/10
 */
public enum ReloadStatus {

    /** 已有类重定义成功 */
    SUCCESS_REDEFINED,

    /** 全新类加载成功 */
    SUCCESS_NEW_LOADED,

    /** 结构性变更导致失败（新增/删除方法或字段等） */
    FAILED_STRUCTURAL_CHANGE,

    /** 类文件格式错误 */
    FAILED_CLASS_FORMAT,

    /** 文件读取失败 */
    FAILED_IO,

    /** defineClass 失败（如类名冲突、LinkageError 等） */
    FAILED_DEFINE_ERROR,

    /** 其他异常 */
    FAILED_OTHER;

    public boolean isSuccess() {
        return this == SUCCESS_REDEFINED || this == SUCCESS_NEW_LOADED;
    }
}
