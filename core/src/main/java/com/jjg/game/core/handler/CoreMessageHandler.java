package com.jjg.game.core.handler;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.pb.ReqGm;
import com.jjg.game.core.pb.ResGm;
import com.jjg.game.core.service.CorePlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author 11
 * @date 2025/6/11 16:09
 */
@Component
@MessageType(MessageConst.CoreMessage.TYPE)
public class CoreMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private CoreSendMessageManager coreSendMessageManager;

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

            String[] arr = req.order.split(" ");
            if(arr.length < 1){
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误2，使用gm失败 playerId = {},order = {}", playerController.playerId(),req.order);
                return;
            }

            String cmd = arr[0];
            String params = arr.length > 1 ? arr[1] : null;

            if("addGold".equals(cmd)){
                addGold(res, playerController, req.order,params);
            }else if("addDiamond".equals(cmd)){
                addDiamond(res, playerController, req.order,params);
            }else {
                Map<String, GmListener> map = CommonUtil.getContext().getBeansOfType(GmListener.class);
                map.forEach((k,v) -> {
                    res.result = v.gm(playerController,cmd,params);
                    playerController.send(res);
                });
                log.info("执行gm命令成功1 playerId = {},order = {}", playerController.playerId(),req.order);
                return;
            }
            playerController.send(res);
            log.info("执行gm命令成功2 playerId = {},order = {}", playerController.playerId(),req.order);
        }catch (Exception e){
            log.error("", e);
            res.code = Code.EXCEPTION;
            playerController.send(res);
        }
    }

    /**
     * gm修改金币
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    public void addGold(ResGm res,PlayerController playerController,String order,String params) throws Exception{
        if(params == null || params.isEmpty()){
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(),order);
            return;
        }

        Long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addGold(playerController.playerId(), num, "gmAdd", null);
        if(!result.success()){
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(),order,result.code);
            return;
        }

        coreSendMessageManager.packMoneyChangeMessage(playerController,result.data.getGold(),result.data.getDiamond());
    }

    /**
     * gm修改钻石
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    public void addDiamond(ResGm res,PlayerController playerController,String order,String params) throws Exception{
        if(params == null || params.isEmpty()){
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(),order);
            return;
        }

        Long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addDiamond(playerController.playerId(), num, "gmAdd", null);
        if(!result.success()){
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}",playerController.playerId(),order,result.code);
            return;
        }
        coreSendMessageManager.packMoneyChangeMessage(playerController,result.data.getGold(),result.data.getDiamond());
    }
}
