package com.slg.common.util;

import java.util.Collection;
import java.util.Map;

/**
 * @author yangxunan
 * @date 2025/12/26
 */
public class CollectionUtil {

    public static boolean isNotBlank(Collection<?> collection){
        return collection != null && !collection.isEmpty();
    }

    public static boolean isBlank(Collection<?> collection){
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotBlank(Map<?, ?> map){
        return map != null && !map.isEmpty();
    }

    public static boolean isBlank(Map<?, ?> map){
        return map == null || map.isEmpty();
    }

    public static <T> boolean isNotBlank(T[] array){
        return array != null && array.length > 0;
    }

    public static <T> boolean isBlank(T[] array){
        return array == null || array.length == 0;
    }

}
