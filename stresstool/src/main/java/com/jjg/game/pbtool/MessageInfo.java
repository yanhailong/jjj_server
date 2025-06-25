package com.jjg.game.pbtool;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 14:04
 */
public class MessageInfo {
    public String name;
    public String msgIdHex;
    public boolean isEnum;
    public String desc;
    public List<FieldInfo> fields = new ArrayList<>();
    public List<String> importList = new ArrayList<>();
    public boolean hasList = false;
}
