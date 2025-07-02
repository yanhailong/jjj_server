package com.jjg.game.core.utils;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 反射工具类
 *
 * @author 11
 * @date 2025/6/6 16:38
 */
public class ReflectionTool {

    /**
     * 查找给定包中指定父类的所有子类。
     *
     * @param <T>         父类的类型
     * @param parentClass 查找子类的父类
     * @param packageName 要搜索的包的名称
     * @return 包含父类的所有子类的集合
     */
    public static <T> Set<Class<? extends T>> findSubclasses(Class<T> parentClass, String packageName) {
        Reflections reflections = new Reflections(packageName, Scanners.SubTypes);
        return reflections.getSubTypesOf(parentClass);
    }

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
     * 获取类的所有的泛型类
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
        Set<Class<R>>  classes = new HashSet<>();
        for (Type genericType : genericTypes) {
            if (genericType instanceof Class<?> typeClass && rClass.isAssignableFrom(typeClass)) {
                classes.add((Class<R>) typeClass);
            }
        }
        return classes;
    }
}
