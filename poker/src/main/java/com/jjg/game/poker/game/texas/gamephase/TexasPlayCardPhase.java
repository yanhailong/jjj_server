package com.jjg.game.poker.game.texas.gamephase;

import cn.hutool.core.lang.Snowflake;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.IDGenUtil;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.TexasCfg;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.TexasHistory;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasPreFlopRoundInfo;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.controller.AbstractPhaseGameController;
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

    public TexasPlayCardPhase(AbstractPhaseGameController<Room_ChessCfg, TexasGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        if (gameController instanceof TexasGameController controller) {
            gameDataVo.resetData(controller);
            //生成id
            gameDataVo.setId(IDGenUtil.TEXAS.getNextId());
            //设置记录
            TexasSaveHistory texasHistory = new TexasSaveHistory();
            texasHistory.setId(gameDataVo.getId());
            texasHistory.setTexasHistoryRoundInfos(new ArrayList<>());
            TexasHistoryRoundInfo roundInfo = new TexasHistoryRoundInfo(gameDataVo.getRound());
            texasHistory.getTexasHistoryRoundInfos().add(roundInfo);
            gameDataVo.setTexasHistory(texasHistory);
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
            //计算初始位置
            gameDataVo.setIndex(controller.getInitIndex());
            //发牌
            Map<Integer, PokerCard> cardListMap = TexasDataHelper.getCardListMap(TexasDataHelper.getPoolId(gameDataVo));
            int sendNum = sendCards(cardListMap, gameDataVo);
            //添加手牌记录
            texasHistory.setAllCards(new HashMap<>(playerSeatInfo.size()));
            //添加总获得记录
            texasHistory.setTotalPlayerBetInfo(new ArrayList<>());
            for (PlayerSeatInfo info : playerSeatInfo) {
                texasHistory.getAllCards().put(info.getPlayerId(), TexasDataHelper.getClientId(info.getCurrentCards(), TexasDataHelper.getPoolId(gameDataVo)));
                // 本轮总获得的值
                Long bet = gameDataVo.getBaseBetInfo().getOrDefault(info.getPlayerId(), 0L);
                texasHistory.getTotalPlayerBetInfo().add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, bet));
            }
            // 小盲 大盲下注
            Pair<Long, Long> bedAndSBBet = BBAndSBBet(controller, texasCfg);
            //2人庄家是小盲 由大盲注（BB）左侧第一个（即下家）玩家开始选择跟注（call）、加注（raise）、或弃牌（fold），按照顺时针方向其他玩家依次表态，大盲注玩家最后表态；
            //设置第一个开始的玩家 并添加定时
            PlayerSeatInfo first = playerSeatInfo.get(gameDataVo.getIndex());
            //通知发牌信息 并带第一个操作人的玩家id
            controller.addNextTimer(first, sendNum);
            NotifyTexasPreFlopRoundInfo.Builder builder = NotifyTexasPreFlopRoundInfo.builder();
            builder.sbBet(bedAndSBBet.getFirst())
                    .bbBet(bedAndSBBet.getSecond())
                    .playerId(first.getPlayerId())
                    .seatId(gameDataVo.getDealerSeatId())
                    .overTime(gameDataVo.getPlayerTimerEvent().getNextTime())
                    .totalBet(gameDataVo.getPool().get(0).getAmount());
            Map<Long, PlayerSeatInfo> collect = playerSeatInfo.stream()
                    .collect(Collectors.toMap(PlayerSeatInfo::getPlayerId, info -> info));
            //通知玩家
            for (SeatInfo info : gameDataVo.getSeatInfo().values()) {
                //添加记录
                Long playerId = info.getPlayerId();
                builder.playerStatus(info.isJoinGame());
                if (!info.isJoinGame()) {
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, builder.build()));
                } else {
                    builder.cards(TexasDataHelper.getClientId(collect.get(playerId).getCurrentCards(), TexasDataHelper.getPoolId(gameDataVo)));
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, builder.build()));
                }
            }
        }
    }

    private Pair<Long, Long> BBAndSBBet(TexasGameController controller, TexasCfg texasCfg) {
        List<PlayerSeatInfo> playerSeatInfoList = controller.getGameDataVo().getPlayerSeatInfoList();
        int dealerIndex = gameDataVo.getDealerIndex();
        int size = playerSeatInfoList.size();
        //添加记录
        TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
        List<TexasHistoryPlayerInfo> list = new ArrayList<>(playerSeatInfoList.size());
        TexasHistoryRoundInfo historyRoundInfo = TexasDataHelper.getHistoryRoundInfo(gameDataVo);
        historyRoundInfo.roundInfo = list;
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
        //添加记录
        historyRoundInfo.roundInfo.add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, betValue));
        //大盲
        info = playerSeatInfoList.get((dealerIndex + startIndex + 1) % size);
        long BBBetValue = texasCfg.getBbNum();
        gameDataVo.getBaseBetInfo().put(info.getPlayerId(), BBBetValue);
        gamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
        controller.changePlayerGold(gamePlayer, -BBBetValue);
        gameDataVo.getPool().get(0).addChips(BBBetValue);
        gameDataVo.getPool().get(0).addEligiblePlayer(info.getPlayerId());
        gameDataVo.setMaxBetValue(BBBetValue);
        //添加记录
        historyRoundInfo.roundInfo.add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, BBBetValue));
        //添加记录
        texasHistory.setSBValue(betValue);
        texasHistory.setBBValue(BBBetValue);
        historyRoundInfo.potAllBet = Arrays.asList(BBBetValue + betValue);
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
