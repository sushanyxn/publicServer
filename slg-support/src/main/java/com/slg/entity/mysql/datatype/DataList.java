package com.slg.entity.mysql.datatype;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 泛型列表容器，用于 JPA 实体中持久化为 JSON 列
 * 持久化类型为普通 POJO，避免 Hibernate 将 List 当作 ElementCollection 处理
 *
 * <p>使用方式：
 * <pre>
 * {@code
 * @Convert(converter = XxxDataListConverter.class)
 * @Column(name = "role_info_list", columnDefinition = "json")
 * private DataList<RoleBriefInfo> roleInfoList = new DataList<>();
 * }
 * </pre>
 * 通过 {@link #getList()} 获取内部列表进行读写，修改后需对实体执行 save/saveField
 *
 * @param <E> 元素类型
 * @author yangxunan
 * @date 2026-03-02
 */
@Data
public class DataList<E> {

    /** 内部列表，对外暴露为可修改 */
    private List<E> list = new ArrayList<>();

    public DataList() {
    }

    public DataList(Collection<? extends E> c) {
        this.list = c != null ? new ArrayList<>(c) : new ArrayList<>();
    }

    /** 获取内部列表（可修改，修改后需持久化实体） */
    public List<E> getList() {
        return list;
    }

    /** 设置内部列表 */
    public void setList(List<E> list) {
        this.list = list != null ? list : new ArrayList<>();
    }
}
