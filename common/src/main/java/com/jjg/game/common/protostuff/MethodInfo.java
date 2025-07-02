package com.jjg.game.common.protostuff;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;

/**
 * @since 1.0
 */
public class MethodInfo {
    public int index;
    public String name;
    public Class[] parms;
    public Type returnType;
    private Command commandAnno;

    public MethodInfo(int index, String name, Class[] parms, Type returnType) {
        this.index = index;
        this.name = name;
        this.parms = parms;
        this.returnType = returnType;
    }

    public Command getCommandAnno() {
        return commandAnno;
    }

    public void setCommandAnno(Command commandAnno) {
        this.commandAnno = commandAnno;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodInfo that = (MethodInfo) o;
        return index == that.index && Objects.equals(name, that.name)
            && Objects.deepEquals(parms, that.parms)
            && Objects.equals(returnType, that.returnType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, name, Arrays.hashCode(parms), returnType);
    }
}
