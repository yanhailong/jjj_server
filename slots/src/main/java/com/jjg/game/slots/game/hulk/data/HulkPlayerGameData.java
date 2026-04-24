package com.jjg.game.slots.game.hulk.data;

import com.jjg.game.slots.data.SlotsPlayerGameData;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11
 * @date 2026/1/15
 */
@Document
public class HulkPlayerGameData extends SlotsPlayerGameData {
    //汽车游戏数据
    private Map<Integer, Long> carMap;
    private boolean carOver;


    public Map<Integer, Long> getCarMap() {
        return carMap;
    }

    public void setCarMap(Map<Integer, Long> carMap) {
        this.carMap = carMap;
    }

    public boolean addCarInfo(int index, long gold) {
        if (this.carMap == null) {
            this.carMap = new HashMap<>();
        }
        return this.carMap.putIfAbsent(index, gold) == null;
    }

    public int carSize() {
        if (this.carMap == null) {
            return 0;
        }
        return this.carMap.size();
    }

    public Long carByIndex(int index){
        if (this.carMap == null) {
            return null;
        }
        return this.carMap.get(index);
    }

    public boolean isCarOver() {
        return carOver;
    }

    public void setCarOver(boolean carOver) {
        this.carOver = carOver;
    }
}
