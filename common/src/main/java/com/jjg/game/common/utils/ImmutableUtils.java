package com.jjg.game.common.utils;

/**
 * @author lm
 * @date 2025/9/18 14:23
 */

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImmutableUtils {

    // 递归将对象内部的 List/Map 转为不可变集合
    @SuppressWarnings("unchecked")
    public static <T> T makeImmutable(T obj) {
        if (obj instanceof List<?> list) {
            List<Object> newList = new ArrayList<>();
            for (Object e : list) newList.add(makeImmutable(e));
            return (T) List.copyOf(newList);
        } else if (obj instanceof Map<?, ?> map) {
            Map<Object, Object> newMap = new HashMap<>();
            map.forEach((k, v) -> newMap.put(k, makeImmutable(v)));
            return (T) Map.copyOf(newMap);
        } else {
            // 如果是自定义对象，反射处理其字段
            Class<?> clazz = obj.getClass();
            if (!clazz.isPrimitive() && !clazz.getName().startsWith("java.")) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field f : fields) {
                    f.setAccessible(true);
                    try {
                        Object value = f.get(obj);
                        if (value instanceof List<?> || value instanceof Map<?, ?> || !isJavaBuiltin(value)) {
                            f.set(obj, makeImmutable(value));
                        }
                    } catch (IllegalAccessException ignored) {}
                }
            }
            return obj;
        }
    }

    private static boolean isJavaBuiltin(Object value) {
        return value == null || value.getClass().getName().startsWith("java.");
    }
}
