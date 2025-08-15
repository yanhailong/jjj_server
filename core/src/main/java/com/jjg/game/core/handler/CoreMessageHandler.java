package com.jjg.game.core.handler;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.pb.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.PlayerPackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/11 16:09
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE)
public class CoreMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private CoreSendMessageManager coreSendMessageManager;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CoreMarqueeManager marqueeManager;

    /**
     * @param playerController
     * @param req
     */
    @Command(MessageConst.CoreMessage.REQ_GM)
    public void reqGm(PlayerController playerController, ReqGm req){
        ResGm res = new ResGm(Code.SUCCESS);
        try{
            if(!nodeConfig.isGm()){
                res.code = Code.FORBID;
                playerController.send(res);
                log.debug("gm功能已经关闭 playerId = {}", playerController.playerId());
                return;
            }

            if(req.order.length() < 1){
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误，使用gm失败 playerId = {},order = {}", playerController.playerId(),req.order);
                return;
            }


            String[] arr = req.order.trim().split("\\s+");
            if(arr.length < 1){
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误2，使用gm失败 playerId = {},order = {}", playerController.playerId(),req.order);
                return;
            }

            if(arr.length < 2){
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误2，gm命令长度必须大于2 playerId = {},order = {}", playerController.playerId(),req.order);
                return;
            }

            String cmd = arr[0];
            String params = arr[1];

            if("addGold".equalsIgnoreCase(cmd)){
                addGold(res, playerController, req.order,params);
                return;
            }

            if("addDiamond".equalsIgnoreCase(cmd)){
                addDiamond(res, playerController, req.order,params);
                return;
            }

            if("setVip".equalsIgnoreCase(cmd)){
                setVip(res,playerController,req.order,params);
                return;
            }

            if("addItem".equalsIgnoreCase(cmd)) {
                addItem(res, playerController, arr);
                return;
            }

            if("playerWinMarquee".equalsIgnoreCase(cmd)){
                marqueeManager.playerWinMarquee("shiyi",17001,100100026,500000);
                return;
            }

            int notFound = 0;
            Map<String, GmListener> map = CommonUtil.getContext().getBeansOfType(GmListener.class);
            for(Map.Entry<String, GmListener> en : map.entrySet()){
                CommonResult<String> gmResult = en.getValue().gm(playerController, arr);
                if(gmResult == null){
                    continue;
                }

                if(gmResult.success()){
                    res.result = gmResult.data;
                    playerController.send(res);
                    log.info("执行gm命令成功1 playerId = {},order = {}", playerController.playerId(),req.order);
                    return;
                }

                if(gmResult.code == Code.NOT_FOUND){
                    notFound++;
                }
            }

            if(notFound == map.size()){
                log.debug("未找到该命令 playerId = {},order = {}", playerController.playerId(),req.order);
                res.code = Code.NOT_FOUND;
            }else {
                res.code = Code.FAIL;
            }
        }catch (Exception e){
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * @param playerController
     * @param req
     */
    @Command(MessageConst.CoreMessage.REQ_MARQUEE)
    public void reqMarquee(PlayerController playerController, ReqMarquee req){
        ResMarquee res = new ResMarquee(Code.SUCCESS);
        try{
            Marquee currentMarquee = marqueeManager.getCurrentMarquee();
            if(currentMarquee != null){
                res.id = currentMarquee.getId();
                res.content = currentMarquee.getContent();
                res.interval = currentMarquee.getInterval();
                res.startTime = currentMarquee.getStartTime();
                res.endTime = currentMarquee.getEndTime();
                res.type = marqueeManager.getClientShowGarqueeType(currentMarquee.getType());
                res.langId = currentMarquee.getLangId();
                res.showTime = currentMarquee.getShowTime();

                if(currentMarquee.getParams() != null && !currentMarquee.getParams().isEmpty()){
                    res.params = new ArrayList<>(currentMarquee.getParams().size());

                    currentMarquee.getParams().forEach(p -> {
                        MarqueeLangParamInfo info = new MarqueeLangParamInfo();
                        info.type = p.getType();
                        info.param = p.getParam();
                        res.params.add(info);
                    });
                }
            }
            log.debug("获取当前跑马灯 playerId = {},marqueeId = {}", playerController.playerId(),res.id);
        }catch (Exception e){
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }



    /**
     * gm修改金币
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    private void addGold(ResGm res,PlayerController playerController,String order,String params) throws Exception{
        if(params == null || params.isEmpty()){
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(),order);
            return;
        }

        Long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addGold(playerController.playerId(), num, "gmAddGold", null);
        if(!result.success()){
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(),order,result.code);
            return;
        }
        playerController.getPlayer().setGold(result.data.getGold());
        coreSendMessageManager.packMoneyChangeMessage(playerController,result.data.getGold(),result.data.getDiamond(),result.data.getVipLevel());
    }

    /**
     * gm修改钻石
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    private void addDiamond(ResGm res,PlayerController playerController,String order,String params) throws Exception{
        if(params == null || params.isEmpty()){
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(),order);
            return;
        }

        Long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addDiamond(playerController.playerId(), num, "gmAddDiamond", null);
        if(!result.success()){
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}",playerController.playerId(),order,result.code);
            return;
        }
        playerController.getPlayer().setDiamond(result.data.getDiamond());
        coreSendMessageManager.packMoneyChangeMessage(playerController,result.data.getGold(),result.data.getDiamond(),result.data.getVipLevel());
    }

    /**
     * gm修改vip等级
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    private void setVip(ResGm res,PlayerController playerController,String order,String params) throws Exception{
        if(params == null || params.isEmpty()){
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(),order);
            return;
        }

        Integer num = Integer.parseInt(params);
        CommonResult<Player> result = playerService.setVip(playerController.playerId(), num, "gmSetVip", null);
        if(!result.success()){
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {},params = {}",playerController.playerId(),order,result.code,params);
            return;
        }
        playerController.getPlayer().setVipLevel(result.data.getVipLevel());
        coreSendMessageManager.packMoneyChangeMessage(playerController,result.data.getGold(),result.data.getDiamond(),result.data.getVipLevel());
    }

    private void addItem(ResGm res,PlayerController playerController,String[] orders) throws Exception{
        if(orders.length < 3){
            res.code = Code.PARAM_ERROR;
            log.debug("orders 为空，使用gm失败 playerId = {},orders = {}", playerController.playerId(),orders);
            return;
        }

        int itemId = Integer.parseInt(orders[1]);
        int count = Integer.parseInt(orders[2]);

        CommonResult<PlayerPack> result = playerPackService.addItem(playerController.playerId(), itemId, count, 999);
        if(!result.success()){
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},orders = {}",playerController.playerId(),orders);
            return;
        }
        playerController.send(res);
        log.debug("添加道具成功 playerId = {},orders = {}", playerController.playerId(),orders);
    }
}
