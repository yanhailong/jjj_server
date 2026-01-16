package com.jjg.game.slots.game.pegasusunbridle.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseLineCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SameInfo;
import com.jjg.game.slots.game.pegasusunbridle.constant.PegasusUnbridleConstant;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleAwardLineInfo;
import com.jjg.game.slots.game.pegasusunbridle.data.PegasusUnbridleResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import jodd.util.StringUtil;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private WeightRandom<Integer> iconRandom;
    //随机到图标得概率（万分比）
    private int weight;
    //转轴上最低出现元素个数
    private int minIconCount;
    //_随机图标变成wild概率
    private int wildChance;
    //元素id->滚轴id
    private Map<Integer, Integer> elementRollMap;

    /**
     * 判断两个icon是否一样
     *
     * @param sameInfo
     * @param iconIdFront 前一个图标
     * @param iconIdBack  后一个图标
     * @return
     */
    protected SameInfo iconSame(SameInfo sameInfo, int iconIdFront, int iconIdBack) {
        if (iconIdFront >= SlotsConst.Common.INVALID_ICON_BEGIN_ID || iconIdBack >= SlotsConst.Common.INVALID_ICON_BEGIN_ID) {
            return sameInfo;
        }

        Set<Integer> noralIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_NORMAL);
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        //是不是普通图标
        boolean normal_Front = noralIconSet.contains(iconIdFront);
        boolean normal_Back = noralIconSet.contains(iconIdBack);

        //是不是wild
        boolean wild_Front = wildIconSet.contains(iconIdFront);
        boolean wild_Back = wildIconSet.contains(iconIdBack);

        if (wild_Front) {  //表示front是wild图标
            if (wild_Back) {  //均为wild，相同
                sameInfo.setSame(true);
                sameInfo.setBaseIconId(iconIdFront);
            } else {
                //如果2是普通图标
                if (normal_Back) {
                    if (sameInfo.getBaseIconId() > 0) {
                        sameInfo.setSame(sameInfo.getBaseIconId() == iconIdBack);
                    } else {
                        sameInfo.setSame(true);
                        sameInfo.setBaseIconId(iconIdBack);
                    }
                }
            }
        } else if (normal_Front) {  //表示fornt是普通图标
            if (wild_Back) { //back是wild
                sameInfo.setSame(true);
                sameInfo.setBaseIconId(iconIdFront);
            } else {
                //如果front是普通，back是非wild，则只有两者id相同
                if (iconIdFront == iconIdBack) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconIdFront);
                }
            }
        }
        return sameInfo;
    }
    @Override
    public PegasusUnbridleResultLib checkAward(int[] arr, PegasusUnbridleResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<PegasusUnbridleAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        if (CollectionUtil.isNotEmpty(lib.getLibTypeSet())) {
            for (Integer libType : lib.getLibTypeSet()) {
                if (libType != PegasusUnbridleConstant.SpecialMode.REAL_FU_MA) {
                    continue;
                }
                //随机元素
                int icon = PegasusUnbridleConstant.ElementId.BASE_ELEMENT_ID;
                if (iconRandom != null) {
                    icon = iconRandom.next();
                }
                lib.setSpecialModeIcon(icon);
                //随机
                int[] iconArr = lib.getIconArr();
                //进行元素随机
                int[] temp = Arrays.copyOf(iconArr, iconArr.length);
                //设置为空白元素
                Arrays.fill(temp, PegasusUnbridleConstant.ElementId.BLANK);
                // Iteratively populates slots; computes special result until full
                int createElementCount = 0;
                while (true) {
                    int iconCount = 0;
                    int changeCount = 0;
                    int index = -1;
                    for (int i = 0; i < temp.length; i++) {
                        int oldIcon = temp[i];
                        if (oldIcon != PegasusUnbridleConstant.ElementId.BLANK) {
                            iconCount++;
                            continue;
                        }
                        if (weight > RandomUtil.randomInt(10000)) {
                            temp[i] = wildChance > RandomUtil.randomInt(10000) ? PegasusUnbridleConstant.ElementId.WILD : icon;
                            changeCount++;
                            createElementCount++;
                        } else {
                            index = index == -1 ? i : RandomUtil.randomBoolean() ? i : index;
                        }
                    }
                    if (changeCount == 0 && createElementCount < minIconCount) {
                        temp[index] = wildChance > RandomUtil.randomInt(10000) ? PegasusUnbridleConstant.ElementId.WILD : icon;
                        changeCount++;
                        createElementCount++;
                    }
                    //生成结果
                    PegasusUnbridleResultLib specialLib = new PegasusUnbridleResultLib();
                    specialLib.setId(RandomUtils.getUUid());
                    specialLib.setRollerMode(elementRollMap.get(icon));
                    specialLib.setGameType(lib.getGameType());
                    specialLib.setSpecialModeIcon(icon);
                    specialLib.setIconArr(Arrays.copyOf(temp, temp.length));
                    List<PegasusUnbridleAwardLineInfo> specialAwardLineInfoList = winLines(specialLib, freeModel);
                    specialLib.setAwardLineInfoList(specialAwardLineInfoList);
                    lib.addSpecialResult(specialLib);
                    calTimes(specialLib);
                    if (iconCount + changeCount == temp.length) {
                        specialLib.addJackpotId(PegasusUnbridleConstant.Common.JACKPOT_ID);
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
    protected PegasusUnbridleAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        PegasusUnbridleAwardLineInfo awardLineInfo = new PegasusUnbridleAwardLineInfo();
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
        loadModelRandom();

        loadIconRandom();

        loadGenerateIcon();

        loadElementRollMap();
    }

    private void loadModelRandom() {
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

    private void loadIconRandom() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(PegasusUnbridleConstant.Common.RANDOM_ICON_PLAY_ID);
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
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(PegasusUnbridleConstant.Common.GENERATE_ICON_PLAY_ID);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        String[] split = specialPlayCfg.getValue().split("_");
        if (split.length != 3) {
            return;
        }
        weight = Integer.parseInt(split[1]);
        minIconCount = Integer.parseInt(split[0]);
        wildChance = Integer.parseInt(split[2]);
    }

    private void loadElementRollMap() {
        SpecialPlayCfg specialPlayCfg = GameDataManager.getSpecialPlayCfg(PegasusUnbridleConstant.Common.ELEMENT_ROLL);
        if (specialPlayCfg == null || StringUtil.isEmpty(specialPlayCfg.getValue())) {
            return;
        }
        Map<Integer, Integer> tempMap = new HashMap<>();
        for (String cfg : specialPlayCfg.getValue().split(";")) {
            String[] kekValue = cfg.split("_");
            if (kekValue.length != 2) {
                continue;
            }
            tempMap.put(Integer.parseInt(kekValue[0]), Integer.parseInt(kekValue[1]));
        }
        elementRollMap = tempMap;
    }
}
