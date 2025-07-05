package com.jjg.game.slots.data;

import java.util.List;

/**
 * @author 11
 * @date 2025/7/4 16:05
 */
public class BaseLineFreeInfo {
    //id
    private int id;
    //滚轴
    private int roller;
    //玩法类型
    private int playerType;
    //最小元素种类
    private int minIconTypeMin;
    //元素id组合
    private List<List<Integer>> elementGroupList;
    //主元素id
    private int mainElementId;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoller() {
        return roller;
    }

    public void setRoller(int roller) {
        this.roller = roller;
    }

    public int getPlayerType() {
        return playerType;
    }

    public void setPlayerType(int playerType) {
        this.playerType = playerType;
    }

    public int getMinIconTypeMin() {
        return minIconTypeMin;
    }

    public void setMinIconTypeMin(int minIconTypeMin) {
        this.minIconTypeMin = minIconTypeMin;
    }

    public List<List<Integer>> getElementGroupList() {
        return elementGroupList;
    }

    public void setElementGroupList(List<List<Integer>> elementGroupList) {
        this.elementGroupList = elementGroupList;
    }

    public int getMainElementId() {
        return mainElementId;
    }

    public void setMainElementId(int mainElementId) {
        this.mainElementId = mainElementId;
    }
}
