package com.jjg.game.sampledata.bean;

import java.util.*;

/**
 * 水池结果库配置
 *
 * @excelName poolresults.xlsx
 * @sheetName poolresults
 */
public class PoolResultsCfg extends BaseCfgBean {

    /** 配置表名 */
    public static final String EXCEL_NAME = "poolresults.xlsx";
    /** 配置表工作薄名 */
    public static final String SHEET_NAME = "poolresults";

    /** 游戏ID */
    protected int gameType;
    /** 调控序列ID (水池模型) */
    protected int modelId;
    /**
     * 连赢/连输修改权重
     * 格式: streakValue -> (sectionKey -> weightDelta)
     * 例: {-6: {-99999: 0, -39: -200, ...}, 3: {-99999: 0, -39: 100, ...}}
     */
    protected Map<Integer, Map<Integer, Integer>> addTypeProp;
    /**
     * 类型权重 (基础权重)
     * 格式: sectionKey -> weight
     * 例: {-99999: 300, -39: 200, -32: 300, ...}
     */
    protected Map<Integer, Integer> typeProp;
    /** 进入条件下限 (水池余额) */
    protected int enterLimitMin;
    /** 进入条件上限 (水池余额) */
    protected int enterLimitMax;

    /** 返回游戏ID */
    public int getGameType() {
        return gameType;
    }

    /** 返回调控序列ID */
    public int getModelId() {
        return modelId;
    }

    /** 返回连赢/连输修改权重 */
    public Map<Integer, Map<Integer, Integer>> getAddTypeProp() {
        return addTypeProp;
    }

    /** 返回类型权重 */
    public Map<Integer, Integer> getTypeProp() {
        return typeProp;
    }

    /** 返回进入条件下限 */
    public int getEnterLimitMin() {
        return enterLimitMin;
    }

    /** 返回进入条件上限 */
    public int getEnterLimitMax() {
        return enterLimitMax;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
