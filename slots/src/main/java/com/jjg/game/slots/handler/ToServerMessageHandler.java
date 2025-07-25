package com.jjg.game.slots.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.data.SpecialResultLibCacheData;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGameManager;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import com.jjg.game.slots.sample.bean.SpecialResultLibCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
    @Autowired
    private DollarExpressGameManager gameManager;

    /**
     * 结果库变更
     *
     * @param playerController
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_SLOTS_LIB_CHANGE)
    public void reqConfigInfo(PlayerController playerController, NoticeSlotsLibChange req) {
        try{
            log.info("收到结果库变化的通知消息 gameType = {},changeType = {}",req.gameType,req.changeType);
            if(req.changeType == 1){
                List<SpecialResultLibCfg> cfgList = new ArrayList<>();
                req.libCfgList.forEach(str -> cfgList.add(JSON.parseObject(str, SpecialResultLibCfg.class)));
                SpecialResultLibCacheData data = gameManager.calSpecialResultLibCacheData(cfgList);
                gameManager.updateSpecialResultLibCacheData(data);
            }

            libDao.reloadLib();
        }catch (Exception e) {
            log.error("",e);
        }
    }
}
