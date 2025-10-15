package com.jjg.game.slots.game.wealthgod.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.slots.data.SlotsResultLib;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 财神结果
 */
@Document
public class WealthGodResultLib extends SlotsResultLib<WealthGodAwardLineInfo> {

    /**
     * 本次触发的jackpotId
     */
    private int jackpotId;

    /**
     * 用于存储图标变更的映射关系。
     * k=index
     * v=变化后的icon
     */
    private Map<Integer, Integer> iconChangeMap;

    /**
     * 元图标数组
     */
    private int[] source;

    /**
     * 用于存储图标变更的映射关系。记录从头到尾的所有图标变化信息
     * k=index
     * v=变化后的icon
     */
    private Map<Integer, Integer> allIconChangeMap;

    public int getJackpotId() {
        return jackpotId;
    }

    public void setJackpotId(int jackpotId) {
        this.jackpotId = jackpotId;
    }

    public Map<Integer, Integer> getIconChangeMap() {
        return iconChangeMap;
    }

    public void setIconChangeMap(Map<Integer, Integer> iconChangeMap) {
        this.iconChangeMap = iconChangeMap;
    }

    public void addChange(int index, int icon) {
        if (this.iconChangeMap == null) {
            this.iconChangeMap = new HashMap<>();
        }
        this.iconChangeMap.put(index, icon);
        addAllIconChange(index, icon);
    }

    public int[] getSource() {
        return source;
    }

    public void setSource(int[] source) {
        if (source == null) {
            this.source = null;
        } else {
            this.source = Arrays.copyOf(source, source.length);
        }
    }

    public void addAllIconChange(int index, int icon) {
        if (this.allIconChangeMap == null) {
            this.allIconChangeMap = new HashMap<>();
        }
        this.allIconChangeMap.put(index, icon);
    }

    public Map<Integer, Integer> getAllIconChangeMap() {
        return allIconChangeMap;
    }

    public void setAllIconChangeMap(Map<Integer, Integer> allIconChangeMap) {
        this.allIconChangeMap = allIconChangeMap;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

}
