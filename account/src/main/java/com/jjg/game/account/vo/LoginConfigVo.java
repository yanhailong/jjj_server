package com.jjg.game.account.vo;

import java.util.List;

/**
 * @author 11
 * @date 2026/1/26
 */
public class LoginConfigVo {
    //渠道相关配置
    private List<ThirdLoginConfigVo> channleConfigList;
    //客服地址
    private String customerUrl;

    public List<ThirdLoginConfigVo> getChannleConfigList() {
        return channleConfigList;
    }

    public void setChannleConfigList(List<ThirdLoginConfigVo> channleConfigList) {
        this.channleConfigList = channleConfigList;
    }

    public String getCustomerUrl() {
        return customerUrl;
    }

    public void setCustomerUrl(String customerUrl) {
        this.customerUrl = customerUrl;
    }
}
