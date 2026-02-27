package com.slg.entity.mysql.entity;

import com.slg.entity.mysql.anno.SerializeFormat;
import com.slg.entity.mysql.anno.Serialized;

/**
 * {@link Serialized} 注解用法示例（非业务代码，仅供参考，不会被 Hibernate 扫描）
 *
 * <h3>核心用法：</h3>
 * <ol>
 *   <li>在复杂字段上标注 {@code @Serialized} 即可自动 JSON 序列化，无需编写 Converter 子类</li>
 *   <li>默认以 JSON 字符串存储，搭配 {@code @Column(columnDefinition = "json")} 使用</li>
 *   <li>需要 byte[] 存储时，指定 {@code @Serialized(format = SerializeFormat.BYTES)} 搭配 blob 列</li>
 * </ol>
 *
 * <h3>完整实体示例：</h3>
 * <pre>
 * {@literal @}Data
 * {@literal @}EqualsAndHashCode(callSuper = true)
 * {@literal @}Entity
 * {@literal @}Table(name = "t_player_profile")
 * public class PlayerProfileEntity extends BaseMysqlEntity&lt;Long&gt; {
 *
 *     // 普通字段
 *     {@literal @}Column(length = 64)
 *     private String name;
 *
 *     // 用法一：自定义对象 → JSON 字符串存储
 *     {@literal @}Serialized
 *     {@literal @}Column(columnDefinition = "json")
 *     private HeroInfo heroInfo;
 *
 *     // 用法二：集合类型 → JSON 字符串存储
 *     {@literal @}Serialized
 *     {@literal @}Column(columnDefinition = "json")
 *     private List&lt;ItemInfo&gt; items;
 *
 *     // 用法三：Map 类型 → JSON 字符串存储
 *     {@literal @}Serialized
 *     {@literal @}Column(columnDefinition = "json")
 *     private Map&lt;String, Integer&gt; attributes;
 *
 *     // 用法四：byte[] 存储模式（数据量大或不需要数据库内检索的场景）
 *     {@literal @}Serialized(format = SerializeFormat.BYTES)
 *     {@literal @}Column(columnDefinition = "blob")
 *     private BattleSnapshot battleSnapshot;
 * }
 * </pre>
 *
 * <h3>对比旧方式：</h3>
 * <pre>
 * // ——— 旧方式（每种类型都要写一个空壳 Converter）———
 * {@literal @}Converter
 * public class HeroInfoConverter extends JsonColumnConverter&lt;HeroInfo&gt; {}
 *
 * {@literal @}Convert(converter = HeroInfoConverter.class)
 * {@literal @}Column(columnDefinition = "json")
 * private HeroInfo heroInfo;
 *
 * // ——— 新方式（一个注解搞定）———
 * {@literal @}Serialized
 * {@literal @}Column(columnDefinition = "json")
 * private HeroInfo heroInfo;
 * </pre>
 *
 * <h3>IDE 警告说明：</h3>
 * <p>IntelliJ 的 JPA 检查器可能对 {@code @Serialized} 标注的复杂类型字段
 * 提示 "Basic attribute type should not be XXX"，这是因为 IDE 无法穿透元注解发现 {@code @Type}。
 * 运行时 Hibernate 会正确使用 {@code SerializedUserType} 处理，可通过
 * {@code @SuppressWarnings("JpaAttributeTypeInspection")} 消除警告。
 *
 * @author yangxunan
 * @date 2026/02/24
 * @see Serialized
 * @see SerializeFormat
 */
public final class SerializedExample {

    private SerializedExample() {
    }
}
