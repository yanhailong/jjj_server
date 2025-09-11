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

    public CleopatraGenerateManager() {
        super(CleopatraResultLib.class);
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

        //icon -> count
        Map<Integer, Integer> firstColIcons = new HashMap<>();

        //获取第一列出现的图标
        for (int i = 1; i <= baseInitCfg.getRows(); i++) {
            int icon = lib.getIconArr()[i];
            firstColIcons.merge(icon, 1, Integer::sum);
        }

        //然后从第二列开始检查，是否出现了第一列的图标
        for (Map.Entry<Integer, Integer> en : firstColIcons.entrySet()) {
            //第一列的图标和数量
            int icon = en.getKey();
            int count = en.getValue();

            boolean firstNormal = normalIconSet.contains(icon);

            boolean lastColShow = false;

            int allCount = count;

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
                        allCount++;
                    } else if (icon == tmpIcon) {
                        flag = true;
                        allCount++;
                    }
                }

                //标记最后一列是否出现
                if (col == baseInitCfg.getCols()) {
                    lastColShow = flag;
                }

                //这一列遍历结束后，检查图标个数是否增长
                if (!flag) { //如果没有增长，表示这一列中没有出现第一列的图标，所以中断
                    break;
                }
            }

            for(Map.Entry<Integer, BaseElementRewardCfg> fullLineCountEn :fullLineCountCfgMap.entrySet()){
                BaseElementRewardCfg cfg = fullLineCountEn.getValue();
                if(cfg.getElementId().contains(icon) && allCount >= cfg.getRewardNum()){
                    //最后一列是否出现
                    if (lastColShow) {
                        lib.addWinIcon(icon, allCount);
                        List<CleopatraAddColumnInfo> addColumnList = new ArrayList<>();
                        addColumnInfo(1, icon, baseInitCfg.getRows(), baseInitCfg.getCols() + 1, addColumnList);
                        return addColumnList;
                    }else {
                        lib.addWinIcon(icon, allCount);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 添加列信息
     *
     * @param winCount
     * @param winIcon
     * @param rows
     * @param awardInfoList
     * @return
     */
    private void addColumnInfo(int winCount, int winIcon, int rows, int colId, List<CleopatraAddColumnInfo> awardInfoList) {
        AddColumnConfig addColumnConfig = this.addColumnInfoMap.get(winCount);
        if (addColumnConfig == null) {
            return;
        }

        //wild图标
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        //根据滚轴id获取1列图案
        int[] colIcons = generateColumnIcons(addColumnConfig.getRollerId(), rows, colId);

        int count = 0;
        for (int i = 0; i < colIcons.length; i++) {
            int icon = colIcons[i];
            boolean wild = false;
            if (wildIconSet != null && wildIconSet.contains(icon)) {
                wild = true;
            }

            if (wild || icon == winIcon) {
                count++;
            }
        }

        CleopatraAddColumnInfo addColumnInfo = new CleopatraAddColumnInfo();
        addColumnInfo.setArr(colIcons);
        addColumnInfo.setCount(count);
        awardInfoList.add(addColumnInfo);

        if (count < 1) {

            return;
        }

        addColumnInfo(winCount + 1, winIcon, rows, colId + 1, awardInfoList);
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

        int winIcon = lib.getWinIcons()[0];
        int winIconInitCount = lib.getWinIcons()[1];

        //获取基础分
        int iconBaseScore = this.iconBaseScoreMap.get(winIcon);

        //计算该图标总数量
        for (CleopatraAddColumnInfo info : lib.getAwardLineInfoList()) {
            winIconInitCount += info.getCount();
        }

        //获取增加列倍数
        int addTimes = this.addColumnInfoMap.get(lib.getAwardLineInfoList().size()).getTimes();

        lib.setTimes(winIconInitCount * iconBaseScore * addTimes);
    }

    public Map<Integer, AddColumnConfig> getAddColumnInfoMap() {
        return addColumnInfoMap;
    }
}
