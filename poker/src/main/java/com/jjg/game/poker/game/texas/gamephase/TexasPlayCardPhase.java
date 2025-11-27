package com.jjg.game.poker.game.texas.gamephase;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.poker.game.common.data.PlayerSeatInfo;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.gamephase.BasePlayCardPhase;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.Pot;
import com.jjg.game.poker.game.texas.data.SeatInfo;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.data.TexasSaveHistory;
import com.jjg.game.poker.game.texas.message.TexasBuilder;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryPlayerInfo;
import com.jjg.game.poker.game.texas.message.bean.TexasHistoryRoundInfo;
import com.jjg.game.poker.game.texas.message.reps.NotifyTexasPreFlopRoundInfo;
import com.jjg.game.poker.game.texas.room.TexasGameController;
import com.jjg.game.poker.game.texas.room.data.TexasGameDataVo;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.Room_ChessCfg;
import com.jjg.game.sampledata.bean.TexasCfg;

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
    public void playCardPhaseDoAction() {
        if (gameController instanceof TexasGameController controller) {
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
            //位置信息
            TreeMap<Integer, SeatInfo> seatInfo = gameDataVo.getSeatInfo();
            //定庄 取比他小的位置 和比他大的位置各一个
            int buttonSeatId = getButtonSeatId(seatInfo, controller);
            gameDataVo.setDealerSeatId(buttonSeatId);
            // 确定执行顺序
            List<PlayerSeatInfo> playerSeatInfo = gameDataVo.getPlayerSeatInfoList();
            controller.genPlayerSeatInfoList(seatInfo, playerSeatInfo);
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
                //手牌记录
                texasHistory.getAllCards().put(info.getPlayerId(), TexasDataHelper.getClientId(gameDataVo, info.getCurrentCards()));
                // 本轮总获得的值
                Long bet = gameDataVo.getBaseBetInfo().getOrDefault(info.getPlayerId(), 0L);
                texasHistory.getTotalPlayerBetInfo().add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, true, bet));
            }
            // 小盲 大盲下注
            Pair<Long, Long> bedAndSBBet = BBAndSBBet(controller, texasCfg);
            //2人庄家是小盲 由大盲注（BB）左侧第一个（即下家）玩家开始选择跟注（call）、加注（raise）、或弃牌（fold），按照顺时针方向其他玩家依次表态，大盲注玩家最后表态；
            //设置第一个开始的玩家 并添加定时
            PlayerSeatInfo first = playerSeatInfo.get(gameDataVo.getIndex());
            //通知发牌信息 并带第一个操作人的玩家id
            controller.addNextTimer(first, sendNum, TexasConstant.Common.FLIP_CARDS);
            NotifyTexasPreFlopRoundInfo.Builder builder = NotifyTexasPreFlopRoundInfo.builder();
            builder.sbBet(bedAndSBBet.getFirst())
                    .bbBet(bedAndSBBet.getSecond())
                    .playerId(first.getPlayerId())
                    .seatId(gameDataVo.getDealerSeatId())
                    .overTime(gameDataVo.getPlayerTimerEvent().getNextTime())
                    .totalBet(gameDataVo.getPool().getFirst().getAmount());
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
                    builder.cards(TexasDataHelper.getClientId(gameDataVo, collect.get(playerId).getCurrentCards()));
                    controller.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(playerId, builder.build()));
                }
            }
        }
    }

    /**
     * 下注大小盲注
     */
    private Pair<Long, Long> BBAndSBBet(TexasGameController controller, TexasCfg texasCfg) {
        List<PlayerSeatInfo> playerSeatInfoList = controller.getGameDataVo().getPlayerSeatInfoList();
        int dealerIndex = gameDataVo.getDealerIndex();
        int size = playerSeatInfoList.size();
        //添加记录
        TexasSaveHistory texasHistory = gameDataVo.getTexasHistory();
        List<TexasHistoryPlayerInfo> list = new ArrayList<>(playerSeatInfoList.size());
        TexasHistoryRoundInfo historyRoundInfo = gameDataVo.getHistoryRoundInfo();
        historyRoundInfo.roundInfo = list;
        //两人时庄家是小盲
        int startIndex = size == 2 ? 0 : 1;
        //小盲
        PlayerSeatInfo info = playerSeatInfoList.get((dealerIndex + startIndex) % size);
        long sBBetValue = texasCfg.getSbNum();
        gameDataVo.getBaseBetInfo().put(info.getPlayerId(), sBBetValue);

        GamePlayer sBGamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
        controller.changePlayerGold(sBGamePlayer, -sBBetValue);
        gameDataVo.getPool().getFirst().addChips(sBBetValue);
        gameDataVo.getPool().getFirst().addEligiblePlayer(info.getPlayerId());
        //添加记录
        historyRoundInfo.roundInfo.add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, sBBetValue));
        texasHistory.getTotalPlayerBetInfoMap().get(info.getPlayerId()).betValue = sBBetValue;
        //大盲
        info = playerSeatInfoList.get((dealerIndex + startIndex + 1) % size);
        long BBBetValue = texasCfg.getBbNum();
        gameDataVo.getBaseBetInfo().put(info.getPlayerId(), BBBetValue);

        GamePlayer bBGamePlayer = gameDataVo.getGamePlayer(info.getPlayerId());
        controller.changePlayerGold(bBGamePlayer, -BBBetValue);
        gameDataVo.getPool().getFirst().addChips(BBBetValue);
        gameDataVo.getPool().getFirst().addEligiblePlayer(info.getPlayerId());
        gameDataVo.setMaxBetValue(BBBetValue);
        //添加记录
        historyRoundInfo.roundInfo.add(TexasBuilder.getTexasHistoryPlayerInfo(info, gameDataVo, BBBetValue));
        texasHistory.getTotalPlayerBetInfoMap().get(info.getPlayerId()).betValue = BBBetValue;
        //添加记录
        texasHistory.setSBValue(sBBetValue);
        texasHistory.setBBValue(BBBetValue);
            Thread.ofVirtual().start(() -> {
                gameController.dealEffectiveBet(bBGamePlayer, BBBetValue);
                gameController.dealBet(bBGamePlayer, BBBetValue);
                gameController.dealEffectiveBet(sBGamePlayer, sBBetValue);
                gameController.dealBet(sBGamePlayer, sBBetValue);
            });
        historyRoundInfo.potAllBet = Arrays.asList(BBBetValue + sBBetValue);
        return Pair.newPair(sBBetValue, BBBetValue);
    }

    /**
     * 获取庄家的位置
     */
    private int getButtonSeatId(TreeMap<Integer, SeatInfo> seatInfo, TexasGameController controller) {
        int less = -1;
        int more = -1;
        for (Map.Entry<Integer, SeatInfo> entry : seatInfo.entrySet()) {
            SeatInfo info = entry.getValue();
            if (controller.playerNotInit(info.getPlayerId())) {
                continue;
            }
            if (!info.isReady() || !info.isSeatDown()) {
                continue;
            }
            if (entry.getKey() > gameDataVo.getDealerSeatId() && info.isSeatDown()) {
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
