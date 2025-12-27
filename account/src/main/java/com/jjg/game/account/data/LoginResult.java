package com.jjg.game.account.data;

import com.jjg.game.core.data.CommonResult;

public class LoginResult<T> extends CommonResult<T> {
    //是否为注册
    private boolean register;

    public LoginResult(int code) {
        this.code = code;
    }

    public boolean isRegister() {
        return register;
    }

    public void setRegister(boolean register) {
        this.register = register;
    }
}
