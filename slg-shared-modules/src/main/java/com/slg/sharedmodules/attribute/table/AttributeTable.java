package com.slg.sharedmodules.attribute.table;

import com.slg.sharedmodules.attribute.compute.AttributeFormula;
import com.slg.sharedmodules.attribute.compute.FormulaParser;
import com.slg.sharedmodules.attribute.type.AttributeType;
import com.slg.table.anno.Table;
import com.slg.table.anno.TableId;
import com.slg.table.extend.TablePostProcessor;
import lombok.Getter;
import lombok.Setter;

/**
 * 属性配置表。
 * <p>
 * 定义每个属性的 id、类型、名称和可选的计算公式。
 * type 字段在 CSV 中以 {@link AttributeType} 枚举名称填写，加载时自动转换为枚举。
 * 若 formula 非空，在行加载后由 {@link TablePostProcessor} 自动解析为
 * {@link AttributeFormula}，后续由
 * {@link com.slg.sharedmodules.attribute.manager.AttributeManager} 收集并注册到全局
 * {@link com.slg.sharedmodules.attribute.compute.AttributeSchema}。
 * </p>
 * <p>
 * 公式语法：使用 AttributeType 枚举名称作为变量，支持 +、-、*、/ 和括号。
 * 示例：{@code BASE_ATK * (10000 + ATK_PCT) / 10000 + EXTRA_ATK}
 * </p>
 *
 * @author slgserver
 * @date 2026-03-12
 */
@Table
@Getter
@Setter
public class AttributeTable implements TablePostProcessor {

    /** 属性 id */
    @TableId
    private int id;

    /** 属性类型枚举，CSV 中填写枚举名称（如 BASE_ATK），加载时自动转换 */
    private AttributeType type;

    /** 属性名称 */
    private String name;

    /** 计算公式表达式，为空表示非计算属性 */
    private String formula;

    /** 解析后的属性公式，仅含公式的行有值 */
    private transient AttributeFormula attributeFormula;

    @Override
    public void postProcessAfterInitialization() {
        if (type == null) {
            throw new IllegalStateException("属性配置表 id=" + id + " 的 type 字段无效");
        }
        if (type.getId() != id) {
            throw new IllegalStateException(
                    "属性配置表 id=" + id + " 与 type=" + type + "(id=" + type.getId() + ") 不一致");
        }
        if (formula != null && !formula.isBlank()) {
            this.attributeFormula = FormulaParser.parse(type, formula);
        }
    }
}
