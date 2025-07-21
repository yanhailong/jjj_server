package com.jjg.game.slots.game.dollarexpress.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 美元现金信息
 * @author 11
 * @date 2025/7/8 17:12
 */
public class DollarCashConfig {
    //美元图标id
    private int dollarIconId;
    //美元图标id应该出现的列
    private List<Integer> dollarIconIdShowColumn;
    //保险箱id
    private int safeBoxId;
    //保险箱id应该出现的列
    private List<Integer> safeBoxIconIdShowColumn;
    //收集的图标id
    private int collectIconId;

    public int getDollarIconId() {
        return dollarIconId;
    }

    public void setDollarIconId(int dollarIconId) {
        this.dollarIconId = dollarIconId;
    }

    public List<Integer> getDollarIconIdShowColumn() {
        return dollarIconIdShowColumn;
    }

    public void setDollarIconIdShowColumn(List<Integer> dollarIconIdShowColumn) {
        this.dollarIconIdShowColumn = dollarIconIdShowColumn;
    }

    public int getSafeBoxId() {
        return safeBoxId;
    }

    public void setSafeBoxId(int safeBoxId) {
        this.safeBoxId = safeBoxId;
    }

    public List<Integer> getSafeBoxIconIdShowColumn() {
        return safeBoxIconIdShowColumn;
    }

    public void setSafeBoxIconIdShowColumn(List<Integer> safeBoxIconIdShowColumn) {
        this.safeBoxIconIdShowColumn = safeBoxIconIdShowColumn;
    }

    public int getCollectIconId() {
        return collectIconId;
    }

    public void setCollectIconId(int collectIconId) {
        this.collectIconId = collectIconId;
    }

    public void addDollarIconIdShowColumn(int columnId) {
        if(this.dollarIconIdShowColumn == null) {
            this.dollarIconIdShowColumn = new ArrayList<Integer>();
        }
        this.dollarIconIdShowColumn.add(columnId);
    }

    public void addSafeBoxIconIdShowColumn(int columnId){
        if(this.safeBoxIconIdShowColumn == null) {
            this.safeBoxIconIdShowColumn = new ArrayList<Integer>();
        }
        this.safeBoxIconIdShowColumn.add(columnId);
    }
}
