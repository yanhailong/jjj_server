package com.jjg.game.slots.service;

import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.res.NotifyGuideTrigger;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import com.jjg.game.slots.manager.SlotsRPCLinkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/** SLOT 场景的新手引导触发与通知。 */
@Service
public class SlotsGuideService {
    private static final Logger log = LoggerFactory.getLogger(SlotsGuideService.class);

    @Autowired
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private SlotsRPCLinkManager slotsRPCLinkManager;

    /** 具体 SLOT 进场响应完成后再延迟检查，确保客户端已经建立对应游戏界面。 */
    public void notifyAfterGameEnter(PlayerController playerController) {
        long playerId = playerController.playerId();
        long workId = playerController.getSession().getWorkId();
        WheelTimerUtil.schedule(() ->
                        PlayerExecutorGroupDisruptor.getDefaultExecutor().publishWithFallback(
                                workId, 0, new BaseHandler<String>() {
                                    @Override
                                    public void action() {
                                        notifyNow(playerController);
                                    }
                                }.setHandlerParamWithSelf("slots guide after game enter")),
                SimConstant.GuideTiming.SLOTS_ENTER_TRIGGER_DELAY_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    private void notifyNow(PlayerController playerController) {
        long playerId = playerController.playerId();
        if (playerController.getSession() == null
                || playerController.getSession().getReference() != playerController) {
            log.info("跳过已离开SLOT节点的引导场景检查 playerId={}", playerId);
            return;
        }
        SlotsPlayerGameData playerGameData = slotsFactoryManager.getPlayerGameData(playerId);
        if (playerGameData == null || playerGameData.getPlayerController() != playerController) {
            log.warn("SLOT进场后检查新手引导失败，未找到当前玩家游戏数据 playerId={}", playerId);
            return;
        }
        CommonResult<List<Integer>> result = slotsRPCLinkManager.enterGuidePath(
                playerGameData, SimConstant.GuidePath.SLOTS);
        if (result == null || !result.success()) {
            log.warn("SLOT进场后检查新手引导失败 playerId={},code={}",
                    playerId, result == null ? null : result.code);
            return;
        }
        if (result.data == null || result.data.isEmpty()) {
            log.info("SLOT进场后没有待触发的新手引导 playerId={}", playerId);
            return;
        }
        NotifyGuideTrigger notify = new NotifyGuideTrigger(Code.SUCCESS);
        notify.guideGroupIds = result.data;
        playerController.send(notify);
        log.info("玩家进入SLOT游戏后触发新手引导 playerId={},groups={}", playerId, result.data);
    }
}
