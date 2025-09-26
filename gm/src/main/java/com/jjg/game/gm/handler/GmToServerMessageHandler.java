package com.jjg.game.gm.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.pb.ResActivityInfos;
import com.jjg.game.core.utils.NodeCommunicationUtil;
import org.springframework.stereotype.Component;

/**
 * @author lm
 * @date 2025/9/25 11:43
 */

@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class GmToServerMessageHandler {

    /**
     * gm请求活动数据
     */
    @Command(MessageConst.ToServer.RES_ACTIVITY_INFOS)
    public void resActivityInfos(ResActivityInfos res) {
        NodeCommunicationUtil.addResponse(res.reqId, res.activityJsonStr);
    }
}
