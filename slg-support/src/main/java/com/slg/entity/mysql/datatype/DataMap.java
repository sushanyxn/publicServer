package com.slg.entity.mysql.datatype;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 泛型 Map 容器，用于 JPA 实体中持久化为 JSON 列
 * 持久化类型为普通 POJO，避免 Hibernate 将 Map 当作集合映射处理
 *
 * <p>使用方式：
 * <pre>
 * {@code
 * @Convert(converter = StringLongDataMapConverter.class)
 * @Column(name = "extra_data", columnDefinition = "json")
 * private DataMap<String, Long> extraData = new DataMap<>();
 * }
 * </pre>
 * 通过 {@link #getMap()} 获取内部 Map 进行读写，修改后需对实体执行 save/saveField
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author yangxunan
 * @date 2026-03-02
 */
@Data
public class DataMap<K, V> {

    /** 内部 Map，对外暴露为可修改 */
    private Map<K, V> map = new HashMap<>();

    public DataMap() {
    }

    public DataMap(Map<K, V> map) {
        this.map = map != null ? new HashMap<>(map) : new HashMap<>();
    }

    /** 获取内部 Map（可修改，修改后需持久化实体） */
    public Map<K, V> getMap() {
        return map;
    }

    /** 设置内部 Map */
    public void setMap(Map<K, V> map) {
        this.map = map != null ? map : new HashMap<>();
    }
}
