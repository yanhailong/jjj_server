package com.jjg.game.slots.manager;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.slots.constant.AuxiliaryAwardType;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * 结果集生成器
 *
 * @author 11
 * @date 2025/7/2 13:54
 */
public class AbstractSlotsGenerateManager<T extends SlotsResultLib> {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected Class<T> resultLibClazz;
    //游戏类型
    protected int gameType;

    protected BaseInitCfg baseInitCfg;
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
    //普通图标 lineType -> sid -> cfg
    protected Map<Integer, Map<Integer, BaseElementRewardCfg>> baseElementRewardCfgMap = null;

    //小游戏随机次数相关的权重信息
    protected Map<Integer, Map<Integer, PropInfo>> specialAuxiliaryRandCountPropMap = null;
    //小游戏随机奖励相关的权重信息
    protected Map<Integer, PropAndAwardInfo<FreeRandAwardInfo>> specialAuxiliaryRandAwardPropMap = null;

    //小游戏奖励相关的权重信息
    protected Map<Integer, PropInfo> specialAuxiliaryAwardInfoMapA = null;
    //    protected Map<Integer,SpecialAuxiliaryAwardInfo<SpecialAuxiliaryAwardC>> specialAuxiliaryAwardInfoMapB = null;
    protected Map<Integer, PropAndAwardInfo<SpecialAuxiliaryAwardC>> specialAuxiliaryAwardInfoMapC = null;

    protected Map<Integer, GirdUpdateConfig> specialGirdCfgMap = null;

    //在生成一条结果时，可能会产生多条分支
    protected Map<Integer,T> branchLibMap;

    public AbstractSlotsGenerateManager(Class<T> resultLibClazz) {
        this.resultLibClazz = resultLibClazz;
    }

    public void init(int gameType) {
        this.gameType = gameType;
        initConfig();
    }

    public void initConfig() {
        baseInitCfg();
        baseRollerModeCfg();
        baseElementConfig();
        baseRollerConfig();
        baseLineConfig();
        baseLineFreeConfig();
        baseElementRewardConfig();

        specialAuxiliaryCconfig();
        specialAuxiliaryAwardCconfig();
        specialGirdConfig();
    }

    /**
     * 生成结果集
     *
     * @param count 生成条数
     */
    public void generate(int count) {
        try {
            for (int i = 0; i < count; i++) {
                log.debug("开始生成第 {} 条",i);
                generateOne();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    protected T createResultLib() throws Exception {
        Constructor<T> constructor = this.resultLibClazz.getConstructor();
        return constructor.newInstance();
    }

    /**
     * 生成一个结果
     */
    public T generateOne() {
        return null;
    }

    /**
     * 生成20个图标
     *
     * @return
     */
    public int[] generateAllIcons(int rollId) {
        int[] arr = new int[this.baseInitCfg.getCols() * this.baseInitCfg.getRows() + 1];

        for (Map.Entry<Integer, BaseRollerCfg> en : this.baseRollerCfgMap.entrySet()) {
            BaseRollerCfg cfg = en.getValue();
            List<Integer> scope = cfg.getAxleCountScope().get(rollId);
            if (scope == null || scope.isEmpty()) {
                log.warn("没有该滚轴的范围,生成结果集失败 gameType = {},rollerId = {}", this.gameType, rollId);
                return null;
            }

            int iconIndex = (cfg.getColumn() - 1) * this.baseInitCfg.getRows() + 1;

            //区间范围的第一个下标
            int first = scope.get(0) - 1;
            //区间范围的最后一个下标
            int last = scope.get(1) - 1;

            //随机生成一个起始位置
            int index = RandomUtils.randomMinMax(first, last);
            for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
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
        return arr;
    }


    /**
     * 判断两个icon是否一样
     *
     * @param sameInfo
     * @param iconId1
     * @param iconId2
     * @return
     */
    protected SameInfo iconSame(SameInfo sameInfo, int iconId1, int iconId2) {
        //是不是普通图标
        boolean normal_1 = this.noralIconSet.contains(iconId1);
        boolean normal_2 = this.noralIconSet.contains(iconId2);

        //是不是wild
        boolean wild_1 = this.wildIconSet.contains(iconId1);
        boolean wild_2 = this.wildIconSet.contains(iconId2);

        if (wild_1) {  //表示1是wild图标
            if (wild_2) {  //均为wild，相同
                sameInfo.setSame(true);
//                log.debug("均为wild图标 iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
            } else {
                //如果2是普通图标
                if (normal_2) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconId2);
//                    log.debug("1为wild，2是普通图标 iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
                } else {
//                    log.debug("1为wild，2是非wild的特殊图标 iconId1 = {},iconId2 = {},same = false", iconId1, iconId2);
                }
            }
        } else if (normal_1) {  //表示1是普通图标
            if (wild_2) { //2是wild
                sameInfo.setSame(true);
                sameInfo.setBaseIconId(iconId1);
//                log.debug("1为普通，2是wild iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
            } else {
                //如果1是普通，2是非wild，则只有两者id相同
                if (iconId1 == iconId2) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconId1);
//                    log.debug("均为普通图标 iconId1 = {},iconId2 = {},same = true", iconId1, iconId2);
                }
            }
        } else {  //表示1是非wild的特殊图标,则无论2为什么，都不可能相同

        }
        return sameInfo;
    }

    /**
     * 随机一个滚轴id
     */
    public T randRollerId() throws Exception {
        T slotsResultLib = createResultLib();
        //首先随机模式id
        int modeId = this.rollModeList.get(RandomUtils.randomInt(this.rollModeList.size()));

        //根据模式id，获取滚轴id列表
        List<Integer> rollerIdList = this.baseRollerModeCfg.getRollerMode().get(modeId);
        if (rollerIdList == null || rollerIdList.isEmpty()) {
            throw new IllegalArgumentException("未找到该游戏的滚轴id列表,生成结果集失败 gameType = " + this.gameType + ",modeId = " + modeId);
        }

        int rollerId = rollerIdList.get(RandomUtils.randomInt(rollerIdList.size()));
        slotsResultLib.setRollerMode(modeId);
        slotsResultLib.setRollerId(rollerId);
        return slotsResultLib;
    }

    /**
     * 根据 SpecialAuxiliary 表的 freeAward的配置，找到 SpecialAuxiliaryAward 中awardTypeA的真正数据
     *
     * @param rewardIds
     * @param count
     * @return key -> SpecialAuxiliaryAward.type
     */
    protected FreeAwardRealData getFreeAwardRealData(List<Integer> rewardIds, int count) {
        FreeAwardRealData freeAwardRealData = new FreeAwardRealData();

        List<int[]> resultListA = new ArrayList<>();
        Map<Integer,List<int[]>> resultMapC = new HashMap<>();
        for (int i = 0; i < rewardIds.size(); i++) {
            int rewardId = rewardIds.get(i);
            SpecialAuxiliaryAwardCfg specialAuxiliaryAwardCfg = GameDataManager.getSpecialAuxiliaryAwardCfg(rewardId);
            if (specialAuxiliaryAwardCfg == null) {
                log.warn("特殊游戏奖励配置为空1 SpecialAuxiliaryCfg.id = {},SpecialAuxiliaryAwardCfg.id = {}", rewardId, rewardIds.get(0));
                continue;
            }

            //获取奖励A
            if (specialAuxiliaryAwardCfg.getAwardTypeA() != null && !specialAuxiliaryAwardCfg.getAwardTypeA().isEmpty()) {
                PropInfo tmpPropInfo = this.specialAuxiliaryAwardInfoMapA.get(specialAuxiliaryAwardCfg.getId()).clone();

                //记录每个key出现的次数
                Map<Integer, Integer> existMap = new HashMap<>();
                for (int j = 0; j < count; j++) {
                    Integer key = tmpPropInfo.getRandKey();
                    if (key == null) {
                        log.warn("randAwardA 随机获取key为空 cfg.id = {}", specialAuxiliaryAwardCfg.getId());
                        continue;
                    }
                    int afterCount = existMap.merge(key, 1, Integer::sum);
                    if (count > 1 && afterCount >= tmpPropInfo.getMaxShowLimit(key)) {
                        //达到最大次数后，要去除，然后重新计算权重随机范围
                        tmpPropInfo.removeKeyAndRecalculate(key);
                    }

                    resultListA.add(new int[]{specialAuxiliaryAwardCfg.getType(), key});
                }
            }

            //获取奖励C
            if(specialAuxiliaryAwardCfg.getAwardTypeC() != null && !specialAuxiliaryAwardCfg.getAwardTypeC().isEmpty()){
                PropAndAwardInfo<SpecialAuxiliaryAwardC> specialAuxiliaryAwardCPropAndAwardInfo = this.specialAuxiliaryAwardInfoMapC.get(specialAuxiliaryAwardCfg.getId());
                List<int[]> resultList = resultMapC.computeIfAbsent(rewardId, k -> new ArrayList<>());

                PropInfo tmpPropInfo = specialAuxiliaryAwardCPropAndAwardInfo.getPropInfo(specialAuxiliaryAwardCfg.getId()).clone();

                //记录每个key出现的次数
                Map<Integer, Integer> existMap = new HashMap<>();
                for (int j = 0; j < count; j++) {
                    Integer key = tmpPropInfo.getRandKey();
                    if (key == null) {
                        log.warn("randAwardC 随机获取key为空 cfg.id = {}", specialAuxiliaryAwardCfg.getId());
                        continue;
                    }
                    int afterCount = existMap.merge(key, 1, Integer::sum);
                    if (count > 1 && afterCount >= tmpPropInfo.getMaxShowLimit(key)) {
                        //达到最大次数后，要去除，然后重新计算权重随机范围
                        tmpPropInfo.removeKeyAndRecalculate(key);
                    }

                    SpecialAuxiliaryAwardC awardCInfo = specialAuxiliaryAwardCPropAndAwardInfo.getAwardInfo(key);
                    if(awardCInfo.getJpId() > 0){
                        resultList.add(new int[]{awardCInfo.getType(), awardCInfo.getJpId()});
                    }else {
                        resultList.add(new int[]{awardCInfo.getType(), awardCInfo.getTimes()});
                    }

                }
            }
        }

        freeAwardRealData.setResultListA(resultListA);
        freeAwardRealData.setResultMapC(resultMapC);
        return freeAwardRealData;
    }


    /********************************************配置相关***********************************************************/


    private void baseInitCfg() {
        BaseInitCfg tmpBaseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        if (tmpBaseInitCfg == null) {
            throw new IllegalArgumentException("未找到该游戏的基础配置(baseInit表),生成结果集失败 gameType = " + this.gameType);
        }
        this.baseInitCfg = tmpBaseInitCfg;
    }

    private void baseRollerModeCfg() {
        BaseRollerModeCfg tempBaseRollerModeCfg = null;
        for (Map.Entry<Integer, BaseRollerModeCfg> en : GameDataManager.getBaseRollerModeCfgMap().entrySet()) {
            if (en.getValue().getGameType() == this.gameType) {
                tempBaseRollerModeCfg = en.getValue();
                break;
            }
        }

        if (tempBaseRollerModeCfg == null || tempBaseRollerModeCfg.getRollerMode() == null || tempBaseRollerModeCfg.getRollerMode().isEmpty()) {
            throw new IllegalArgumentException("未找到该游戏的滚轴模式,生成结果集失败 gameType = " + this.gameType);
        }

        List<Integer> tmpList = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> en : tempBaseRollerModeCfg.getRollerMode().entrySet()) {
            tmpList.add(en.getKey());
        }
        this.baseRollerModeCfg = tempBaseRollerModeCfg;
        this.rollModeList = tmpList;
    }

    /**
     * 元素相关
     */
    private void baseElementConfig() {
        Set<Integer> tmpWildIconSet = new HashSet<>();
        Set<Integer> tmpNoralIconSet = new HashSet<>();

        for (Map.Entry<Integer, BaseElementCfg> en : GameDataManager.getBaseElementCfgMap().entrySet()) {
            BaseElementCfg cfg = en.getValue();
            if (cfg.getType() == SlotsConst.BaseElement.TYPE_NORMAL) {
                tmpNoralIconSet.add(cfg.getElementId());
            } else if (cfg.getType() == SlotsConst.BaseElement.TYPE_WILD) {
                tmpWildIconSet.add(cfg.getElementId());
            }
        }
        this.wildIconSet = tmpWildIconSet;
        this.noralIconSet = tmpNoralIconSet;
    }

    /**
     * 滚轴相关
     */
    private void baseRollerConfig() {
        Map<Integer, BaseRollerCfg> tmpBaseRollerCfgMap = new HashMap<>();
        //根据游戏type筛选
        for (Map.Entry<Integer, BaseRollerCfg> en : GameDataManager.getBaseRollerCfgMap().entrySet()) {
            BaseRollerCfg cfg = en.getValue();
            if (cfg.getGameType() == this.gameType) {
                tmpBaseRollerCfgMap.put(cfg.getColumn(), cfg);
            }
        }

        if (tmpBaseRollerCfgMap.size() != this.baseInitCfg.getCols()) {
            throw new IllegalArgumentException("该游戏滚轴配置中没有足够的列数,生成结果集失败 gameType = " + this.gameType);
        }
        this.baseRollerCfgMap = tmpBaseRollerCfgMap;
    }

    /**
     * 中奖线相关
     */
    private void baseLineConfig() {
        //column -> cfg
        Map<Integer, BaseLineCfg> tmpBaseLineCfgMap = new HashMap<>();

        //根据游戏type筛选
        for (Map.Entry<Integer, BaseLineCfg> en : GameDataManager.getBaseLineCfgMap().entrySet()) {
            BaseLineCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            tmpBaseLineCfgMap.put(cfg.getLineId(), cfg);
        }

        if (tmpBaseLineCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏滚轴配置中没有配置中奖线,生成结果集失败 gameType = " + this.gameType);
        }
        this.baseLineCfgMap = tmpBaseLineCfgMap;
    }

    /**
     * 特殊中奖线相关
     */
    private void baseLineFreeConfig() {
        Map<Integer, Map<Integer, BaseLineFreeInfo>> tmpBaseLineFreeCfgMap = new HashMap<>();

        //根据游戏type筛选
        for (Map.Entry<Integer, BaseLineFreeCfg> en : GameDataManager.getBaseLineFreeCfgMap().entrySet()) {
            BaseLineFreeCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //线路类型
            Map<Integer, Integer> gamePlayMap = cfg.getGamePlay();
            if (gamePlayMap == null || gamePlayMap.isEmpty()) {
                throw new IllegalArgumentException("该游戏滚轴配置中特殊中奖线的玩法为空,生成结果集失败 gameType = " + this.gameType + ",lineId = " + cfg.getLineId());
            }

            //指定元素列表
            Map<Integer, List<Integer>> appointElementMap = cfg.getAppointElementList();
            if (appointElementMap == null || appointElementMap.isEmpty()) {
                throw new IllegalArgumentException("该游戏滚轴配置中特殊中奖线的指定元素列表为空,生成结果集失败 gameType = " + this.gameType + ",lineId = " + cfg.getLineId());
            }

            //最少元素种类
            List<List<Integer>> leastElementKindList = cfg.getLeastElementKind();
            if (leastElementKindList == null || leastElementKindList.isEmpty()) {
                throw new IllegalArgumentException("该游戏滚轴配置中特殊中奖线的最少元素种类为空,生成结果集失败 gameType = " + this.gameType + ",lineId = " + cfg.getLineId());
            }

            Map<Integer, BaseLineFreeInfo> tempMap = tmpBaseLineFreeCfgMap.computeIfAbsent(cfg.getLineId(), k -> new HashMap<>());

            for (List<Integer> tempKindList : leastElementKindList) {
//            for (int i = 0; i < leastElementKindList.size(); i++) {
//                List<Integer> tempKindList = leastElementKindList.get(i);

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
                baseLineFreeInfo.setPlayType(gamePlayMap.get(baseLineFreeInfo.getId()));

                tempMap.put(baseLineFreeInfo.getId(), baseLineFreeInfo);
            }
        }

        this.baseLineFreeCfgMap = tmpBaseLineFreeCfgMap;
    }

    private void baseElementRewardConfig() {
        Map<Integer, Map<Integer, BaseElementRewardCfg>> tmpBaseElementRewardCfgMap = new HashMap<>();

        //根据游戏type筛选
        for (Map.Entry<Integer, BaseElementRewardCfg> en : GameDataManager.getBaseElementRewardCfgMap().entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            Map<Integer, BaseElementRewardCfg> tempMap = tmpBaseElementRewardCfgMap.computeIfAbsent(cfg.getLineType(), k -> new HashMap<>());
            tempMap.put(cfg.getId(), cfg);
        }

        if (tmpBaseElementRewardCfgMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏元素奖励信息为空,生成结果集失败 gameType = " + this.gameType);
        }
        this.baseElementRewardCfgMap = tmpBaseElementRewardCfgMap;
    }

    /**
     * 小游戏
     */
    private void specialAuxiliaryCconfig() {
        Map<Integer, Map<Integer, PropInfo>> tmpSpecialAuxiliaryRandCountPropMap = new HashMap<>();
        Map<Integer, PropAndAwardInfo<FreeRandAwardInfo>> tmpSpecialAuxiliaryRandAwardPropMap = new HashMap<>();


        for (Map.Entry<Integer, SpecialAuxiliaryCfg> en : GameDataManager.getSpecialAuxiliaryCfgMap().entrySet()) {
            SpecialAuxiliaryCfg cfg = en.getValue();
            if (cfg.getRandCount() == null || cfg.getRandCount().isEmpty()) {
                throw new IllegalArgumentException("该游戏小游戏随机次数配置为空,生成结果集失败 gameType = " + this.gameType + ",auxiliaryId = " + cfg.getId());
            }

            Map<Integer, PropInfo> tempMap = tmpSpecialAuxiliaryRandCountPropMap.computeIfAbsent(en.getKey(), k -> new HashMap<>());

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

            //分析免费随机奖励的配置
            if (cfg.getFreeRandAward() != null && !cfg.getFreeRandAward().isEmpty()) {
                PropAndAwardInfo<FreeRandAwardInfo> propAndAwardInfo = tmpSpecialAuxiliaryRandAwardPropMap.computeIfAbsent(en.getKey(), k -> new PropAndAwardInfo<>());
                for (Map.Entry<Integer, List<List<Integer>>> en2 : cfg.getFreeRandAward().entrySet()) {
                    //模式id
                    int modelId = en2.getKey();
                    List<List<Integer>> tmpList = en2.getValue();

                    PropInfo propInfo = new PropInfo();

                    //概率起始和结尾
                    int begin = 0;
                    int end = 0;
                    for (List<Integer> awardIdList : tmpList) {
                        FreeRandAwardInfo randAwardInfo = new FreeRandAwardInfo();
                        randAwardInfo.setModelId(modelId);
                        randAwardInfo.setAwardId(awardIdList.get(0));
                        randAwardInfo.setProp(awardIdList.get(1));
                        randAwardInfo.setMaxLimit(awardIdList.get(2));

                        begin = end;
                        end += randAwardInfo.getProp();

                        propInfo.addProp(randAwardInfo.getAwardId(), begin, end);
                        propAndAwardInfo.addAwardInfo2(randAwardInfo.getModelId(),randAwardInfo.getAwardId(), randAwardInfo);
                    }

                    propInfo.setSum(end);
                    propAndAwardInfo.addProp(modelId, propInfo);
                }
            }
        }

        this.specialAuxiliaryRandCountPropMap = tmpSpecialAuxiliaryRandCountPropMap;
        this.specialAuxiliaryRandAwardPropMap = tmpSpecialAuxiliaryRandAwardPropMap;
    }

    /**
     * 小游戏奖励
     */
    private void specialAuxiliaryAwardCconfig() {
        Map<Integer, PropInfo> tmpSpecialAuxiliaryAwardInfoMapA = new HashMap<>();
        Map<Integer, PropAndAwardInfo<SpecialAuxiliaryAwardC>> tmpSpecialAuxiliaryAwardInfoMapC = new HashMap<>();

        for (Map.Entry<Integer, SpecialAuxiliaryAwardCfg> en : GameDataManager.getSpecialAuxiliaryAwardCfgMap().entrySet()) {
            SpecialAuxiliaryAwardCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //解读奖励类型A
            if (cfg.getAwardTypeA() != null && !cfg.getAwardTypeA().isEmpty()) {

                AuxiliaryAwardType auxiliaryAwardType = AuxiliaryAwardType.getType(cfg.getType());

                switch (auxiliaryAwardType) {
                    case FREE_GAME_COUNT, APPOINT_ROLLER, GOLD_PROP, REWARD_MINI_GAME,
                         SPIN_COUNT_AGAIN -> tmpSpecialAuxiliaryAwardInfoMapA.put(cfg.getId(), handAwardANormal(cfg));
                    default -> handAwardADefault(cfg);
                }
            }

            //解读奖励类型B
            if (cfg.getAwardTypeB() != null && !cfg.getAwardTypeB().isEmpty()) {

            }

            //解读奖励类型C
            if (cfg.getAwardTypeC() != null && !cfg.getAwardTypeC().isEmpty()) {
                PropAndAwardInfo<SpecialAuxiliaryAwardC> propAndAwardInfo = new PropAndAwardInfo<>();
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
                        awardC.setType(Integer.parseInt(arr2[0]));
                        awardC.setTimes(Integer.parseInt(arr2[1]));
                    }
                    awardC.setMaxShow(Integer.parseInt(arr1[2]));
                    awardC.setProp(Integer.parseInt(arr1[1]));

                    end += awardC.getProp();

                    propInfo.addProp(i, begin, end);
                    propAndAwardInfo.addAwardInfo(awardC.getId(), awardC);
                }
                propInfo.setSum(end);
                propAndAwardInfo.addProp(cfg.getId(), propInfo);
                tmpSpecialAuxiliaryAwardInfoMapC.put(cfg.getId(), propAndAwardInfo);
            }
        }

        this.specialAuxiliaryAwardInfoMapA = tmpSpecialAuxiliaryAwardInfoMapA;
        this.specialAuxiliaryAwardInfoMapC = tmpSpecialAuxiliaryAwardInfoMapC;
    }

    /**
     * 格子修改配置
     */
    private void specialGirdConfig() {
        Map<Integer, GirdUpdateConfig> tmpSpecialGirdCfgMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialGirdCfg> en : GameDataManager.getSpecialGirdCfgMap().entrySet()) {
            SpecialGirdCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }
            if(cfg.getGirdUpdateType() != SlotsConst.SpecialGird.GIRD_UPDATE_TYPE_RAND){
                continue;
            }
            GirdUpdateConfig config = new GirdUpdateConfig();
            config.setId(cfg.getId());

            PropInfo specialPropInfo = new PropInfo();
            PropInfo propInfo = new PropInfo();
            int specialBegin = 0;
            int specialEnd = 0;

            int begin = 0;
            int end = 0;

            //元素
            for (Map.Entry<Integer, Integer> elementEn : cfg.getElement().entrySet()) {
                specialBegin = specialEnd;
                begin = end;

                int iconId = elementEn.getKey();
                //检查元素是不是特殊元素
                if (girdSpecialElement(iconId)) {
                    specialEnd += elementEn.getValue();
                    specialPropInfo.addProp(iconId, specialBegin, specialEnd);
                    config.setSpecialIconId(iconId);
                } else {
                    end += elementEn.getValue();
                    propInfo.addProp(iconId, begin, end);
                }
            }
            specialPropInfo.setSum(specialEnd);
            propInfo.setSum(end);
            config.setSpecialIconPropInfo(specialPropInfo);
            config.setOtherIconPropInfo(propInfo);

            //允许去替换的格子id -> 元素id
            Map<Integer,Integer> girdMap = new HashMap<>();
            //巨型块大小
            for (List<String> list : cfg.getBigBlockSize()) {
                int iconId = Integer.parseInt(list.get(0));
                String[] arr = list.get(1).split("-");
                for (String str : arr) {
                    //列
                    int columnId = Integer.parseInt(str);
                    int index = (columnId - 1) * this.baseInitCfg.getRows() + 1;
                    //计算这一列所有的格子id
                    for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
                        girdMap.put(index + i, iconId);
                    }
                }
            }

            specialPropInfo = new PropInfo();
            propInfo = new PropInfo();

            specialBegin = 0;
            specialEnd = 0;

            begin = 0;
            end = 0;

            //影响格子
            for (List<Integer> list : cfg.getAffectGird()) {
                int girdId = list.get(0);
                int prop = list.get(1);
                int maxShowLimit = list.get(2);

                specialBegin = specialEnd;
                begin = end;

                Integer iconId = girdMap.get(girdId);
                if(iconId == null){
                    continue;
                }

                if(girdSpecialElement(iconId)){
                    specialEnd += prop;
                    specialPropInfo.addProp(girdId, specialBegin, specialEnd,maxShowLimit);
                }else {
                    end += prop;
                    propInfo.addProp(girdId, begin, end,maxShowLimit);
                }
            }
            specialPropInfo.setSum(specialEnd);
            propInfo.setSum(end);

            config.setSpecialIconAffectGirdPropInfo(specialPropInfo);
            config.setOtherIconAffectGirdPropInfo(propInfo);


            propInfo = new PropInfo();

            begin = 0;
            end = 0;
            //随机次数
            for (Map.Entry<Integer,Integer> randCountEn : cfg.getRandCount().entrySet()) {
                begin = end;
                end += randCountEn.getValue();
                propInfo.addProp(randCountEn.getKey(), begin, end);
            }
            propInfo.setSum(end);
            config.setRandCountPropInfo(propInfo);

            tmpSpecialGirdCfgMap.put(cfg.getId(), config);
        }

        this.specialGirdCfgMap = tmpSpecialGirdCfgMap;
    }

    protected boolean girdSpecialElement(int iconId) {
        return false;
    }

    /**
     * 处理 awardA 中的 通用类型
     *
     * @param cfg
     */
    private PropInfo handAwardANormal(SpecialAuxiliaryAwardCfg cfg) {
        PropInfo propInfo = new PropInfo();

        int begin = 0;
        int end = 0;
        for (List<Integer> list : cfg.getAwardTypeA()) {

            begin = end;
            end += list.get(1);

            propInfo.addProp(list.get(0), begin, end, list.get(2));
        }
        propInfo.setSum(end);
        return propInfo;
    }

    /**
     * 处理 awardA 中的 默认 类型
     */
    private void handAwardADefault(SpecialAuxiliaryAwardCfg cfg) {

    }

    /**
     * 每个图标出现的次数
     *
     * @param arr
     * @return
     */
    protected Map<Integer, Integer> iconShowCount(int[] arr) {
        // 统计arr中每个元素的出现次数
        Map<Integer, Integer> iconShowCountMap = new HashMap<>();
        for (int num : arr) {
            iconShowCountMap.merge(num, 1, Integer::sum);
        }
        return iconShowCountMap;
    }

    /**
     * 格子修改
     *
     * @param spinType
     * @param arr
     */
    protected int[] girdUpdate(int modelId, int spinType, int rotatestate, int[] arr) {
        if (spinType == 1) {
            return arr;
        }

        for (Map.Entry<Integer, SpecialGirdCfg> en : GameDataManager.getSpecialGirdCfgMap().entrySet()) {
            SpecialGirdCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }
            if (!checkModelId(cfg.getModelId(),modelId)) {
                continue;
            }
            if (spinType != cfg.getSpinType()) {
                continue;
            }
            if (rotatestate != cfg.getSpinStatus()) {
                continue;
            }

            GirdUpdateConfig girdUpdateConfig = this.specialGirdCfgMap.get(en.getKey());
            //获取随机次数
            Integer randCount = girdUpdateConfig.getRandCountPropInfo().getRandKey();

            if(girdUpdateConfig.getSpecialIconId() > 0){
                //获取一个特殊图标需要替换的格子id
                Integer specialIconGirdId = girdUpdateConfig.getSpecialIconAffectGirdPropInfo().getRandKey();
                if(specialIconGirdId != null && specialIconGirdId > 0){
                    int oldIconId = arr[specialIconGirdId];
                    arr[specialIconGirdId] = girdUpdateConfig.getSpecialIconId();
                    log.debug("修改格子(特殊) girdId = {},oldIconId = {},newIconId = {}", specialIconGirdId,oldIconId, girdUpdateConfig.getSpecialIconId());
                    randCount--;
                }
            }

            PropInfo clonePropInfo = girdUpdateConfig.getOtherIconAffectGirdPropInfo().clone();
            //记录每个key出现的次数
            Map<Integer, Integer> existMap = new HashMap<>();
            for(int i=0;i<randCount;i++){
                //获取一个其他特殊图标需要替换的格子id
                Integer otherIconGirdId = clonePropInfo.getRandKey();
                int maxShowLimit = clonePropInfo.getMaxShowLimit(otherIconGirdId);
                int afterCount = existMap.merge(otherIconGirdId, 1, Integer::sum);
                if (afterCount >= maxShowLimit) {
                    //达到最大次数后，要去除，然后重新计算权重随机范围
                    clonePropInfo.removeKeyAndRecalculate(otherIconGirdId);
                }

                Integer otherIconId = girdUpdateConfig.getOtherIconPropInfo().getRandKey();
                if(otherIconGirdId != null && otherIconGirdId > 0 && otherIconId != null && otherIconId > 0){
                    int oldIconId = arr[otherIconGirdId];
                    arr[otherIconGirdId] = otherIconId;
                    log.debug("修改格子 girdId = {},oldIconId = {},newIconId = {}", otherIconGirdId,oldIconId, otherIconId);
                }
            }

            break;
        }
        return arr;
    }

    /**
     * 检查当前模式id和配置的模式id是否一致
     * @param configModelId
     * @param currentModelId
     * @return
     */
    protected boolean checkModelId(int configModelId,int currentModelId) {
        if(configModelId == 0){
            return true;
        }
        return configModelId == currentModelId;
    }

    public Map<Integer, T> getBranchLibMap() {
        return branchLibMap;
    }

    public BaseInitCfg getBaseInitCfg() {
        return baseInitCfg;
    }
}
