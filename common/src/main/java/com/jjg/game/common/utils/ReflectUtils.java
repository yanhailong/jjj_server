package com.jjg.game.common.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 反射工具
 *
 * @author 2CL
 */
public class ReflectUtils {


    /**
     * 获取类的所有的泛型类
     */
    public static <R> Set<Class<R>> getClassSuperActualType(Class<?> aClass) {
        Type genericType = aClass.getGenericSuperclass();
        if (genericType instanceof ParameterizedType pType) {
            Type[] actualTypeArguments = pType.getActualTypeArguments();
            return Arrays.stream(actualTypeArguments).map((type) -> (Class<R>) type).collect(Collectors.toSet());
        }
        return null;
    }

    /**
     * 获取类的指定的泛型类
     */
    public static <R> Set<Class<R>> getClassSuperActualType(Class<?> aClass, Class<R> rClass) {
        Set<Type> genericTypes = new HashSet<>();
        while (aClass != null && aClass != Object.class) {
            Type genericSuperclass = aClass.getGenericSuperclass();
            if (genericSuperclass instanceof ParameterizedType pt) {
                Collections.addAll(genericTypes, pt.getActualTypeArguments());
            }

            // 处理接口泛型
            for (Type genericInterface : aClass.getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType pt) {
                    Collections.addAll(genericTypes, pt.getActualTypeArguments());
                }
            }

            aClass = aClass.getSuperclass();
        }
        Set<Class<R>> classes = new HashSet<>();
        for (Type genericType : genericTypes) {
            if (genericType instanceof Class<?> typeClass && rClass.isAssignableFrom(typeClass)) {
                classes.add((Class<R>) typeClass);
            }
        }
        return classes;
    }
}
