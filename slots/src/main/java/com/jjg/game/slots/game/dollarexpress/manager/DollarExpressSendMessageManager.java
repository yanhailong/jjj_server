package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.SendInfo;
import com.jjg.game.core.manager.BaseSendMessageManager;
import com.jjg.game.slots.data.DollarInfo;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConstant;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressAwardLineInfo;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressGameRunInfo;
import com.jjg.game.slots.game.dollarexpress.data.Train;
import com.jjg.game.slots.game.dollarexpress.pb.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 11
 * @date 2025/6/12 17:21
 */
@Component
public class DollarExpressSendMessageManager extends BaseSendMessageManager {

    /**
     * 发送游戏结果
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendStartGameMessage(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();

        ResStartGame res = new ResStartGame(gameRunInfo.getCode());
        if (gameRunInfo.success()) {
            //总计获得金币
            res.allWinGold = gameRunInfo.getAllWinGold();
            //当前状态
            res.status = gameRunInfo.getStatus();
            //图标信息
            res.iconList = IntStream.range(1, 21).map(i -> gameRunInfo.getIconArr()[i]).boxed().collect(Collectors.toList());
            //中奖线信息
            if (gameRunInfo.getAwardLineInfos() != null && !gameRunInfo.getAwardLineInfos().isEmpty()) {
                res.resultLineInfoList = new ArrayList<>(gameRunInfo.getAwardLineInfos().size());
                for (DollarExpressAwardLineInfo lineInfo : gameRunInfo.getAwardLineInfos()) {
                    ResultLineInfo resultLineInfo = new ResultLineInfo();
                    resultLineInfo.id = lineInfo.getId();
                    resultLineInfo.sameCount = lineInfo.getSameCount();
                    resultLineInfo.times = lineInfo.getBaseTimes();
                    resultLineInfo.winGold = gameRunInfo.getBet() * lineInfo.getBaseTimes();
                    res.resultLineInfoList.add(resultLineInfo);
                }
            }
            //火车
            if (gameRunInfo.getTrainList() != null && !gameRunInfo.getTrainList().isEmpty()) {
                res.trainInfoList = new ArrayList<>(gameRunInfo.getTrainList().size());
                for (Train t : gameRunInfo.getTrainList()) {
                    TrainInfo trainInfo = new TrainInfo();
                    trainInfo.type = t.getTrainIconId();
                    if (t.getCoachs() != null && !t.getCoachs().isEmpty()) {
                        trainInfo.goldList = new ArrayList<>(t.getCoachs().size());
                        for (int[] arr : t.getCoachs()) {
                            trainInfo.goldList.add(gameRunInfo.getBet() * arr[1]);
                        }
                    }
                    trainInfo.pool = t.getPoolId() > 0;
                }
            }
            //美元信息
            res.dollarsInfo = gameRunInfo.getDollarsInfo();
            res.totalDollars = gameRunInfo.getTotalDollars();
        } else {
            log.debug("开始游戏错误  playerId={},code={}", playerController.playerId(), gameRunInfo.getCode());
        }

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回押注结果", false);
    }


    /**
     * 发送二选一
     *
     * @param playerController
     * @param gameRunInfo
     */
    public void sendChooseOneMessage(PlayerController playerController, DollarExpressGameRunInfo gameRunInfo) {
        SendInfo sendInfo = new SendInfo();
        ResChooseFreeModel res = new ResChooseFreeModel(gameRunInfo.getCode());

        sendInfo.addPlayerMsg(playerController.playerId(), res);
        sendInfo.getLogMessage().add(res);
        sendRun(playerController, sendInfo, "返回二选一结果", false);
    }
}
