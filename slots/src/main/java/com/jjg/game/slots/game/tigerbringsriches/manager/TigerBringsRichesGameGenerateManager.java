package com.jjg.game.slots.game.tigerbringsriches.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.tigerbringsriches.constant.TigerBringsRichesConstant;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesAwardLineInfo;
import com.jjg.game.slots.game.tigerbringsriches.data.TigerBringsRichesResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author lm
 * @date 2025/12/8 17:24
 */
@Component
public class TigerBringsRichesGameGenerateManager extends AbstractSlotsGenerateManager<TigerBringsRichesAwardLineInfo, TigerBringsRichesResultLib> {
    public TigerBringsRichesGameGenerateManager() {
        super(TigerBringsRichesResultLib.class);
    }

    private Pair<Integer, Integer> modelRandom;
    private WeightRandom<Integer> iconRandom;
    private int weight;
    private int jackpotId;

    @Override
    public TigerBringsRichesResultLib checkAward(int[] arr, TigerBringsRichesResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<TigerBringsRichesAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        if (CollectionUtil.isNotEmpty(lib.getLibTypeSet())) {
            for (Integer libType : lib.getLibTypeSet()) {
                if (libType != TigerBringsRichesConstant.SpecialMode.TIGER_BRINGS_RICHES) {
                    continue;
                }
                //随机元素
                int icon = TigerBringsRichesConstant.ElementId.BASE_ELEMENT_ID;
                if (iconRandom != null) {
                    icon = iconRandom.next();
                }
                //随机
                int[] iconArr = lib.getIconArr();
                //进行元素随机
                int[] temp = Arrays.copyOf(iconArr, iconArr.length);
                //设置为空白元素
                Arrays.fill(temp, TigerBringsRichesConstant.ElementId.BLANK);
                while (true) {
                    int iconCount = 0;
                    int changeCount = 0;
                    for (int i = 0; i < temp.length; i++) {
                        int oldIcon = temp[i];
                        if (oldIcon != TigerBringsRichesConstant.ElementId.BLANK) {
                            iconCount++;
                            continue;
                        }
                        if (weight > RandomUtil.randomInt(10000)) {
                            temp[i] = RandomUtil.randomBoolean() ? icon : TigerBringsRichesConstant.ElementId.WILD;
                            changeCount++;
                        }
                    }
                    //生成结果
                    TigerBringsRichesResultLib specialLib = new TigerBringsRichesResultLib();
                    specialLib.setId(RandomUtils.getUUid());
                    specialLib.setRollerMode(lib.getRollerMode());
                    lib.setGameType(this.gameType);
                    specialLib.setIconArr(Arrays.copyOf(temp, temp.length));
                    List<TigerBringsRichesAwardLineInfo> specialAwardLineInfoList = winLines(specialLib, freeModel);
                    specialLib.setAwardLineInfoList(specialAwardLineInfoList);
                    lib.addSpecialResult(specialLib);
                    calTimes(specialLib);
                    if (iconCount + changeCount == temp.length) {
                        specialLib.setJackpotId(jackpotId);
                        break;
                    }
                    if (changeCount == 0) {
                        break;
                    }
                }
            }
        }
        calTimes(lib);
        return lib;
    }

    @Override
    protected TigerBringsRichesAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        TigerBringsRichesAwardLineInfo awardLineInfo = new TigerBringsRichesAwardLineInfo();
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
    public void calTimes(TigerBringsRichesResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<TigerBringsRichesAwardLineInfo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return 0;
        }
        int times = 0;
        for (TigerBringsRichesAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        loadModelRandom();

        loadIconRandom();

        loadGenerateIcon();
    }

    private void loadModelRandom() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TigerBringsRichesConstant.Common.SPECIAL_PLAY_ID);
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
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TigerBringsRichesConstant.Common.RANDOM_ICON_PLAY_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] cfgArr = specialPlayCfg.getValue().split(";");
        if (cfgArr.length == 0) {
            return;
        }
        WeightRandom<Integer> random = new WeightRandom<>();
        for (String cfg : cfgArr) {
            String[] weightCfg = cfg.split("_");
            random.add(Integer.parseInt(weightCfg[0]), Integer.parseInt(weightCfg[1]));
        }
        iconRandom = random;
    }

    private void loadGenerateIcon() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(TigerBringsRichesConstant.Common.GENERATE_ICON_PLAY_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        weight = Integer.parseInt(specialPlayCfg.getValue());
    }
}
