package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.slots.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResChooseFreeModel;
import com.jjg.game.slots.game.dollarexpress.pb.ResConfigInfo;
import com.jjg.game.slots.game.dollarexpress.pb.ResStartGame;
import com.jjg.game.slots.game.dollarexpress.sample.GameDataManager;
import com.jjg.game.slots.game.dollarexpress.sample.bean.DollarExpressWareHouseCfg;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
@Component
public class DollarExpressSendMessageManager extends BaseSendMessageManager {

    /**
     * 发送配置信息
     * @param playerController
     */
    public void sendConfigMessage(PlayerController playerController,int wareId) {
        SendInfo sendInfo = new SendInfo();
        ResConfigInfo res = new ResConfigInfo(Code.SUCCESS);

        DollarExpressWareHouseCfg wareHouseCfg = GameDataManager.getDollarExpressWareHouseCfg(wareId);
        if(wareHouseCfg != null) {
            res.stakeList = wareHouseCfg.getBetList();
            res.defaultBet = res.stakeList.get(wareHouseCfg.getDefaultBet());
        }else {
            log.warn("没有该场次配置 wareId = {}",wareId);
            res.code = Code.NOT_FOUND;
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController,sendInfo,"返回配置信息",false);
    }

    /**
     * 发送游戏结果
     * @param playerController
     * @param gameRunInfo
     */
    public void sendStartGameMessage(PlayerController playerController, GameRunInfo gameRunInfo){
        SendInfo sendInfo = new SendInfo();

        ResStartGame res = new ResStartGame(gameRunInfo.getCode());
        if(gameRunInfo.success()){
            res.iconList = Arrays.stream(gameRunInfo.getIntArray()).boxed().collect(Collectors.toList());
            res.resultLineInfoList = gameRunInfo.getResultLineInfoList();
            res.allWinGold = gameRunInfo.getAllWinGold();
            res.specialType = gameRunInfo.getSpecialType();
            res.freeCount = gameRunInfo.getFreeCount();
            res.goldTrainInFree = gameRunInfo.isGoldTrainInFree();
            res.trainInfoList = gameRunInfo.getTrainInfoList();
            res.safeBoxInfoList = gameRunInfo.getSafeBoxInfoList();
        }else {
            log.debug("开始游戏错误  playerId={},code={}",playerController.playerId(),gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController,sendInfo,"返回押注结果",false);
    }

    /**
     * 返回选择免费游戏类型
     * @param playerController
     * @param gameRunInfo
     */
    public void sendChooseFreeTypeMessage(PlayerController playerController, GameRunInfo gameRunInfo){
        SendInfo sendInfo = new SendInfo();

        ResChooseFreeModel res = new ResChooseFreeModel(gameRunInfo.getCode());
        if(gameRunInfo.success()){
            res.trainInfo = gameRunInfo.getTrainInfo();
        }else {
            log.debug("返回选择免费游戏类型错误  playerId={},code={}",playerController.playerId(),gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController,sendInfo,"返回选择免费游戏类型",false);
    }
}
