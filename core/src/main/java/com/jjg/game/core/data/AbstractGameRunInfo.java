package com.jjg.game.core.data;

import com.jjg.game.core.constant.Code;

/**
 * @author 11
 * @date 2025/6/12 17:38
 */
public class AbstractGameRunInfo {
    private int code;

    public AbstractGameRunInfo(int code) {
        this.code = code;
    }

    public boolean success(){
        return this.code == Code.SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }
}
