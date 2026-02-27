package com.slg.entity.db.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MongoDB 文档基类
 * 提供泛型 ID 字段和审计支持（创建/更新时间戳）
 * 
 * @author yangxunan
 * @date 2025-12-18
 * @param <ID> 实体标识符的类型
 */
@Data
public abstract class BaseEntity<ID extends Serializable> implements Serializable {

    /**
     * 泛型主键
     * 常用类型：String、Long、ObjectId
     */
    @Id
    protected ID id;

    /**
     * 创建时间戳
     * 实体首次保存时自动填充
     */
    @CreatedDate
    protected LocalDateTime createTime;

    /**
     * 最后修改时间戳
     * 实体修改时自动更新
     */
    @LastModifiedDate
    protected LocalDateTime updateTime;

    /**
     * 保存整个实体
     */
    public abstract void save();

    /**
     * 保存单个字段
     */
    public abstract void saveField(String fieldName);

}

