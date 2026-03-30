package com.jjg.game.slots.data;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/10 10:44
 */
public class FreeAwardRealData {
    //奖励A
    private List<int[]> resultListA;
    private Map<Integer,List<int[]>> resultMapC;

    public List<int[]> getResultListA() {
        return resultListA;
    }

    public void setResultListA(List<int[]> resultListA) {
        this.resultListA = resultListA;
    }

    public Map<Integer, List<int[]>> getResultMapC() {
        return resultMapC;
    }

    public void setResultMapC(Map<Integer, List<int[]>> resultMapC) {
        this.resultMapC = resultMapC;
    }
}
