package com.jjg.game.dollarexpress.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.dollarexpress.pb.ResChooseWare;
import com.jjg.game.dollarexpress.pb.ResStartGame;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
@Component
public class SendMessageManager extends CoreSendMessageManager {
    /**
     * 发送选择房间的返回信息
     * @param playerController
     * @param gameRunInfo
     */
    public void sendChooseWareMessage(PlayerController playerController, GameRunInfo gameRunInfo){
        SendInfo sendInfo = new SendInfo();

        ResChooseWare res = new ResChooseWare(gameRunInfo.getCode());
        if(gameRunInfo.success()){
            res.stakeList = gameRunInfo.getLongList();
        }else {
            log.debug("选择场次错误  playerId={},code={}",playerController.playerId(),gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController,sendInfo,"返回押注结果",false);
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
            res.specialId = gameRunInfo.getSpecialId();
        }else {
            log.debug("开始游戏错误  playerId={},code={}",playerController.playerId(),gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController,sendInfo,"返回押注结果",false);
    }
}
