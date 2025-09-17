package com.jjg.game.slots.game.wealthgod.data;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.slots.data.SlotsResultLib;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 财神结果
 */
@Document
public class WealthGodResultLib extends SlotsResultLib<WealthGodAwardLineInfo> {

    private final Logger log = LoggerFactory.getLogger(WealthGodResultLib.class);

    /**
     * 有值则等待免费游戏进行处理
     */
    private SpecialAuxiliaryInfo waitFree;

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

    public SpecialAuxiliaryInfo getWaitFree() {
        return waitFree;
    }

    public void setWaitFree(SpecialAuxiliaryInfo waitFree) {
        this.waitFree = waitFree;
    }

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
    }

    public int[] getSource() {
        return source;
    }

    public void setSource(int[] source) {
        if (source == null) {
            this.source = null;
            log.error("source is null this={}", this);
        } else {
            this.source = Arrays.copyOf(source, source.length);
        }
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

}
