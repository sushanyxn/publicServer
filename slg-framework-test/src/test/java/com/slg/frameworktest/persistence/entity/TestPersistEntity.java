package com.slg.frameworktest.persistence.entity;

import com.slg.entity.mysql.entity.BaseMysqlEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 集成测试用 MySQL 实体，仅用于 slg-framework-test 持久化端到端测试
 *
 * @author framework-test
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "test_persist_entity")
public class TestPersistEntity extends BaseMysqlEntity<Long> {

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "level")
    private int level;

    @Override
    public void save() {
        // 集成测试中直接使用 AsyncPersistenceService.save(entity)，不通过实体 save()
    }

    @Override
    public void saveField(String fieldName) {
        // 集成测试中直接使用 AsyncPersistenceService.updateField，不通过实体 saveField
    }
}
