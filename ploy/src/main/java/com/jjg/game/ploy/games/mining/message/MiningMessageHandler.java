package com.jjg.game.ploy.games.mining.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.games.mining.MiningConstant;
import com.jjg.game.ploy.games.mining.MiningService;
import org.springframework.stereotype.Component;

/** 挖矿小游戏协议入口。 */
@Component
@MessageType(MessageConst.MessageTypeDef.MINIGAME)
public class MiningMessageHandler {
    private final MiningService miningService;

    public MiningMessageHandler(MiningService miningService) {
        this.miningService = miningService;
    }

    @Command(MiningConstant.REQ_INFO)
    public void miningInfo(PlayerController playerController, ReqMiningInfo msg) {
        playerController.send(miningService.info(playerController.getPlayer()));
    }

    @Command(MiningConstant.REQ_ACTION)
    public void miningAction(PlayerController playerController, ReqMiningAction msg) {
        playerController.send(miningService.action(playerController.getPlayer(), msg));
    }

    @Command(MiningConstant.REQ_RANK)
    public void miningRank(PlayerController playerController, ReqMiningRank msg) {
        playerController.send(miningService.rank(playerController.getPlayer()));
    }

    @Command(MiningConstant.REQ_EXCHANGE_SHOP)
    public void miningExchangeShop(PlayerController playerController, ReqMiningExchangeShop msg) {
        playerController.send(miningService.exchangeShop(playerController.getPlayer()));
    }

    @Command(MiningConstant.REQ_BUNDLE_SHOP)
    public void miningBundleShop(PlayerController playerController, ReqMiningBundleShop msg) {
        playerController.send(miningService.bundleShop(playerController.getPlayer()));
    }

    @Command(MiningConstant.REQ_ACHIEVEMENTS)
    public void miningAchievements(PlayerController playerController, ReqMiningAchievements msg) {
        playerController.send(miningService.achievements(playerController.getPlayer()));
    }

    @Command(MiningConstant.REQ_DAILY_TASKS)
    public void miningDailyTasks(PlayerController playerController, ReqMiningDailyTasks msg) {
        playerController.send(miningService.dailyTasks(playerController.getPlayer()));
    }
}
