package com.jjg.game.sim.service;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.bean.VisitorLevelCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.pb.struct.DestinationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 游客交互奖励发放
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimRewardService {
    private static final Logger log = LoggerFactory.getLogger(SimRewardService.class);

    //BonusRate 行最小列数: [probability, itemId, count]
    private static final int BONUS_ROW_LEN = 3;

    @Autowired
    private SimConfigCacheService configCache;

    /**
     * 发放本次交互产出
     */
    public void grantReward(GuestData guest, DestinationInfo info) {
        List<ItemInfo> itemInfos = new ArrayList<>();
        VisitorLevelCfg levelCfg = getLevelCfg(guest.getId(), guest.getLevel());
        VisitorStarCfg starCfg = getStarCfg(guest.getId(), guest.getStar());

        //固定产出
        if (levelCfg != null && levelCfg.getReward() != null && !levelCfg.getReward().isEmpty()) {
            levelCfg.getReward().forEach((itemId, count) ->
                    itemInfos.add(ItemUtils.buildItemInfo(itemId, count.longValue())));
        }

        //额外材料掉落 — 等级表给配置, 星级表只覆盖概率
        if (levelCfg != null && levelCfg.getBonusRate() != null && !levelCfg.getBonusRate().isEmpty()) {
            int overrideProb = (starCfg != null && starCfg.getProbability() > 0) ? starCfg.getProbability() : 0;
            for (List<Integer> row : levelCfg.getBonusRate()) {
                if (row == null || row.size() < BONUS_ROW_LEN) {
                    continue;
                }
                int prob = overrideProb > 0 ? overrideProb : row.get(0);
                if (prob <= 0) {
                    continue;
                }
                //prob 配置约定: 10000 分母 (与 VisitorStarCfg.Probability 同基数)
                if (RandomUtils.getRandomNumInt10000() <= prob) {
                    itemInfos.add(ItemUtils.buildItemInfo(row.get(1), row.get(2).longValue()));
                }
            }
        }

        if (!itemInfos.isEmpty()) {
            info.rewards = itemInfos;
        }
    }

    /**
     * 获取指定游客等级的产出预览。
     *
     * <p>返回固定产出和所有概率大于 0 的额外掉落；这里只展示配置内容，不进行随机判定。</p>
     */
    public List<ItemInfo> getOutputPreview(int guestId, int level) {
        VisitorLevelCfg levelCfg = getLevelCfg(guestId, level);
        if (levelCfg == null) {
            return Collections.emptyList();
        }

        List<ItemInfo> itemInfos = new ArrayList<>();
        if (levelCfg.getReward() != null) {
            levelCfg.getReward().forEach((itemId, count) -> {
                if (itemId > 0 && count != null && count > 0) {
                    itemInfos.add(ItemUtils.buildItemInfo(itemId, count.longValue()));
                }
            });
        }
        if (levelCfg.getBonusRate() != null) {
            for (List<Integer> row : levelCfg.getBonusRate()) {
                if (row == null || row.size() < BONUS_ROW_LEN
                        || row.get(0) <= 0 || row.get(1) <= 0 || row.get(2) <= 0) {
                    continue;
                }
                itemInfos.add(ItemUtils.buildItemInfo(row.get(1), row.get(2).longValue()));
            }
        }
        return itemInfos;
    }

    public VisitorStarCfg getStarCfg(int guestId, int star) {
        if (configCache.getVisitorStarCfgMap() == null || configCache.getVisitorStarCfgMap().isEmpty()) {
            return null;
        }
        Map<Integer, VisitorStarCfg> tmpMap = configCache.getVisitorStarCfgMap().get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(star);
    }

    public VisitorLevelCfg getLevelCfg(int guestId, int level) {
        if (configCache.getVisitorLevelCfgMap() == null || configCache.getVisitorLevelCfgMap().isEmpty()) {
            return null;
        }
        Map<Integer, VisitorLevelCfg> tmpMap = configCache.getVisitorLevelCfgMap().get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(level);
    }
}
