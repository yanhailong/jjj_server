package com.vegasnight.game.core.utils;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.Set;

/**
 * @author 11
 * @date 2025/6/6 16:38
 */
public class Tool {
    public static <T> Set<Class<? extends T>> findSubclasses(Class<T> parentClass, String packageName) {
        Reflections reflections = new Reflections(packageName, new SubTypesScanner(false));
        return reflections.getSubTypesOf(parentClass);
    }
}
