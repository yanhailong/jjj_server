package com.jjg.game.ploy.games.luckypoker;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PropInfo;
import com.jjg.game.ploy.controller.AbstractSinglePloyController;
import com.jjg.game.ploy.data.PloyBetDivideInfo;
import com.jjg.game.ploy.data.PloyCard;
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
import java.util.HashMap;
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
    public void init() {
        super.init();
        log.info("初始化鸿运扑克控制器");
    }

    @Override
    public AbstractMessage reqPloyRecord(PlayerController playerController, ReqPloyRecord req) {
        ResLuckyPokerPloyRecord res = new ResLuckyPokerPloyRecord(Code.SUCCESS);
        List<LuckyPokerRecord> list = recordDao.findLastRecords(playerController.playerId(), playerController.getPlayer().getRoomCfgId(), LuckyPokerRecord.class);
        if (list == null || list.isEmpty()) {
            return res;
        }

        res.records = new ArrayList<>(list.size());
        for (LuckyPokerRecord record : list) {
            LuckyPokerRecordInfo info = new LuckyPokerRecordInfo();
            info.finalCardIds = record.getFinalCardIds();
            info.times = record.getTimes();
            info.betAmount = record.getBetAmount();
            info.winAmount = record.getWinAmount();
            info.tax = record.getTax();
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
    protected AbstractResponse buildResPloyConfigMessage(int code, int gameType, int roomCfgId, LuckyPokerPlayerPloyGameData playerGameData) {
        ResLuckyPokerEnterGame res = new ResLuckyPokerEnterGame(code);
        if (code != Code.SUCCESS) {
            return res;
        }
        PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
        res.stakeList = cfg.getLineBetScore();
        //优先使用玩家上次选择的下注值；没有则回退到配置默认值
        long lastBet = playerGameData == null ? 0 : playerGameData.getLastBet();
        res.defaultBet = lastBet > 0 && cfg.getLineBetScore() != null && cfg.getLineBetScore().contains((int) lastBet)
                ? lastBet
                : cfg.getDefaultBet();

        if (playerGameData.getFirstCardList() != null && !playerGameData.getFirstCardList().isEmpty() && playerGameData.getSecondCardList() != null && !playerGameData.getSecondCardList().isEmpty()) {
            res.pokerIds = LuckyPokerUtils.card2Ids(playerGameData.getFirstCardList());
            //断线重连/重新进入时，补发建议保留的牌，保持与下注返回一致
            List<PloyCard> suggestCards = LuckyPokerUtils.suggestSavePokerIds(playerGameData.getFirstCardList());
            res.suggestSavePokerIds = LuckyPokerUtils.card2Ids(suggestCards);
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
        //生成端拦截：奖池兜不住的高倍率牌型直接从权重池里剔除，避免后续 winFromPool 触发负值回滚
        long maxAffordableTimes = computeMaxAffordableTimes(playerGameData, betValue);
        Integer randKey = pickAffordableRank(propInfo, ploygameRoomCfg.getOdds(), maxAffordableTimes);
        PokerRank pokerRank2 = PokerRank.rankOf(randKey);
        if (pokerRank2 == null) {
            log.warn("第二手牌型生成失败（无任何可承受档位且兜底也失败）playerId = {},maxAffordableTimes = {}", playerGameData.playerId(), maxAffordableTimes);
            res.code = Code.FAIL;
            return res;
        }

        //GM测试用：若设置了强制牌型，则两次手牌都按该牌型生成，方便前端调试
        Integer forceRank = playerGameData.getTestForceRank();
        if (forceRank != null) {
            PokerRank forced = PokerRank.rankOf(forceRank);
            if (forced != null) {
                pokerRank1 = forced;
                pokerRank2 = forced;
                log.info("GM强制牌型生效 playerId = {},forceRank = {}", playerGameData.playerId(), forced);
            }
        }

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
        //生成端拦截：第一手牌也可能因玩家"全部保留"而最终生效，所以这里同样按奖池承受能力做过滤
        long maxAffordableTimes = computeMaxAffordableTimes(playerGameData, betValue);
        Integer randKey = pickAffordableRank(propInfo, gameRoomCfg.getOdds(), maxAffordableTimes);
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
     * 按 "本次下注后的奖池值 / 本次下注额" 算出本局可以承受的最大倍率上限。
     * 任何 odds 大于此值的牌型若被抽中，winFromPool 将把奖池减为负数从而触发回滚——
     * 那种情况下手牌已经发给客户端，体验极差。生成端必须先把这些档位剔除。
     */
    private long computeMaxAffordableTimes(LuckyPokerPlayerPloyGameData playerGameData, long betValue) {
        if (betValue <= 0) {
            return Long.MAX_VALUE;
        }
        PloyBetDivideInfo info = playerGameData.getPloyBetDivideInfo();
        if (info == null) {
            return Long.MAX_VALUE;
        }
        long poolAfter = info.getPoolAfterValue();
        if (poolAfter <= 0) {
            return 0;
        }
        return poolAfter / betValue;
    }

    /**
     * 在 PropInfo 的权重池里按 odds 上限做过滤，再做加权随机：
     *   - 只在 odds[key] <= maxAffordableTimes 的 key 之间按原权重比例抽
     *   - 若一档都过不了（极端情况，玩家把池子打到连最低赔率档都赔不起），
     *     兜底取 odds 最小的那档，避免抛 null 让下注链路崩掉
     *   - 上面兜底也找不到，才返回 null（调用方应回滚下注）
     */
    private Integer pickAffordableRank(PropInfo propInfo, Map<Integer, Integer> odds, long maxAffordableTimes) {
        if (propInfo == null || propInfo.getPropMap() == null || propInfo.getPropMap().isEmpty()) {
            return null;
        }

        Map<Integer, int[]> propMap = propInfo.getPropMap();

        //先累加可承受档位的权重
        long filteredSum = 0;
        for (Map.Entry<Integer, int[]> en : propMap.entrySet()) {
            if (isRankAffordable(odds, en.getKey(), maxAffordableTimes)) {
                int[] range = en.getValue();
                filteredSum += (range[1] - range[0]);
            }
        }

        if (filteredSum <= 0) {
            //池子连最低档都赔不起：兜底降级到 odds 最小的档位
            log.warn("奖池连最低赔率档都赔不起，降级到 odds 最低档 maxAffordableTimes = {}", maxAffordableTimes);
            return pickLowestOddsKey(propMap, odds);
        }

        //标准上限内 → 仍按原权重比例随机
        long rand = filteredSum <= Integer.MAX_VALUE
                ? RandomUtils.randomInt((int) filteredSum)
                : (long) (Math.random() * filteredSum);
        long acc = 0;
        for (Map.Entry<Integer, int[]> en : propMap.entrySet()) {
            if (!isRankAffordable(odds, en.getKey(), maxAffordableTimes)) {
                continue;
            }
            int[] range = en.getValue();
            int weight = range[1] - range[0];
            acc += weight;
            if (rand < acc) {
                return en.getKey();
            }
        }
        return null;
    }

    private boolean isRankAffordable(Map<Integer, Integer> odds, Integer rank, long maxAffordableTimes) {
        if (odds == null) {
            return true;
        }
        Integer multiplier = odds.get(rank);
        if (multiplier == null || multiplier <= 0) {
            //没配赔率/赔率<=0：根本不会扣池子，永远可承受
            return true;
        }
        return multiplier.longValue() <= maxAffordableTimes;
    }

    private Integer pickLowestOddsKey(Map<Integer, int[]> propMap, Map<Integer, Integer> odds) {
        Integer best = null;
        int bestOdds = Integer.MAX_VALUE;
        for (Integer key : propMap.keySet()) {
            int v = 0;
            if (odds != null) {
                Integer m = odds.get(key);
                if (m != null) {
                    v = m;
                }
            }
            if (v < bestOdds) {
                bestOdds = v;
                best = key;
            }
        }
        return best;
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
            LuckyPokerPlayerPloyGameData playerGameData = getPlayerGameData(playerController.playerId());
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
            Pair<PokerRank, List<Integer>> rankResult = LuckyPokerUtils.checkPokerRank(drawResult.data.getFirst());
            PokerRank pokerRank = rankResult.getFirst();
            //获取牌型对应的赔率
            PloygameRoomCfg cfg = GameDataManager.getPloygameRoomCfg(playerGameData.getRoomCfgId());
            Integer times = cfg.getOdds().get(pokerRank.rank);

            long value = times * playerGameData.getLastBet();
            long tax = 0;
            Player player;
            if (value > 0) {
                CommonResult<Pair<PloyBetDivideInfo, Player>> winResult = winFromPool(playerGameData, value, cfg.getTaxRate());
                if (!winResult.success()) {
                    res.code = winResult.code;
                    return res;
                }
                tax = winResult.data.getFirst().getTax();
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
            record.setPokerRank(pokerRank);
            record.setTimes(times);
            record.setBetAmount(playerGameData.getLastBet());
            record.setWinAmount(value - tax);
            record.setTax(tax);
            recordDao.saveRecord(record);

            //组装返回消息
            res.level = player.getLevel();
            res.exp = player.getExp();
            res.allWinGold = value - tax;
            res.allGold = player.getGold();
            res.pokerIds = drawResult.data.getSecond();
            res.pokerRank = pokerRank.rank;
            res.pokerRankPokerIds = rankResult.getSecond();

            //发送日志
            logger.luckpoker(player, playerGameData);
            HashMap<String, Object> settlementData = new HashMap<>();
            settlementData.put("firstCards", LuckyPokerUtils.card2Ids(playerGameData.getFirstCardList()));
            settlementData.put("finalCards", LuckyPokerUtils.card2Ids(drawResult.data.getFirst()));
            settlementData.put("pokerRank", pokerRank.rank);
            settlementData.put("times", times);
            settlementData.put("tax", tax);
            sendSettlementDataTrack(playerGameData, playerGameData.getLastBet(), value - tax, settlementData);

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

    @Override
    public int getGameType() {
        return CoreConst.GameType.LUCKY_POKER;
    }
}
