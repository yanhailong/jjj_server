package com.jjg.game.poker.game.tosouth.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.jjg.game.poker.game.tosouth.constant.ToSouthConstant.*;

@Component
public class ToSouthStartManager implements IRoomStartListener, GmListener {
    private static final Logger log = LoggerFactory.getLogger(ToSouthStartManager.class);

    /** GM指定发牌数据：playerId -> List<int[]{suit, rank}> */
    private static final Map<Long, List<int[]>> GM_DEAL_CARDS = new ConcurrentHashMap<>();

    /** 最大手牌数 */
    private static final int MAX_HAND_CARDS = 13;

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("dealCards".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到dealCards的gm命令 playerId = {}, gmOrders = {}", playerController.playerId(), gmOrders);
                return handleDealCards(playerController, gmOrders);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("GM命令处理异常", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /**
     * 处理 dealCards GM命令
     * 格式: dealCards ♠A ♥K ♦10 ♣2
     * 花色符号: ♠(黑桃) ♥(红心) ♦(方块) ♣(梅花)
     * 点数: A,2,3,4,5,6,7,8,9,10,J,Q,K
     * 规则: 最少1张，最多13张，不能重复
     */
    private CommonResult<String> handleDealCards(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);

        // 校验：至少指定1张手牌
        if (gmOrders.length < 2) {
            res.code = Code.FAIL;
            res.data = "请至少指定1张手牌，格式：dealCards ♠A ♥K ♦10 ♣2";
            return res;
        }

        // 校验：最多13张手牌
        if (gmOrders.length - 1 > MAX_HAND_CARDS) {
            res.code = Code.FAIL;
            res.data = "最多指定" + MAX_HAND_CARDS + "张手牌";
            return res;
        }

        List<int[]> cardSpecs = new ArrayList<>();
        Set<String> suitRankSet = new HashSet<>();

        for (int i = 1; i < gmOrders.length; i++) {
            String cardStr = gmOrders[i].trim();
            if (cardStr.isEmpty()) {
                continue;
            }

            int[] parsed = parseCard(cardStr);
            if (parsed == null) {
                res.code = Code.FAIL;
                res.data = "无法识别的手牌：" + cardStr + "，格式示例：♠A ♥K ♦10 ♣2";
                return res;
            }

            // 校验：不能重复添加手牌（相同花色+点数）
            String key = parsed[0] + "_" + parsed[1];
            if (!suitRankSet.add(key)) {
                res.code = Code.FAIL;
                res.data = "手牌不能重复：" + cardStr;
                return res;
            }

            cardSpecs.add(parsed);
        }

        if (cardSpecs.isEmpty()) {
            res.code = Code.FAIL;
            res.data = "请至少指定1张有效手牌";
            return res;
        }

        long playerId = playerController.playerId();
        GM_DEAL_CARDS.put(playerId, cardSpecs);

        // 构建回显信息
        StringBuilder sb = new StringBuilder("GM发牌已设置，下一把将获得：");
        for (int i = 1; i < gmOrders.length; i++) {
            if (i > 1) {
                sb.append(" ");
            }
            sb.append(gmOrders[i]);
        }
        if (cardSpecs.size() < MAX_HAND_CARDS) {
            sb.append("，剩余").append(MAX_HAND_CARDS - cardSpecs.size()).append("张随机补全");
        }
        res.data = sb.toString();
        log.info("GM发牌设置成功 - 玩家: {}, 指定手牌数: {}", playerId, cardSpecs.size());
        return res;
    }

    /**
     * 解析手牌字符串，如 ♠A, ♥K, ♦10, ♣2
     *
     * @param cardStr 手牌字符串
     * @return int[]{suit, rank}，解析失败返回null
     */
    private static int[] parseCard(String cardStr) {
        if (cardStr == null || cardStr.length() < 2) {
            return null;
        }

        // 解析花色（第一个字符）
        char suitChar = cardStr.charAt(0);
        int suit;
        switch (suitChar) {
            case '♠' -> suit = SPADE_SUIT;
            case '♥' -> suit = HEART_SUIT;
            case '♦' -> suit = DIAMOND_SUIT;
            case '♣' -> suit = CLUB_SUIT;
            default -> { return null; }
        }

        // 解析点数（剩余字符）
        String rankStr = cardStr.substring(1).toUpperCase();
        int rank;
        switch (rankStr) {
            case "A" -> rank = RANK_A;
            case "2" -> rank = RANK_2;
            case "3" -> rank = 3;
            case "4" -> rank = 4;
            case "5" -> rank = 5;
            case "6" -> rank = 6;
            case "7" -> rank = 7;
            case "8" -> rank = 8;
            case "9" -> rank = 9;
            case "10" -> rank = 10;
            case "J" -> rank = 11;
            case "Q" -> rank = 12;
            case "K" -> rank = 13;
            default -> { return null; }
        }

        return new int[]{suit, rank};
    }

    /**
     * 消费GM指定的手牌（一次性使用，取出后自动移除）
     *
     * @param playerId 玩家ID
     * @return GM指定的手牌列表 int[]{suit, rank}，无GM数据返回null
     */
    public static List<int[]> consumeGmCards(long playerId) {
        return GM_DEAL_CARDS.remove(playerId);
    }

    /**
     * 检查玩家是否有待使用的GM手牌
     */
    public static boolean hasGmCards(long playerId) {
        return GM_DEAL_CARDS.containsKey(playerId);
    }

    @Override
    public void start() {
        log.info("正在启动南方前进游戏...");
    }

    @Override
    public void shutdown() {
        log.info("正在关闭南方前进游戏...");
        GM_DEAL_CARDS.clear();
    }
}
