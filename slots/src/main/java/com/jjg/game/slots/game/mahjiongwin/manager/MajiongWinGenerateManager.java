package com.jjg.game.slots.game.mahjiongwin.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.BaseRollerCfg;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.mahjiongwin.MahjiongWinConstant;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinAddIconInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinAwardLineInfo;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/1 17:33
 */
@Component
public class MajiongWinGenerateManager extends AbstractSlotsGenerateManager<MahjiongWinAwardLineInfo, MahjiongWinResultLib> {
    public MajiongWinGenerateManager() {
        super(MahjiongWinResultLib.class);
    }



    @Override
    public MahjiongWinResultLib checkAward(int[] arr, MahjiongWinResultLib lib) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查满线图案
        List<MahjiongWinAwardLineInfo> fullLineInfoList = fullLine(arr);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib.getLibTypeSet(), arr, lib.getSpecialGirdInfoList());
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);


        //存储消除后添加的图标
        List<MahjiongWinAddIconInfo> addIconInfoList = new ArrayList<>();
        //是否有消除
        repairIcons(lib.getIconArr(), lib.getAwardLineInfoList(), addIconInfoList);

        if (!addIconInfoList.isEmpty()) {
            lib.setAddIconInfos(addIconInfoList);
        }

        calTimes(lib);
        return lib;
    }

    @Override
    protected MahjiongWinAwardLineInfo addFullLineAwardInfo(Map<Integer, Set<Integer>> map, BaseElementRewardCfg cfg) {
        MahjiongWinAwardLineInfo info = new MahjiongWinAwardLineInfo();
        info.setSameMap(map);
        info.setBaseTimes(cfg.getBet());
        return info;
    }

    /**
     * 修补图标
     */
    public void repairIcons(int[] arr, List<MahjiongWinAwardLineInfo> list, List<MahjiongWinAddIconInfo> addIconInfoList) {
        if (list == null || list.isEmpty()) {
            return;
        }

        //拷贝数组
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);

        Map<Integer, Integer> addIconSizeMap = new HashMap<>();

        MahjiongWinAddIconInfo addIconInfo = new MahjiongWinAddIconInfo();

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (MahjiongWinAwardLineInfo info : list) {
            if (info.getSameMap() == null || info.getSameMap().isEmpty()) {
                continue;
            }

            for (Map.Entry<Integer, Set<Integer>> en : info.getSameMap().entrySet()) {
                allSameMap.computeIfAbsent(en.getKey(), k -> new HashSet<>()).addAll(en.getValue());
            }
        }

        for (Map.Entry<Integer, Set<Integer>> en : allSameMap.entrySet()) {
            int colIndex = en.getKey();
            Set<Integer> set = en.getValue();
            processIcons(colIndex, set, arr);

            addIconSizeMap.put(colIndex, set.size());
        }

        addIconInfo.setAddIconCountMap(addIconSizeMap);

        //检查中奖
        List<MahjiongWinAwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);
        addIconInfoList.add(addIconInfo);

        repairIcons(newArr, newAwardInfoList, addIconInfoList);
    }

    /**
     * 处理图标消除、下落和补充
     *
     * @param colIndex
     * @param removedIndexes 被消除的图标索引集合
     * @param arr
     * @return 新增的图标id
     */
    public void processIcons(int colIndex, Set<Integer> removedIndexes, int[] arr) {
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
            if(removedIndexes.contains(i)){
                //判断消除的图标是不是金色图标
                if(icon >= MahjiongWinConstant.BaseElement.GOLD_MIN && icon <= MahjiongWinConstant.BaseElement.GOLD_MAX){
                    Integer replaceIcon = this.replaceIconMap.get(icon);
                    if(replaceIcon != null){
                        validIndexes.add(replaceIcon);
                    }
                }
            }else {
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
            if(oldIcon > 0){
                continue;
            }

            int elementId = baseRollerCfg.getElements().get(scopeIndex);
            arr[index] = elementId;
            log.debug("补充新图标 index = {}, icon = {}", index, elementId);

            scopeIndex++;
        }

//        System.out.println("补充后打印 ");
//        printResult(arr);
//        System.out.println();
    }


    @Override
    public void calTimes(MahjiongWinResultLib lib) throws Exception {
        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //免费
        lib.addTimes(calFree(lib));
        //免费
        lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    private int calLineTimes(List<MahjiongWinAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (MahjiongWinAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 计算免费游戏的总倍数
     *
     * @param lib
     * @return
     */
    private long calFree(MahjiongWinResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                MahjiongWinResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), MahjiongWinResultLib.class);
                calTimes(tmpLib);
                times += tmpLib.getTimes();
            }
        }
        return times;
    }

    /**
     * 计算消除补齐后的中奖倍数
     *
     * @param addIconInfos
     * @return
     */
    private long calAfterAddIcons(List<MahjiongWinAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (MahjiongWinAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (MahjiongWinAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    private void printResult(int[] arr) {
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= 4; i++) {
            for (int j = 0; j < 5; j++) {
                int index = 4 * j + i;
                int id = arr[index];
                sb.append(id);
                if (id < 10) {
                    sb.append("   ");
                } else {
                    sb.append("  ");
                }
            }
            sb.append("\n");
        }
        System.out.println(sb);
    }
}
