package com.jjg.game.core.listener;

import com.jjg.game.common.proto.Pair;

/**
 * @author 11
 * @date 2025/6/11 16:34
 */
public interface GmListener {
    //true 阻止传播 string 执行结果
    Pair<Boolean,String> gm(long playerId, String cmd, String params);
}
