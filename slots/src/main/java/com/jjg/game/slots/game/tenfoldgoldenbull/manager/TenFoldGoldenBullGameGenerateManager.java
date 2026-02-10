package com.jjg.game.slots.game.tenfoldgoldenbull.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialModeCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.PropInfo;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.constant.TenFoldGoldenBullConstant;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullAwardLineInfo;
import com.jjg.game.slots.game.tenfoldgoldenbull.data.TenFoldGoldenBullResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class TenFoldGoldenBullGameGenerateManager extends AbstractSlotsGenerateManager<TenFoldGoldenBullAwardLineInfo, TenFoldGoldenBullResultLib> {
    public TenFoldGoldenBullGameGenerateManager() {
        super(TenFoldGoldenBullResultLib.class);
    }

    private Pair<Integer, Integer> modelRandom;
    private WeightRandom<Integer> randomCount = new WeightRandom<>();
    private WeightRandom<Integer> randomIcon = new WeightRandom<>();

    @Override
    public TenFoldGoldenBullResultLib checkAward(int[] arr, TenFoldGoldenBullResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<TenFoldGoldenBullAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        //福牛模式转到中奖为止
        if (CollectionUtil.isNotEmpty(lib.getLibTypeSet())) {
            //最大100次
            for (Integer libType : lib.getLibTypeSet()) {
                if (libType == TenFoldGoldenBullConstant.SpecialMode.NORMAL) {
                    continue;
                }
                if (libType == TenFoldGoldenBullConstant.SpecialMode.REAL_LUCKY_BULL) {
                    dealLuckyBull(lib, libType);
                }
                if (libType == TenFoldGoldenBullConstant.SpecialMode.JACKPOT) {
                    dealJackpot(lib, libType);
                }
            }
        }
        calTimes(lib);
        return lib;
    }

    /**
     * 生成一个结果
     *
     * @param libType
     * @return
     */
    public TenFoldGoldenBullResultLib generateOne(int libType, SpecialModeCfg specialModeCfg) throws Exception {
        //创建结果库对象
        TenFoldGoldenBullResultLib lib = new TenFoldGoldenBullResultLib();
        lib.setId(RandomUtils.getUUid());
        lib.setRollerMode(specialModeCfg.getRollerMode());

        //生成所有的图标
        int[] arr = generateAllIcons(specialModeCfg.getRollerMode(), specialModeCfg.getCols(), specialModeCfg.getRows());
        if (arr == null) {
            return null;
        }
        //修改格子策略组
        PropInfo propInfo = this.specialModeGroupGirdPropMap.get(libType);
        if (propInfo != null) {
            Integer randKey = propInfo.getRandKey();
            if (randKey == null) {
                randKey = 0;
            }
            SpecialGirdInfo specialGirdInfo = gridUpdate(lib, randKey, arr);
            if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                lib.addSpecialGirdInfo(specialGirdInfo);
            }
        }
        modifyGirdAction(specialModeCfg, lib, arr);
        //判断中奖，返回
        return checkAward(arr, lib);
    }

    private void dealJackpot(TenFoldGoldenBullResultLib lib, Integer libType) throws Exception {
        //随机元素
        Integer count = randomCount.next();
        Integer icon = randomIcon.next();
        if (count == null || icon == null) {
            log.error("牛气冲天大奖配置表错误");
            return;
        }
        //随机次数
        SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(libType);
        if (specialModeCfg == null) {
            log.warn("生成图标时，specialModeCfg 配置为空 gameType = {},libType = {}", this.gameType, libType);
            return;
        }
        for (int i = 0; i < 100; i++) {
            TenFoldGoldenBullResultLib tempLib = generateOne(libType, specialModeCfg);
            if (CollectionUtil.isNotEmpty(tempLib.getAwardLineInfoList())) {
                continue;
            }
            lib.addRandomResult(tempLib);
            if (lib.getRandomResult().size() >= count) {
                //再生成一个全屏的
                TenFoldGoldenBullResultLib jackPot = generateOne(libType, specialModeCfg);
                int[] iconArr = jackPot.getIconArr();
                for (int j = 1; j < iconArr.length; j++) {
                    int oldIcon = iconArr[j];
                    if (oldIcon == SlotsConst.Common.IMMUTABLE_ELEMENTS || oldIcon == TenFoldGoldenBullConstant.ElementId.WILD) {
                        continue;
                    }
                    iconArr[j] = icon;
                }
                jackPot.setJackpotId(TenFoldGoldenBullConstant.Common.JACKPOT_ID);
                checkAward(iconArr, jackPot);
                lib.addRandomResult(jackPot);
                break;
            }
        }
    }

    /**
     * 处理福牛模式
     *
     * @param lib 初始结果库
     */
    private void dealLuckyBull(TenFoldGoldenBullResultLib lib, int libType) throws Exception {
        //获取模式配置
        SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(libType);
        if (specialModeCfg == null) {
            log.warn("生成游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, libType);
            return;
        }
        for (int i = 0; i < 100; i++) {
            TenFoldGoldenBullResultLib tempLib = generateOne(libType, specialModeCfg);
            lib.addLibType(libType);
            lib.addRandomResult(tempLib);
            if (CollectionUtil.isNotEmpty(tempLib.getAwardLineInfoList())) {
                break;
            }
        }
    }


    @Override
    protected TenFoldGoldenBullAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        TenFoldGoldenBullAwardLineInfo awardLineInfo = new TenFoldGoldenBullAwardLineInfo();
        awardLineInfo.setId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setIconId(baseIconId);
        return awardLineInfo;
    }

    public Pair<Integer, Integer> getModelRandom() {
        return modelRandom;
    }


    @Override
    public void calTimes(TenFoldGoldenBullResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<TenFoldGoldenBullAwardLineInfo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }
        int times = 0;
        for (TenFoldGoldenBullAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        loadModelRandom();
        loadIconRandom();
        loadCountRandom();
    }


    private void loadModelRandom() {
        modelRandom = Pair.newPair(1, 500);
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TenFoldGoldenBullConstant.Common.SPECIAL_PLAY_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] modeArr = specialPlayCfg.getValue().split(",");
        if (modeArr.length != 2) {
            return;
        }
        modelRandom = Pair.newPair(Integer.parseInt(modeArr[0]), Integer.parseInt(modeArr[1]));
    }

    private void loadIconRandom() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TenFoldGoldenBullConstant.Common.RANDOM_ICON);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] iconArr = StringUtils.split(specialPlayCfg.getValue(), "|");
        for (String iconCfg : iconArr) {
            String[] split = StringUtils.split(iconCfg, "_");
            if (split.length != 2) {
                continue;
            }
            randomIcon.add(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }
    }

    private void loadCountRandom() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TenFoldGoldenBullConstant.Common.RANDOM_TIME);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] iconArr = StringUtils.split(specialPlayCfg.getValue(), "|");
        for (String iconCfg : iconArr) {
            String[] split = StringUtils.split(iconCfg, "_");
            if (split.length != 2) {
                continue;
            }
            randomCount.add(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        }
    }
}
