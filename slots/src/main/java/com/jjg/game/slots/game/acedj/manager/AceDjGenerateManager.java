package com.jjg.game.slots.game.acedj.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.acedj.AceDjConstant;
import com.jjg.game.slots.game.acedj.data.AceDjAddFreeInfo;
import com.jjg.game.slots.game.acedj.data.AceDjAddIconInfo;
import com.jjg.game.slots.game.acedj.data.AceDjAwardLineInfo;
import com.jjg.game.slots.game.acedj.data.AceDjResultLib;
import com.jjg.game.slots.game.frozenThrone.FrozenThroneConstant;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class AceDjGenerateManager extends AbstractSlotsGenerateManager<AceDjAwardLineInfo, AceDjResultLib> {
    public AceDjGenerateManager() {
        super(AceDjResultLib.class);
    }

    //wild中奖倍数 list
    private List<Integer> wildTimesList;
    //连续中奖增加倍数时，最大连续中奖次数
    private int maxWinCount;
    //
    private List<AceDjAddFreeInfo> aceDjAddFreeInfoList;

    private Map<Integer, BaseElementCfg> baseElementCfgMap;

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(AceDjResultLib lib) {
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
                    int count = checkAddFreeCount(lib);
                    if (count > 0) {
                        Set<Integer> libTypeSet = new HashSet<>();
                        libTypeSet.add(AceDjConstant.SpecialMode.FREE);
                        lib.setLibTypeSet(libTypeSet);
                    }
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
    public AceDjResultLib checkAward(int[] arr, AceDjResultLib lib, boolean freeModel) throws Exception {
        if (freeModel) {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<AceDjAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //存储 Wild 中奖倍数
            lib.setWildTimes(wildTimesList.stream()
                    .limit(4)
                    .collect(Collectors.toList()));

            //存储消除后添加的图标
            List<AceDjAddIconInfo> addIconInfoList = new ArrayList<>();

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            //是否有消除
            repairIcons(AceDjConstant.SpecialMode.FREE, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);

            if (!addIconInfoList.isEmpty()) {
                lib.setAddIconInfos(addIconInfoList);
            }

            calTimes(lib);
            return lib;
        } else {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<AceDjAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //存储 Wild 中奖倍数
            lib.setWildTimes(wildTimesList.stream()
                    .limit(4)
                    .collect(Collectors.toList()));

            //存储消除后添加的图标
            List<AceDjAddIconInfo> addIconInfoList = new ArrayList<>();

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            if (lib.getLibTypeSet() != null && !lib.getLibTypeSet().isEmpty()) {
                lib.getLibTypeSet().forEach(type -> {
                    //是否有消除
                    repairIcons(type, newArr, lib.getAwardLineInfoList(), addIconInfoList, 0);
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
    protected AceDjAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
        AceDjAwardLineInfo info = new AceDjAwardLineInfo();

        info.setSameIconSet(sameIconIndexSet);
        info.setSameIcon(cfg.getElementId().getFirst() % 10);

        if (info.getSameIconSet() != null && !info.getSameIconSet().isEmpty()) {
            //记录每一列中奖的个数
            BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

            Map<Integer, Integer> columIconCountMap = new HashMap<>();
            for (int index : info.getSameIconSet()) {
                //根据坐标，计算它在哪一列
                int colId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    colId++;
                }
                columIconCountMap.merge(colId, 1, Integer::sum);
            }

            int addTimes = 1;
            for (Map.Entry<Integer, Integer> en : columIconCountMap.entrySet()) {
                addTimes *= en.getValue();
            }

            info.setBaseTimes(cfg.getBet() * addTimes);
        } else {
            info.setBaseTimes(cfg.getBet());
        }
        return info;
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

        log.debug("增加免费游戏次数 addCount = {}", freeCount);

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

            AceDjResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            int addCount = checkAddFreeCount(lib);
            lib.setAddFreeCount(addCount);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            log.debug("--------------{}------------", remainFreeCount);
            remainFreeCount--;
        }
    }

    /**
     * 检查是否增加免费次数
     *
     * @param lib
     * @return
     */
    private int checkAddFreeCount(AceDjResultLib lib) {
//        if (this.aceDjAddFreeInfo.getLibType() != AceDjConstant.SpecialMode.FREE) {
//            return 0;
//        }

        int addCount = 0;
        for (AceDjAddFreeInfo aceDjAddFreeInfo : aceDjAddFreeInfoList) {
            int num = 0;
            for (int i = 1; i < lib.getIconArr().length; i++) {
                int icon = lib.getIconArr()[i];
                //是否出现了目标图标
                if (icon != aceDjAddFreeInfo.getTargetIcon()) {
                    continue;
                }
                num++;
            }
            //是否命中 万分比
//            boolean flag = SlotsUtil.calProp(aceDjAddFreeInfo.getProp());
            if (num == aceDjAddFreeInfo.getNum()) {
                addCount = aceDjAddFreeInfo.getAddFreeCount();
            }
        }
        return addCount;
    }

    /**
     * 修补图标
     */
    public void repairIcons(int libType, int[] arr, List<AceDjAwardLineInfo> list, List<AceDjAddIconInfo> addIconInfoList, int wildNum) {
        if (list == null || list.isEmpty()) {
            return;
        }

        List<Integer> wildTimes;
        if (addIconInfoList == null || addIconInfoList.isEmpty()) {
            wildTimes =wildTimesList.stream()
                    .limit(4)
                    .collect(Collectors.toList());
            wildNum = 4;
        } else {
            AceDjAddIconInfo aceDjAddIconInfo = addIconInfoList.getLast();
            wildTimes = aceDjAddIconInfo.getWildTimes();
        }


        //连续中奖后重置中奖倍数
        resetLineRewardTimes(arr, wildTimes, list);

        AceDjAddIconInfo addIconInfo = new AceDjAddIconInfo();

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //将所有需要消除的图标进行汇总
        Map<Integer, Set<Integer>> allSameMap = new HashMap<>();
        for (AceDjAwardLineInfo info : list) {
            if (info.getSameIconSet() == null || info.getSameIconSet().isEmpty()) {
                continue;
            }

            //替换成wild的坐标
            Set<Integer> replaceWildIndexs = new HashSet<>();

            info.getSameIconSet().forEach(index -> {
                int columnId = index / baseInitCfg.getRows();
                if ((index % baseInitCfg.getRows()) != 0) {
                    columnId++;
                }
                allSameMap.computeIfAbsent(columnId, k -> new HashSet<>()).add(index);

                int icon = arr[index];

                //判断消除的图标是不是金色图标
                if (icon >= AceDjConstant.BaseElement.GOLD_MIN && icon <= AceDjConstant.BaseElement.GOLD_MAX) {
                    Integer replaceIcon = getPostChangeIcon(icon);
                    if (replaceIcon != null) {
                        replaceWildIndexs.add(index);
                    }
                }
            });

            info.setReplaceWildIndexs(replaceWildIndexs);
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
        List<AceDjAwardLineInfo> newAwardInfoList = fullLine(arr);

        addIconInfo.setAwardLineInfoList(newAwardInfoList);


        //判断中奖图标 是否包含wild
        Map<Integer, Integer> winTimes = addIconInfo.getWinTimes();
        if (winTimes == null) {
            winTimes = new HashMap<>();
        }
        Set<Integer> colsSet = new HashSet<>();
        for (AceDjAwardLineInfo lineInfo : newAwardInfoList) {
            for (Integer set : lineInfo.getSameIconSet()) {
                List<Integer> wildList = Arrays.stream(AceDjConstant.BaseElement.ID_WILD_ARR)
                        .boxed()
                        .collect(Collectors.toList());
                if (wildList.contains(arr[set])) {
                    int cols = set % baseInitCfg.getRows() > 0 ? set / baseInitCfg.getRows() - 1 : set / baseInitCfg.getRows() - 2;
                    winTimes.put(cols, wildTimes.get(cols));
                    addIconInfo.setWinTimes(winTimes);
                    colsSet.add(cols);
                }
            }
        }
        addIconInfo.setWinTimes(winTimes);

        for (Integer cols : colsSet) {
            wildTimes.remove((int)cols);
            wildNum++;
            if (wildNum >= wildTimesList.size()) {
                wildTimes.add(wildTimesList.get(wildTimesList.getLast()));
            } else {
                wildTimes.add(wildTimesList.get(wildNum));
            }
        }
        addIconInfo.setWildTimes(wildTimes);

        addIconInfoList.add(addIconInfo);

        repairIcons(libType, arr, newAwardInfoList, addIconInfoList, wildNum);
    }

    private void resetLineRewardTimes(int[] arr, List<Integer> wildTimes, List<AceDjAwardLineInfo> list) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        //修改倍数
        list.forEach(info -> {
            AtomicInteger wildTime = new AtomicInteger(0);
            Map<Integer, Integer> wildTimesMap = new HashMap<>();
            for (int i = 0; i < arr.length; i++) {
                int finalI = i;
                List<Integer> wildList = Arrays.stream(AceDjConstant.BaseElement.ID_WILD_ARR)
                        .boxed()
                        .collect(Collectors.toList());
                if (wildList.contains(arr[i])) {
                    int cols = finalI % baseInitCfg.getRows() > 0 ? finalI / baseInitCfg.getRows() - 1 : finalI / baseInitCfg.getRows() - 2;
                    wildTimesMap.put(cols, wildTimes.get(cols));
                }
            }
            Set<Integer> replaceWildIndexs = info.getReplaceWildIndexs();
            if (replaceWildIndexs != null) {
                for (Integer set : replaceWildIndexs) {
                    int cols = set % baseInitCfg.getRows() > 0 ? set / baseInitCfg.getRows() - 1 : set / baseInitCfg.getRows() - 2;
                    wildTimesMap.put(cols, wildTimes.get(cols));
                }
            }
            wildTimesMap.forEach((key, value) -> wildTime.set(wildTime.get() + value));
            if (wildTime.get() == 0) {
                info.setBaseTimes(info.getBaseTimes());
            } else {
                info.setBaseTimes(info.getBaseTimes() * wildTime.get());
            }
        });
    }

    /**
     * 处理图标消除、下落和补充
     *
     * @param colIndex
     * @param removedIndexes 被消除的图标索引集合
     * @param arr
     * @return 新增的图标id
     */
    public void processIcons(int colIndex, Set<Integer> removedIndexes, int[] arr, Map<Integer, Integer> addIconMap) {
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
                if (icon >= AceDjConstant.BaseElement.GOLD_MIN && icon <= AceDjConstant.BaseElement.GOLD_MAX) {
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
    public void calTimes(AceDjResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            throw new IllegalArgumentException("检查结果有错误 lib = " + JSONObject.toJSONString(lib));
        }

        if (triggerFreeLib(lib, AceDjConstant.SpecialMode.FREE)) {
            //免费
            lib.addTimes(calFree(lib));
        } else {
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
    public int calLineTimes(List<AceDjAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (AceDjAwardLineInfo awardLineInfo : list) {
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
    public long calAfterAddIcons(List<AceDjAddIconInfo> addIconInfos) {
        if (addIconInfos == null || addIconInfos.isEmpty()) {
            return 0;
        }

        long times = 0;
        for (AceDjAddIconInfo info : addIconInfos) {
            if (info.getAwardLineInfoList() == null || info.getAwardLineInfoList().isEmpty()) {
                continue;
            }

            for (AceDjAwardLineInfo awardLineInfo : info.getAwardLineInfoList()) {
                times += awardLineInfo.getBaseTimes();
            }
        }
        return times;
    }

    @Override
    protected void specialPlayConfig() {
        //初始化元素
        Map<Integer, BaseElementCfg> tmpBaseElementCfgMap = new HashMap<>();
        for (BaseElementCfg baseElementCfg : GameDataManager.getBaseElementCfgList()) {
            //游戏id
            if (baseElementCfg.getGameId() == this.gameType) {
                tmpBaseElementCfgMap.put(baseElementCfg.getElementId(), baseElementCfg);
            }
        }
        this.baseElementCfgMap = tmpBaseElementCfgMap;

        List<Integer> tmpWildTimesList = new ArrayList<>();

        int tmpMaxWinCount = 0;
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }
            //连续中奖 倍数 2_2_2_2_4_6_8_10_12_14_16_18_20_22_24_26_28_30
            if (cfg.getPlayType() == AceDjConstant.SpecialPlay.TYPE_CONSECUTIVE_WINS) {
                String[] arr = cfg.getValue().split("_");
                int count = 0;
                for (String str : arr) {
                    int time = Integer.parseInt(str);
                    if (count > time) {
                        tmpMaxWinCount = count;
                    }
                    tmpWildTimesList.add(time);
                }
            } else if (cfg.getPlayType() == AceDjConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {  //增加免费次数 2,13,4_10_10000|5_15_1000|6_20_100
                List<AceDjAddFreeInfo> tmpAceDjAddFreeInfoList = new ArrayList<>();

                String[] arr = cfg.getValue().split(",");
                String splitStr = arr[2];
                String[] arr2 = splitStr.split("\\|");
                for (String splitStr2 : arr2) {
                    String[] arr3 = splitStr2.split("_");
                    AceDjAddFreeInfo tmpAceDjAddFreeInfo = new AceDjAddFreeInfo();
                    tmpAceDjAddFreeInfo.setLibType(Integer.parseInt(arr[0]));
                    tmpAceDjAddFreeInfo.setTargetIcon(Integer.parseInt(arr[1]));
                    tmpAceDjAddFreeInfo.setNum(Integer.parseInt(arr3[0]));
                    tmpAceDjAddFreeInfo.setAddFreeCount(Integer.parseInt(arr3[1]));
                    tmpAceDjAddFreeInfo.setProp(Integer.parseInt(arr3[2]));
                    tmpAceDjAddFreeInfoList.add(tmpAceDjAddFreeInfo);
                }

                this.aceDjAddFreeInfoList = tmpAceDjAddFreeInfoList;
            }
        }
        this.wildTimesList = tmpWildTimesList;
        this.maxWinCount = tmpMaxWinCount;
    }

    /**
     * 格子修改
     *
     * @param cfgId specialGirdCfg的配置id
     * @param arr   图标数组
     * @return
     */
    public SpecialGirdInfo girdUpdate(int cfgId, int[] arr) {
        log.debug("开始修改格子 specialGirdCfgId = {}", cfgId);
        SpecialGirdCfg specialGirdCfg = GameDataManager.getSpecialGirdCfg(cfgId);
        if (specialGirdCfg == null) {
            log.debug("修改格子未找到对应的配置 cfgId = {}", cfgId);
            return null;
        }

        GirdUpdatePropConfig girdUpdatePropConfig = this.specialGirdCfgMap.get(cfgId);
        if (girdUpdatePropConfig == null) {
            log.debug("修改格子未找到计算后的权重信息 cfgId = {}", cfgId);
            return null;
        }

        if (girdUpdatePropConfig.getRandCountPropInfo() == null) {
            log.debug("修改格子未找到计算后的随机次数权重信息 cfgId = {}", cfgId);
            return null;
        }

        //获取随机次数
        Integer randCount = girdUpdatePropConfig.getRandCountPropInfo().getRandKey();
        if (randCount == null || randCount < 1) {
            return null;
        }

        log.debug("获取到随机次数 cfgId = {},randCount = {}", cfgId, randCount);
        //因为有最大次数限制，所以先clone
        PropInfo cloneAffectGirdPropInfo = girdUpdatePropConfig.getAffectGirdPropInfo().clone();
        //出现的次数记录
        Map<Integer, Integer> girdShowMap = new HashMap<>();

        SpecialGirdInfo info = new SpecialGirdInfo();
        info.setCfgId(specialGirdCfg.getId());

        //记录实际修改格子的次数
        int x = 0;
        int maxForCount = arr.length * 2;
        for (int i = 0; i < maxForCount; i++) {
            //获取一个需要替换的格子
            Integer girdId = cloneAffectGirdPropInfo.getRandKey();
            girdShowMap.merge(girdId, 1, Integer::sum);

            //该格子上的图标id
            int icon = arr[girdId];
            //检查该格子是否不可替换
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(icon)) {
                continue;
            }

            //随机一个需要出现的图标
            int newIcon = girdUpdatePropConfig.getShowIconPropInfo().getRandKey();
            log.debug("修改格子 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);

            if (cfgId == AceDjConstant.SpecialGird.GRID_TWO
                    || cfgId == AceDjConstant.SpecialGird.GRID_THERE
                    || cfgId == AceDjConstant.SpecialGird.GRID_FOUR) {
                BaseElementCfg baseElement = baseElementCfgMap.get(newIcon);
                if (baseElement != null && baseElement.getSpace() > 1) {
                    Map<Integer, Integer> updateMap = new HashMap<>();
                    boolean isUpdate = true;
                    for (int i1 = 1; i1 < baseElement.getSpace(); i1++) {
                        //把向上的格子配成 0
                        BaseElementCfg baseElementI1 = baseElementCfgMap.get(arr[girdId - i1]);
                        if (baseElementI1 != null && baseElementI1.getSpace() ==0) {
                            updateMap.put(girdId - i1, AceDjConstant.BaseElement.ID_NULL);
                        } else {
                            isUpdate = false;
                            break;
                        }
                    }
                    updateMap.put(girdId, newIcon);
                    if (isUpdate) {
                        updateMap.forEach((k, v) -> arr[k] = v);
                        log.debug("修改大格子 cfgId = {}, oldIcon = {}, newIcon = {}, newArr = {}", cfgId, arr[girdId], newIcon, JSONObject.toJSONString(arr));
                    }
                }
            } else {
                arr[girdId] = newIcon;
            }
            //赋值
            if (girdUpdatePropConfig.getValuePropInfo() != null) {
                int value = girdUpdatePropConfig.getValuePropInfo().getRandKey();
                info.addValue(girdId, value);
                log.debug("赋值 girdId = {}, value = {}", girdId, value);
            }

            //达到最大次数限制后，移除
            if (girdShowMap.get(girdId) >= cloneAffectGirdPropInfo.getMaxShowLimit(girdId)) {
                cloneAffectGirdPropInfo.removeKeyAndRecalculate(girdId);
            }

            x++;
            if (x >= randCount) {
                break;
            }
        }

        //值类型
        if (specialGirdCfg.getValueType() != null && !specialGirdCfg.getValueType().isEmpty()) {
            info.setValueType(specialGirdCfg.getValueType().get(0));
            info.setMiniGameId(specialGirdCfg.getValueType().get(1));
        }

        log.debug("修改后的图标 arr = {}", Arrays.toString(arr));
        return info;
    }


    /**
     * 检查满线图案
     *
     * @param arr
     */
//    @Override
    public List<AceDjAwardLineInfo> fullLine(int[] arr) {
        //获取满线图案_数量的配置
        Map<Integer, BaseElementRewardCfg> fullLineCountCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_FULL);
        if (fullLineCountCfgMap == null || fullLineCountCfgMap.isEmpty()) {
            return null;
        }

        List<AceDjAwardLineInfo> awardInfoList = new ArrayList<>();

        log.debug("检查满线图案");

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //wild图标
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);
        //普通图标
        Set<Integer> normalIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_NORMAL);

        //icon -> count
        Map<Integer, Set<Integer>> firstColIcons = new HashMap<>();

        //获取第一列出现的图标
        for (int i = 1; i <= baseInitCfg.getRows(); i++) {
            int icon = arr[i];
            //如果第一列出现了wild图标
            if (wildIconSet.contains(icon)) {
                for (Integer normalIcon : normalIconSet) {
                    firstColIcons.computeIfAbsent(normalIcon, k -> new HashSet<>()).add(i);
                }
            }
            firstColIcons.computeIfAbsent(icon, k -> new HashSet<>()).add(i);
        }

        //然后从第二列开始检查，是否出现了第一列的图标
        for (Map.Entry<Integer, Set<Integer>> en : firstColIcons.entrySet()) {
            //第一列的图标和数量
            int icon = en.getKey();
            Set<Integer> iconIndexSet = en.getValue();

            boolean firstNormal = normalIconSet.contains(icon);
            Set<Integer> sameIconIndexSet = new HashSet<>();

            //找到baseElementReward表中关于该图标的配置
            for (Map.Entry<Integer, BaseElementRewardCfg> rewardCfgEn : fullLineCountCfgMap.entrySet()) {
                int maxCol = 1;

                BaseElementRewardCfg cfg = rewardCfgEn.getValue();
                //配置表中的图标是否包含该图标
                if (!cfg.getElementId().contains(icon)) {
                    continue;
                }

                //从第2列开始，检查每一个图标
                for (int col = 2; col <= baseInitCfg.getCols(); col++) {
                    int beginIndex = (col - 1) * baseInitCfg.getRows() + 1;

                    boolean flag = false;

                    for (int i = 0; i < baseInitCfg.getRows(); i++) {
                        int index = beginIndex + i;
                        int tmpIcon = arr[index];
                        if (tmpIcon == AceDjConstant.BaseElement.ID_NULL) {
                            continue;
                        }

                        boolean wild = false;
                        if (wildIconSet != null && wildIconSet.contains(tmpIcon)) {
                            wild = true;
                        }

                        if (wild && firstNormal) {
                            flag = true;
                            sameIconIndexSet.add(index);
                        } else if (cfg.getElementId().contains(tmpIcon)) {
                            flag = true;
                            sameIconIndexSet.add(index);
                        }
                    }

                    //这一列遍历结束后，检查图标个数是否增长
                    if (!flag) { //如果没有增长，表示这一列中没有出现第一列的图标，所以中断
                        break;
                    }
                    maxCol = col;
                }

                if (maxCol == cfg.getRewardNum()) {
                    sameIconIndexSet.addAll(iconIndexSet);

                    AceDjAwardLineInfo rewardInfo = addFullLineAwardInfo(sameIconIndexSet, cfg);
                    awardInfoList.add(rewardInfo);
                }
            }
        }

        return awardInfoList;
    }


    public List<Integer> getWildTimesList() {
        return wildTimesList;
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
    private void addShowAuxiliaryId(AceDjResultLib lib, Set<Integer> set) {
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
    private boolean checkJackpool(AceDjResultLib lib) {
        if (lib.getJackpotId() < 1) {
            return false;
        }

        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == AceDjConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == AceDjConstant.BaseElement.ID_MINI || icon == AceDjConstant.BaseElement.ID_MINOR || icon == AceDjConstant.BaseElement.ID_MAJOR || icon == AceDjConstant.BaseElement.ID_GRAND) {
                jackpool++;
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
    private boolean checkTriggerFree(AceDjResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == AceDjConstant.BaseElement.ID_SCATTER) {
                count++;
            }
        }
        return count >= 3;
    }

    /**
     * 检查元素与小游戏所需要的参数是否匹配
     *
     * @param lib
     */
    private boolean checkElement(AceDjResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(AceDjConstant.SpecialMode.FREE) && !checkTriggerFree(lib)) {
            log.warn("检查免费触发局失败");
            return false;
        }

        //检查jackpool模式
        if (lib.getLibTypeSet().contains(AceDjConstant.SpecialMode.JACKPOOL) && !checkJackpool(lib)) {
            log.warn("检查jackpool模式失败");
            return false;
        }

        return true;
    }

}
