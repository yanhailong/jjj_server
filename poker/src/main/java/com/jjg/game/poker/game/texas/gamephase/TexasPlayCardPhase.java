package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.TexasCfg;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.message.reps.NotifyPreFlopRoundInfo;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_ChessCfg;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/7/28 14:48
 */
public class TexasPlayCardPhase extends BasePlayCardPhase<TexasGameDataVo> {

    public TexasPlayCardPhase(AbstractGameController<Room_ChessCfg, TexasGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        if (gameController instanceof TexasGameController controller) {
            gameDataVo.resetData(controller);
            //取配置表
            Room_ChessCfg roomCfg = gameDataVo.getRoomCfg();
            TexasCfg texasCfg = GameDataManager.getTexasCfg(roomCfg.getId());
            TreeMap<Integer, SeatInfo> seatInfo = gameDataVo.getSeatInfo();
            //定庄 取比他小的位置 和比他大的位置各一个
            int buttonSeatId = getButtonSeatId(seatInfo);
            gameDataVo.setDealerSeatId(buttonSeatId);
            // 确定执行顺序
            List<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList();
            playerSeatInfo.clear();
            for (Map.Entry<Integer, SeatInfo> entry : seatInfo.entrySet()) {
                SeatInfo info = entry.getValue();
                GamePlayer gamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
                if (Objects.isNull(gamePlayer)) {
                    continue;
                }
                if (gamePlayer.getPokerPlayerGameData().isInit() && info.isSeatDown()) {
                    info.setJoinGame(true);
                    playerSeatInfo.add(new PlayerSeatInfo(entry.getKey(), info.getPlayerId()));
                } else {
                    info.setJoinGame(false);
                }
            }
            //确定庄家在执行列表中的位置
            for (int i = 0; i < playerSeatInfo.size(); i++) {
                if (playerSeatInfo.get(i).getSeatId() == buttonSeatId) {
                    gameDataVo.setDealerIndex(i);
                    break;
                }
            }
            //设置主池
            gameDataVo.getPool().clear();
            gameDataVo.getPool().add(new Pot());
            // 小盲 大盲下注
            Pair<Long, Long> bedAndSBBet = BBAndSBBet(controller, texasCfg);
            //计算初始位置
            gameDataVo.setIndex(controller.getInitIndex());
            //2人庄家是小盲 由大盲注（BB）左侧第一个（即下家）玩家开始选择跟注（call）、加注（raise）、或弃牌（fold），按照顺时针方向其他玩家依次表态，大盲注玩家最后表态；
            //发牌
            Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(roomCfg.getId());
            gameDataVo.setCards(new ArrayList<>(cardListMap.keySet()));
            List<Integer> cards = gameDataVo.getCards();
            Collections.shuffle(cards);
            List<PlayerSeatInfo> playerSeatInfoList = gameDataVo.getPlayerSeatInfoList();
            int sendNum = 0;
            //从第一个执行者开始发牌
            for (PlayerSeatInfo info : playerSeatInfoList) {
                sendNum += roomCfg.getHandPoker();
                List<Integer> playCard = new ArrayList<>();
                for (int i = 0; i < roomCfg.getHandPoker(); i++) {
                    playCard.add(cards.remove(0));
                }
                info.setCards(new ArrayList<>());
                info.getCards().add(playCard);
            }
            //设置第一个开始的玩家 并添加定时
            PlayerSeatInfo first = playerSeatInfoList.get(gameDataVo.getIndex());
            //通知发牌信息 并带第一个操作人的玩家id
            controller.addNextTimer(first, sendNum);
            NotifyPreFlopRoundInfo.Builder builder = NotifyPreFlopRoundInfo.builder();
            builder.sbBet(bedAndSBBet.getFirst())
                    .bbBet(bedAndSBBet.getSecond())
                    .playerId(first.getPlayerId())
                    .seatId(gameDataVo.getDealerSeatId())
                    .overTime(gameDataVo.getPlayerTimerEvent().getNextTime())
                    .totalBet(gameDataVo.getPool().get(0).getAmount());
            Map<Long, PlayerSeatInfo> collect = playerSeatInfo.stream()
                    .collect(Collectors.toMap(PlayerSeatInfo::getPlayerId, info -> info));
            //发送没有牌的玩家
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                Long playerId = info.getPlayerId();
                builder.playerStatus(info.isJoinGame());
                if (!info.isJoinGame()) {
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, builder.build()));
                } else {
                    builder.cards(TexasDataHelper.getClientId(collect.get(playerId).getCurrentCards(), gameDataVo.getRoomCfg().getId()));
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, builder.build()));
                }
            }
        }
    }

    private Pair<Long, Long> BBAndSBBet(TexasGameController controller, TexasCfg texasCfg) {
        List<PlayerSeatInfo> playerSeatInfoList = controller.getGameDataVo().getPlayerSeatInfoList();
        int dealerIndex = gameDataVo.getDealerIndex();
        int size = playerSeatInfoList.size();
        //两人时庄家是小盲
        int startIndex = size == 2 ? 0 : 1;
        //小盲
        PlayerSeatInfo info = playerSeatInfoList.get((dealerIndex + startIndex) % size);
        long betValue = texasCfg.getSbNum();
        gameDataVo.getBaseBetInfo().put(info.getPlayerId(), betValue);
        GamePlayer gamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
        controller.changePlayerGold(gamePlayer, -betValue);
        gameDataVo.getPool().get(0).addChips(betValue);
        gameDataVo.getPool().get(0).addEligiblePlayer(info.getPlayerId());
        //大盲
        info = playerSeatInfoList.get((dealerIndex + startIndex + 1) % size);
        long BBBetValue = texasCfg.getBbNum();
        gameDataVo.getBaseBetInfo().put(info.getPlayerId(), BBBetValue);
        gamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
        controller.changePlayerGold(gamePlayer, -BBBetValue);
        gameDataVo.getPool().get(0).addChips(BBBetValue);
        gameDataVo.getPool().get(0).addEligiblePlayer(info.getPlayerId());
        return Pair.newPair(betValue, BBBetValue);
    }

    @Override
    public int getPhaseRunTime() {
        return -1;
    }

    private int getButtonSeatId(TreeMap<Integer, SeatInfo> seatInfo) {
        int less = -1;
        int more = -1;
        for (Map.Entry<Integer, SeatInfo> entry : seatInfo.entrySet()) {
            if (entry.getKey() > gameDataVo.getDealerIndex() && entry.getValue().isSeatDown()) {
                more = entry.getKey();
                break;
            } else if (less == -1) {
                less = entry.getKey();
            }
        }
        //确定新的庄家位置
        return more == -1 ? less : more;
    }
}
