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
    //汽车小游戏是否结束
    private boolean carOver;
    //是否在免费又触发免费的游戏中
    private boolean innerFreeGame;
    private int innerAuxiliaryIdex;
    //内层免费局的下标
    private int innerFreeGameIdex;


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

    public boolean isInnerFreeGame() {
        return innerFreeGame;
    }

    public void setInnerFreeGame(boolean innerFreeGame) {
        this.innerFreeGame = innerFreeGame;
    }

    public int getInnerAuxiliaryIdex() {
        return innerAuxiliaryIdex;
    }

    public void setInnerAuxiliaryIdex(int innerAuxiliaryIdex) {
        this.innerAuxiliaryIdex = innerAuxiliaryIdex;
    }

    public int getInnerFreeGameIdex() {
        return innerFreeGameIdex;
    }

    public void setInnerFreeGameIdex(int innerFreeGameIdex) {
        this.innerFreeGameIdex = innerFreeGameIdex;
    }

    public void clearInnerFree() {
        this.innerFreeGame = false;
        this.innerAuxiliaryIdex = 0;
        this.innerFreeGameIdex = 0;
    }
}
