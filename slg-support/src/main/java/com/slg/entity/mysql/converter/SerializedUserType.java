package com.slg.entity.mysql.converter;

import com.slg.common.log.LoggerUtil;
import com.slg.common.util.JsonUtil;
import com.slg.entity.mysql.anno.SerializeFormat;
import com.slg.entity.mysql.anno.Serialized;
import org.hibernate.usertype.DynamicParameterizedType;
import org.hibernate.usertype.UserType;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

/**
 * 统一 JSON 序列化 UserType
 * 配合 {@link Serialized} 注解使用，通过 {@link DynamicParameterizedType} 自动获取字段类型和注解配置
 *
 * <p>支持两种存储格式：
 * <ul>
 *   <li>{@link SerializeFormat#JSON} — JSON 字符串，映射 SQL {@link Types#LONGVARCHAR}</li>
 *   <li>{@link SerializeFormat#BYTES} — JSON byte[]，映射 SQL {@link Types#VARBINARY}</li>
 * </ul>
 *
 * @author yangxunan
 * @date 2026/02/24
 */
public class SerializedUserType implements UserType<Object>, DynamicParameterizedType {

    private Class<?> targetType;
    private SerializeFormat format = SerializeFormat.JSON;

    @Override
    public void setParameterValues(Properties parameters) {
        ParameterType parameterType = (ParameterType) parameters.get(PARAMETER_TYPE);
        if (parameterType != null) {
            targetType = parameterType.getReturnedClass();
            for (Annotation ann : parameterType.getAnnotationsMethod()) {
                if (ann instanceof Serialized serialized) {
                    format = serialized.format();
                    break;
                }
            }
        }
        if (targetType == null) {
            String className = (String) parameters.get(RETURNED_CLASS);
            if (className != null) {
                try {
                    targetType = Class.forName(className);
                } catch (ClassNotFoundException e) {
                    LoggerUtil.error("[SerializedUserType] 无法加载类型: {}", className, e);
                }
            }
        }
    }

    @Override
    public int getSqlType() {
        return format == SerializeFormat.BYTES ? Types.VARBINARY : Types.LONGVARCHAR;
    }

    @Override
    public Class<Object> returnedClass() {
        @SuppressWarnings("unchecked")
        Class<Object> clazz = (Class<Object>) targetType;
        return clazz;
    }

    @Override
    public boolean equals(Object x, Object y) {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, int position,
                              SharedSessionContractImplementor session, Object owner) throws SQLException {
        if (targetType == null) {
            return null;
        }
        if (format == SerializeFormat.BYTES) {
            byte[] bytes = rs.getBytes(position);
            if (bytes == null || rs.wasNull()) {
                return null;
            }
            String json = new String(bytes, StandardCharsets.UTF_8);
            return JsonUtil.fromJson(json, targetType);
        } else {
            String json = rs.getString(position);
            if (json == null || rs.wasNull()) {
                return null;
            }
            return JsonUtil.fromJson(json, targetType);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index,
                            SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, getSqlType());
            return;
        }
        if (format == SerializeFormat.BYTES) {
            String json = JsonUtil.toJson(value);
            st.setBytes(index, json == null ? null : json.getBytes(StandardCharsets.UTF_8));
        } else {
            st.setString(index, JsonUtil.toJson(value));
        }
    }

    @Override
    public Object deepCopy(Object value) {
        if (value == null || targetType == null) {
            return null;
        }
        return JsonUtil.clone(value, targetType);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) {
        return (Serializable) deepCopy(value);
    }

    @Override
    public Object assemble(Serializable cached, Object owner) {
        return deepCopy(cached);
    }
}
