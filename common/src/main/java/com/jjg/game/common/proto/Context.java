package com.jjg.game.common.proto;

import java.util.Map;
import java.util.TreeMap;

/**
 * @author 11
 * @date 2022/5/11
 */
public class Context {
    public static final Context instance = new Context();

    private Context(){}

    private final Map<Integer,String> reqMap = new TreeMap<>();

    public String toHex(Object obj){
        return "0x" + Integer.toHexString(Integer.parseInt(obj.toString())).toUpperCase();
    }

    public Map<Integer, String> getReqMap() {
        return reqMap;
    }

    //将首字母转化为大写
    public String toUpperFirst(String str){
        return str.substring(0,1).toUpperCase() + str.substring(1,str.length());
    }
}
