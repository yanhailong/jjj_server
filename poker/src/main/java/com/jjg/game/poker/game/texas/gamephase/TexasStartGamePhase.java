package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.core.utils.TipUtils;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.gamephase.BaseStartGamePhase;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasSeatStateChange;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.message.RoomMessageBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author lm
 * @date 2025/9/19 15:58
 */
public class TexasStartGamePhase extends BaseStartGamePhase<TexasGameDataVo> {

    public TexasStartGamePhase(TexasGameController gameController) {
        super(gameController, gameController.getGameDataVo().getId());
    }

    @Override
    public void nextPhase() {
        //设置当前游戏阶段为发牌
        if (gameController instanceof TexasGameController controller) {
            TexasPlayCardPhase gamePhase = new TexasPlayCardPhase(controller);
            controller.addPokerPhase(gamePhase);
            //通知场上信息
            PokerBuilder.buildNotifyPhaseChange(EGamePhase.PLAY_CART, -1);
        }
    }

    @Override
    public void onPhaseReset() {
        //当倒计时结束时，将强制将没有准备的玩家离开座位，玩家收到飘字提示300200098 由於您長時間沒有準備，已被請離座位
        NotifyTexasSeatStateChange notify = new NotifyTexasSeatStateChange();
        TreeMap<Integer, SeatInfo> seatInfoTreeMap = gameDataVo.getSeatInfo();
        if (seatInfoTreeMap.isEmpty()) {
            return;
        }
        if (gameController instanceof TexasGameController controller) {
            notify.playerChange = new ArrayList<>(seatInfoTreeMap.size());
            Set<Long> leaveSeatIds = new HashSet<>(seatInfoTreeMap.size());
            for (SeatInfo info : seatInfoTreeMap.values()) {
                if (info.isSeatDown() && !info.isJoinGame()) {
                    //未准备的站起
                    if (!info.isReady()) {
                        //修改座位状态
                        info.setSeatDown(false);
                        notify.playerChange.addAll(PokerBuilder.buildPlayerInfoList(gameDataVo.getGamePlayer(info.getPlayerId()), info, controller));
                        leaveSeatIds.add(info.getPlayerId());
                    } else {
                        //准备的设置为未准备
                        info.setReady(false);
                        //更新操作时间
                        controller.updatePlayerLatestOperateTime(info.getPlayerId());
                        notify.playerChange.addAll(PokerBuilder.buildPlayerInfoList(gameDataVo.getGamePlayer(info.getPlayerId()), info, controller));
                    }
                }
            }
            //通知房间的人座位变化
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notify));
            //通知具体的人提示
            for (Long levelSeatPlayerId : leaveSeatIds) {
                TipUtils.sendTip(levelSeatPlayerId, TipUtils.TipType.TOAST, 300200098);
            }
        }
    }
}
