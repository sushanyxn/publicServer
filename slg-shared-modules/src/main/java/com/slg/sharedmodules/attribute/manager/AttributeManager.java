package com.slg.sharedmodules.attribute.manager;

import com.slg.common.log.LoggerUtil;
import com.slg.sharedmodules.attribute.compute.AttributeSchema;
import com.slg.sharedmodules.attribute.table.AttributeTable;
import com.slg.table.anno.Table;
import com.slg.table.model.TableInt;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * 属性管理器。
 * <p>
 * 启动时从 {@link AttributeTable} 收集已由 {@link com.slg.table.extend.TablePostProcessor}
 * 解析好的公式，注册到全局 {@link AttributeSchema} 并构建。
 * 全局 Schema 构建完成后，所有 {@link com.slg.sharedmodules.attribute.container.AttributeContainer}
 * 实例共享使用。
 * </p>
 *
 * @author slgserver
 * @date 2026-03-12
 */
@Component
public class AttributeManager {

    @Table
    private TableInt<AttributeTable> attributeTable;

    private static AttributeSchema schema;

    @PostConstruct
    public void init() {
        AttributeSchema newSchema = new AttributeSchema();
        int formulaCount = 0;

        for (AttributeTable row : attributeTable.getAll()) {
            if (row.getAttributeFormula() != null) {
                newSchema.register(row.getAttributeFormula());
                formulaCount++;
            }
        }

        newSchema.build();
        schema = newSchema;
        LoggerUtil.info("属性系统初始化完成：加载 {} 条属性配置，注册 {} 个计算公式",
                attributeTable.getAll().size(), formulaCount);
    }

    /**
     * 获取全局属性计算图谱
     *
     * @return 已构建的全局 Schema
     * @throws IllegalStateException 管理器尚未初始化
     */
    public static AttributeSchema getSchema() {
        if (schema == null) {
            throw new IllegalStateException("AttributeManager 尚未初始化，无法获取全局 AttributeSchema");
        }
        return schema;
    }
}
