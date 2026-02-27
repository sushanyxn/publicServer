package com.slg.table.anno;

import java.lang.annotation.*;

/**
 * TableBean 注解
 * 用于标记配置表中的复杂嵌套对象类型
 * 
 * <p>被此注解标记的类会在 Spring 启动时被 {@link com.slg.table.manager.TableBeanManager} 扫描并注册
 * 这些类可以作为配置表字段的类型，用于表达复杂的数据结构
 * 
 * <p>使用场景：
 * <ul>
 *   <li>配置表中的嵌套对象（如奖励配置、属性配置等）</li>
 *   <li>需要在 CSV 文件中以特定格式表达的复杂类型</li>
 *   <li>可复用的配置数据结构</li>
 * </ul>
 * 
 * <p>示例：
 * <pre>
 * // 定义一个奖励配置类
 * &#64;TableBean
 * public class RewardConfig {
 *     private int itemId;
 *     private int count;
 *     // ... getters and setters
 * }
 * 
 * // 在配置表中使用
 * &#64;Table
 * public class QuestConfig {
 *     &#64;TableId
 *     private int id;
 *     
 *     // CSV 中可能格式为: "1001:10,1002:20" (itemId:count,itemId:count)
 *     private List&lt;RewardConfig&gt; rewards;
 * }
 * </pre>
 * 
 * @author yangxunan
 * @date 2026/01/14
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableBean {
}
