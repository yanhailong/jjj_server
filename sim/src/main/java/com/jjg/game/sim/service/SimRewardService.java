package com.jjg.game.sim.service;

import com.jjg.game.common.utils.WeightRandom;
import com.jjg.game.sampledata.bean.VisitorLevelCfg;
import com.jjg.game.sampledata.bean.VisitorStarCfg;
import com.jjg.game.sim.data.GuestData;
import com.jjg.game.sim.data.SimPlayerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    private SimConfigCacheService configCache;

    /**
     * 发放本次交互产出 (按 VisitorStar.bonusRate 加权随机一行 + VisitorLevel.reward 固定奖励)
     */
    public void grantReward(SimPlayerContext ctx, GuestData guest) {
        Map<Integer, Map<Integer, VisitorStarCfg>> starMap = configCache.getVisitorStarCfgMap();
        Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap = configCache.getVisitorLevelCfgMap();

        //星级配置, 概率奖励
        VisitorStarCfg starCfg = getStarCfg(guest.getId(), guest.getStar(), starMap);
        if (starCfg != null && starCfg.getBonusRate() != null && !starCfg.getBonusRate().isEmpty()) {
            List<List<Integer>> rewardList = starCfg.getBonusRate();
            WeightRandom<List<Integer>> random = WeightRandom.create();
            for (List<Integer> row : rewardList) {
                if (row != null && row.size() >= 3 && row.get(0) > 0) {
                    random.add(row, row.get(0));
                }
            }
            List<Integer> picked = random.next();
            if (picked != null) {
                log.info("交互时随机奖励 playerId={},visitorStarCfgId={},picked={}", ctx.playerId(), starCfg.getId(), picked);
            }
        }

        //等级配置, 固定奖励
        VisitorLevelCfg levelCfg = getLevelCfg(guest.getId(), guest.getLevel(), levelMap);
        if (levelCfg != null && levelCfg.getReward() != null && !levelCfg.getReward().isEmpty()) {
            log.info("交互时固定奖励 playerId={},levelCfgId={},reward={}", ctx.playerId(), levelCfg.getId(), levelCfg.getReward());
        }
    }

    public VisitorStarCfg getStarCfg(int guestId, int star, Map<Integer, Map<Integer, VisitorStarCfg>> starMap) {
        if (starMap == null || starMap.isEmpty()) {
            return null;
        }
        Map<Integer, VisitorStarCfg> tmpMap = starMap.get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(star);
    }

    public VisitorLevelCfg getLevelCfg(int guestId, int level, Map<Integer, Map<Integer, VisitorLevelCfg>> levelMap) {
        if (levelMap == null || levelMap.isEmpty()) {
            return null;
        }
        Map<Integer, VisitorLevelCfg> tmpMap = levelMap.get(guestId);
        if (tmpMap == null || tmpMap.isEmpty()) {
            return null;
        }
        return tmpMap.get(level);
    }
}
