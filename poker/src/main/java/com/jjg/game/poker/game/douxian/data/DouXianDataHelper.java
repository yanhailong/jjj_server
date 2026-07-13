package com.jjg.game.poker.game.douxian.data;

import com.jjg.game.core.data.Card;
import com.jjg.game.poker.game.common.data.PokerCard;
import com.jjg.game.poker.game.common.data.PokerDataHelper;
import com.jjg.game.poker.game.douxian.room.data.DouXianGameDataVo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ImmortalCardCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 牌库设计说明(douxian/DESIGN.md 三)：整局最多同时有 4人×(手牌8+区域10)=52 张牌在场，
 * 和标准52张牌库刚好打满，公共牌库在峰值时刻可能完全抽空。这依赖每回合严格按照
 * "出牌->结算->飞升(回收仙界牌到牌库)->弃牌(回收弃牌到牌库)->补牌(从牌库抽牌)"的顺序执行，
 * 回收永远发生在下一次抽牌之前，因此不会出现抽牌时牌库不够的情况——除非上层调用顺序被打乱。
 */
public final class DouXianDataHelper {

    private static final Logger log = LoggerFactory.getLogger(DouXianDataHelper.class);

    private DouXianDataHelper() {
    }

    /**
     * ImmortalCardCfg 的id和对应的 Room_ChessCfg 行id是同一个值(仿 BlackJackDataHelper#getBlackjackCfg
     * 用 roomCfg.getId() 直接查同id的 BlackjackCfg 这套约定)，见 douxian/DESIGN.md 八.2。
     */
    public static ImmortalCardCfg getImmortalCardCfg(DouXianGameDataVo gameDataVo) {
        int roomCfgId = gameDataVo.getRoomCfg().getId();
        ImmortalCardCfg cfg = GameDataManager.getImmortalCardCfg(roomCfgId);
        if (cfg == null) {
            log.error("斗仙牌找不到对应的ImmortalCardCfg配置 roomCfgId:{}", roomCfgId);
        }
        return cfg;
    }

    public static int getPoolId(DouXianGameDataVo gameDataVo) {
        ImmortalCardCfg cfg = getImmortalCardCfg(gameDataVo);
        return cfg == null ? 0 : cfg.getPoolId();
    }

    public static int getClientCardId(DouXianGameDataVo gameDataVo, int cardCfgId) {
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(gameDataVo.getPoolId());
        return cardMap.get(cardCfgId).getClientId();
    }

    public static List<Integer> getClientCardIds(DouXianGameDataVo gameDataVo, List<Integer> cardCfgIds) {
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(gameDataVo.getPoolId());
        return cardCfgIds.stream().map(id -> cardMap.get(id).getClientId()).collect(Collectors.toList());
    }

    /**
     * 把 pokerPool 配置id列表转换成判型算法需要的 Card 对象列表
     */
    public static List<Card> toCards(DouXianGameDataVo gameDataVo, List<Integer> cardCfgIds) {
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(gameDataVo.getPoolId());
        List<Card> cards = new ArrayList<>(cardCfgIds.size());
        for (Integer cfgId : cardCfgIds) {
            cards.add(cardMap.get(cfgId));
        }
        return cards;
    }

    /**
     * 反查一张 Card 对应的 pokerPool 配置id。
     * toCards() 拿到的 Card 实例实际都是 PokerDataHelper 缓存的 PokerCard 单例，直接强转取
     * pokerPoolId 即可，不需要按花色点数重新搜索。
     */
    public static int toCfgId(Card card) {
        if (card instanceof PokerCard pokerCard) {
            return pokerCard.getPokerPoolId();
        }
        throw new IllegalStateException("斗仙牌预期的Card实例都应该是PokerCard，实际:" + card.getClass());
    }

    /**
     * 客户端牌id反查配置id，用于校验/解析摆牌请求(客户端只知道clientId，不知道服务端的cfgId)。
     * 每池只有52张，线性扫描足够快，不需要额外建反查表。
     */
    public static Integer findCfgIdByClientId(DouXianGameDataVo gameDataVo, int clientId) {
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(gameDataVo.getPoolId());
        for (Map.Entry<Integer, PokerCard> entry : cardMap.entrySet()) {
            if (entry.getValue().getClientId() == clientId) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 开局时生成一副全新洗好的公共牌库
     */
    public static void shuffleNewDeck(DouXianGameDataVo gameDataVo) {
        Map<Integer, PokerCard> cardMap = PokerDataHelper.getCardListMap(gameDataVo.getPoolId());
        List<Integer> deck = new ArrayList<>(cardMap.keySet());
        Collections.shuffle(deck);
        gameDataVo.setCards(deck);
        log.info("斗仙牌洗牌完成 roomCfgId:{} deckSize:{}", gameDataVo.getRoomCfg().getId(), deck.size());
    }

    /**
     * 从公共牌库顶部抽 count 张牌，牌库不足时打印错误日志并返回能抽到的数量
     * (正常流程不应触发，见类注释)
     */
    public static List<Integer> drawCards(DouXianGameDataVo gameDataVo, int count) {
        List<Integer> pool = gameDataVo.getCards();
        if (pool == null) {
            log.error("斗仙牌抽牌失败，公共牌库未初始化 roomCfgId:{}", gameDataVo.getRoomCfg().getId());
            return List.of();
        }
        if (pool.size() < count) {
            log.error("斗仙牌公共牌库数量不足，期望抽{}张，实际只剩{}张 roomCfgId:{}",
                    count, pool.size(), gameDataVo.getRoomCfg().getId());
            count = pool.size();
        }
        List<Integer> drawn = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            drawn.add(pool.removeFirst());
        }
        return drawn;
    }

    /**
     * 把牌重新放回公共牌库并洗匀(仙界飞升舍弃/玩家主动弃牌都走这里)
     */
    public static void returnCardsToPool(DouXianGameDataVo gameDataVo, List<Integer> cardCfgIds) {
        if (cardCfgIds.isEmpty()) {
            return;
        }
        List<Integer> pool = gameDataVo.getCards();
        if (pool == null) {
            pool = new ArrayList<>();
            gameDataVo.setCards(pool);
        }
        pool.addAll(cardCfgIds);
        Collections.shuffle(pool);
    }

    // ------------------------------------------------------------------
    // 日志可读化辅助：Card 自带的 toString() 按 suit 0~3 数组下标取花色符号，
    // 但 PokerPool.xlsx 里配置的 suitNum 是 1~4(Diamond/Club/Heart/Spade)，
    // 直接用会越界/错位(黑桃会打印成"?")，这里按本项目实际的花色约定重新格式化。
    // ------------------------------------------------------------------

    private static final String[] SUIT_SYMBOLS = {"?", "♦", "♣", "♥", "♠"};
    private static final String[] RANK_NAMES = {
            "?", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"
    };

    public static String cardToString(Card card) {
        int suit = card.getSuit();
        int rank = card.getRank();
        String suitStr = suit >= 0 && suit < SUIT_SYMBOLS.length ? SUIT_SYMBOLS[suit] : "?";
        String rankStr = rank >= 0 && rank < RANK_NAMES.length ? RANK_NAMES[rank] : String.valueOf(rank);
        return suitStr + rankStr;
    }

    public static String cardsToString(List<Card> cards) {
        return cards.stream().map(DouXianDataHelper::cardToString).collect(Collectors.joining(" ", "[", "]"));
    }

    /**
     * pokerPool配置id列表 -> 可读牌面字符串，日志里最常用的入口
     */
    public static String cfgIdsToString(DouXianGameDataVo gameDataVo, List<Integer> cardCfgIds) {
        return cardsToString(toCards(gameDataVo, cardCfgIds));
    }
}
