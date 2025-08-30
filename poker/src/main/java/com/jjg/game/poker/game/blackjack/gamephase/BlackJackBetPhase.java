package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.NotifyTableExitRoom;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BaseBetPhase;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPhaseChange;
import com.jjg.game.poker.game.common.message.reps.NotifyPokerPlayerChange;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.listener.RoomEventListener;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.bean.Room_ChessCfg;

import java.util.*;

import static com.jjg.game.poker.game.blackjack.constant.BlackJackConstant.Common.BET_FIX_TIME;

/**
 * @author lm
 * @date 2025/7/28 14:16
 */
public class BlackJackBetPhase extends BaseBetPhase<BlackJackGameDataVo> {

    private final RoomEventListener roomEventListener;
    private final long id;

    public BlackJackBetPhase(AbstractPhaseGameController<Room_ChessCfg, BlackJackGameDataVo> gameController, long id) {
        super(gameController);
        roomEventListener = CommonUtil.getContext().getBean(RoomEventListener.class);
        this.id = id;
    }


    @Override
    public void nextPhase() {
        //设置当前游戏阶段为发牌
        if (gameController instanceof BlackJackGameController controller) {
            BlackJackPlayCardPhase gamePhase = new BlackJackPlayCardPhase(controller);
            controller.addPokerPhase(gamePhase);
            //通知场上信息
            PokerBuilder.buildNotifyPhaseChange(EGamePhase.PLAY_CART, -1);
        }
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();

    }

    @Override
    public void phaseFinish() {
        if (id != gameDataVo.getId()) {
            log.info("该定时器已经取消或者不需要再执行 id:{}", id);
            return;
        }
        if (gameController instanceof BlackJackGameController controller) {
            //生成执行列表
            TreeMap<Integer, SeatInfo> seatInfoTreeMap = gameDataVo.getSeatInfo();
            List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
            controller.genPlayerSeatInfoList(seatInfoTreeMap, playerSeatInfoList);
            //没下注的人直接踢掉
            List<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList();
            List<PlayerSeatInfo> noBetPlayer = new ArrayList<>(playerSeatInfo.size());
            long timeMillis = System.currentTimeMillis();
            Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
            for (PlayerSeatInfo seatInfo : playerSeatInfo) {
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
                if (Objects.nonNull(gamePlayer)) {
                    if (!baseBetInfo.containsKey(gamePlayer.getId()) && timeMillis - BET_FIX_TIME > gamePlayer.getPokerPlayerGameData().getJoinTime()) {
                        noBetPlayer.add(seatInfo);
                    }
                }
            }
            for (PlayerSeatInfo info : noBetPlayer) {
                Map<Long, PlayerController> playerControllers = gameController.getRoomController().getPlayerControllers();
                PlayerController playerController = playerControllers.get(info.getPlayerId());
                if (Objects.nonNull(playerController)) {
                    SeatInfo seatInfo = gameDataVo.getSeatInfo().get(info.getSeatId());
                    if (Objects.nonNull(seatInfo)) {
                        seatInfo.setSeatDown(false);
                        NotifyPokerPlayerChange notifyPokerPlayerChange = new NotifyPokerPlayerChange();
                        notifyPokerPlayerChange.pokerPlayerInfo = PokerBuilder.buildPlayerInfo(gameDataVo.getGamePlayer(seatInfo.getPlayerId()), seatInfo, gameDataVo);
                        broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendAllPlayer(notifyPokerPlayerChange).exceptPlayer(seatInfo.getPlayerId()));
                    }
                    log.info("玩家：{}  未押注直接踢掉", info.getPlayerId());
                    NotifyTableExitRoom timeNoOperate = new NotifyTableExitRoom();
                    timeNoOperate.langId = 16008;
                    broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().sendPlayer(playerController.playerId(), timeNoOperate));
                }
                playerSeatInfo.remove(info);
            }
            if (gameDataVo.canStartGame() && !gameDataVo.getAllBetInfo().isEmpty()) {
                //进入下个阶段
                nextPhase();
            } else {
                //重新等待
                gameDataVo.getPlayerSeatInfoList().clear();
                controller.goBackWaitReadyPhase();
                NotifyPokerPhaseChange notifyPokerPhaseChange = PokerBuilder.buildNotifyPhaseChange(EGamePhase.WAIT_READY, -1);
                broadcastMsgToRoom(notifyPokerPhaseChange);
            }
        }
    }
}
