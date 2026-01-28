package com.jjg.game.slots.game.zeusVsHades.data;

import java.util.Map;

/**
 * @author lihaocao
 * @date 2026/1/16 13:49
 */
public class ZeusVsHadesNormalChooseInfo {
    private int auxiliaryId;

    private int column;

    private int zeusAuxiliaryId;

    private int hadesAuxiliaryId;

    public ZeusVsHadesNormalChooseInfo() {
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public int getAuxiliaryId() {
        return auxiliaryId;
    }

    public void setAuxiliaryId(int auxiliaryId) {
        this.auxiliaryId = auxiliaryId;
    }

    public int getZeusAuxiliaryId() {
        return zeusAuxiliaryId;
    }

    public void setZeusAuxiliaryId(int zeusAuxiliaryId) {
        this.zeusAuxiliaryId = zeusAuxiliaryId;
    }

    public int getHadesAuxiliaryId() {
        return hadesAuxiliaryId;
    }

    public void setHadesAuxiliaryId(int hadesAuxiliaryId) {
        this.hadesAuxiliaryId = hadesAuxiliaryId;
    }
}
