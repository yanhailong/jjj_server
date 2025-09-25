package com.jjg.game.core.config.bean;

import com.jjg.game.core.config.AbstractExcelConfig;
import com.jjg.game.core.config.ExcelConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 积分大奖的转盘配置
 */
@ExcelConfig(name = "PointsAwardTurntable")
public class PointsAwardTurntable extends AbstractExcelConfig {

    /**
     * 转盘格子
     */
    private int grid;

    /**
     * 转盘奖励
     */
    private List<Integer> getItem = new ArrayList<>();

    public int getGrid() {
        return grid;
    }

    public void setGrid(int grid) {
        this.grid = grid;
    }

    public List<Integer> getGetItem() {
        return getItem;
    }

    public void setGetItem(List<Integer> getItem) {
        this.getItem = getItem;
    }
}
