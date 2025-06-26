package com.jjg.game.dollarexpress.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.dollarexpress.pb.NoticeConfigInfo;
import com.jjg.game.dollarexpress.pb.ResChooseFreeModel;
import com.jjg.game.dollarexpress.pb.ResStartGame;
import com.jjg.game.dollarexpress.sample.GameDataManager;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressWareHouseCfg;
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
        DollarExpressWareHouseCfg wareHouseCfg = GameDataManager.getDollarExpressWareHouseCfg(wareId);
        if(wareHouseCfg != null) {
            log.warn("没有该场次配置 wareId = {}",wareId);
            return;
        }

        SendInfo sendInfo = new SendInfo();
        NoticeConfigInfo notice = new NoticeConfigInfo();
        notice.stakeList = wareHouseCfg.getBetList();
        notice.defaultBet = notice.stakeList.get(wareHouseCfg.getDefaultBet());

        sendInfo.addPlayerMsg(playerController.playerId(), notice);
        sendInfo.getLogMessage().add(notice);
        sendRun(playerController,sendInfo,"推送配置信息",false);
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
