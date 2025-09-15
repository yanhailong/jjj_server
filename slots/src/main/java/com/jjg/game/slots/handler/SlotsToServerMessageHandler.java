package com.jjg.game.slots.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.handler.CoreToServerMessageHandler;
import com.jjg.game.core.pb.gm.NotifyGenrateLib;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.slots.data.SpecialResultLibCacheData;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGameManager;
import com.jjg.game.slots.game.dollarexpress.manager.DollarExpressGenerateManager;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import com.jjg.game.slots.pb.NoticeSlotsLibChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/7/23 17:31
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE)
public class SlotsToServerMessageHandler extends CoreToServerMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsFactoryManager slotsFactoryManager;

    /**
     * 生成结果库
     *
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_GENERATE_LIB)
    public void genrateLib(NotifyGenrateLib req) {
        try{
            log.info("收到生成结果库的请求 gameType = {},count = {}",req.gameType,req.count);

            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(req.gameType);
            if(gameManager == null){
                log.debug("获取 gameManager 为空，生成结果库失败 gameType = {},count = {}",req.gameType,req.count);
                return;
            }

            gameManager.addGenerateLibEvent(countMap(req.gameType, req.count));

            log.info("添加生成结果库事件成功  gameType = {},count = {}",req.gameType,req.count);
        }catch (Exception e) {
            log.error("",e);
        }
    }

    /**
     * 结果库变更
     *
     * @param req
     */
    @Command(MessageConst.ToServer.NOTICE_SLOTS_LIB_CHANGE)
    public void reqConfigInfo(NoticeSlotsLibChange req) {
        try{
            log.info("收到结果库变化的通知消息 gameType = {},changeType = {}",req.gameType,req.changeType);
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(req.gameType);
            if(gameManager == null){
                log.debug("获取 gameManager 为空，处理specialLib变化失败 gameType = {},changeType = {}",req.gameType,req.changeType);
                return;
            }

            List<SpecialResultLibCfg> cfgList = new ArrayList<>();
            if(req.changeType == 1){
                req.libCfgList.forEach(str -> cfgList.add(JSON.parseObject(str, SpecialResultLibCfg.class)));
            }

            gameManager.notifySpecialResultLibCacheData(cfgList);
        }catch (Exception e) {
            log.error("",e);
        }
    }

    private Map<Integer,Integer> countMap(int gameType, int count){
        Map<Integer,Integer> countMap = new HashMap<>();
        GameDataManager.getSpecialModeCfgMap().forEach((k, v)->{
            if(v.getGameType() == gameType) {
                countMap.put(v.getType(), count);
            }
        });
        return countMap;
    }
}
