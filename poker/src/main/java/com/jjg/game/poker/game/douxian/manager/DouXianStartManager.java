package com.jjg.game.poker.game.douxian.manager;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.poker.game.douxian.room.DouXianGameController;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.room.manager.RoomManager;
import com.jjg.game.sampledata.bean.RoomCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 斗仙牌房间启停生命周期监听 + GM指令入口，实际发牌/洗牌逻辑在 {@link com.jjg.game.poker.game.douxian.data.DouXianDataHelper}。
 */
@Component
public class DouXianStartManager implements IRoomStartListener, GmListener {

    private static final Logger log = LoggerFactory.getLogger(DouXianStartManager.class);

    @Autowired
    private RoomManager roomManager;

    @Override
    public void start() {
        log.info("正在启动斗仙牌游戏...");
    }

    @Override
    public void shutdown() {
        log.info("正在关闭斗仙牌游戏...");
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        if (gmOrders == null || gmOrders.length == 0) {
            return new CommonResult<>(Code.NOT_FOUND);
        }
        String command = gmOrders[0].startsWith("/") ? gmOrders[0].substring(1) : gmOrders[0];
        return switch (command.toLowerCase()) {
            case "card" -> handleCard(playerController, gmOrders);
            case "round" -> handleRound(playerController, gmOrders);
            case "special" -> handleSpecial(playerController, gmOrders);
            default -> new CommonResult<>(Code.NOT_FOUND);
        };
    }

    /**
     * 强制当前玩家在本回合触发特殊规则。格式：special win / special lose。
     */
    private CommonResult<String> handleSpecial(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        if (gmOrders.length != 2) {
            res.code = Code.FAIL;
            res.data = "格式错误，请使用：special win 或 special lose";
            return res;
        }

        int ruleType = switch (gmOrders[1].toLowerCase()) {
            case "win" -> 1;
            case "lose" -> 2;
            default -> 0;
        };
        if (ruleType == 0) {
            res.code = Code.FAIL;
            res.data = "特殊规则类型只能是win或lose：" + gmOrders[1];
            return res;
        }

        DouXianGameController douXianGameController = getDouXianGameController(playerController, res);
        if (douXianGameController == null) {
            return res;
        }
        log.info("斗仙牌收到强制特殊规则GM命令 playerId:{} ruleType:{}", playerController.playerId(), ruleType);
        return douXianGameController.gmForceSpecialRule(playerController.playerId(), ruleType);
    }

    /**
     * 重建并跳到指定回合的开局状态。格式：round 3。
     */
    private CommonResult<String> handleRound(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        if (gmOrders.length != 2) {
            res.code = Code.FAIL;
            res.data = "格式错误，请使用：round 1（支持1到4回合）";
            return res;
        }

        int targetRound;
        try {
            targetRound = Integer.parseInt(gmOrders[1]);
        } catch (NumberFormatException e) {
            res.code = Code.FAIL;
            res.data = "回合必须是1到4之间的整数：" + gmOrders[1];
            return res;
        }

        DouXianGameController douXianGameController = getDouXianGameController(playerController, res);
        if (douXianGameController == null) {
            return res;
        }
        log.info("斗仙牌收到跳回合GM命令 playerId:{} targetRound:{}", playerController.playerId(), targetRound);
        return douXianGameController.gmJumpToRound(playerController.playerId(), targetRound);
    }

    /**
     * 立即替换当前手牌。格式：card A♠ K♠ Q♠ J♠ 10♠（也兼容 ♠A）。
     */
    private CommonResult<String> handleCard(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        if (gmOrders.length < 2) {
            res.code = Code.FAIL;
            res.data = "请至少指定1张牌，格式：card A♠ K♠ Q♠ J♠ 10♠";
            return res;
        }
        if (gmOrders.length - 1 > 8) {
            res.code = Code.FAIL;
            res.data = "斗仙牌最多指定8张当前手牌";
            return res;
        }

        List<int[]> cardSpecs = new ArrayList<>();
        Set<String> distinctCards = new HashSet<>();
        for (int i = 1; i < gmOrders.length; i++) {
            int[] cardSpec = parseCard(gmOrders[i]);
            if (cardSpec == null) {
                res.code = Code.FAIL;
                res.data = "无法识别的牌：" + gmOrders[i] + "，格式示例：A♠ K♥ 10♦ 2♣";
                return res;
            }
            String key = cardSpec[0] + "_" + cardSpec[1];
            if (!distinctCards.add(key)) {
                res.code = Code.FAIL;
                res.data = "指定手牌不能重复：" + gmOrders[i];
                return res;
            }
            cardSpecs.add(cardSpec);
        }

        DouXianGameController douXianGameController = getDouXianGameController(playerController, res);
        if (douXianGameController == null) {
            return res;
        }

        log.info("斗仙牌收到指定手牌GM命令 playerId:{} cardCount:{}", playerController.playerId(), cardSpecs.size());
        return douXianGameController.gmReplaceHandCards(playerController.playerId(), cardSpecs);
    }

    private DouXianGameController getDouXianGameController(PlayerController playerController, CommonResult<String> res) {
        AbstractGameController<? extends RoomCfg, ? extends GameDataVo<? extends RoomCfg>> gameController =
                roomManager.getGameControllerByPlayerId(playerController.playerId());
        if (gameController instanceof DouXianGameController douXianGameController) {
            return douXianGameController;
        }
        res.code = Code.FAIL;
        res.data = "玩家当前不在斗仙牌房间中";
        return null;
    }

    /** @return int[]{PokerPool花色值, 点数值}，解析失败返回null。 */
    static int[] parseCard(String value) {
        if (value == null) {
            return null;
        }
        String card = value.trim().toUpperCase().replace("\uFE0F", "");
        if (card.length() < 2) {
            return null;
        }

        int suit;
        String rankText;
        char first = card.charAt(0);
        char last = card.charAt(card.length() - 1);
        if (isSuit(first)) {
            suit = parseSuit(first);
            rankText = card.substring(1);
        } else if (isSuit(last)) {
            suit = parseSuit(last);
            rankText = card.substring(0, card.length() - 1);
        } else {
            return null;
        }

        int rank = switch (rankText) {
            case "A" -> 14;
            case "2" -> 2;
            case "3" -> 3;
            case "4" -> 4;
            case "5" -> 5;
            case "6" -> 6;
            case "7" -> 7;
            case "8" -> 8;
            case "9" -> 9;
            case "10" -> 10;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            default -> 0;
        };
        return rank == 0 ? null : new int[]{suit, rank};
    }

    private static boolean isSuit(char value) {
        return value == '♦' || value == '♣' || value == '♥' || value == '♠';
    }

    private static int parseSuit(char value) {
        return switch (value) {
            case '♦' -> 1;
            case '♣' -> 2;
            case '♥' -> 3;
            case '♠' -> 4;
            default -> 0;
        };
    }
}
