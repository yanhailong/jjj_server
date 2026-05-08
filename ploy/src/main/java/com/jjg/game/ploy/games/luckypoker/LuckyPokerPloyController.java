package com.jjg.game.ploy.games.luckypoker;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.ploy.controller.AbstractSinglePloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.data.PloyCard;
import com.jjg.game.ploy.data.PropInfo;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerPlayerPloyGameData;
import com.jjg.game.ploy.games.luckypoker.data.LuckyPokerRecord;
import com.jjg.game.ploy.games.luckypoker.data.PokerRank;
import com.jjg.game.ploy.games.luckypoker.pb.*;
import com.jjg.game.ploy.games.luckypoker.utils.LuckyPokerUtils;
import com.jjg.game.ploy.pb.ReqPloyRecord;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PloygameRoomCfg;
import com.jjg.game.sampledata.bean.PoolResultLibCfg;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 鸿运扑克游戏控制器
 *
 * @author 11
 * @date 2026/3/19
 */
@Component
public class LuckyPokerPloyController extends AbstractSinglePloyController<LuckyPokerPlayerPloyGameData> {

    private final int POKER_SIZE = 5;

    public LuckyPokerPloyController() {
        super(LoggerFactory.getLogger(LuckyPokerPloyController.class), LuckyPokerPlayerPloyGameData.class);
    }

    @Override
    public void init(int gameType) {
        super.init(gameType);
        log.info("初始化鸿运扑克控制器");
    }

    @Override
    public AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        ResLuckyPokerPloyRecord res = new ResLuckyPokerPloyRecord(Code.SUCCESS);
        List<LuckyPokerRecord> list = recordDao.findLastRecords(playerController.playerId(), this.roomCfgId, LuckyPokerRecord.class);
        if (list == null || list.isEmpty()) {
            return res;
        }

        res.records = new ArrayList<>(list.size());
        for (LuckyPokerRecord record : list) {
            LuckyPokerRecordInfo info = new LuckyPokerRecordInfo();
            info.finalCardIds = record.getFinalCardIds();
            info.times = record.getTimes();
            res.records.add(info);
        }
        return res;
    }

    /**
     * 构建玩家进入游戏时的返回消息
     *
     * @param code
     * @param playerGameData
     * @return
     */
    @Override
    protected AbstractResponse buildResEnterGameMessage(int code, int gameType, int roomCfgId, LuckyPokerPlayerPloyGameData playerGameData) {
        ResLuckyPokerEnterGame res = new ResLuckyPokerEnterGame(code);
        if (code != Code.SUCCESS) {
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        res.stakeList = cfg.getLineBetScore();
        res.defaultBet = cfg.getDefaultBet();

        if (playerGameData.getFirstCardList() != null && !playerGameData.getFirstCardList().isEmpty() && playerGameData.getSecondCardList() != null && !playerGameData.getSecondCardList().isEmpty()) {
            res.pokerIds = LuckyPokerUtils.card2Ids(playerGameData.getFirstCardList());
        }else {
            playerGameData.setFinalCardList(null);
            playerGameData.setSecondCardList(null);
        }
        return res;
    }

    @Override
    public int beforeMoneyToPoolCheck(LuckyPokerPlayerPloyGameData playerGameData) {
        if (playerGameData.getFirstCardList() != null && !playerGameData.getFirstCardList().isEmpty() && playerGameData.getSecondCardList() != null && !playerGameData.getSecondCardList().isEmpty()) {
            log.warn("当前应该继续请求发牌1 playerId = {}", playerGameData.playerId());
            return Code.FORBID;
        }
        return Code.SUCCESS;
    }

    /**
     * 构建玩家下注后的返回消息
     *
     * @param code
     * @param playerGameData
     * @return
     */
    @Override
    protected AbstractResponse buildResBetMessage(int code, LuckyPokerPlayerPloyGameData playerGameData, long betValue, int value) {
        ResLuckyPokerBet res = new ResLuckyPokerBet(code);
        if (code != Code.SUCCESS) {
            return res;
        }

        if (playerGameData.getFirstCardList() != null && !playerGameData.getFirstCardList().isEmpty() && playerGameData.getSecondCardList() != null && !playerGameData.getSecondCardList().isEmpty()) {
            res.code = Code.FORBID;
            log.warn("当前应该继续请求发牌2 playerId = {}", playerGameData.playerId());
            return res;
        }

        PloygameRoomCfg ploygameRoomCfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());

        CommonResult<Pair<Integer, Integer>> oddsResult = randOdds(playerGameData, ploygameRoomCfg, betValue);
        if (!oddsResult.success()) {
            res.code = oddsResult.code;
            return res;
        }

        //获取牌型
        PokerRank pokerRank1 = PokerRank.rankOf(oddsResult.data.getSecond());
        //获取第二次的牌型
        PoolResultLibCfg poolResultLibCfg = GameDataManager.getPoolResultLibCfg(oddsResult.data.getFirst());
        //根据权重随机获取一种结
        PropInfo propInfo = this.poolResultLibPropMap.get(poolResultLibCfg.getModelId());
        Integer randKey = propInfo.getRandKey();
        PokerRank pokerRank2 = PokerRank.rankOf(randKey);

        //生成两次手牌
        List<List<PloyCard>> pokersList = LuckyPokerUtils.getCardIdsByRanks(List.of(pokerRank1, pokerRank2));
        playerGameData.setFirstCardList(pokersList.get(0));
        playerGameData.setSecondCardList(pokersList.get(1));

        log.info("初始化第一次手牌 playerId = {},cards = {}", playerGameData.playerId(), playerGameData.getFirstCardList());
        //建议保留的牌
        List<PloyCard> suggestCards = LuckyPokerUtils.suggestSavePokerIds(playerGameData.getFirstCardList());

        res.pokerIds = LuckyPokerUtils.card2Ids(playerGameData.getFirstCardList());
        res.suggestSavePokerIds = LuckyPokerUtils.card2Ids(suggestCards);

        log.info("建议保留 playerId = {},cards = {}", playerGameData.playerId(), suggestCards);
        log.info("初始化第二次手牌 playerId = {},cards = {}", playerGameData.playerId(), playerGameData.getSecondCardList());
        log.info("第一次发牌 playerId = {},cards = {}", playerGameData.playerId(), playerGameData.getFirstCardList());
        return res;
    }

    /**
     * 获取赔率
     *
     * @param playerGameData
     * @param gameRoomCfg
     * @param betValue
     * @return
     */
    private CommonResult<Pair<Integer, Integer>> randOdds(LuckyPokerPlayerPloyGameData playerGameData, PloygameRoomCfg gameRoomCfg, long betValue) {
        CommonResult<Pair<Integer, Integer>> result = new CommonResult<>(Code.SUCCESS);
        PloyBetDivideInfo ployBetDivideInfo = playerGameData.getPloyBetDivideInfo();
        //计算偏差范围
        long diff = BigDecimal.valueOf(ployBetDivideInfo.getPoolAfterValue() - gameRoomCfg.getInitBasePool()).divide(BigDecimal.valueOf(gameRoomCfg.getInitBasePool()), 6, RoundingMode.HALF_UP).multiply(GameConstant.TEN_THOUSAND_BD).setScale(0, BigDecimal.ROUND_HALF_UP).longValue();
        PoolResultLibCfg libCfg = getLibCfgByPoolDiff(diff);
        if (libCfg == null) {
            log.warn("获取结果库配置失败,下注失败 playerId = {},roomCfgId = {},betValue = {},poolValue = {},diff = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), betValue, ployBetDivideInfo.getPoolAfterValue(), diff);
            poolToPlayer(playerGameData, ployBetDivideInfo.getPoolChangeValue(), betValue, AddType.FAIL_ROLLBACK);
            result.code = Code.FAIL;
            return result;
        }

        //根据权重随机获取一种结果
        PropInfo propInfo = this.poolResultLibPropMap.get(libCfg.getModelId());
        Integer randKey = propInfo.getRandKey();
        if (randKey == null) {
            log.warn("获取结果库配置失败,下注失败 playerId = {},roomCfgId = {},betValue = {},poolValue = {},diff = {}", playerGameData.playerId(), playerGameData.getRoomCfgId(), betValue, ployBetDivideInfo.getPoolAfterValue(), diff);
            poolToPlayer(playerGameData, ployBetDivideInfo.getPoolChangeValue(), betValue, AddType.FAIL_ROLLBACK);
            result.code = Code.FAIL;
            return result;
        }
        result.data = new Pair<>(libCfg.getId(), randKey);
        return result;
    }

    /**
     * 玩家发牌
     *
     * @param playerController
     * @param cardIds
     */
    public ResDealCards dealCards(PlayerController playerController, List<Integer> cardIds) {
        ResDealCards res = new ResDealCards(Code.SUCCESS);
        try {
            LuckyPokerPlayerPloyGameData playerGameData = getPlayerGameData(playerController.playerId(), this.roomCfgId);
            if (playerGameData == null) {
                res.code = Code.NOT_FOUND;
                log.warn("未找到玩家的 playerGameData ，故发牌失败 playerId = {}", playerController.playerId());
                return res;
            }

            if (CollectionUtil.isEmpty(playerGameData.getFirstCardList())) {
                res.code = Code.NOT_FOUND;
                log.warn("未找到玩家的上一次牌数据，故发牌失败 playerId = {}", playerController.playerId());
                return res;
            }

            //进行补牌
            CommonResult<Pair<List<PloyCard>, List<Integer>>> drawResult = drawCards(playerController, playerGameData, cardIds);
            if (!drawResult.success()) {
                res.code = drawResult.code;
                return res;
            }

            // 检查当前牌
            PokerRank pokerRank = LuckyPokerUtils.checkPokerRank(drawResult.data.getFirst());
            //获取牌型对应的赔率
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
            Integer times = cfg.getOdds().get(pokerRank.rank);

            long value = times * playerGameData.getLastBet();
            Player player;
            if (value > 0) {
                CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, value, cfg.getTaxRate());
                if (!winResult.success()) {
                    res.code = winResult.code;
                    return res;
                }
                player = winResult.data.getSecond();
            } else {
                player = playerService.get(playerController.playerId());
            }

            //设置对应数据
            playerGameData.setFinalCardList(drawResult.data.getFirst());
            playerGameData.setWinTimes(times);
            playerGameData.setWin(value);
            playerGameData.changePlayerLastMoney(player.getGold());

            //保存记录
            LuckyPokerRecord record = new LuckyPokerRecord();
            record.setPlayerId(player.getId());
            record.setRoomCfgId(playerGameData.getRoomCfgId());
            record.setFinalCardIds(LuckyPokerUtils.card2Ids(drawResult.data.getFirst()));
            recordDao.saveRecord(record);

            //组装返回消息
            res.level = player.getLevel();
            res.exp = player.getExp();
            res.allWinGold = value;
            res.allGold = player.getGold();
            res.pokerIds = drawResult.data.getSecond();
            res.pokerRank = pokerRank.rank;

            //发送日志
            logger.luckpoker(player, playerGameData);

            //清除数据
            playerGameData.setFirstCardList(null);
            playerGameData.setSecondCardList(null);
            playerGameData.setFinalCardList(null);
            log.info("鸿运扑克第二次返回 playerId = {},res = {}", playerController.playerId(), JSONArray.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 补牌
     *
     * @param playerController
     * @param playerGameData
     * @param cardIds
     * @return
     */
    private CommonResult<Pair<List<PloyCard>, List<Integer>>> drawCards(PlayerController playerController, LuckyPokerPlayerPloyGameData playerGameData, List<Integer> cardIds) {
        CommonResult<Pair<List<PloyCard>, List<Integer>>> result = new CommonResult<>(Code.SUCCESS);

        //最终当前手牌
        List<PloyCard> finalCardList = new ArrayList<>(POKER_SIZE);
        //补发的牌
        List<Integer> newPokerIds = new ArrayList<>();

        //仅做打印使用
        List<PloyCard> tmpDdealtCards = null;
        List<PloyCard> tmpSaveCards = new ArrayList<>();


        // 未选择换牌：直接使用第二次发的整手牌
        if (CollectionUtil.isEmpty(cardIds)) {
            if (CollectionUtil.isEmpty(playerGameData.getSecondCardList()) || playerGameData.getSecondCardList().size() < POKER_SIZE) {
                log.warn("补牌数据不足，发牌失败 playerId={}", playerController.playerId());
                result.code = Code.NOT_FOUND;
                return result;
            }

            finalCardList.addAll(playerGameData.getSecondCardList());
            newPokerIds.addAll(
                    playerGameData.getSecondCardList().stream()
                            .map(PloyCard::getClientCardId)
                            .collect(Collectors.toList())
            );

            tmpDdealtCards = playerGameData.getSecondCardList();
        } else {
            // 校验要替换的牌是否合法
            Map<Integer, PloyCard> firstCardMap = playerGameData.getFirstCardList().stream()
                    .collect(Collectors.toMap(PloyCard::getClientCardId, Function.identity()));

            for (Integer cardId : cardIds) {
                PloyCard remove = firstCardMap.remove(cardId);
                if (remove == null) {
                    log.warn("玩家手牌中不包含该牌，发牌失败 playerId={}, cardId={}", playerController.playerId(), cardId);
                    result.code = Code.PARAM_ERROR;
                    return result;
                }
                tmpSaveCards.add(remove);
                //保留的牌
                finalCardList.add(remove);
            }

            // 需要补的牌数量 = 被替换的牌数量
            int needDealCount = POKER_SIZE - cardIds.size();
            if (needDealCount > 0) {
                if (CollectionUtil.isEmpty(playerGameData.getSecondCardList()) || playerGameData.getSecondCardList().size() < needDealCount) {
                    log.warn("补牌数据不足，发牌失败 playerId={}, needDealCount={}", playerController.playerId(), needDealCount);
                    result.code = Code.NOT_FOUND;
                    return result;
                }

                List<PloyCard> dealtCards = new ArrayList<>(playerGameData.getSecondCardList().subList(0, needDealCount));
                finalCardList.addAll(dealtCards);

                newPokerIds.addAll(
                        dealtCards.stream()
                                .map(PloyCard::getClientCardId)
                                .collect(Collectors.toList())
                );

                tmpDdealtCards = dealtCards;
            }
        }

        result.data = new Pair<>(finalCardList, newPokerIds);

        log.info("玩家手动保留 playerId = {},tmpSaveCards = {}", playerController.playerId(), tmpSaveCards);
        log.info("补发 playerId = {},tmpDdealtCards = {}", playerController.playerId(), tmpDdealtCards);
        log.info("最终手牌 playerId = {},finalCardList = {}", playerController.playerId(), finalCardList);
        return result;
    }
}
