package com.jjg.game.activity.common.message;

import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.core.constant.Code;

/**
 * @author lm
 * @date 2025/9/5 10:13
 */
public class ActivityBuilder {

    private final static AbstractResponse defaultResponse = new AbstractResponse(Code.ERROR_REQ);

    public static AbstractResponse getDefaultResponse() {
        return defaultResponse;
    }
}
