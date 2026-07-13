package com.jjg.game.poker.game.douxian.data;

import com.jjg.game.poker.game.douxian.constant.DouXianZone;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个玩家在单个区域中已放置的牌，存的是 pokerPool 配置id(和 {@code DouXianGameDataVo#getHandCards()}
 * 手牌的表示方式保持一致，判型时再统一通过 {@code DouXianDataHelper#toCards} 转成 Card 对象)。
 * <p>
 * 拆成两部分：
 * - carriedCards：上一回合飞升带过来的牌，本回合内锁定，不能取回手牌，对应 DESIGN.md 8.6；
 * - newCards：本回合玩家自己摆入的牌，确认出牌前可以随时取回手牌重新选择。
 * 两部分合起来才是该区域参与判型/结算的完整牌组。
 */
public class DouXianZoneCards {

    private final DouXianZone zone;
    private final List<Integer> carriedCards = new ArrayList<>();
    private final List<Integer> newCards = new ArrayList<>();

    public DouXianZoneCards(DouXianZone zone) {
        this.zone = zone;
    }

    public DouXianZone getZone() {
        return zone;
    }

    public List<Integer> getCarriedCards() {
        return carriedCards;
    }

    public List<Integer> getNewCards() {
        return newCards;
    }

    /**
     * 该区域参与判型/结算的全部牌(配置id) = 锁定牌 + 本回合新摆的牌
     */
    public List<Integer> getAllCards() {
        List<Integer> all = new ArrayList<>(carriedCards);
        all.addAll(newCards);
        return all;
    }

    public int remainingCapacity() {
        return zone.getCapacity() - carriedCards.size() - newCards.size();
    }

    public boolean isFull() {
        return remainingCapacity() <= 0;
    }

    public void clear() {
        carriedCards.clear();
        newCards.clear();
    }
}
