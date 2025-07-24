package com.jjg.game.slots.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/7/23 17:31
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class ToServerMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressResultLibDao libDao;

    /**
     * 结果库变更
     *
     * @param playerController
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_SLOTS_LIB_CHANGE)
    public void reqConfigInfo(PlayerController playerController, NoticeSlotsLibChange req) {
        try{
            log.info("收到结果库变化的通知消息 gameType = {}",req.gameType);
            libDao.reloadLib();
        }catch (Exception e) {
            log.error("",e);
        }
    }
}
