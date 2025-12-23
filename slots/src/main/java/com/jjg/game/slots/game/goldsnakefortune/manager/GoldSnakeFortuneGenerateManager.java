package com.jjg.game.slots.game.goldsnakefortune.manager;

import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneAwardLineInfo;
import com.jjg.game.slots.game.goldsnakefortune.data.GoldSnakeFortuneResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoldSnakeFortuneGenerateManager extends AbstractSlotsGenerateManager<GoldSnakeFortuneAwardLineInfo, GoldSnakeFortuneResultLib> {
    public GoldSnakeFortuneGenerateManager() {
        super(GoldSnakeFortuneResultLib.class);
    }

    @Override
    protected GoldSnakeFortuneAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        GoldSnakeFortuneAwardLineInfo awardLineInfo = new GoldSnakeFortuneAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);

        return awardLineInfo;
    }

    @Override
    public void calTimes(GoldSnakeFortuneResultLib lib) throws Exception {
        if(triggerFreeLib(lib)){
            //免费
            lib.addTimes(calFree(lib));
        }else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<GoldSnakeFortuneAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (GoldSnakeFortuneAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }
}
