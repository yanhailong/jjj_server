package com.jjg.game.slots.game.pegasusunbridle.manager;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.GirdUpdatePropConfig;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleAwardLineInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class PegasusUnbridleGameGenerateManager extends AbstractSlotsGenerateManager<PegasusUnbridleAwardLineInfo, PegasusUnbridleResultLib> {
    public PegasusUnbridleGameGenerateManager() {
        super(PegasusUnbridleResultLib.class);
    }

    private Pair<Integer, Integer> modelRandom;

    @Override
    public PegasusUnbridleResultLib checkAward(int[] arr, PegasusUnbridleResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<PegasusUnbridleAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);
        calTimes(lib);
        return lib;
    }

    @Override
    protected PegasusUnbridleAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        PegasusUnbridleAwardLineInfo awardLineInfo = new PegasusUnbridleAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);
        return awardLineInfo;
    }

    @Override
    protected void modifyGirdAction(SpecialModeCfg cfg, PegasusUnbridleResultLib lib, int[] arr) {
        List<Integer> specialGirdID = cfg.getSpecialGirdID();
        if (specialGirdID != null && !specialGirdID.isEmpty()) {
            for (int i = 0; i < specialGirdID.size(); i++) {
                int specialGirdCfgId = specialGirdID.get(i);
                boolean allUpdate = girdUpdateIsAllUpdate(specialGirdCfgId, arr);
                if (cfg.getType() != PegasusUnbridleConstant.SpecialMode.NORMAL && i > 0) {
                    PegasusUnbridleResultLib resultLib = new PegasusUnbridleResultLib();
                    lib.setRollerMode(cfg.getRollerMode());
                    resultLib.setIconArr(Arrays.copyOf(arr, arr.length));
                    resultLib.setAwardLineInfoList(winLines(resultLib, false));
                    lib.addRandomResult(resultLib);
                }
                if (!allUpdate) {
                    break;
                }
            }

        }
    }

    public Pair<Integer, Integer> getModelRandom() {
        return modelRandom;
    }

    public boolean girdUpdateIsAllUpdate(int cfgId, int[] arr) {
        log.debug("开始修改格子 specialGirdCfgId = {}", cfgId);
        SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(cfgId);
        if (specialGirdCfg == null) {
            log.debug("修改格子未找到对应的配置 cfgId = {}", cfgId);
            return false;
        }

        GirdUpdatePropConfig girdUpdatePropConfig = this.specialGirdCfgMap.get(cfgId);
        if (girdUpdatePropConfig == null) {
            log.debug("修改格子未找到计算后的权重信息 cfgId = {}", cfgId);
            return false;
        }

        if (girdUpdatePropConfig.getRandCountPropInfo() == null) {
            log.debug("修改格子未找到计算后的随机次数权重信息 cfgId = {}", cfgId);
            return false;
        }

        //获取随机次数
        Integer randCount = girdUpdatePropConfig.getRandCountPropInfo().getRandKey();
        if (randCount == null || randCount < 1) {
            return false;
        }

        log.debug("获取到随机次数 cfgId = {},randCount = {}", cfgId, randCount);
        //因为有最大次数限制，所以先clone
        PropInfo cloneAffectGirdPropInfo = girdUpdatePropConfig.getAffectGirdPropInfo().clone();
        //出现的次数记录
        Map<Integer, Integer> girdShowMap = new HashMap<>();

        //记录实际修改格子的次数
        int x = 0;
        int maxForCount = arr.length * 2;
        for (int i = 0; i < maxForCount; i++) {
            //获取一个需要替换的格子
            Integer girdId = cloneAffectGirdPropInfo.getRandKey();
            //该格子上的图标id
            int icon = arr[girdId];
            //检查该格子是否不可替换
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(icon)) {
                continue;
            }
            girdShowMap.merge(girdId, 1, Integer::sum);
            //随机一个需要出现的图标
            int newIcon = girdUpdatePropConfig.getShowIconPropInfo().getRandKey();
            log.debug("修改格子 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);
            if (newIcon == icon) {
                break;
            }
            arr[girdId] = newIcon;

            //达到最大次数限制后，移除
            if (girdShowMap.get(girdId) >= cloneAffectGirdPropInfo.getMaxShowLimit(girdId)) {
                cloneAffectGirdPropInfo.removeKeyAndRecalculate(girdId);
            }
            x++;
            if (x >= randCount) {
                break;
            }
        }
        log.debug("修改后的图标 arr = {}", Arrays.toString(arr));

        return x >= randCount;
    }

    /**
     * 全局分散
     */
    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(PegasusUnbridleResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }
        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());
        log.debug("检查全局分散");
        //小游戏
        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            //检查出现的个数是否满足
            int elementsCount = 0;
            for (int iconId : cfg.getElementId()) {
                Integer count = showCountMap.get(iconId);
                if (count != null) {
                    elementsCount += count;
                }
            }
            if (elementsCount != cfg.getRewardNum()) {
                continue;
            }
            if (cfg.getJackpotID() > 0) {
                lib.setJackpotId(cfg.getJackpotID());
                break;
            }

        }
        return List.of();
    }

    @Override
    public void calTimes(PegasusUnbridleResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<PegasusUnbridleAwardLineInfo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }
        int times = 0;
        for (PegasusUnbridleAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(PegasusUnbridleConstant.Common.SPECIAL_PLAY_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split(",");
        if (modeArr.length != 2) {
            return;
        }
        modelRandom = Pair.newPair(Integer.parseInt(modeArr[0]), Integer.parseInt(modeArr[1]));
    }
}
