package com.jjg.game.slots.game.steamAge.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.steamAge.SteamAgeConstant;
import com.jjg.game.slots.game.steamAge.data.*;

import com.jjg.game.slots.game.steamAge.data.SteamAgeAddFreeInfo;
import com.jjg.game.slots.game.steamAge.data.SteamAgeAwardLineInfo;
import com.jjg.game.slots.game.steamAge.data.SteamAgeResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @auSteamAge lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class SteamAgeGenerateManager extends AbstractSlotsGenerateManager<SteamAgeAwardLineInfo, SteamAgeResultLib> {

    public SteamAgeGenerateManager() {
        super(SteamAgeResultLib.class);
    }

    private SteamAgeAddFreeInfo steamAgeAddFreeInfo;

    private Map<Integer, Map<Integer, SteamAgeExpandRollerInfo>> steamAgeExpandRollerInfoMap;

    public Map<Integer, Map<Integer, SteamAgeExpandRollerInfo>> getSteamAgeExpandRollerInfoMap() {
        return steamAgeExpandRollerInfoMap;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(SteamAgeResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());
        //已经出现的小游戏id
        Set<Integer> showAuxiliaryIdSet = new HashSet<>();
        addShowAuxiliaryId(lib, showAuxiliaryIdSet);

        log.debug("检查全局分散");

        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();

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

            //是否触发小游戏
            if (cfg.getFeatureTriggerId() != null && !cfg.getFeatureTriggerId().isEmpty()) {
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    if (!showAuxiliaryIdSet.contains(miniGameId)) { //如果没出现过的小游戏可以触发
                        lib.getLibTypeSet().forEach(libType -> {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                            if (specialAuxiliaryInfo != null) {
                                showAuxiliaryIdSet.add(miniGameId);
                                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                            }
                        });
                    }

                });
            }

            if (lib.getJackpotId() < 1) {
                lib.setJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public SteamAgeResultLib checkAward(int[] arr, SteamAgeResultLib lib, boolean freeModel) throws Exception {
        if (freeModel) {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<SteamAgeAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //存储消除后添加的图标
            List<SteamAgeExpandIconInfo> addIconInfoList = new ArrayList<>();

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            //是否有消除
            repairIcons(SteamAgeConstant.SpecialMode.FREE, newArr, lib.getAwardLineInfoList(), addIconInfoList, lib.getExpandTimes());

            if (!addIconInfoList.isEmpty()) {
                lib.setAddIconInfos(addIconInfoList);
            }

            calTimes(lib);
            return lib;
        } else {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<SteamAgeAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //存储消除后添加的图标
            List<SteamAgeExpandIconInfo> addIconInfoList = new ArrayList<>();

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            if (lib.getAwardLineInfoList() != null && !lib.getAwardLineInfoList().isEmpty()) {
                lib.getLibTypeSet().forEach(type -> {
                    //是否新增列
                    repairIcons(SteamAgeConstant.SpecialMode.NORMAL, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);
                });
            }

            if (!addIconInfoList.isEmpty()) {
                lib.setAddIconInfos(addIconInfoList);
            }

            calTimes(lib);

            return lib;
        }
    }

    @Override
    protected SteamAgeAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
        SteamAgeAwardLineInfo info = new SteamAgeAwardLineInfo();

        info.setSameIconSet(sameIconIndexSet);
        info.setSameIcon(cfg.getElementId().getFirst() % 10);
        info.setBaseTimes(1);
        info.setLineTimes(cfg.getBet());
        info.setTotalTimes(info.getLineTimes());
        return info;
    }

    @Override
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        log.debug("增加免费游戏次数 addCount = {}", freeCount);

        int remainFreeCount = freeCount;

        //累计 新增列数
        int expandTimes = 0;

        while (remainFreeCount > 0) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            SteamAgeResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID,expandTimes);
            int addCount = checkAddFreeCount(lib);
            lib.setAddFreeCount(addCount);
            expandTimes = expandTimes + (lib.getAddIconInfos() == null || lib.getAddIconInfos().isEmpty() ? 0 : lib.getAddIconInfos().size());
//            System.out.println("expandTimes= "+expandTimes);
//            lib.setExpandTimes(expandTimes);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            log.debug("--------------{}------------", remainFreeCount);
            remainFreeCount--;
        }
    }

    public SteamAgeResultLib generateFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID,int expandTimes) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            SteamAgeResultLib lib = createResultLib();
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
            //赋值扩展中奖
            lib.setExpandTimes(expandTimes);
            //判断中奖，返回
            return checkAward(arr, lib, true);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }


    /**
     * 检查是否增加免费次数
     *
     * @param lib
     * @return
     */
    private int checkAddFreeCount(SteamAgeResultLib lib) {
        if (this.steamAgeAddFreeInfo.getLibType() != SteamAgeConstant.SpecialMode.FREE) {
            return 0;
        }

        int addCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            //是否出现了目标图标
            if (icon != this.steamAgeAddFreeInfo.getTargetIcon()) {
                continue;
            }
            boolean flag = SlotsUtil.calProp(this.steamAgeAddFreeInfo.getProp());
            if (flag) {
                addCount += this.steamAgeAddFreeInfo.getAddFreeCount();
            }
        }
        return addCount;
    }

    /**
     * 修补图标
     */
    public void repairIcons(int libType, int[] arr, List<SteamAgeAwardLineInfo> list, List<SteamAgeExpandIconInfo> addIconInfoList, int expandTimes) {
        if (list == null || list.isEmpty()) {
            return;
        }
//        //连续中奖后重置中奖倍数
        resetLineRewardTimes(libType, list, expandTimes);

        SteamAgeExpandIconInfo addIconInfo = new SteamAgeExpandIconInfo();


        List<SteamAgeAwardLineInfo> newAwardInfoList = processIcons(libType, expandTimes, arr, addIconInfo);

        //检查中奖
        addIconInfoList.add(addIconInfo);

        if (newAwardInfoList != null && !newAwardInfoList.isEmpty()) {
            expandTimes++;
//            System.out.println("===========expandTimes= "+expandTimes);
            repairIcons(libType, arr, newAwardInfoList, addIconInfoList, expandTimes);
        }

    }

    private void resetLineRewardTimes(int libType, List<SteamAgeAwardLineInfo> list, int expandTimes) {
        for (int i = 0; i < list.size(); i++) {
            if (steamAgeExpandRollerInfoMap.get(libType) == null || steamAgeExpandRollerInfoMap.get(libType).get(expandTimes) == null) {
                return;
            }
            //新增列
            SteamAgeExpandRollerInfo steamAgeExpandRollerInfo = steamAgeExpandRollerInfoMap.get(libType).get(expandTimes);
            Integer times = steamAgeExpandRollerInfo.getBaseTimes();
            SteamAgeAwardLineInfo steamAgeAwardLineInfo = list.get(i);
            steamAgeAwardLineInfo.setBaseTimes(times);
            steamAgeAwardLineInfo.setTotalTimes(times * steamAgeAwardLineInfo.getLineTimes());
        }
    }

    /**
     * 处理图标新增列和补充
     *
     * @param expandTimes
     * @param arr
     * @return 新增的图标id
     */
    public List<SteamAgeAwardLineInfo> processIcons(int libType, int expandTimes, int[] arr, SteamAgeExpandIconInfo addIconInfo) {
        //坐标对应添加的
        List addIconList = new ArrayList<>();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        //jackPool 玩法其实免费转 默认 1
        if (libType == SteamAgeConstant.SpecialMode.JACKPOOL) {
            libType = 1;
        }
        if (steamAgeExpandRollerInfoMap.get(libType) == null || steamAgeExpandRollerInfoMap.get(libType).get(expandTimes) == null) {
            throw new RuntimeException(libType + ":玩法" + expandTimes + ":新增列，SpecialPlay.xlsx 配置没有");
        }
        SteamAgeExpandRollerInfo steamAgeExpandRollerInfo = steamAgeExpandRollerInfoMap.get(libType).get(expandTimes);
        BaseRollerCfg baseRollerCfg = GameDataManager.getBaseRollerCfg(steamAgeExpandRollerInfo.getRollerId());

        int first = baseRollerCfg.getAxleCountScope().get(0) - 1;
        int last = baseRollerCfg.getAxleCountScope().get(1) - 1;
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        int[] bakArr = new int[arr.length];
        System.arraycopy(arr, 0, bakArr, 0, arr.length);
//        for (int i = 0; i < addIconList.size(); i++) {
//            newArr[i] = addIconList.get(i);
//        }
        //后移动 补充图标
        for (int i = 0; i < arr.length; i++) {
            //第一列是补充的新图标
            if (i > 4) {
                arr[i] = bakArr[i - 4];
            }
        }
        //根据权重是否中奖
        int weight = steamAgeExpandRollerInfo.getWeight();
        int randomNumber = RandomUtils.nextInt(10000);

        //100次确认是否权重中奖，权重不一致弹出
        for (int i = 0; i < 1000; i++) {
            addIconList.clear();
            // 补充新图标 只补充第一列
            for (int j = 1; j <= baseInitCfg.getRows(); j++) {
                if (scopeIndex > last) {
                    scopeIndex = first;
                }
                int index = j;
                int elementId = baseRollerCfg.getElements().get(scopeIndex);
                arr[index] = elementId;
                addIconList.add(elementId);
                log.debug("补充新图标 index = {}, icon = {}", index, elementId);
                scopeIndex++;
            }
            log.debug("weight >= randomNumber {} ,weight {},randomNumber {}", weight >= randomNumber, weight, randomNumber);
            if (weight >= randomNumber) {
                List<SteamAgeAwardLineInfo> steamAgeAwardLineInfos = fullLine(arr);
                if (steamAgeAwardLineInfos != null && !steamAgeAwardLineInfos.isEmpty()) {
                    addIconInfo.setAwardLineInfoList(steamAgeAwardLineInfos);
//                    steamAgeAwardLineInfos.add(addIconInfo);
                    addIconInfo.setAddIconList(addIconList);
                    return steamAgeAwardLineInfos;
                }
            } else {
                List<SteamAgeAwardLineInfo> steamAgeAwardLineInfos = fullLine(arr);
                if (steamAgeAwardLineInfos == null || steamAgeAwardLineInfos.isEmpty()) {
                    addIconInfo.setAddIconList(addIconList);
                    addIconInfo.setAwardLineInfoList(null);
                    return steamAgeAwardLineInfos;
                }
            }
        }
        List<SteamAgeAwardLineInfo> steamAgeAwardLineInfos = fullLine(arr);
        addIconInfo.setAddIconList(addIconList);
        addIconInfo.setAwardLineInfoList(steamAgeAwardLineInfos);
        return steamAgeAwardLineInfos;
    }


    @Override
    public void calTimes(SteamAgeResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            throw new IllegalArgumentException("检查结果有错误 lib = " + JSONObject.toJSONString(lib));
        }

        if(triggerFreeLib(lib,SteamAgeConstant.SpecialMode.FREE)){
            //免费
            lib.addTimes(calFree(lib));
        }else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
            //消除后新增图标
            lib.addTimes(calAfterAddIcons(lib.getAddIconInfos()));
        }
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<SteamAgeAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (SteamAgeAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getTotalTimes();
        }
        return times;
    }

    /**
     * 计算消除补齐后的中奖倍数
     *
     * @param addIconInfos
     * @return
     */
    public long calAfterAddIcons(List<SteamAgeExpandIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SteamAgeExpandIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (SteamAgeAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getTotalTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        Map<Integer, Map<Integer, Integer>> tmpAddTimesMap = new HashMap<>();

        int tmpMaxWinCount = 0;
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            if (cfg.getPlayType() == SteamAgeConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {  //增加免费次数
                SteamAgeAddFreeInfo tmpSteamAgeAddFreeInfo = new SteamAgeAddFreeInfo();
                String[] arr = cfg.getValue().split("_");

                tmpSteamAgeAddFreeInfo.setLibType(Integer.parseInt(arr[0]));
                tmpSteamAgeAddFreeInfo.setTargetIcon(Integer.parseInt(arr[1]));
                tmpSteamAgeAddFreeInfo.setAddFreeCount(Integer.parseInt(arr[2]));
                tmpSteamAgeAddFreeInfo.setProp(Integer.parseInt(arr[3]));

                this.steamAgeAddFreeInfo = tmpSteamAgeAddFreeInfo;
            } else if (cfg.getPlayType() == SteamAgeConstant.SpecialPlay.TYPE_EXTEND_ICON_NORMAL) {  //新增一列进行摇奖 正常
                if (this.steamAgeExpandRollerInfoMap == null) {
                    this.steamAgeExpandRollerInfoMap = new HashMap<>();
                }

                String[] arr = cfg.getValue().split(",");
                String[] arr1 = arr[1].split("\\|");
                Map<Integer, SteamAgeExpandRollerInfo> trmpSteamAgeExpandRollerInfoMap = new HashMap<>();
                for (String str : arr1) {
                    SteamAgeExpandRollerInfo trmpSteamAgeExpandRollerInfo = new SteamAgeExpandRollerInfo();
                    String[] arr3 = str.split("_");
                    trmpSteamAgeExpandRollerInfo.setLibType(SteamAgeConstant.SpecialMode.NORMAL);
                    trmpSteamAgeExpandRollerInfo.setWinTimes(Integer.parseInt(arr3[0]));
                    trmpSteamAgeExpandRollerInfo.setRollerId(Integer.parseInt(arr3[1]));
                    trmpSteamAgeExpandRollerInfo.setWeight(Integer.parseInt(arr3[2]));
                    trmpSteamAgeExpandRollerInfo.setBaseTimes(Integer.parseInt(arr3[3]));
                    trmpSteamAgeExpandRollerInfoMap.put(trmpSteamAgeExpandRollerInfo.getWinTimes(), trmpSteamAgeExpandRollerInfo);
                }


                this.steamAgeExpandRollerInfoMap.put(SteamAgeConstant.SpecialMode.NORMAL, trmpSteamAgeExpandRollerInfoMap);
            } else if (cfg.getPlayType() == SteamAgeConstant.SpecialPlay.TYPE_EXTEND_ICON_FREE) {  //新增一列进行摇奖 免费转
                if (this.steamAgeExpandRollerInfoMap == null) {
                    this.steamAgeExpandRollerInfoMap = new HashMap<>();
                }
                String[] arr = cfg.getValue().split(",");
                String[] arr1 = arr[1].split("\\|");
                Map<Integer, SteamAgeExpandRollerInfo> trmpSteamAgeExpandRollerInfoMap = new HashMap<>();
                for (String str : arr1) {
                    SteamAgeExpandRollerInfo trmpSteamAgeExpandRollerInfo = new SteamAgeExpandRollerInfo();
                    String[] arr3 = str.split("_");
                    trmpSteamAgeExpandRollerInfo.setLibType(SteamAgeConstant.SpecialMode.FREE);
                    trmpSteamAgeExpandRollerInfo.setWinTimes(Integer.parseInt(arr3[0]));
                    trmpSteamAgeExpandRollerInfo.setRollerId(Integer.parseInt(arr3[1]));
                    trmpSteamAgeExpandRollerInfo.setWeight(Integer.parseInt(arr3[2]));
                    trmpSteamAgeExpandRollerInfo.setBaseTimes(Integer.parseInt(arr3[3]));
                    trmpSteamAgeExpandRollerInfoMap.put(trmpSteamAgeExpandRollerInfo.getWinTimes(), trmpSteamAgeExpandRollerInfo);
                }


                this.steamAgeExpandRollerInfoMap.put(SteamAgeConstant.SpecialMode.FREE, trmpSteamAgeExpandRollerInfoMap);
            }
        }
    }


    protected void printResult(int[] arr) {
        BaseInitCfg cfg = GameDataManager.getBaseInitCfg(this.gameType);

        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= cfg.getRows(); i++) {
            for (int j = 0; j < cfg.getCols(); j++) {
                int index = cfg.getRows() * j + i;
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

    /**
     * 添加已经出现的小游戏id
     *
     * @param lib
     * @param set
     */
    private void addShowAuxiliaryId(SteamAgeResultLib lib, Set<Integer> set) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }

        lib.getSpecialAuxiliaryInfoList().forEach(info -> {
            set.add(info.getCfgId());
        });
    }

    /**
     * 检查奖池模式
     *
     * @param lib
     * @return
     */
    private boolean checkJackpool(SteamAgeResultLib lib) {
        if (lib.getJackpotId() < 1) {
            return false;
        }
        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == SteamAgeConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == SteamAgeConstant.BaseElement.ID_MINI || icon == SteamAgeConstant.BaseElement.ID_MINOR ||
                    icon == SteamAgeConstant.BaseElement.ID_MAJOR || icon == SteamAgeConstant.BaseElement.ID_GRAND) {
                jackpool++;
            }
        }
        List<SteamAgeExpandIconInfo> addIconInfos = lib.getAddIconInfos();
        if (addIconInfos != null && !addIconInfos.isEmpty()) {
            for (SteamAgeExpandIconInfo addIconInfo : addIconInfos) {
                for (Integer icon : addIconInfo.getAddIconList()) {
                    if (icon == SteamAgeConstant.BaseElement.ID_SCATTER) {
                        count++;
                    } else if (icon == SteamAgeConstant.BaseElement.ID_MINI || icon == SteamAgeConstant.BaseElement.ID_MINOR ||
                            icon == SteamAgeConstant.BaseElement.ID_MAJOR || icon == SteamAgeConstant.BaseElement.ID_GRAND) {
                        jackpool++;
                    }
                }
            }
        }
        return count >= 2 && jackpool > 0;
    }

    /**
     * 检查免费触发局
     *
     * @param lib
     * @return
     */
    private boolean checkTriggerFree(SteamAgeResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == SteamAgeConstant.BaseElement.ID_SCATTER) {
                count++;
            }
        }
        List<SteamAgeExpandIconInfo> addIconInfos = lib.getAddIconInfos();
        if (addIconInfos != null && !addIconInfos.isEmpty()) {
            for (SteamAgeExpandIconInfo addIconInfo : addIconInfos) {
                for (Integer icon : addIconInfo.getAddIconList()) {
                    if (icon == SteamAgeConstant.BaseElement.ID_SCATTER) {
                        count++;
                    }
                }
            }
        }
        return count >= 3;
    }

    /**
     * 检查元素与小游戏所需要的参数是否匹配
     *
     * @param lib
     */
    private boolean checkElement(SteamAgeResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(SteamAgeConstant.SpecialMode.FREE)
                && !checkTriggerFree(lib)) {
            log.warn("检查免费触发局失败");
            return false;
        }

        //检查jackpool模式
        if (lib.getLibTypeSet().contains(SteamAgeConstant.SpecialMode.JACKPOOL)
                && !checkJackpool(lib)) {
            log.warn("检查jackpool模式失败");
            return false;
        }

        return true;
    }


}
