package com.jjg.game.slots.generate;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.slots.constant.AuxiliaryAwardType;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 结果集生成器
 *
 * @author 11
 * @date 2025/7/2 13:54
 */
public class SlotsGenerate {
    protected Logger log = LoggerFactory.getLogger(getClass());

    //游戏类型
    protected int gameType;
    protected int columnCount;

    //滚轴模式配置
    protected BaseRollerModeCfg baseRollerModeCfg;
    //滚轴模式id列表
    protected List<Integer> rollModeList;
    //wild图标
    protected Set<Integer> wildIconSet = null;
    //普通图标
    protected Set<Integer> noralIconSet = null;
    //column -> cfg
    protected Map<Integer, BaseRollerCfg> baseRollerCfgMap = null;
    //lineId -> cfg
    protected Map<Integer, BaseLineCfg> baseLineCfgMap = null;
    //lineId -> id - > cfg
    protected Map<Integer, Map<Integer, BaseLineFreeInfo>> baseLineFreeCfgMap = null;
    //普通图标 sid -> cfg
    protected Map<Integer, BaseElementRewardCfg> baseNormalIconRewardCfgMap = null;
    //特殊图标 sid -> cfg
    protected Map<Integer, BaseElementRewardCfg> baseSpecialIconRewardCfgMap = null;

    //小游戏相关的权重信息
    protected Map<Integer, Map<Integer, PropInfo>> specialAuxiliaryPropMap = null;

    //小游戏奖励相关的权重信息
    protected Map<Integer, SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardA>> specialAuxiliaryAwardInfoMapA = null;
    //    protected Map<Integer,SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardC>> specialAuxiliaryAwardInfoMapB = null;
    protected Map<Integer, SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardC>> specialAuxiliaryAwardInfoMapC = null;

    //因为小游戏奖励可能会触发另外一个小游戏，所以这里有递归跳出条件
    private int auxiliaryAecursionCount = 0;


    public SlotsGenerate(int gameType, int columnCount) {
        this.gameType = gameType;
        this.columnCount = columnCount;

        baseRollerModeCfg();
        baseElementConfig();
        baseRollerConfig();
        baseLineConfig();
        baseLineFreeConfig();
        baseElementRewardConfig();

        specialAuxiliaryCconfig();
        specialAuxiliaryAwardCconfig();
    }

    /**
     * 生成结果集
     *
     * @param count 生成条数
     */
    public void generate(int rowCount, int count) {
        try {
            for (int i = 0; i < count; i++) {
                generateOne(rowCount);
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 生成一个结果
     *
     * @param rowCount
     */
    public void generateOne(int rowCount) {
        try {
            this.auxiliaryAecursionCount = 0;

            int[] arr = new int[this.columnCount * rowCount + 1];

            //获取模式id和滚轴id
            SlotsResultInfo slotsResultInfo = randRollerId();

            for (Map.Entry<Integer, BaseRollerCfg> en : this.baseRollerCfgMap.entrySet()) {
                BaseRollerCfg cfg = en.getValue();
                List<Integer> scope = cfg.getAxleCountScope().get(slotsResultInfo.getRollerId());
                if (scope == null || scope.isEmpty()) {
                    log.warn("没有该滚轴的范围,生成结果集失败 gameType = {},modeId = {},rollerId = {}", this.gameType, slotsResultInfo.getRollerMode(), slotsResultInfo.getRollerId());
                    return;
                }

                int iconIndex = (cfg.getColumn() - 1) * rowCount + 1;

                //区间范围的第一个下标
                int first = scope.get(0) - 1;
                //区间范围的最后一个下标
                int last = scope.get(1) - 1;

                //随机生成一个起始位置
                int index = RandomUtils.randomMinMax(first, last);
                for (int i = 0; i < rowCount; i++) {
                    //首尾相连
                    if (index > last) {
                        index = first;
                    }

                    int elementId = cfg.getElements().get(index);
                    arr[iconIndex] = elementId;
                    iconIndex++;
                    index++;
                }
            }

            slotsResultInfo.setGameType(this.gameType);
            slotsResultInfo.setIconArr(arr);

            slotsResultInfo = normalAward(slotsResultInfo);
            slotsResultInfo = specialAward(slotsResultInfo);


        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 检查普通奖励
     *
     * @param slotsResultInfo
     * @return
     */
    public SlotsResultInfo normalAward(SlotsResultInfo slotsResultInfo) {
        for (Map.Entry<Integer, BaseLineCfg> en : this.baseLineCfgMap.entrySet()) {
            BaseLineCfg cfg = en.getValue();
            int id = cfg.getGamePlay().get(0).get(1);
            List<Integer> lineList = cfg.getPosLocation().get(id);

            SameInfo sameInfo = new SameInfo();

            int last = lineList.size() - 1;

            //标记是否连线
            int sameCount = 0;

            for (int i = 0; i < last; i++) {
                int index1 = lineList.get(i);

                int behind = i + 1;
                int index2 = lineList.get(behind);

                sameInfo = iconSame(sameInfo, slotsResultInfo.getIconArr()[index1], slotsResultInfo.getIconArr()[index2]);
                if (sameInfo.isSame()) {
                    sameInfo.setSame(false);
                    sameCount = sameCount < 1 ? 2 : sameCount + 1;
                } else {
                    break;
                }
            }

            //如果有连线
            if (sameCount > 1) {
                for (Map.Entry<Integer, BaseElementRewardCfg> rewardEn : this.baseNormalIconRewardCfgMap.entrySet()) {
                    BaseElementRewardCfg rewardCfg = rewardEn.getValue();
                    //匹配连线的元素id和个数
                    if (rewardCfg.getElementId() != sameInfo.getBaseIconId() || sameCount != rewardCfg.getRewardNum()) {
                        continue;
                    }

                    AwardLineInfo awardLineInfo = new AwardLineInfo();
                    awardLineInfo.setId(cfg.getLineId());
                    awardLineInfo.setBaseTimes(rewardCfg.getBet());
                    awardLineInfo.setSameCount(sameCount);
                    awardLineInfo.setIconId(sameInfo.getBaseIconId());

                    slotsResultInfo.addTimes(rewardCfg.getBet());
                    log.debug("中奖！！ 添加基础倍率 lineId = {},addTimes = {}", cfg.getLineId(), rewardCfg.getBet());

                    //特殊图标出现的次数
                    Map<Integer, Integer> specialShowMap = checkSpecialAllShow(slotsResultInfo.getIconArr(), lineList);
                    for (List<Integer> specialRwardList : rewardCfg.getBetTimes()) {
                        int iconId = specialRwardList.get(0);
                        //出现的次数
                        Integer showCount = specialShowMap.get(iconId);
                        if (showCount != null && showCount == specialRwardList.get(1)) {
                            int addTimes = specialRwardList.get(2);
                            slotsResultInfo.addTimes(addTimes);
                            awardLineInfo.addSpecialAwardInfo(iconId, new SpecialAwardInfo(showCount, addTimes));
                            log.debug("特殊图标添加倍率 iconId = {},showCount = {},addTimes = {}", iconId, showCount, addTimes);
                        }
                    }

                    slotsResultInfo.addAwardLineInfo(awardLineInfo);
                    break;
                }
            }
        }
        return slotsResultInfo;
    }

    /**
     * 检查特殊奖励
     *
     * @param slotsResultInfo
     * @return
     */
    public SlotsResultInfo specialAward(SlotsResultInfo slotsResultInfo) {
        // 统计arr中每个元素的出现次数
        Map<Integer, Integer> iconShowCountMap = new HashMap<>();
        for (int num : slotsResultInfo.getIconArr()) {
            iconShowCountMap.merge(num, 1, Integer::sum);
        }

        //检查出现的特殊元素，主元素id -> 出现总次数
        Map<Integer, Integer> showIdMap = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, BaseLineFreeInfo>> en : this.baseLineFreeCfgMap.entrySet()) {
            Map<Integer, BaseLineFreeInfo> freeInfoMap = en.getValue();
            for (Map.Entry<Integer, BaseLineFreeInfo> en2 : freeInfoMap.entrySet()) {
                BaseLineFreeInfo freeInfo = en2.getValue();

                group:
                for (List<Integer> groupList : freeInfo.getElementGroupList()) {
                    for (int specialIcon : groupList) {
                        Integer count = iconShowCountMap.get(specialIcon);
                        if (count == null) {
                            showIdMap.remove(freeInfo.getMainElementId());
                            continue group;
                        }
                        showIdMap.merge(freeInfo.getMainElementId(), count, Integer::sum);
                    }
                }
            }
        }

        if (showIdMap.isEmpty()) {
            return slotsResultInfo;
        }

        //遍历所有的特殊图标的奖励
        for (Map.Entry<Integer, BaseElementRewardCfg> en : this.baseSpecialIconRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            int iconId = cfg.getElementId();

            Integer showCount = showIdMap.get(iconId);

            //匹配连线的元素id和个数
            if (showCount == null || showCount != cfg.getRewardNum()) {
                continue;
            }

            slotsResultInfo.addTimes(cfg.getBet());

            //是否有小游戏配置
            Map<Integer, Integer> featureTriggerIdMap = cfg.getFeatureTriggerId();
            if (featureTriggerIdMap == null || featureTriggerIdMap.isEmpty()) {
                continue;
            }

            //小游戏id
            Integer miniGameId = featureTriggerIdMap.get(slotsResultInfo.getRollerId());
            if (miniGameId == null || miniGameId < 1) {
                continue;
            }

            //触发小游戏
            triggerMiniGame(slotsResultInfo, miniGameId);
        }
        return slotsResultInfo;
    }

    /**
     * 触发小游戏
     * @param slotsResultInfo
     * @param miniGameId
     */
    private void triggerMiniGame(SlotsResultInfo slotsResultInfo,int miniGameId) {
        if(this.auxiliaryAecursionCount > 10){
            log.error("已经递归触发了10次小游戏，强制跳出  miniGameId = {}", miniGameId);
            return;
        }

        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return;
        }
        log.debug("触发小游戏 miniGameId = {}",  miniGameId);

        PropInfo propInfo = this.specialAuxiliaryPropMap.get(miniGameId).get(slotsResultInfo.getRollerMode());
        if (propInfo == null) {
            log.warn("该小游戏的权重信息未找到 miniGameId = {}", miniGameId);
            return;
        }

        //随机次数
        int rand = RandomUtils.randomMinMax(0, propInfo.getSum());
        int count = 0;
        for (Map.Entry<Integer, int[]> en2 : propInfo.getPropMap().entrySet()) {
            int[] arr = en2.getValue();
            if (rand >= arr[0] && rand < arr[1]) {
                count = en2.getKey();
                break;
            }
        }

        if (count < 1) {
            return;
        }

        slotsResultInfo.setAuxiliaryType(specialAuxiliaryCfg.getType());
        //处理固定奖励
        handSpecialFixedReward(slotsResultInfo, specialAuxiliaryCfg, count);
        //处理随机奖励
        handSpecialRandReward(slotsResultInfo, specialAuxiliaryCfg, count);

        this.auxiliaryAecursionCount++;
    }

    /**
     * 处理特殊游戏的固定奖励
     */
    public void handSpecialFixedReward(SlotsResultInfo slotsResultInfo, SpecialAuxiliaryCfg cfg, int count) {
        if (cfg.getFreeAward() == null || cfg.getFreeAward().isEmpty()) {
            return;
        }

        for (int rewardId : cfg.getFreeAward()) {
            SpecialAuxiliaryAwardCfg specialAuxiliaryAwardCfg = GameDataManager.getSpecialAuxiliaryAwardCfg(rewardId);
            if (specialAuxiliaryAwardCfg == null) {
                log.warn("特殊游戏奖励配置为空 SpecialAuxiliaryCfg.id = {},SpecialAuxiliaryAwardCfg.id = {}", cfg.getId(), rewardId);
                continue;
            }

            //获取奖励A
            if (specialAuxiliaryAwardCfg.getAwardTypeA() != null && !specialAuxiliaryAwardCfg.getAwardTypeA().isEmpty()) {
                randAwardA(slotsResultInfo, specialAuxiliaryAwardCfg, count);
            }

            //获取奖励C
            if (specialAuxiliaryAwardCfg.getAwardTypeC() != null && !specialAuxiliaryAwardCfg.getAwardTypeC().isEmpty()) {
                randAwardC(slotsResultInfo, specialAuxiliaryAwardCfg, count);
            }
        }
    }

    /**
     * 处理特殊游戏的随机奖励
     */
    public void handSpecialRandReward(SlotsResultInfo slotsResultInfo, SpecialAuxiliaryCfg cfg, int count) {
        if (cfg.getFreeRandAward() == null || cfg.getFreeRandAward().isEmpty()) {
            return;
        }
    }

    /**
     * 检查在连线上特殊图标出现的次数
     *
     * @param arr
     * @param lineList
     * @return
     */
    private Map<Integer, Integer> checkSpecialAllShow(int[] arr, List<Integer> lineList) {
        Map<Integer, Integer> specialShowMap = new HashMap<>();
        for (int i = 0; i < lineList.size(); i++) {
            int index = lineList.get(i);
            int icon = arr[index];

            if (!this.noralIconSet.contains(icon)) {
                specialShowMap.merge(icon, 1, Integer::sum);
            }
        }
        return specialShowMap;
    }


    /**
     * 判断两个icon是否一样
     * @param sameInfo
     * @param iconId1
     * @param iconId2
     * @return
     */
    private SameInfo iconSame(SameInfo sameInfo, int iconId1, int iconId2) {
        //是不是普通图标
        boolean normal_1 = this.noralIconSet.contains(iconId1);
        boolean normal_2 = this.noralIconSet.contains(iconId2);

        //是不是wild
        boolean wild_1 = this.wildIconSet.contains(iconId1);
        boolean wild_2 = this.wildIconSet.contains(iconId2);

        if (wild_1) {  //表示1是wild图标
            if (wild_2) {  //均为wild，相同
                sameInfo.setSame(true);
                log.debug("均为wild图标 iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
            } else {
                //如果2是普通图标
                if (normal_2) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconId2);
                    log.debug("1为wild，2是普通图标 iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
                } else {
                    log.debug("1为wild，2是非wild的特殊图标 iconId1 = {},iconId2 = {},same = false", iconId1, iconId2);
                }
            }
        } else if (normal_1) {  //表示1是普通图标
            if (wild_2) { //2是wild
                sameInfo.setSame(true);
                sameInfo.setBaseIconId(iconId1);
                log.debug("1为普通，2是wild iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
            } else {
                //如果1是普通，2是非wild，则只有两者id相同
                if (iconId1 == iconId2) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconId1);
                    log.debug("均为普通图标 iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
                }
            }
        } else {  //表示1是非wild的特殊图标,则无论2为什么，都不可能相同

        }
        return sameInfo;
    }

    /**
     * 随机一个滚轴id
     */
    private SlotsResultInfo randRollerId() {
        SlotsResultInfo slotsResultInfo = new SlotsResultInfo();
        //首先随机模式id
        int modeId = this.rollModeList.get(RandomUtils.randomInt(this.rollModeList.size()));

        //根据模式id，获取滚轴id列表
        List<Integer> rollerIdList = this.baseRollerModeCfg.getRollerMode().get(modeId);
        if (rollerIdList == null || rollerIdList.isEmpty()) {
            throw new IllegalArgumentException("未找到该游戏的滚轴id列表,生成结果集失败 gameType = " + this.gameType + ",modeId = " + modeId);
        }

        int rollerId = rollerIdList.get(RandomUtils.randomInt(rollerIdList.size()));
        slotsResultInfo.setRollerMode(modeId);
        slotsResultInfo.setRollerId(rollerId);
        return slotsResultInfo;
    }

    /**
     * 获取特殊奖励A
     *
     * @param slotsResultInfo
     * @param cfg
     * @param count
     * @return
     */
    private SlotsResultInfo randAwardA(SlotsResultInfo slotsResultInfo, SpecialAuxiliaryAwardCfg cfg, int count) {
        SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardA> awardCInfo = this.specialAuxiliaryAwardInfoMapA.get(cfg.getId());

        PropInfo tmpPropInfo = awardCInfo.getPropInfo(cfg.getId()).copy();
        //记录每个key出现的次数
        Map<Integer, Integer> existMap = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Integer key = tmpPropInfo.getRandKey();
            if (key == null) {
                log.warn("randAwardC 随机获取key为空");
                continue;
            }
            SpecialAuxiliaryAwardA awardA = awardCInfo.getAwardInfo(key);
            int afterCount = existMap.merge(key, 1, Integer::sum);
            if (afterCount >= awardA.getMaxShow()) {
                //达到最大次数后，要去除，然后重新计算权重随机范围
                tmpPropInfo.removeKeyAndRecalculate(key);
            }
            AuxiliaryAwardType auxiliaryAwardType = AuxiliaryAwardType.getType(cfg.getType());
            switch (auxiliaryAwardType){
                case REWARD_MINI_GAME -> triggerMiniGame(slotsResultInfo,awardA.getId());
//                case SPIN_COUNT_AGAIN -> test();
            }
            slotsResultInfo.addAwardAId(awardA.getId());
        }
        return slotsResultInfo;
    }

    /**
     * 获取特殊奖励C
     *
     * @param slotsResultInfo
     * @param cfg
     * @param count
     * @return
     */
    private SlotsResultInfo randAwardC(SlotsResultInfo slotsResultInfo, SpecialAuxiliaryAwardCfg cfg, int count) {
        SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardC> awardCInfo = this.specialAuxiliaryAwardInfoMapC.get(cfg.getId());

        PropInfo tmpPropInfo = awardCInfo.getPropInfo(cfg.getId()).copy();

        //记录每个key出现的次数
        Map<Integer, Integer> existMap = new HashMap<>();
        for (int i = 0; i < count; i++) {
            Integer key = tmpPropInfo.getRandKey();
            if (key == null) {
                log.warn("randAwardC 随机获取key为空");
                continue;
            }
            SpecialAuxiliaryAwardC awardC = awardCInfo.getAwardInfo(key);
            int afterCount = existMap.merge(key, 1, Integer::sum);
            if (afterCount >= awardC.getMaxShow()) {
                //达到最大次数后，要去除，然后重新计算权重随机范围
                tmpPropInfo.removeKeyAndRecalculate(key);
            }

            slotsResultInfo.addAwardC(new int[]{key, awardC.getTimes()});
        }
        return slotsResultInfo;
    }

    /********************************************配置相关***********************************************************/


    private void baseRollerModeCfg() {
        for (Map.Entry<Integer, BaseRollerModeCfg> en : GameDataManager.getBaseRollerModeCfgMap().entrySet()) {
            if (en.getValue().getGameType() == this.gameType) {
                this.baseRollerModeCfg = en.getValue();
                break;
            }
        }

        if (this.baseRollerModeCfg == null || this.baseRollerModeCfg.getRollerMode() == null || this.baseRollerModeCfg.getRollerMode().isEmpty()) {
            throw new IllegalArgumentException("未找到该游戏的滚轴模式,生成结果集失败 gameType = " + this.gameType);
        }

        this.rollModeList = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> en : this.baseRollerModeCfg.getRollerMode().entrySet()) {
            this.rollModeList.add(en.getKey());
        }
    }

    /**
     * 元素相关
     */
    private void baseElementConfig() {
        this.wildIconSet = new HashSet<>();
        this.noralIconSet = new HashSet<>();

        for (Map.Entry<Integer, BaseElementCfg> en : GameDataManager.getBaseElementCfgMap().entrySet()) {
            BaseElementCfg cfg = en.getValue();
            if (cfg.getType() == SlotsConst.BaseElement.TYPE_NORMAL) {
                this.noralIconSet.add(cfg.getElementId());
            } else if (cfg.getType() == SlotsConst.BaseElement.TYPE_WILD) {
                this.wildIconSet.add(cfg.getElementId());
            }
        }
    }

    /**
     * 滚轴相关
     */
    private void baseRollerConfig() {
        this.baseRollerCfgMap = new HashMap<>();
        //根据游戏type筛选
        for (Map.Entry<Integer, BaseRollerCfg> en : GameDataManager.getBaseRollerCfgMap().entrySet()) {
            BaseRollerCfg cfg = en.getValue();
            if (cfg.getGameType() == this.gameType) {
                this.baseRollerCfgMap.put(cfg.getColumn(), cfg);
            }
        }

        if (baseRollerCfgMap.size() != columnCount) {
            throw new IllegalArgumentException("该游戏滚轴配置中没有足够的列数,生成结果集失败 gameType = " + this.gameType);
        }
    }

    /**
     * 中奖线相关
     */
    private void baseLineConfig() {
        //column -> cfg
        this.baseLineCfgMap = new HashMap<>();
        //根据游戏type筛选
        for (Map.Entry<Integer, BaseLineCfg> en : GameDataManager.getBaseLineCfgMap().entrySet()) {
            BaseLineCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            this.baseLineCfgMap.put(cfg.getLineId(), cfg);
        }

        if (this.baseLineCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏滚轴配置中没有配置中奖线,生成结果集失败 gameType = " + this.gameType);
        }
    }

    /**
     * 特殊中奖线相关
     */
    private void baseLineFreeConfig() {
        this.baseLineFreeCfgMap = new HashMap<>();
        //根据游戏type筛选
        for (Map.Entry<Integer, BaseLineFreeCfg> en : GameDataManager.getBaseLineFreeCfgMap().entrySet()) {
            BaseLineFreeCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            List<List<Integer>> gamePlayList = cfg.getGamePlay();
            if (gamePlayList == null || gamePlayList.isEmpty()) {
                throw new IllegalArgumentException("该游戏滚轴配置中特殊中奖线的玩法为空,生成结果集失败 gameType = " + this.gameType + ",lineId = " + cfg.getLineId());
            }

            Map<Integer, List<Integer>> appointElementMap = cfg.getAppointElementList();
            if (appointElementMap == null || appointElementMap.isEmpty()) {
                throw new IllegalArgumentException("该游戏滚轴配置中特殊中奖线的指定元素列表为空,生成结果集失败 gameType = " + this.gameType + ",lineId = " + cfg.getLineId());
            }

            List<List<Integer>> leastElementKindList = cfg.getLeastElementKind();
            if (leastElementKindList == null || leastElementKindList.isEmpty()) {
                throw new IllegalArgumentException("该游戏滚轴配置中特殊中奖线的最少元素种类为空,生成结果集失败 gameType = " + this.gameType + ",lineId = " + cfg.getLineId());
            }

            Map<Integer, BaseLineFreeInfo> tempMap = this.baseLineFreeCfgMap.computeIfAbsent(cfg.getLineId(), k -> new HashMap<>());

            for (List<Integer> tempKindList : leastElementKindList) {
                BaseLineFreeInfo baseLineFreeInfo = new BaseLineFreeInfo();
                //设置最少种类
                baseLineFreeInfo.setId(tempKindList.get(0));
                baseLineFreeInfo.setMinIconTypeMin(tempKindList.get(1));

                //设置指定元素列表,主元素
                List<Integer> tempElmentList = appointElementMap.get(baseLineFreeInfo.getId());
                if (baseLineFreeInfo.getMinIconTypeMin() < 3) {
                    baseLineFreeInfo.setMainElementId(tempElmentList.get(0));
                    baseLineFreeInfo.setElementGroupList(List.of(tempElmentList));
                } else {
                    baseLineFreeInfo.setMainElementId(tempElmentList.get(1));
                    baseLineFreeInfo.setElementGroupList(List.of(tempElmentList.subList(0, 1)));
                    baseLineFreeInfo.setElementGroupList(List.of(tempElmentList.subList(1, tempElmentList.size())));
                }

                tempMap.put(baseLineFreeInfo.getId(), baseLineFreeInfo);
            }
        }
    }

    private void baseElementRewardConfig() {
        this.baseNormalIconRewardCfgMap = new HashMap<>();
        this.baseSpecialIconRewardCfgMap = new HashMap<>();

        //根据游戏type筛选
        for (Map.Entry<Integer, BaseElementRewardCfg> en : GameDataManager.getBaseElementRewardCfgMap().entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            if (cfg.getLineType() == SlotsConst.BaseElementReward.LINT_TYPE_NORMAL) {
                this.baseNormalIconRewardCfgMap.put(cfg.getId(), cfg);
            } else if (cfg.getLineType() == SlotsConst.BaseElementReward.LINT_TYPE_SPECIAL) {
                this.baseSpecialIconRewardCfgMap.put(cfg.getId(), cfg);
            }
        }

        if (this.baseNormalIconRewardCfgMap.isEmpty() || this.baseSpecialIconRewardCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏元素奖励信息为空,生成结果集失败 gameType = " + this.gameType);
        }
    }

    /**
     * 小游戏
     */
    private void specialAuxiliaryCconfig() {
        this.specialAuxiliaryPropMap = new HashMap<>();
        for (Map.Entry<Integer, SpecialAuxiliaryCfg> en : GameDataManager.getSpecialAuxiliaryCfgMap().entrySet()) {
            SpecialAuxiliaryCfg cfg = en.getValue();
            if (cfg.getRandCount() == null || cfg.getRandCount().isEmpty()) {
                throw new IllegalArgumentException("该游戏小游戏随机次数配置为空,生成结果集失败 gameType = " + this.gameType + ",auxiliaryId = " + cfg.getId());
            }

            Map<Integer, PropInfo> tempMap = this.specialAuxiliaryPropMap.computeIfAbsent(en.getKey(), k -> new HashMap<>());

            for (Map.Entry<Integer, Map<Integer, Integer>> en2 : cfg.getRandCount().entrySet()) {
                int tmpModeId = en2.getKey();
                Map<Integer, Integer> countMap = en2.getValue();
                PropInfo propInfo = tempMap.computeIfAbsent(tmpModeId, k -> new PropInfo());

                //概率起始和结尾
                int begin = 0;
                int end = 0;
                for (Map.Entry<Integer, Integer> en3 : countMap.entrySet()) {
                    begin = end;
                    end += en3.getValue();

                    propInfo.addProp(en3.getKey(), begin, end);
                }
                propInfo.setSum(end);
            }
        }
    }

    /**
     * 小游戏奖励
     */
    private void specialAuxiliaryAwardCconfig() {
        this.specialAuxiliaryAwardInfoMapA = new HashMap<>();
        this.specialAuxiliaryAwardInfoMapC = new HashMap<>();


        for (Map.Entry<Integer, SpecialAuxiliaryAwardCfg> en : GameDataManager.getSpecialAuxiliaryAwardCfgMap().entrySet()) {
            SpecialAuxiliaryAwardCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //解读奖励类型A
            if (cfg.getAwardTypeA() != null && !cfg.getAwardTypeA().isEmpty()) {
                SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardA> specialAuxiliaryAwardInfo = new SpecialAuxiliaryAwardInfo<>();

                AuxiliaryAwardType auxiliaryAwardType = AuxiliaryAwardType.getType(cfg.getGameType());

                switch (auxiliaryAwardType) {
                    case FREE_GAME_COUNT, APPOINT_ROLLER, GOLD_PROP, REWARD_MINI_GAME,
                         SPIN_COUNT_AGAIN -> handAwardANormal(cfg, specialAuxiliaryAwardInfo);
                    default -> handAwardADefault(cfg);
                }
                this.specialAuxiliaryAwardInfoMapA.put(cfg.getId(), specialAuxiliaryAwardInfo);
            }

            //解读奖励类型B
            if (cfg.getAwardTypeB() != null && !cfg.getAwardTypeB().isEmpty()) {

            }

            //解读奖励类型C
            if (cfg.getAwardTypeC() != null && !cfg.getAwardTypeC().isEmpty()) {
                SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardC> specialAuxiliaryAwardInfo = new SpecialAuxiliaryAwardInfo<>();
                PropInfo propInfo = new PropInfo();
                int begin = 0;
                int end = 0;

                for (int i = 0; i < cfg.getAwardTypeC().size(); i++) {
                    String str = cfg.getAwardTypeC().get(i);
                    begin = end;

                    String[] arr1 = str.split(",");
                    String[] arr2 = arr1[0].split("&");

                    SpecialAuxiliaryAwardC awardC = new SpecialAuxiliaryAwardC();
                    awardC.setId(i);
                    String type = arr2[0];
                    if (type.startsWith("JP")) {
                        awardC.setJpId(Integer.parseInt(arr2[1]));
                    } else {
                        awardC.setJpId(Integer.parseInt(arr2[0]));
                        awardC.setTimes(Integer.parseInt(arr2[1]));
                    }
                    awardC.setMaxShow(Integer.parseInt(arr1[2]));
                    awardC.setProp(Integer.parseInt(arr1[1]));

                    end += awardC.getProp();

                    propInfo.addProp(i, begin, end);
                    specialAuxiliaryAwardInfo.addAwardInfo(awardC.getId(), awardC);
                }
                propInfo.setSum(end);
                specialAuxiliaryAwardInfo.addProp(cfg.getId(), propInfo);
                this.specialAuxiliaryAwardInfoMapC.put(cfg.getId(), specialAuxiliaryAwardInfo);
            }
        }
    }

    /**
     * 处理 awardA 中的 通用类型
     *
     * @param cfg
     */
    private void handAwardANormal(SpecialAuxiliaryAwardCfg cfg, SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardA> specialAuxiliaryAwardInfo) {
        PropInfo propInfo = new PropInfo();

        int begin = 0;
        int end = 0;
        for (List<Integer> list : cfg.getAwardTypeA()) {
            SpecialAuxiliaryAwardA awardA = new SpecialAuxiliaryAwardA();
            awardA.setId(list.get(0));
            awardA.setProp(list.get(1));
            awardA.setMaxShow(list.get(2));

            begin = end;
            end += awardA.getProp();

            propInfo.addProp(awardA.getId(), begin, end);
            specialAuxiliaryAwardInfo.addAwardInfo(awardA.getId(), awardA);
        }
        specialAuxiliaryAwardInfo.addProp(cfg.getId(), propInfo);
    }

    /**
     * 处理 awardA 中的 默认 类型
     */
    private void handAwardADefault(SpecialAuxiliaryAwardCfg cfg) {

    }
}
