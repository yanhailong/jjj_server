package com.jjg.game.slots.game.cleopatra;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/8/26 20:58
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CLEOPATRA)
public class CleopatraMessageHandler implements GmListener {
    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        return null;
    }
}
