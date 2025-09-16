package com.jjg.game.slots.game.cleopatra.manager;

import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseElementCfg;
import com.jjg.game.sampledata.bean.BaseElementRewardCfg;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.SpecialPlayCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.game.cleopatra.CleopatraConstant;
import com.jjg.game.slots.game.cleopatra.data.AddColumnConfig;
import com.jjg.game.slots.game.cleopatra.data.CleopatraAddColumnInfo;
import com.jjg.game.slots.game.cleopatra.data.CleopatraResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/1 17:33
 */
@Component
public class CleopatraGenerateManager extends AbstractSlotsGenerateManager<CleopatraAddColumnInfo, CleopatraResultLib> {
    //新增列信息
    private Map<Integer, AddColumnConfig> addColumnInfoMap;
    //图标基础分数
    private Map<Integer, Integer> iconBaseScoreMap;

    private List<int[]> testAddIcons;

    public CleopatraGenerateManager() {
        super(CleopatraResultLib.class);
    }

    @Override
    public CleopatraResultLib generateOne(int libType) throws Exception {
        try{
            return super.generateOne(libType);
        }catch (Exception e){
            return null;
        }
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(CleopatraResultLib lib) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(lib.getIconArr());

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
                    lib.getLibTypeSet().forEach(libType -> {
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                        if (specialAuxiliaryInfo != null) {
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
                    });
                });
            }

            if (cfg.getJackpotID() > 0) {
                lib.addJackpotId(cfg.getJackpotID());
            }

        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public List<CleopatraAddColumnInfo> fullLineCount(CleopatraResultLib lib) {
        //获取满线图案_数量的配置
        Map<Integer, BaseElementRewardCfg> fullLineCountCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_FULL_COUNT);
        if (fullLineCountCfgMap == null || fullLineCountCfgMap.isEmpty()) {
            return null;
        }

        log.debug("检查满线图案_数量");

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //wild图标
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);
        //普通图标
        Set<Integer> normalIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_NORMAL);

        //icon -> 坐标列表
        Map<Integer, Set<Integer>> firstColIcons = new HashMap<>();

        //获取第一列出现的图标
        for (int i = 1; i <= baseInitCfg.getRows(); i++) {
            int icon = lib.getIconArr()[i];

            firstColIcons.computeIfAbsent(icon, k -> new HashSet<>()).add(i);
        }

        //然后从第二列开始检查，是否出现了第一列的图标
        boolean add = false;
        Iterator<Map.Entry<Integer, Set<Integer>>> it = firstColIcons.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Set<Integer>> en = it.next();
            //第一列的图标和数量
            int icon = en.getKey();
            //图标对应的坐标id
            Set<Integer> indexSet = en.getValue();

            //第一列中这个图标是不是普通图标
            boolean firstNormal = normalIconSet.contains(icon);

            //从第2列开始，检查每一个图标
            for (int col = 2; col <= baseInitCfg.getCols(); col++) {
                int beginIndex = (col - 1) * baseInitCfg.getRows() + 1;

                boolean flag = false;

                for (int i = 0; i < baseInitCfg.getRows(); i++) {
                    int index = beginIndex + i;
                    int tmpIcon = lib.getIconArr()[index];

                    boolean wild = false;
                    if (wildIconSet != null && wildIconSet.contains(tmpIcon)) {
                        wild = true;
                    }

                    if (wild && firstNormal) {
                        flag = true;
                        indexSet.add(index);
                    } else if (icon == tmpIcon) {
                        flag = true;
                        indexSet.add(index);
                    }
                }

                //表示这一列没有出现相关图标，所以中断
                if(!flag){
                    it.remove();
                    break;
                }

                //检查中奖
                for(Map.Entry<Integer, BaseElementRewardCfg> fullLineCountEn :fullLineCountCfgMap.entrySet()){
                    BaseElementRewardCfg cfg = fullLineCountEn.getValue();
                    if(cfg.getElementId().contains(icon) && indexSet.size() >= cfg.getRewardNum()){
                        lib.addWinIcon(icon,indexSet);
                        add = true;
                    }
                }
            }
        }

        if(add && !firstColIcons.isEmpty()){
            List<CleopatraAddColumnInfo> addColumnList = new ArrayList<>();
            addColumnInfo(1, firstColIcons, baseInitCfg.getRows(), baseInitCfg.getCols() + 1, addColumnList,fullLineCountCfgMap);
            return addColumnList;
        }

        return null;
    }

    /**
     * 添加列信息
     *
     * @param winCount
     * @param rows
     * @param awardInfoList
     * @return
     */
    private void addColumnInfo(int winCount, Map<Integer, Set<Integer>> firstColIcons, int rows, int colId,
                               List<CleopatraAddColumnInfo> awardInfoList,Map<Integer, BaseElementRewardCfg> fullLineCountCfgMap) {
        AddColumnConfig addColumnConfig = this.addColumnInfoMap.get(winCount);
        if (addColumnConfig == null) {
            throw new IllegalArgumentException("获取 addColumnConfig 为空 winCount=" + winCount);
        }

        if(firstColIcons.isEmpty()){
            return;
        }

        //wild图标
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        //根据滚轴id获取1列图案
        int[] colIcons;
        if(this.testAddIcons != null && this.testAddIcons.size() >= winCount){
            colIcons = this.testAddIcons.get(winCount-1);
            System.out.println("添加测试图标 winCount = " + winCount);
        }else {
            colIcons = generateColumnIcons(addColumnConfig.getRollerId(), rows, colId);
        }

        //中奖图标坐标
        Map<Integer,Set<Integer>> indexMap = new HashMap<>();
        for (int i = 0; i < colIcons.length; i++) {
            int icon = colIcons[i];
            if (wildIconSet != null && wildIconSet.contains(icon)) { //该图标为wild
                for(Map.Entry<Integer,Set<Integer>> en : firstColIcons.entrySet()){
                    indexMap.computeIfAbsent(en.getKey(), k -> new HashSet<>()).add(i);
                }
                continue;
            }

            if (firstColIcons.containsKey(icon)) {
                indexMap.computeIfAbsent(icon,k -> new HashSet<>()).add(i);
            }
        }

        firstColIcons.entrySet().removeIf(en -> {
            int icon = en.getKey();
            return !indexMap.containsKey(icon);
        });

        Iterator<Map.Entry<Integer, Set<Integer>>> it = indexMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, Set<Integer>> en = it.next();
            int icon = en.getKey();
            Set<Integer> addIconIndexList = en.getValue();

            Set<Integer> indexSet = firstColIcons.get(icon);
            indexSet.addAll(addIconIndexList);

            boolean win = false;
            //检查中奖
            for(Map.Entry<Integer, BaseElementRewardCfg> fullLineCountEn :fullLineCountCfgMap.entrySet()){
                BaseElementRewardCfg cfg = fullLineCountEn.getValue();
                if(cfg.getElementId().contains(icon) && indexSet.size() >= cfg.getRewardNum()){
                    win = true;
                }
            }

            if(!win){
                it.remove();
            }
        }

        CleopatraAddColumnInfo addColumnInfo = new CleopatraAddColumnInfo();
        addColumnInfo.setArr(colIcons);
        addColumnInfo.setWinIconIndexMap(indexMap);
        awardInfoList.add(addColumnInfo);

        if (indexMap.isEmpty()) {
            return;
        }

        addColumnInfo(winCount + 1, firstColIcons, rows, colId + 1, awardInfoList,fullLineCountCfgMap);
    }

    @Override
    protected void baseElementRewardConfig() {
        Map<Integer, Map<Integer, BaseElementRewardCfg>> tmpBaseElementRewardCfgMap = new HashMap<>();
        Map<Integer, Integer> tmpIconBaseScoreMap = new HashMap<>();

        //根据游戏type筛选
        for (Map.Entry<Integer, BaseElementRewardCfg> en : GameDataManager.getBaseElementRewardCfgMap().entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            Map<Integer, BaseElementRewardCfg> tempMap = tmpBaseElementRewardCfgMap.computeIfAbsent(cfg.getLineType(), k -> new HashMap<>());
            tempMap.put(cfg.getId(), cfg);

            if (cfg.getBet() > 0) {
                tmpIconBaseScoreMap.put(cfg.getElementId().getFirst(), cfg.getBet());
            }
        }

        this.baseElementRewardCfgMap = tmpBaseElementRewardCfgMap;
        this.iconBaseScoreMap = tmpIconBaseScoreMap;
    }

    @Override
    protected void specialPlayConfig() {
        Map<Integer, AddColumnConfig> tmpAddColumnInfoMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //新增列
            if (cfg.getPlayType() == CleopatraConstant.SpecialPlay.TYPE_ADD_ICON) {
                String[] arr = cfg.getValue().split("\\|");


                for (String s : arr) {
                    AddColumnConfig addColumnConfig = new AddColumnConfig();
                    String[] arr1 = s.split("_");

                    addColumnConfig.setWinCount(Integer.parseInt(arr1[0]));
                    addColumnConfig.setRollerId(Integer.parseInt(arr1[1]));
                    addColumnConfig.setTimes(Integer.parseInt(arr1[2]));
                    tmpAddColumnInfoMap.put(addColumnConfig.getWinCount(), addColumnConfig);
                }
            }
        }
        this.addColumnInfoMap = tmpAddColumnInfoMap;
    }

    @Override
    public void calTimes(CleopatraResultLib lib) throws Exception {
        if (lib.getAwardLineInfoList() == null || lib.getAwardLineInfoList().isEmpty()) {
            return;
        }

        if(lib.getWinIcons() == null || lib.getWinIcons().isEmpty()) {
            return;
        }

        //wild图标
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);

        //图标组合在一起
        int[] arr = new int[lib.getIconArr().length + lib.getAwardLineInfoList().size() * baseInitCfg.getRows()];
        System.arraycopy(lib.getIconArr(),0,arr,0,lib.getIconArr().length);
        int beginIndex = lib.getIconArr().length;
        for(CleopatraAddColumnInfo info : lib.getAwardLineInfoList()){
            System.arraycopy(info.getArr(),0,arr,beginIndex,info.getArr().length);
            beginIndex += info.getArr().length;
        }

//        System.out.println(Arrays.toString(arr));

        //所有的中奖的图标
        Set<Integer> allWinIconSet = new HashSet<>();
        for(Map.Entry<Integer,Set<Integer>> en : lib.getWinIcons().entrySet()){
            allWinIconSet.add(en.getKey());
        }
        for(CleopatraAddColumnInfo info : lib.getAwardLineInfoList()){
            if(info.getWinIconIndexMap() == null || info.getWinIconIndexMap().isEmpty()) {
                continue;
            }
            info.getWinIconIndexMap().forEach((k, v) -> {
                allWinIconSet.add(k);
            });
        }

        //获取增加列倍数
        int addTimes = this.addColumnInfoMap.get(lib.getAwardLineInfoList().size()).getTimes();
        for(int icon : allWinIconSet){

            //获取基础分
            int iconBaseScore = this.iconBaseScoreMap.get(icon);

            int count = 0;
            //计算该图标总数量
            for(int i=1;i<arr.length;i++){
                int tmpIcon = arr[i];

                if(wildIconSet.contains(tmpIcon) || tmpIcon == icon){
                    count++;
                }
            }

            lib.addTimes(count * iconBaseScore * addTimes);
        }

//        System.out.println("times = " + lib.getTimes());
    }

    public Map<Integer, AddColumnConfig> getAddColumnInfoMap() {
        return addColumnInfoMap;
    }

    public void addTestAddIcon(int[] arr){
        if(this.testAddIcons == null){
            this.testAddIcons = new ArrayList<>();
        }
        this.testAddIcons.add(arr);
    }
}
