package com.slg.entity.mysql.entity;

import com.slg.entity.db.entity.BaseEntity;
import com.slg.entity.mysql.converter.SerializedUserType;
import com.slg.entity.mysql.datatype.DataList;
import com.slg.entity.mysql.datatype.DataMap;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.TypeRegistration;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MySQL 实体基类
 * 继承 {@link BaseEntity}，通过 JPA 注解提供 MySQL 持久化能力
 *
 * <p>设计要点：
 * <ul>
 *   <li>继承 {@code BaseEntity<ID>}，使 {@code AsyncPersistenceService}、{@code EntityCache} 等框架组件无需修改即可兼容</li>
 *   <li>使用 {@code @Access(AccessType.PROPERTY)} 让 JPA 通过 getter 方法做映射，绕开无法在父类字段上加 JPA 注解的限制</li>
 *   <li>覆写 getter 添加 JPA 注解（{@code @jakarta.persistence.Id}、{@code @Column}），父类的 Spring Data 注解保持原样</li>
 *   <li>{@code @MappedSuperclass} + {@code @EntityListeners} 使 JPA 审计功能在 JPA 模式下也能自动填充时间戳</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>
 * {@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * @Entity
 * @Table(name = "new_entity")
 * public class NewEntity extends BaseMysqlEntity<Long> {
 *     private String name;
 *     private int level;
 * }
 * }
 * </pre>
 *
 * @author yangxunan
 * @date 2026/02/24
 * @param <ID> 实体标识符的类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Access(AccessType.PROPERTY)
@TypeRegistration(basicClass = DataList.class, userType = SerializedUserType.class)
@TypeRegistration(basicClass = DataMap.class, userType = SerializedUserType.class)
public abstract class BaseMysqlEntity<ID extends Serializable> extends BaseEntity<ID> {

    @jakarta.persistence.Id
    @Override
    public ID getId() {
        return id;
    }

    @Column(name = "create_time")
    @Override
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    @Column(name = "update_time")
    @Override
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    @Column(name = "deleted", columnDefinition = "boolean default false")
    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Column(name = "delete_time")
    @Override
    public LocalDateTime getDeleteTime() {
        return deleteTime;
    }
}
