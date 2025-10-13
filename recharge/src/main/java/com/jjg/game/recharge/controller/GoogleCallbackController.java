package com.jjg.game.recharge.controller;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author 11
 * @date 2025/9/22 19:32
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "googlepay")
public class GoogleCallbackController extends AbstractCallbackController{

    /**
     * 回调
     *
     * @return
     */
    @RequestMapping("callback")
    public String callback(@RequestParam Map<String,String> map) {
        //TODO 进行校验

//        payCallback(order);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", 200);
        jsonObject.put("msg", "谷歌支付回调成功");

        return jsonObject.toJSONString();
    }
}
