package com.jjg.game.poker.game.blackjack.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.poker.game.blackjack.room.BlackJackGameController;
import com.jjg.game.poker.game.blackjack.room.data.BlackJackGameDataVo;
import com.jjg.game.poker.game.common.PokerBuilder;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.gamephase.BaseBetPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.listener.RoomEventListener;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public void phaseFinish() {
        if (id != gameDataVo.getId()) {
            log.info("该定时器已经取消或者不需要再执行 id:{}", id);
            return;
        }
        //没下注的人直接踢掉
        List<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList();
        List<PlayerSeatInfo> noBetPlayer = new ArrayList<>(playerSeatInfo.size());
        long timeMillis = System.currentTimeMillis();
        Map<Long, Long> baseBetInfo = gameDataVo.getBaseBetInfo();
        for (PlayerSeatInfo seatInfo : playerSeatInfo) {
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(seatInfo.getPlayerId());
            if (Objects.nonNull(gamePlayer)) {
                if (!baseBetInfo.containsKey(gamePlayer.getId()) && timeMillis - 2000 > gamePlayer.getPokerPlayerGameData().getJoinTime()) {
                    noBetPlayer.add(seatInfo);
                }
            }
        }
        for (PlayerSeatInfo info : noBetPlayer) {
            Map<Long, PlayerController> playerControllers = gameController.getRoomController().getPlayerControllers();
            PlayerController playerController = playerControllers.get(info.getPlayerId());
            if (Objects.nonNull(playerController)) {
                log.info("玩家：{}  未押注直接踢掉", info.getPlayerId());
                roomEventListener.exitGame(playerController);
            }
            playerSeatInfo.remove(info);
        }
        //进入下个阶段
        nextPhase();
    }


}
