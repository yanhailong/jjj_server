package com.jjg.game.common.protostuff;

import java.lang.reflect.Type;

/**
 * @since 1.0
 */
public class MethodInfo {
    public int index;
    public String name;
    public Class[] parms;
    public Type returnType;

    public MethodInfo(int index, String name, Class[] parms, Type returnType) {
        this.index = index;
        this.name = name;
        this.parms = parms;
        this.returnType = returnType;
    }
}
