package com.jjg.game.slots.game.pegasusunbridle.manager;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
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
    private WeightRandom<Integer> iconRandom;
    //随机到图标得概率（万分比）
    private int weight;
    //转轴上最低出现元素个数
    private int minIconCount;
    //_随机图标变成wild概率
    private int wildChance;
    //元素id->滚轴id
    private Map<Integer, Integer> elementRollMap;



    @Override
    public PegasusUnbridleResultLib checkAward(int[] arr, PegasusUnbridleResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);
        //检查连线
        List<PegasusUnbridleAwardLineInfo> awardLineInfoList = winLines(lib, freeModel);
        lib.setAwardLineInfoList(awardLineInfoList);
        if (CollectionUtil.isNotEmpty(lib.getLibTypeSet())) {
            for (Integer libType : lib.getLibTypeSet()) {
                if (libType == PegasusUnbridleConstant.SpecialMode.NORMAL) {
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
                int[] temp = new int[iconArr.length];
                System.arraycopy(iconArr, 1, iconArr, 0, iconArr.length - 1);
                //设置为空白元素
                Arrays.fill(temp, 1, temp.length, PegasusUnbridleConstant.ElementId.BLANK);
                int realCount = temp.length - 1;
                int maxRandomNum = libType == PegasusUnbridleConstant.SpecialMode.JACKPOT ? realCount : realCount - 1;
                int minRandomNum = libType == PegasusUnbridleConstant.SpecialMode.JACKPOT ? realCount : minIconCount;
                int createElementCount = 0;
                while (true) {
                    RandomIconResult randomIcon = getRandomIcon(temp, icon, createElementCount, minRandomNum, maxRandomNum);
                    createElementCount = randomIcon.createElementCount;
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
                    if (randomIcon.createElementCount == realCount) {
                        specialLib.addJackpotId(PegasusUnbridleConstant.Common.JACKPOT_ID);
                        break;
                    }
                    if (randomIcon.changeCount == 0) {
                        break;
                    }
                }
            }
        }
        calTimes(lib);
        return lib;
    }

    public RandomIconResult getRandomIcon(int[] temp, int icon, int createElementCount, int minIconCount, int maxRandomNum) {
        int changeCount = 0;
        int index = -1;
        for (int i = 1; i < temp.length; i++) {
            int oldIcon = temp[i];
            if (oldIcon != PegasusUnbridleConstant.ElementId.BLANK) {
                continue;
            }
            if (maxRandomNum == createElementCount) {
                return new RandomIconResult(changeCount, createElementCount);
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
        return new RandomIconResult(changeCount, createElementCount);
    }

    public record RandomIconResult(int changeCount, int createElementCount) {

    }

    @Override
    protected PegasusUnbridleAwardLineInfo getAwardLineInfo() {
        return new PegasusUnbridleAwardLineInfo();
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
