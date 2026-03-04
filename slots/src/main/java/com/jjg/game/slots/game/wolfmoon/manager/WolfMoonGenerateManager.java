package com.jjg.game.slots.game.wolfmoon.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.sampledata.bean.SpecialAuxiliaryCfg;
import com.jjg.game.sampledata.bean.SpecialModeCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.wolfmoon.WolfMoonConstant;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAddIconInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonAwardLineInfo;
import com.jjg.game.slots.game.wolfmoon.data.WolfMoonResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/2/27 15:33
 */
@Component
public class WolfMoonGenerateManager extends AbstractSlotsGenerateManager<WolfMoonAwardLineInfo, WolfMoonResultLib> {

    public WolfMoonGenerateManager() {
        super(WolfMoonResultLib.class);
    }

    @Override
    public WolfMoonResultLib generateFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            WolfMoonResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());

            //获取rollerMode
            int rollerMode = specialAuxiliaryCfg.getRollerMode();
            if (rollerMode < 1) {
                rollerMode = specialModeCfg.getRollerMode();
            }

            //生成所有的图标
            int[] arr = generateAllIcons(rollerMode, specialModeCfg.getCols(), specialModeCfg.getRows());
            if (arr == null) {
                return null;
            }

            log.debug("生成免费游戏图标 arr = {}", Arrays.toString(arr));

            //修改格子策略组
            if (specialGroupGirdID > 0) {
                SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGroupGirdID, arr);
                if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                    lib.addSpecialGirdInfo(specialGirdInfo);
                }
            }
            //修改格子
            if (specialAuxiliaryCfg.getSpecialGirdID() != null && !specialAuxiliaryCfg.getSpecialGirdID().isEmpty()) {
                for (int specialGirdCfgId : specialAuxiliaryCfg.getSpecialGirdID()) {
                    SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGirdCfgId, arr);
                    if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                        lib.addSpecialGirdInfo(specialGirdInfo);
                    }
                }
            }
            //判断中奖，返回
            return checkAward(arr, lib, true);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    @Override
    public WolfMoonResultLib checkAward(int[] arr, WolfMoonResultLib lib, boolean freeModel) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查满线图案_x连
        List<WolfMoonAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //检查满线图案_数量
        List<WolfMoonAwardLineInfo> fullLineCountInfoList = fullLineCount(lib);
        lib.addAllAwardLineInfo(fullLineCountInfoList);

        //检查连线分散数量
        List<WolfMoonAwardLineInfo> lineDispersionCount = lineDispersionCount(lib);
        lib.addAllAwardLineInfo(lineDispersionCount);

        //存储消除后添加的图标
        List<WolfMoonAddIconInfo> addIconInfoList = new ArrayList<>();
        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);

        //是否有消除
//        repairIcons(newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);

        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }

        //计算倍数
        calTimes(lib);
        return lib;
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        int remainFreeCount = freeCount;
        while (remainFreeCount > 0) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            WolfMoonResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            int addCount = checkAddFreeCount(lib);
            lib.setAddFreeCount(addCount);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            remainFreeCount--;
        }
    }

    /**
     * 检查是否增加免费次数
     *
     * @param lib
     * @return
     */
    private int checkAddFreeCount(WolfMoonResultLib lib) {
        int addCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            //是否出现了目标图标
            if (icon != WolfMoonConstant.BaseElement.EXTRA_FREE) {
                continue;
            }
            addCount++;
        }
        return addCount;
    }

    @Override
    protected WolfMoonAwardLineInfo getAwardLineInfo() {
        return new WolfMoonAwardLineInfo();
    }


    /**
     * 修补图标
     */
    public void repairIcons(int[] arr, List<WolfMoonAwardLineInfo> list, List<WolfMoonAddIconInfo> addIconInfoList, int winCount) {
        if (list == null || list.isEmpty()) {
            return;
        }

        winCount++;

        WolfMoonAddIconInfo addIconInfo = new WolfMoonAddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (WolfMoonAwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            info.getSameIconSet().forEach(index -> {
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);
            });
        }

        //坐标对应添加的
        Map<Integer, Integer> addIconMap = new HashMap<>();

        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            int colIndex = en.getKey();
            Set<Integer> set = en.getValue();
            //处理图标消除、下落和补充
            processIcons(colIndex, set, arr, addIconMap);
        }

        addIconInfo.setAddIconMap(addIconMap);

        //检查中奖
        List<WolfMoonAwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

//        repairIcons(arr, newAwardInfoList, addIconInfoList, winCount);
    }


    /**
     * 处理图标消除、下落和补充
     *
     * @param colIndex
     * @param removedIndexes 被消除的图标索引集合
     * @param arr
     * @return 新增的图标id
     */
    public void processIcons(int colIndex, Set<Integer> removedIndexes, int[] arr,
                             Map<Integer, Integer> addIconMap) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();

//        System.out.println("需要消除的坐标 removedIndexes = " + removedIndexes);
//        System.out.println("消除前打印 ");
//        printResult(arr);

        //这一列开始坐标
        int beginIndex = (colIndex - 1) * rows + 1;
        //这一列结束坐标
        int endIndex = beginIndex + rows - 1;

        //找到这一列，消除后应该剩余的图标
        List<Integer> validIndexes = new ArrayList<>(baseInitCfg.getRows() - removedIndexes.size());
        for (int i = beginIndex; i <= endIndex; i++) {
            int icon = arr[i];
            if (removedIndexes.contains(i)) {
                //判断消除的图标是不是金色图标
                if (icon >= MahjiongWinConstant.BaseElement.GOLD_MIN && icon <= MahjiongWinConstant.BaseElement.GOLD_MAX) {
                    Integer replaceIcon = getPostChangeIcon(icon);
                    if (replaceIcon != null) {
                        validIndexes.add(replaceIcon);
                    }
                }
            } else {
                validIndexes.add(icon);
            }
            arr[i] = -1;
        }

        validIndexes = validIndexes.reversed();

        //将剩余的图标重新填充回去
        int curIndex = endIndex;
        for (int i = 0; i < validIndexes.size(); i++) {
            arr[curIndex] = validIndexes.get(i);
            curIndex--;
        }

//        System.out.println("消除后打印 ");
//        printResult(arr);

        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.entrySet().stream().findFirst().get().getValue();
        BaseRollerCfg baseRollerCfg = rollerCfgMap.get(colIndex);

        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        // 从顶部开始补充新图标
        for (int i = 0; i < baseInitCfg.getRows(); i++) {
            if (scopeIndex > last) {
                scopeIndex = first;
            }
            int index = beginIndex + i;
            int oldIcon = arr[index];
            if (oldIcon > 0) {
                continue;
            }

            int elementId = baseRollerCfg.getElements().get(scopeIndex);
            arr[index] = elementId;
            addIconMap.put(index, elementId);
            log.debug("补充新图标 index = {}, icon = {}", index, elementId);

            scopeIndex++;
        }

//        System.out.println("补充后打印 ");
//        printResult(arr);
//        System.out.println();
    }

    @Override
    public void calTimes(WolfMoonResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //消除后新增图标
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<WolfMoonAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (WolfMoonAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 计算消除补齐后的中奖倍数
     *
     * @param addIconInfos
     * @return
     */
    public long calAfterAddIcons(List<WolfMoonAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (WolfMoonAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (WolfMoonAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }
}
