package com.jjg.game.account.vo;

/**
 * 第三方登录开关配置
 * @author 11
 * @date 2025/10/21 17:18
 */
public class ThirdLoginConfigVo {
    //1.游客  2.google  3.apple  4.facebook  5.phone
    private int type;
    //是否开启
    private boolean open;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }
}
