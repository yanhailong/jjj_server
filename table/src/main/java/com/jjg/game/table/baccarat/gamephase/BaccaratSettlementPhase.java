package com.jjg.game.table.baccarat.gamephase;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.utils.PokerCardUtils;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.RoomBankerChangeParam;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.datatrack.DataTrackNameConstant;
import com.jjg.game.room.datatrack.EDataTrackLogType;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.WinPosWeightCfg;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.BaccaratCardState;
import com.jjg.game.table.baccarat.message.resp.BaccaratSettlementInfo;
import com.jjg.game.table.baccarat.message.resp.NotifyBaccaratSettlementInfo;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.common.gamephase.BaseSettlementPhase;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.utils.BetDataTrackLogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 进入结算阶段
 *
 * @author 2CL
 */
public class BaccaratSettlementPhase extends BaseSettlementPhase<BaccaratGameDataVo> {

    private static final Logger log = LoggerFactory.getLogger(BaccaratSettlementPhase.class);
    private static final List<Integer> WIN_STATE = List.of(1, 2, 3);

    public BaccaratSettlementPhase(BaseTableGameController<BaccaratGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        List<Byte> cardList = null;
        Pair<Long, Long> currentPool = canTriggerRecycling();
        if (currentPool != null) {
            List<Byte> bytes = generateRecyclingResults();
            if (bytes == null) {
                log.error("百家乐奖池回收触发 生成结果失败 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            } else {
                cardList = bytes;
                log.info("百家乐奖池回收触发 生成结果成功 当前池:{} 标准池:{}", currentPool.getFirst(), currentPool.getSecond());
            }
        }
        if (cardList == null) {
            cardList = gameDataVo.getCardList();
        }
        // 进行结算
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
        // 是否有对子
        checkHasDouble(baccaratSettlementInfo);
        // 检查闲家是否补牌
        if (!baccaratSettlementInfo.cardState.hasKingCard &&
                checkNeedFillCard(baccaratSettlementInfo.playerCardIds, true, (byte) 0)) {
            baccaratSettlementInfo.extraPlayerCardId = removeFirst(cardList);
            baccaratSettlementInfo.playerCardIds.add(baccaratSettlementInfo.extraPlayerCardId);
            gameDataVo.setFillCard(true);
        }
        // 检查庄家是否补牌
        if (!baccaratSettlementInfo.cardState.hasKingCard &&
                checkNeedFillCard(baccaratSettlementInfo.bankerCardIds, false, baccaratSettlementInfo.extraPlayerCardId)) {
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
        Map<Long, PlayerChangedGold> changedGolds = playerGameSettlement(baccaratSettlementInfo);
        gameDataTracker.addGameLogData(DataTrackNameConstant.SETTLEMENT_DATA, baccaratSettlementInfo);
        List<PlayerChangedGold> playerChangedGolds = new ArrayList<>(changedGolds.values());
        NotifyBaccaratSettlementInfo baccaratTableInfo =
                BaccaratMessageBuilder.buildNotifySettlementMessage(gameController, gameDataVo, playerChangedGolds, baccaratSettlementInfo);
        // 将结算信息写入到场上，方便中途加入的玩家读取
        gameDataVo.setBaccaratSettlementInfo(baccaratTableInfo);
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            // 获取每个玩家的信息
            baccaratTableInfo.baccaratTableInfo.tableAreaInfos = BaccaratMessageBuilder.buildPlayerBetInfo(baccaratTableInfo.baccaratTableInfo,
                    gameDataVo, entry.getKey());
            //log.debug("玩家：{} 结算数据: {}", entry.getKey(), JSON.toJSONString(baccaratTableInfo));
            PlayerChangedGold changedGold = changedGolds.get(entry.getKey());
            // 玩家有赢钱且大于0
            if (changedGold != null && changedGold.playerWinGold > 0) {
                // 给玩家添加历史记录
                entry.getValue().getTableGameData().addBetRecord(changedGold.playerWinGold);
            } else if (gameDataVo.getPlayerBetInfo().containsKey(entry.getKey())) {
                // 玩家有下注但是没有赢奖
                entry.getValue().getTableGameData().addBetRecord(0);
            }
            // 向每个玩家发送通知消息
            broadcastBuilderToRoom(RoomMessageBuilder.newBuilder().setData(baccaratTableInfo).setPlayerIds(Collections.singleton(entry.getKey())));
            if (gameDataVo.getPlayerBetInfo().containsKey(entry.getKey())) {
                gameDataTracker.addPlayerLogData(
                        entry.getValue(), DataTrackNameConstant.AREA_DATA,
                        JSON.toJSONString(baccaratTableInfo.baccaratTableInfo.tableAreaInfos));
            }
        }
        // 通知所有观察者
        BaccaratMessageBuilder.notifyObserversOnPhaseChange((BaseTableGameController<BaccaratGameDataVo>) gameController);
        // 发送打点日志
        gameDataTracker.flushDataLog(EDataTrackLogType.SETTLEMENT);
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

    private byte removeFirst(List<Byte> cardList) {
        return cardList.removeFirst();
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
    }

    /**
     * 检查是否有对子
     */
    private void checkHasDouble(BaccaratSettlementInfo baccaratSettlementInfo) {
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
    private Map<Long, PlayerChangedGold> playerGameSettlement(BaccaratSettlementInfo baccaratSettlementInfo) {
        Map<Long, PlayerChangedGold> playerChangedGolds = new HashMap<>();
        // 庄家变化的钱
        RoomBankerChangeParam changeParam = getRoomBankerChangeParam(gameDataVo.getRealPlayerAreaBetInfo());
        Map<Long, SettlementData> settlementDataMap = new HashMap<>();
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
            SettlementData settlementData = checkPlayerBetWin(gamePlayer, playerBetInfo, baccaratSettlementInfo, changeParam);
            if (settlementData.getTotalWin() > 0) {
                PlayerChangedGold playerGoldChange = new PlayerChangedGold();
                playerGoldChange.playerId = playerEntry.getKey();
                playerGoldChange.playerWinGold = settlementData.getTotalWin();
                playerGoldChange.playerBetGold = playerTotalBetGold;
                // 给玩家添加金币
                gameController.addItem(
                        gamePlayer.getId(), settlementData.getTotalWin(),
                        AddType.GAME_SETTLEMENT, gameDataVo.getRoomCfg().getId() + "");
                playerGoldChange.playerCurGold = gameController.getTransactionItemNum(gamePlayer.getId());
                playerChangedGolds.put(playerEntry.getKey(), playerGoldChange);
            }
            settlementDataMap.put(gamePlayer.getId(), settlementData);
            if (!(gamePlayer instanceof GameRobotPlayer)) {
                // 记录押注日志
                BetDataTrackLogUtils.recordBetLog(settlementData, gamePlayer, gameController, playerBetInfo);
            }
        }
        if (changeParam != null) {
            calculationFinalBankerChange(changeParam);
            dealRoomPool(changeParam);
            gameController.dealBankerFlowing(changeParam, settlementDataMap);
        }
        // 处理庄家输赢金币
        return playerChangedGolds;
    }

    /**
     * 检查是否需要补牌
     */
    private boolean checkNeedFillCard(List<Byte> cardIds, boolean isPlayer, byte playerThirdCardId) {
        // 总分统计
        int cardSumPoint = cardIds.stream().mapToInt(this::getCardPointId).sum() % 10;
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
    private SettlementData checkPlayerBetWin(GamePlayer gamePlayer, Map<Integer, List<Integer>> playerBetInfo,
                                             BaccaratSettlementInfo settlementInfo, RoomBankerChangeParam changeParam) {
        // 下注区域                        1:庄对     2:和    3: 闲对 4: 闲 5: 庄
        // winState          输赢状态      1:庄赢     2:闲赢  3：和
        // cardTypeWinState  牌型的输赢状态 0:默认状态，1:庄对  2：闲对，3：庄和闲都有对子
        Map<Integer, WinPosWeightCfg> weightCfgMap =
                GameDataManager.getWinPosWeightCfgList()
                        .stream()
                        .filter(cfg -> cfg.getGameID() == EGameType.BACCARAT.getGameTypeId())
                        .collect(HashMap::new, (map, cfg) -> map.put(cfg.getWinPosID(), cfg), HashMap::putAll);
        SettlementData playerSettlementData = new SettlementData();
        for (Map.Entry<Integer, List<Integer>> entry : playerBetInfo.entrySet()) {
            long areaTotal = entry.getValue().stream().mapToInt(Integer::intValue).sum();
            switch (entry.getKey()) {
                // 压庄
                case 1: {
                    if (settlementInfo.cardState.cardTypeWinState == 1 || settlementInfo.cardState.cardTypeWinState == 3) {
                        SettlementData settlementData =
                                calcGold(gamePlayer, weightCfgMap.get(entry.getKey()), areaTotal);
                        playerSettlementData.increaseBySettlementData(settlementData);
                        if (changeParam != null) {
                            changeParam.removeArea(entry.getKey());
                        }
                    }
                    break;
                }
                case 2:
                    if (settlementInfo.cardState.winState == 3) {
                        SettlementData settlementData =
                                calcGold(gamePlayer, weightCfgMap.get(entry.getKey()), areaTotal);
                        playerSettlementData.increaseBySettlementData(settlementData);
                        if (changeParam != null) {
                            changeParam.removeArea(entry.getKey());
                        }
                    }
                    break;
                case 3:
                    if (settlementInfo.cardState.cardTypeWinState == 2 || settlementInfo.cardState.cardTypeWinState == 3) {
                        SettlementData settlementData =
                                calcGold(gamePlayer, weightCfgMap.get(entry.getKey()), areaTotal);
                        playerSettlementData.increaseBySettlementData(settlementData);
                        if (changeParam != null) {
                            changeParam.removeArea(entry.getKey());
                        }
                    }
                    break;
                case 4:
                    if (settlementInfo.cardState.winState == 2) {
                        SettlementData settlementData =
                                calcGold(gamePlayer, weightCfgMap.get(entry.getKey()), areaTotal);
                        playerSettlementData.increaseBySettlementData(settlementData);
                        if (changeParam != null) {
                            changeParam.removeArea(entry.getKey());
                        }
                    }
                    break;
                case 5:
                    if (settlementInfo.cardState.winState == 1) {
                        SettlementData settlementData =
                                calcGold(gamePlayer, weightCfgMap.get(entry.getKey()), areaTotal);
                        playerSettlementData.increaseBySettlementData(settlementData);
                        if (changeParam != null) {
                            changeParam.removeArea(entry.getKey());
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        if (changeParam != null && !(gamePlayer instanceof GameRobotPlayer)) {
            changeParam.addBankerChangeGold(Math.max(0, playerSettlementData.getTotalGet() - playerSettlementData.getBankerWind()));
            changeParam.addTotalTaxRevenue(playerSettlementData.getTaxation());
        }
        return playerSettlementData;
    }

    private byte getCardPointId(byte cardId) {
        byte pokerPointId = PokerCardUtils.getPointId(cardId);
        return pokerPointId >= PokerCardUtils.POKER_POINT_J ? 0 : pokerPointId;
    }


    private List<Byte> generateRecyclingResults() {
        //1.庄赢  2.闲赢 3.和
        Map<Long, Map<Integer, List<Integer>>> playerBetInfo = gameDataVo.getRealPlayerBetInfo();
        if (CollectionUtil.isEmpty(playerBetInfo)) {
            return null;
        }
        Map<Integer, WinPosWeightCfg> weightCfgMap =
                GameDataManager.getWinPosWeightCfgList()
                        .stream()
                        .filter(cfg -> cfg.getGameID() == EGameType.BACCARAT.getGameTypeId())
                        .collect(HashMap::new, (map, cfg) -> map.put(cfg.getWinPosID(), cfg), HashMap::putAll);
        List<Integer> tempWinState = new ArrayList<>(WIN_STATE);
        Collections.shuffle(tempWinState);
        for (Integer winState : tempWinState) {
            long totalWin = 0;
            long totalLose = 0;
            for (Map.Entry<Long, Map<Integer, List<Integer>>> entry : playerBetInfo.entrySet()) {
                Map<Integer, List<Integer>> betValueList = entry.getValue();
                for (Map.Entry<Integer, List<Integer>> areaBet : betValueList.entrySet()) {
                    long areaTotal = areaBet.getValue().stream().mapToInt(Integer::intValue).sum();
                    SettlementData settlementData = null;
                    switch (areaBet.getKey()) {
                        case 2:
                                settlementData = calcGold(null, weightCfgMap.get(areaBet.getKey()), areaTotal);
                            break;
                        case 4:
                                settlementData = calcGold(null, weightCfgMap.get(areaBet.getKey()), areaTotal);
                            break;
                        case 5:
                                settlementData = calcGold(null, weightCfgMap.get(areaBet.getKey()), areaTotal);
                            break;
                        default:
                            break;
                    }
                    if (settlementData == null) {
                        BigDecimal multiAdd = BigDecimal.valueOf(areaTotal).multiply(BigDecimal.valueOf((10000 - gameDataVo.getRoomCfg().getWinRatio())))
                                .divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN);
                        totalWin += multiAdd.longValue();
                    } else {
                        totalLose += settlementData.getBetWin();
                    }
                }
            }
            if (totalLose < totalWin) {
                // 1：庄赢，2：闲赢，3：和
                // 1闲对 2庄对 3都有
                int[] indexCardArr = findCardIds(gameDataVo.getCardList(), winState);
                if (indexCardArr != null) {
                    List<Byte> cardList = new ArrayList<>();
                    for (int i : indexCardArr) {
                        if (i >= 0) {
                            cardList.add(gameDataVo.getCardList().get(i));
                        }
                    }
                    if (!checkResult(new ArrayList<>(cardList))) {
                        log.error("百家乐随机结果检查失败 cardIds:{}", cardList);
                        return null;
                    }
                    Arrays.sort(indexCardArr);
                    for (int i = indexCardArr.length - 1; i >= 0; i--) {
                        int index = indexCardArr[i];
                        if (index >= 0) {
                            gameDataVo.getCardList().remove(index);
                        }
                    }
                    if (!cardList.isEmpty()) {
                        return cardList;
                    }
                }
            }
        }
        return null;
    }


    private boolean checkResult(List<Byte> checkList) {
        BaccaratSettlementInfo baccaratSettlementInfo = new BaccaratSettlementInfo();
        baccaratSettlementInfo.playerCardIds = new ArrayList<>();
        baccaratSettlementInfo.bankerCardIds = new ArrayList<>();
        baccaratSettlementInfo.playerCardIds.add(removeFirst(checkList));
        baccaratSettlementInfo.bankerCardIds.add(removeFirst(checkList));
        baccaratSettlementInfo.playerCardIds.add(removeFirst(checkList));
        baccaratSettlementInfo.bankerCardIds.add(removeFirst(checkList));
        // 是否出现天王牌
        checkHasKingCard(baccaratSettlementInfo);
        // 是否有对子
        checkHasDouble(baccaratSettlementInfo);
        // 检查闲家是否补牌
        if (!baccaratSettlementInfo.cardState.hasKingCard && checkNeedFillCard(baccaratSettlementInfo.playerCardIds, true, (byte) 0)) {
            if (checkList.isEmpty()) {
                return false;
            }
            baccaratSettlementInfo.extraPlayerCardId = removeFirst(checkList);
        }
        // 检查庄家是否补牌
        if (!baccaratSettlementInfo.cardState.hasKingCard && checkNeedFillCard(baccaratSettlementInfo.bankerCardIds, false, baccaratSettlementInfo.extraPlayerCardId)) {
            if (checkList.isEmpty()) {
                return false;
            }
            baccaratSettlementInfo.extraBankerCardId = removeFirst(checkList);
        }
        return true;
    }

    /**
     * 找到符合获胜状态的牌 id
     *
     * @param cardIds  临时牌 ID
     * @param winState 获胜状态
     * @return 生成的牌 ID
     */
    private int[] findCardIds(List<Byte> cardIds, int winState) {
        //点数->位置索引
        Map<Byte, List<Integer>> pointIdMap = new HashMap<>();
        for (int i = 0; i < cardIds.size(); i++) {
            int pointId = getCardPointId(cardIds.get(i));
            pointIdMap.computeIfAbsent((byte) pointId, k -> new ArrayList<>()).add(i);
        }
        //多次循环遍历寻找符合条件的
        //临时map
        byte[] cardArr;
        int[] indexCardArr = new int[6];
        List<Byte> keysList1 = new ArrayList<>(pointIdMap.keySet());
        List<Byte> keysList2 = new ArrayList<>(pointIdMap.keySet());
        List<Byte> keysList3 = new ArrayList<>(pointIdMap.keySet());
        List<Byte> keysList4 = new ArrayList<>(pointIdMap.keySet());
        List<Byte> keysList5 = new ArrayList<>(pointIdMap.keySet());
        List<Byte> keysList6 = new ArrayList<>(pointIdMap.keySet());
        Collections.shuffle(keysList1);
        Collections.shuffle(keysList2);
        Collections.shuffle(keysList3);
        Collections.shuffle(keysList4);
        Collections.shuffle(keysList5);
        Collections.shuffle(keysList6);
        for (Byte key1 : keysList1) {
            cardArr = new byte[6];
            Arrays.fill(indexCardArr, -1);
            Integer index = pointIdMap.get(key1).getFirst();
            indexCardArr[0] = index;
            cardArr[0] = key1;
            for (Byte key2 : keysList2) {
                if (pointIdMap.get(key2).isEmpty() || cardArr[0] == key2) {
                    continue;
                }
                int index1 = getIndex(pointIdMap.get(key2), indexCardArr);
                if (index1 == -1) {
                    continue;
                }
                cardArr[1] = key2;
                indexCardArr[1] = index1;
                for (Byte key3 : keysList3) {
                    if (pointIdMap.get(key3).isEmpty() || cardArr[1] == key3) {
                        continue;
                    }
                    int index2 = getIndex(pointIdMap.get(key3), indexCardArr);
                    if (index2 == -1) {
                        continue;
                    }
                    indexCardArr[2] = index2;
                    cardArr[2] = key3;
                    for (Byte key4 : keysList4) {
                        if (pointIdMap.get(key4).isEmpty() || cardArr[2] == key4) {
                            continue;
                        }
                        int index3 = getIndex(pointIdMap.get(key4), indexCardArr);
                        if (index3 == -1) {
                            continue;
                        }
                        indexCardArr[3] = index3;
                        cardArr[3] = key4;
                        //判断需要补牌不
                        // 检查闲家是否补牌
                        boolean has = (cardArr[0] + cardArr[2]) % 10 >= 8 || (cardArr[1] + cardArr[3]) % 10 >= 8;
                        byte extraPlayerCardId = 0;
                        if (!has && checkNeedFillCard(List.of(cardIds.get(indexCardArr[0]), cardIds.get(indexCardArr[2])), true, (byte) 0)) {
                            for (Byte key5 : keysList5) {
                                if (pointIdMap.get(key5).isEmpty()) {
                                    continue;
                                }
                                int index4 = getIndex(pointIdMap.get(key5), indexCardArr);
                                if (index4 == -1) {
                                    continue;
                                }
                                indexCardArr[4] = index4;
                                cardArr[4] = key5;
                                extraPlayerCardId = cardIds.get(index4);
                            }
                        }
                        // 检查庄家是否补牌
                        if (!has && checkNeedFillCard(List.of(cardIds.get(indexCardArr[1]), cardIds.get(indexCardArr[3])), false, extraPlayerCardId)) {
                            for (Byte key6 : keysList6) {
                                if (pointIdMap.get(key6).isEmpty()) {
                                    continue;
                                }
                                int index5 = getIndex(pointIdMap.get(key6), indexCardArr);
                                if (index5 == -1) {
                                    continue;
                                }
                                indexCardArr[5] = index5;
                                cardArr[5] = key6;
                            }
                        }
                        //计算结果
                        int first = (cardArr[0] + cardArr[2] + cardArr[4]) % 10;
                        int second = (cardArr[1] + cardArr[3] + cardArr[5]) % 10;
                        if (first > second && winState == 1) {
                            return indexCardArr;
                        }
                        if (first < second && winState == 2) {
                            return indexCardArr;
                        }
                        if (first == second && winState == 3) {
                            return indexCardArr;
                        }
                    }
                }
            }
        }
        return null;
    }

    private int getIndex(List<Integer> indexList, int[] indexCardArr) {
        int index = -1;
        for (Integer tempIndex1 : indexList) {
            boolean flag = false;
            for (int oldIndex : indexCardArr) {
                if (oldIndex == tempIndex1) {
                    flag = true;
                    break;
                }
            }
            if (!flag) {
                return tempIndex1;
            }
        }
        return index;
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
