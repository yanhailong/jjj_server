package com.jjg.game.core.utils;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.Set;

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

}
