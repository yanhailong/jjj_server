package com.jjg.game.hall.minigame.message;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.LuckyTreasureConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.hall.minigame.MinigameManager;
import com.jjg.game.hall.minigame.constant.MinigameConstant;
import com.jjg.game.hall.minigame.game.luckytreasure.message.req.*;
import com.jjg.game.hall.minigame.game.luckytreasure.message.res.*;
import com.jjg.game.hall.minigame.game.luckytreasure.service.LuckyTreasureService;
import com.jjg.game.hall.minigame.message.req.ReqMinigameList;
import com.jjg.game.hall.minigame.message.res.ResMinigameList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 小游戏协议入口
 */
@MessageType(MessageConst.MessageTypeDef.MINIGAME)
@Component
public class MinigameMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final MinigameManager minigameManager;
    private final LuckyTreasureService luckyTreasureService;
    private final GameFunctionService gameFunctionService;

    public MinigameMessageHandler(MinigameManager minigameManager, LuckyTreasureService luckyTreasureService, GameFunctionService gameFunctionService) {
        this.minigameManager = minigameManager;
        this.luckyTreasureService = luckyTreasureService;
        this.gameFunctionService = gameFunctionService;
    }

    /**
     * 请求获取开启的小游戏列表
     */
    @Command(MinigameConstant.Message.REQ_MINIGAME_LIST)
    public void gameList(PlayerController playerController, ReqMinigameList msg) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.MINI_GAME)) {
            return;
        }
        List<Integer> openGameList = minigameManager.getOpenGameList();
        ResMinigameList resMinigameList = new ResMinigameList(Code.SUCCESS);
        resMinigameList.setGameIdList(openGameList);
        playerController.send(resMinigameList);
        log.debug("返回开启的小游戏列表 res = {}", JSON.toJSONString(resMinigameList));
    }

    /**
     * 请求夺宝奇兵详情
     */
    @Command(LuckyTreasureConstant.Message.REQ_LUCKY_TREASURE)
    public void reqInfo(PlayerController playerController, ReqLuckyTreasureInfo msg) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.LUCK_TREASURE)) {
            return;
        }
        ResLuckyTreasureInfo response = luckyTreasureService.getLuckyTreasureInfo(playerController, msg.getCurrPage(), msg.getPageSize());
        playerController.send(response);
        log.debug("返回夺宝奇兵详情 res = {}", JSON.toJSONString(response));
    }

    /**
     * 请求购买夺宝奇兵道具
     */
    @Command(LuckyTreasureConstant.Message.REQ_BUY_LUCKY_TREASURE)
    public void buy(PlayerController playerController, ReqBuyLuckyTreasure msg) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.LUCK_TREASURE)) {
            return;
        }
        CommonResult<ResBuyLuckyTreasure> result = luckyTreasureService.buyLuckyTreasure(playerController, msg.getIssueNumber(), msg.getCount());
        if (result.code != Code.SUCCESS) {
//            playerController.send(new ResBuyLuckyTreasure(result.code));
            log.debug("购买夺宝奇兵道具失败 playerId = {},code = {}", playerController.playerId(), result.code);
        } else {
            playerController.send(result.data);
            log.debug("返回购买夺宝奇兵道具 res = {}", JSON.toJSONString(result.data));
        }

    }

    /**
     * 请求领取夺宝奇兵道具
     */
    @Command(LuckyTreasureConstant.Message.REQ_RECEIVE_LUCKY_TREASURE)
    public void receive(PlayerController playerController, ReqReceiveLuckyTreasure msg) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.LUCK_TREASURE)) {
            return;
        }
        ResReceiveLuckyTreasure response = new ResReceiveLuckyTreasure(Code.SUCCESS);
        boolean reward = luckyTreasureService.receiveReward(playerController, msg.getIssueNumber());
        if (!reward) {
            response.code = Code.FAIL;
        }
        playerController.send(response);
        log.debug("返回领取夺宝奇兵道具 res = {}", JSON.toJSONString(response));
    }

    /**
     * 请求查看自己参加的所有的幸运夺宝(包含已结束的)
     */
    @Command(LuckyTreasureConstant.Message.REQ_LUCKY_TREASURE_RECORD)
    public void record(PlayerController playerController, ReqLuckyTreasureRecord msg) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.LUCK_TREASURE)) {
            return;
        }
        ResLuckyTreasureRecord response = luckyTreasureService.getLuckyTreasureRecord(playerController, msg.getCurrPage(), msg.getPageSize());
        log.debug("请求查看自己参加的所有的幸运夺宝(包含已结束的) res = {}", JSON.toJSONString(response));
        playerController.send(response);
    }

    /**
     * 请求查看夺宝奇兵所有的开奖历史记录
     */
    @Command(LuckyTreasureConstant.Message.REQ_LUCKY_TREASURE_AWARD_HISTORY)
    public void history(PlayerController playerController, ReqLuckyTreasureHistory msg) {
        if (!gameFunctionService.checkGameFunctionOpen(playerController, EFunctionType.LUCK_TREASURE)) {
            return;
        }
        ResLuckyTreasureHistory response = luckyTreasureService.getLuckyTreasureHistory(msg.getCurrPage(), msg.getPageSize());
        log.debug("请求查看夺宝奇兵所有的开奖历史记录 res = {}", JSON.toJSONString(response));
        playerController.send(response);
    }


}
