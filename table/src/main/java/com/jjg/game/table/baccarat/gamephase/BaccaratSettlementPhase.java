package com.jjg.game.table.baccarat.gamephase;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.table.baccarat.BaccaratGameController;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.*;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.WinPosWeightCfg;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 进入结算阶段
 *
 * @author 2CL
 */
public class BaccaratSettlementPhase extends BaseSettlementPhase<BaccaratGameDataVo> {

    private static final Logger log = LoggerFactory.getLogger(BaccaratSettlementPhase.class);

    public BaccaratSettlementPhase(BaccaratGameController gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        // 进行结算
        ArrayList<Byte> cardList = gameDataVo.getCardList();
        BaccaratSettlementInfo baccaratSettlementInfo = new BaccaratSettlementInfo();
        // 初始闲家和庄家手牌
        baccaratSettlementInfo.playerCardIds = new ArrayList<>();
        baccaratSettlementInfo.bankerCardIds = new ArrayList<>();
        baccaratSettlementInfo.playerCardIds.add(removeFirst(cardList));
        baccaratSettlementInfo.bankerCardIds.add(removeFirst(cardList));
        baccaratSettlementInfo.playerCardIds.add(removeFirst(cardList));
        baccaratSettlementInfo.bankerCardIds.add(removeFirst(cardList));
        // 是否出现天王牌
        checkHasKingCard(baccaratSettlementInfo);
        // 检查闲家是否补牌
        if (checkNeedFillCard(baccaratSettlementInfo.playerCardIds, true, (byte) 0)) {
            baccaratSettlementInfo.extraPlayerCardId = removeFirst(cardList);
            baccaratSettlementInfo.playerCardIds.add(baccaratSettlementInfo.extraPlayerCardId);
            gameDataVo.setFillCard(true);
        }
        // 检查庄家是否补牌
        if (checkNeedFillCard(baccaratSettlementInfo.bankerCardIds, false, baccaratSettlementInfo.extraPlayerCardId)) {
            baccaratSettlementInfo.extraBankerCardId = removeFirst(cardList);
            baccaratSettlementInfo.bankerCardIds.add(baccaratSettlementInfo.extraBankerCardId);
            gameDataVo.setFillCard(true);
        }
        // 总分统计
        baccaratSettlementInfo.playerPointId =
            (byte) (baccaratSettlementInfo.playerCardIds.stream().mapToInt(this::getCardPointId).sum() % 10.0);
        baccaratSettlementInfo.bankerPointId =
            (byte) (baccaratSettlementInfo.bankerCardIds.stream().mapToInt(this::getCardPointId).sum() % 10.0);
        // 计算牌桌的输赢状态
        calcWinState(baccaratSettlementInfo);
        // 将牌局的输赢保存到牌桌上
        gameDataVo.getBetRecord().add(baccaratSettlementInfo.cardState);
        // 游戏结算，给玩家发送结算信息
        List<PlayerChangedGold> changedGolds = playerGameSettlement(baccaratSettlementInfo);
        NotifyBaccaratSettlementInfo baccaratTableInfo =
            BaccaratMessageBuilder.buildNotifySettlementMessage(gameDataVo, changedGolds,
                baccaratSettlementInfo);
        baccaratTableInfo.playerChangedGolds = changedGolds;
        // 将结算信息写入到场上，方便中途加入的玩家读取
        gameDataVo.setBaccaratSettlementInfo(baccaratTableInfo);
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            // 获取每个玩家的信息
            baccaratTableInfo.baccaratTableInfo.tableAreaInfos =
                BaccaratMessageBuilder.buildPlayerBetInfo(
                    baccaratTableInfo.baccaratTableInfo, gameDataVo, entry.getKey());
            log.debug("玩家：{} 结算数据: {}", entry.getKey(), JSON.toJSONString(baccaratTableInfo));
            // 向每个玩家发送通知消息
            broadcastBuilderToRoom(
                RoomMessageBuilder.newBuilder().setData(baccaratTableInfo).setPlayerIds(Collections.singleton(entry.getKey())));
        }
        // 通知所有观察者
        BaccaratMessageBuilder.notifyObserversOnPhaseChange((BaccaratGameController) gameController);
    }

    /**
     * 检查是否有天王牌
     */
    public void checkHasKingCard(BaccaratSettlementInfo baccaratSettlementInfo) {
        // 总分统计
        byte playerPointId =
            (byte) (baccaratSettlementInfo.playerCardIds.stream().mapToInt(this::getCardPointId).sum() % 10.0);
        byte bankerPointId =
            (byte) (baccaratSettlementInfo.bankerCardIds.stream().mapToInt(this::getCardPointId).sum() % 10.0);
        // 是否有天王牌
        baccaratSettlementInfo.cardState = new BaccaratCardState();
        baccaratSettlementInfo.cardState.hasKingCard = playerPointId >= 8 || bankerPointId >= 8;
    }

    @Override
    public int getPhaseRunTime() {
        // 如果补牌需要额外添加一秒的时间 phaseDoAction在getPhaseRunTime之前执行
        return gameDataVo.isFillCard() ? super.getPhaseRunTime() + 1 : super.getPhaseRunTime();
    }

    private byte removeFirst(ArrayList<Byte> cardList) {
        return cardList.remove(0);
    }

    /**
     * 计算输赢状态
     */
    private void calcWinState(BaccaratSettlementInfo baccaratSettlementInfo) {
        // 输赢计算
        if (baccaratSettlementInfo.playerPointId == baccaratSettlementInfo.bankerPointId) {
            baccaratSettlementInfo.cardState.winState = 3;
        } else if (baccaratSettlementInfo.playerPointId < baccaratSettlementInfo.bankerPointId) {
            baccaratSettlementInfo.cardState.winState = 1;
        } else {
            baccaratSettlementInfo.cardState.winState = 2;
        }
        // 计算牌型输赢,计算是否有对子
        boolean playerDoubleCard =
            baccaratSettlementInfo.playerCardIds.stream().map(PokerCardUtils::getPointId).distinct().count()
                != baccaratSettlementInfo.playerCardIds.size();
        boolean bankerDoubleCard =
            baccaratSettlementInfo.bankerCardIds.stream().map(PokerCardUtils::getPointId).distinct().count()
                != baccaratSettlementInfo.bankerCardIds.size();
        // 庄对子为1
        if (bankerDoubleCard) {
            baccaratSettlementInfo.cardState.cardTypeWinState = 1;
        }
        // 闲对子为2
        if (playerDoubleCard) {
            baccaratSettlementInfo.cardState.cardTypeWinState = 2;
        }
        // 两个都是对子为3
        if (playerDoubleCard && bankerDoubleCard) {
            baccaratSettlementInfo.cardState.cardTypeWinState = 3;
        }
    }

    /**
     * 玩家游戏结算
     */
    private List<PlayerChangedGold> playerGameSettlement(BaccaratSettlementInfo baccaratSettlementInfo) {
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>();
        // 获取玩家的押注信息，让后结算
        for (Map.Entry<Long, GamePlayer> playerEntry : gameDataVo.getGamePlayerMap().entrySet()) {
            GamePlayer gamePlayer = playerEntry.getValue();
            // 获取玩家所有区域的下注倍数，然后进行结算
            Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(gamePlayer.getId());
            // 如果玩家当前没有下注
            if (playerBetInfo == null) {
                continue;
            }
            long playerTotalBetGold =
                playerBetInfo.values()
                    .stream()
                    .map(a -> a.stream().mapToInt(Integer::intValue).sum())
                    .mapToLong(Integer::longValue)
                    .sum();
            // 玩家押注赢
            long playerBetWin = checkPlayerBetWin(playerBetInfo, baccaratSettlementInfo);
            if (playerBetWin > 0) {
                PlayerChangedGold playerGoldChange = new PlayerChangedGold();
                playerGoldChange.playerId = playerEntry.getKey();
                playerGoldChange.playerWinGold = playerBetWin;
                playerGoldChange.playerBetGold = playerTotalBetGold;
                playerChangedGolds.add(playerGoldChange);
                // TODO 给玩家添加金币
                gamePlayer.setGold(gamePlayer.getGold() + playerBetWin);
            }
        }
        return playerChangedGolds;
    }

    /**
     * 检查是否需要补牌
     */
    private boolean checkNeedFillCard(List<Byte> cardIds, boolean isPlayer, byte playerThirdCardId) {
        int cardSumPoint = cardIds.stream().mapToInt(this::getCardPointId).sum();
        // 如果点数为7或者超过7则不补牌
        if (cardSumPoint >= 7) {
            return false;
        }
        // 闲家点数小于等于5时补牌，庄家点数小于等于2时补牌
        if ((cardSumPoint <= 5 && isPlayer) || (!isPlayer && cardSumPoint <= 2)) {
            return true;
        }
        // 如过是闲家
        if (!isPlayer && playerThirdCardId > 0) {
            int playerThirdPointId = PokerCardUtils.getPointId(playerThirdCardId);
            return switch (cardSumPoint) {
                case 3 -> playerThirdPointId != 8;
                case 4 -> playerThirdPointId > 1 && playerThirdPointId < 8;
                case 5 -> playerThirdPointId > 3 && playerThirdPointId < 8;
                case 6 -> playerThirdPointId != 6 && playerThirdPointId != 7;
                default -> false;
            };
        }
        return false;
    }

    /**
     * 通过玩家下注数据，计算获得的金币值
     */
    private long checkPlayerBetWin(Map<Integer, List<Integer>> playerBetInfo, BaccaratSettlementInfo settlementInfo) {
        // 下注区域                        1:庄对     2:和    3: 闲对 4: 闲 5: 庄
        // winState          输赢状态      1:庄赢     2:闲赢  3：和
        // cardTypeWinState  牌型的输赢状态 0:默认状态，1:庄对  2：闲对，3：庄和闲都有对子
        Map<Integer, WinPosWeightCfg> weightCfgMap =
            GameDataManager.getWinPosWeightCfgList()
                .stream()
                .filter(cfg -> cfg.getGameID() == EGameType.BACCARAT.getGameTypeId())
                .collect(HashMap::new, (map, cfg) -> map.put(cfg.getWinPosID(), cfg), HashMap::putAll);
        long playerBetWin = 0;
        for (Map.Entry<Integer, List<Integer>> entry : playerBetInfo.entrySet()) {
            long areaTotal = entry.getValue().stream().mapToInt(Integer::intValue).sum();
            switch (entry.getKey()) {
                // 压庄
                case 1: {
                    if (settlementInfo.cardState.cardTypeWinState == 1 || settlementInfo.cardState.cardTypeWinState == 3) {
                        playerBetWin += calcGold(weightCfgMap.get(entry.getKey()), areaTotal);
                    }
                    break;
                }
                case 2:
                    if (settlementInfo.cardState.winState == 3) {
                        playerBetWin += calcGold(weightCfgMap.get(entry.getKey()), areaTotal);
                    }
                    break;
                case 3:
                    if (settlementInfo.cardState.cardTypeWinState == 2 || settlementInfo.cardState.cardTypeWinState == 3) {
                        playerBetWin += calcGold(weightCfgMap.get(entry.getKey()), areaTotal);
                    }
                    break;
                case 4:
                    if (settlementInfo.cardState.winState == 2) {
                        playerBetWin += calcGold(weightCfgMap.get(entry.getKey()), areaTotal);
                    }
                    break;
                case 5:
                    if (settlementInfo.cardState.winState == 1) {
                        playerBetWin += calcGold(weightCfgMap.get(entry.getKey()), areaTotal);
                    }
                    break;
                default:
                    break;
            }
        }
        return playerBetWin;
    }

    /**
     * 计算金币数量
     */
    private long calcGold(WinPosWeightCfg weightCfg, long betValue) {
        int winRatio = gameDataVo.getRoomCfg().getWinRatio();
        // 倍率计算
        long multiAdd =
            (long) Math.floor(betValue * weightCfg.getOdds() * ((10000 - winRatio) / 10000.0));
        long betReturn = (long) Math.floor(betValue * (weightCfg.getReturnRate() / 10000.0));
        // 赢的总值
        long totalWin = multiAdd + betReturn;
        log.info("【百家乐】玩家在压分区域：{}，押注：{}，获得： 赢 {} + 抽水返还 {}, 总值：{}"
            , weightCfg.getId(), betValue, multiAdd, betReturn, totalWin);
        // 倍率 + 压分返还
        return totalWin;
    }

    private byte getCardPointId(byte cardId) {
        byte pokerPointId = PokerCardUtils.getPointId(cardId);
        return pokerPointId >= PokerCardUtils.POKER_POINT_J ? 0 : pokerPointId;
    }

    @Override
    public void phaseFinish() {
        super.phaseFinish();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
