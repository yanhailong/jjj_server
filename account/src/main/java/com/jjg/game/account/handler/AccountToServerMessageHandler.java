package com.jjg.game.account.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.pb.gm.NotifyLoadBlackList;
import com.jjg.game.core.service.BlackListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/10/22 13:42
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class AccountToServerMessageHandler extends CoreToServerMessageHandler {
    @Autowired
    private BlackListService blackListService;

    /**
     * gm推送重新加载黑名单
     */
    @Command(MessageConst.ToServer.NOTIFY_LOAD_BLACK_LIST)
    public void notifyLoadBlackList(NotifyLoadBlackList notify) {
        try {
            log.info("收到gm推送重新加载黑名单");

            if(notify.loadId){
                blackListService.loadAllBlackId();
            }
            if(notify.loadIp){
                blackListService.loadAllBlackIp();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
