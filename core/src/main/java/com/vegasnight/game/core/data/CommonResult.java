package com.vegasnight.game.core.data;

import com.vegasnight.game.core.constant.Code;

/**
 * @author 11
 * @date 2025/5/26 11:34
 */
public class CommonResult<T>{
    public int code;
    public T data;

    public CommonResult() {
    }

    public CommonResult(int code) {
        this.code = code;
    }

    public CommonResult(int code, T data) {
        this.code = code;
        this.data = data;
    }

    public boolean success(){
        return this.code == Code.SUCCESS;
    }
}