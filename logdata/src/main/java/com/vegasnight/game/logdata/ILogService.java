package com.vegasnight.game.logdata;

import com.alibaba.fastjson.JSONObject;

/**
 * @author 11
 * @date 2025/5/27 17:48
 */
public interface ILogService {
    void log(JSONObject jsonObject);
}
