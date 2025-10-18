package com.jjg.game.core.base.condition.check.record;

import java.util.List;

/**
 * @author lm
 * @date 2025/10/16 18:02
 */
public class PlayerSampleParam extends BaseCheckParam {
    private int id;
    private List<Long> paramList;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Long> getParamList() {
        return paramList;
    }

    public void setParamList(List<Long> paramList) {
        this.paramList = paramList;
    }
}
