package com.jjg.game.dollarexpress.data;

import com.jjg.game.core.data.AbstractGameRunInfo;
import com.jjg.game.dollarexpress.pb.ResultLineInfo;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
public class GameRunInfo extends AbstractGameRunInfo {
    private long playerId;
    private List<Integer> intList;
    private List<Long> longList;
    private List<ResultLineInfo> resultLineInfoList;
    private int[] intArray;
    private int specialId;


    public GameRunInfo(int code, long playerId) {
        super(code);
        this.playerId = playerId;
    }

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public List<Integer> getIntList() {
        return intList;
    }

    public void setIntList(List<Integer> intList) {
        this.intList = intList;
    }

    public List<Long> getLongList() {
        return longList;
    }

    public void setLongList(List<Long> longList) {
        this.longList = longList;
    }

    public List<ResultLineInfo> getResultLineInfoList() {
        return resultLineInfoList;
    }

    public void setResultLineInfoList(List<ResultLineInfo> resultLineInfoList) {
        this.resultLineInfoList = resultLineInfoList;
    }

    public int[] getIntArray() {
        return intArray;
    }

    public void setIntArray(int[] intArray) {
        this.intArray = intArray;
    }

    public int getSpecialId() {
        return specialId;
    }

    public void setSpecialId(int specialId) {
        this.specialId = specialId;
    }
}
