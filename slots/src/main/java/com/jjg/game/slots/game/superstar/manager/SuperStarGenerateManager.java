package com.jjg.game.slots.game.superstar.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SameInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.superstar.SuperStarConstant;
import com.jjg.game.slots.game.superstar.data.SuperStarAwardLineInfo;
import com.jjg.game.slots.game.superstar.data.SuperStarResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 超级明星
 */
@Component
public class SuperStarGenerateManager extends AbstractSlotsGenerateManager<SuperStarAwardLineInfo, SuperStarResultLib> {

    public SuperStarGenerateManager() {
        super(SuperStarResultLib.class);
    }

    protected SuperStarAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount,
                                                      int baseIconId, List<Integer> lineList, int[] arr) {
        SuperStarAwardLineInfo awardLineInfo = new SuperStarAwardLineInfo();
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setLineId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        for (List<Integer> otherIconList : rewardCfg.getBetTimes()) {
            int iconId = otherIconList.get(0);
            //该元素在这条线上出现的次数
            long showCount = lineList.stream().filter(tmpId -> arr[tmpId] == iconId).count();
            if (showCount == otherIconList.get(1)) {
                int addTimes = otherIconList.get(2);
                awardLineInfo.addSpecialAwardInfo(iconId, addTimes);
            }
        }
        return awardLineInfo;
    }

    @Override
    public void calTimes(SuperStarResultLib lib) throws Exception {
        if(triggerFreeLib(lib)){
            lib.addTimes(calFree(lib));
        }else {
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        }
    }

    /**
     * 计算中奖线的倍数
     */
    private int calLineTimes(List<SuperStarAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Map<Integer, Integer> otherIconAwardInfoMap;
        int times = 0;
        for (SuperStarAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
            otherIconAwardInfoMap = awardLineInfo.getOtherIconAwardInfoMap();
            //额外倍率
            if (otherIconAwardInfoMap != null && !otherIconAwardInfoMap.isEmpty()) {
                times += otherIconAwardInfoMap.values().stream().mapToInt(Integer::intValue).sum();
            }
        }

        return times;
    }

    /**
     * 检查连线_分散_数量
     *
     * @param lib
     * @return
     */
    protected List<SuperStarAwardLineInfo> lineDispersionCount(SuperStarResultLib lib) {
        int[] arr = lib.getIconArr();
        //获取连线_分散_数量的配置
        Map<Integer, BaseElementRewardCfg> dispersionLineCountCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE);
        if (dispersionLineCountCfgMap == null || dispersionLineCountCfgMap.isEmpty()) {
            return null;
        }
        List<SuperStarAwardLineInfo> resultList = new ArrayList<>();
        log.debug("开始检测 连线_分散_数量");
        //遍历所有中奖线配置
        GameDataManager.getBaseLineCfgList().stream()
                .filter(cfg -> cfg.getGameType() == this.gameType)
                .forEach(baseLineCfg -> {
                    List<Integer> posLocation = baseLineCfg.getPosLocation();
                    //k:图标id -> v:图标数量
                    Map<Integer, Integer> countMap = new HashMap<>();
                    posLocation.forEach(location -> {
                        int icon = arr[location];
                        countMap.merge(icon, 1, Integer::sum);
                    });
                    //根据图标id和数量查看奖励配置是否存在
                    countMap.forEach((icon, count) -> dispersionLineCountCfgMap.values().stream()
                            .filter(cfg -> cfg.getElementId().contains(icon) && cfg.getRewardNum() == count)
                            .forEach(cfg -> {
                                SuperStarAwardLineInfo rewardInfo = addDispersionLineCountAwardInfo(lib, baseLineCfg, cfg, count);
                                resultList.add(rewardInfo);
                            }));
                });
        return resultList;
    }

    private SuperStarAwardLineInfo addDispersionLineCountAwardInfo(SuperStarResultLib lib, BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int count) {
        SuperStarAwardLineInfo awardLineInfo = new SuperStarAwardLineInfo();
        awardLineInfo.setSameCount(count);
        awardLineInfo.setLineId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        int jackpotId = rewardCfg.getJackpotID();
        //触发jackpot
        if (jackpotId > 0) {
            awardLineInfo.setJackpotId(jackpotId);
            lib.setJackpotId(jackpotId);
        }
        return awardLineInfo;
    }

    @Override
    public List<SuperStarAwardLineInfo> winLines(SuperStarResultLib lib) {

        int[] arr = lib.getIconArr();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        if (baseInitCfg.getLineType() != SlotsConst.BaseInit.NEED_BASE_LINE) {
            return null;
        }
        log.debug("开始检查中奖线信息 ");
        List<SuperStarAwardLineInfo> awardLineInfoList = new ArrayList<>();

        Map<Integer, BaseLineCfg> lineCfgMap = this.baseLineCfgMap.get(0);
        if(lineCfgMap == null || lineCfgMap.isEmpty()){
            lineCfgMap = this.baseLineCfgMap.get(1);
        }

        for (Map.Entry<Integer, BaseLineCfg> en : lineCfgMap.entrySet()) {
            BaseLineCfg cfg = en.getValue();
            List<Integer> lineList = cfg.getPosLocation();

            SameInfo sameInfo = new SameInfo();

            int last = lineList.size() - 1;

            for (int direction : cfg.getDirection()) {
                //标记是否连线
                int sameCount = 0;
                for (int i = 0; i < last; i++) {
                    int index1;
                    int index2;
                    //检查方向算法
                    if (direction == SlotsConst.BaseLine.DIRECTION_LEFT) {
                        index1 = lineList.get(i);
                        index2 = lineList.get(i + 1);
                    } else if (direction == SlotsConst.BaseLine.DIRECTION_RIGHT) {
                        index1 = lineList.get(last - i);
                        index2 = lineList.get(last - i - 1);
                    } else {
                        index1 = lineList.get(i);
                        index2 = lineList.get(i + 1);
                    }
                    //只要有空元素 就不检测连线了  空元素 必定无法连线
                    if (arr[index1] == SuperStarConstant.Common.EMPTY_ICON || arr[index2] == SuperStarConstant.Common.EMPTY_ICON) {
                        break;
                    }
//                log.debug("index1={}, index2={}", index1, index2);
                    sameInfo = iconSame(sameInfo, arr[index1], arr[index2]);
                    if (sameInfo.isSame()) {
                        sameInfo.setSame(false);
                        sameCount = sameCount < 1 ? 2 : sameCount + 1;
                    } else {
                        break;
                    }
                }

                //如果有连线
                if (sameCount > 1) {
                    log.debug("sameInfo = {}", JSON.toJSONString(sameInfo));
                    Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_NORMAL);
                    for (Map.Entry<Integer, BaseElementRewardCfg> rewardEn : normalRewardCfgMap.entrySet()) {
                        BaseElementRewardCfg rewardCfg = rewardEn.getValue();
                        //匹配连线的元素id和个数
                        if (!rewardCfg.getElementId().contains(sameInfo.getBaseIconId()) || sameCount != rewardCfg.getRewardNum()) {
                            continue;
                        }

                        SuperStarAwardLineInfo info = addAwardLineInfo(cfg, rewardCfg, sameCount, sameInfo.getBaseIconId(), lineList, arr);
                        awardLineInfoList.add(info);
                        break;
                    }
                }
            }

        }
        return awardLineInfoList;
    }
}
