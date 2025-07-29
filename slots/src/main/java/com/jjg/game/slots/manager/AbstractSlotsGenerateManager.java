package com.jjg.game.slots.manager;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
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
public class AbstractSlotsGenerateManager<A extends AwardLineInfo,T extends SlotsResultLib<A>> implements ConfigExcelChangeListener {
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
    //lineId -> 主元素id - > cfg
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
    public List<T> generateOne() {
        try {
            //获取模式id和滚轴id
            T lib = randRollerId();

            //生成20个图标
            int[] arr = generateAllIcons(lib.getRollerId());
            if (arr == null) {
                return Collections.EMPTY_LIST;
            }

            return checkAward(arr, lib);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    /**
     * 检查奖励
     *
     * @param arr
     * @param lib
     * @return
     * @throws Exception
     */
    public List<T> checkAward(int[] arr, T lib) throws Exception {
        List<T> libList = new ArrayList<>();

        lib.setId(RandomUtils.getUUid());
        lib.setLibType(SlotsConst.SpecialResultLib.TYPE_NORMAL);
        libList.add(lib);

        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查中奖线
        List<A> awardLineInfoList = normalAward(lib.getIconArr(), SlotsConst.BaseElementReward.ROTATESTATE_NORMAL);
        lib.setAwardLineInfoList(awardLineInfoList);

        //检查特殊奖励
        specialAward(arr, lib, SlotsConst.BaseElementReward.ROTATESTATE_NORMAL,libList,-1);

        for(T tmpLib : libList){
            calTimes(tmpLib);
        }
        return libList;
    }

    /**
     * 按照配置生成所有的图标元素
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
     * 检查普通奖励
     *
     * @param arr
     * @param rotateState 旋转状态
     * @return
     */
    public List<A> normalAward(int[] arr, int rotateState) {
//        log.debug("开始检查中奖线信息 rotateState = {}", rotateState);
        List<A> awardLineInfoList = new ArrayList<>();

        for (Map.Entry<Integer, BaseLineCfg> en : this.baseLineCfgMap.entrySet()) {
            BaseLineCfg cfg = en.getValue();
            int id = cfg.getGamePlay().get(0).get(0);
            List<Integer> lineList = cfg.getPosLocation().get(id);

            SameInfo sameInfo = new SameInfo();

            int last = lineList.size() - 1;

            //标记是否连线
            int sameCount = 0;

            for (int i = 0; i < last; i++) {
                int index1 = lineList.get(i);

                int behind = i + 1;
                int index2 = lineList.get(behind);

//                log.debug("index1={}, index2={}", index1, index2);
                sameInfo = iconSame(sameInfo, arr[index1], arr[index2]);
                if (sameInfo.isSame()) {
                    sameInfo.setSame(false);
                    sameCount = sameCount < 1 ? 2 : sameCount + 1;
                } else {
                    break;
                }
            }

            //如果有连线
            if (sameCount > 1) {
                Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_NORMAL);
                for (Map.Entry<Integer, BaseElementRewardCfg> rewardEn : normalRewardCfgMap.entrySet()) {
                    BaseElementRewardCfg rewardCfg = rewardEn.getValue();
                    if (rewardCfg.getRotateState() != SlotsConst.BaseElementReward.ROTATESTATE_ALL && rewardCfg.getRotateState() != rotateState) {
                        continue;
                    }

                    //匹配连线的元素id和个数
                    if (rewardCfg.getElementId() != sameInfo.getBaseIconId() || sameCount != rewardCfg.getRewardNum()) {
                        continue;
                    }

                    A info = addAwardLineInfo(cfg,rewardCfg,sameCount,sameInfo.getBaseIconId(),lineList,arr);
                    awardLineInfoList.add(info);
                    break;
                }
            }
        }
        return awardLineInfoList;
    }

    /**
     * 检查特殊奖励
     * @param iconArr
     * @param lib
     * @param rotateState
     * @param libList
     * @param index
     * @return
     * @throws Exception
     */
    public T specialAward(int[] iconArr, T lib, int rotateState,List<T> libList,int index) throws Exception {
        //        log.debug("开始检查特殊中奖");
        // 统计arr中每个元素的出现次数
        Map<Integer, Integer> iconShowCountMap = iconShowCount(iconArr);

        //检查baseLineFree配置的出现的特殊元素，  线路玩法 -> 主元素id -> 出现总次数
        Map<Integer, Map<Integer, Integer>> showIdMap = baseLineFreeShowId(iconShowCountMap);

        for (Map.Entry<Integer, Map<Integer, BaseElementRewardCfg>> specialElementCfgEn : this.baseElementRewardCfgMap.entrySet()) {
            //线路玩法
            int playType = specialElementCfgEn.getKey();
            //只检测特殊线路玩法
            if (playType == SlotsConst.BaseElementReward.LINE_TYPE_NORMAL) {
                continue;
            }

            //从 baseLineFree 读取icon的数据
            Map<Integer, Integer> tempShowIdMap = null;
            if (!showIdMap.isEmpty()) {
                tempShowIdMap = showIdMap.get(playType);
            }

            Map<Integer, BaseElementRewardCfg> specialElementCfgMap = specialElementCfgEn.getValue();
            for (Map.Entry<Integer, BaseElementRewardCfg> en : specialElementCfgMap.entrySet()) {
                BaseElementRewardCfg cfg = en.getValue();
                //元素id
                int iconId = cfg.getElementId();
                //匹配连线的元素id和个数
                Integer showCount = null;

                //检查tempShowIdMap是否有特殊元素的个数
                if (tempShowIdMap != null && !tempShowIdMap.isEmpty()) {
                    showCount = tempShowIdMap.get(iconId);
                }

                if (showCount == null) {
                    showCount = iconShowCountMap.get(iconId);
                    if (showCount == null) {
                        continue;
                    }
                }

                //出现的个数没有达到要求
                if (showCount != cfg.getRewardNum()) {
                    continue;
                }

                //是否有小游戏配置
                Map<Integer, Integer> featureTriggerIdMap = cfg.getFeatureTriggerId();
                if (featureTriggerIdMap == null || featureTriggerIdMap.isEmpty()) {
                    continue;
                }

                //小游戏id
                Integer miniGameId = featureTriggerIdMap.get(lib.getRollerMode());
                if (miniGameId == null || miniGameId < 1) {
                    continue;
                }

                //出现的元素种类数
                if(!checkIconTypes(iconId,iconShowCountMap)){
                    continue;
                }

                lib = triggerMiniGame(lib, iconId, miniGameId, rotateState,libList,index);
            }
        }

        return lib;
    }

    /**
     * 触发小游戏
     *
     * @param lib
     * @param miniGameId
     * @param rotateState
     * @return
     */
    public T triggerMiniGame(T lib, int iconId,int miniGameId, int rotateState,List<T> libList,int index) throws Exception {
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return lib;
        }

        //随机次数
        PropInfo propInfo = this.specialAuxiliaryRandCountPropMap.get(miniGameId).get(lib.getRollerMode());
        if (propInfo == null) {
            log.warn("该小游戏的权重信息未找到 miniGameId = {}", miniGameId);
            return lib;
        }
        Integer count = propInfo.getRandKey();
        if (count == null || count < 1) {
            log.warn("获取的随机次数为空或小于0， miniGameId = {},rollerMode = {},count = {}", miniGameId, lib.getRollerMode(), count);
            return lib;
        }
        log.debug("触发小游戏id = {},rotateState = {},count = {},modelId = {}", miniGameId, rotateState, count,lib.getRollerMode());

        //处理固定奖励
        if (specialAuxiliaryCfg.getFreeAward() != null && !specialAuxiliaryCfg.getFreeAward().isEmpty()) {
            for (List<Integer> rewardIds : specialAuxiliaryCfg.getFreeAward()) {
                lib = handSpecialAuxiliaryAward(lib, rewardIds, specialAuxiliaryCfg, iconId, count, rotateState,libList,index);
            }
        }

        lib = handSpecialRandReward(lib, specialAuxiliaryCfg, count, rotateState,libList,index);
        return lib;
    }

    /**
     * 触发重转
     *
     * @param lib
     * @param specialAuxiliaryCfg
     * @param count
     * @param rotateState
     * @return
     */
    protected T triggerAgainGame(T lib,List<int[]> specialAuxiliaryAwardDataList,
                                                    SpecialAuxiliaryCfg specialAuxiliaryCfg,
                                                    int count,
                                                    int rotateState,
                                                    List<T> libList) throws Exception {

        return lib;
    }

    /**
     * 处理 SpecialAuxiliary 表中的奖励
     *
     * @param lib
     * @param rewardIds
     * @param specialAuxiliaryCfg
     * @param count
     * @param rotateState
     * @return
     */
    public T handSpecialAuxiliaryAward(T lib,List<Integer> rewardIds,SpecialAuxiliaryCfg specialAuxiliaryCfg,
                                                            int iconId,
                                                            int count,
                                                            int rotateState,
                                                            List<T> libList,
                                                            int index) throws Exception {
        log.debug("处理 specialAuxiliartAward 奖励逻辑 specialAuxiliaryId =  {},modelId = {}", rewardIds,lib.getRollerMode());

        //根据配置，找到真正的数据
        FreeAwardRealData freeAwardRealData = getFreeAwardRealData(rewardIds, count);

        //奖励A
        lib = handAwardA(lib,freeAwardRealData,iconId,count,rotateState,specialAuxiliaryCfg,libList,index);
        //奖励C
        lib = handAwardC(lib,freeAwardRealData,iconId,count,rotateState,specialAuxiliaryCfg,libList,index);
        return lib;
    }

    /**
     * 处理奖励A
     * @param lib
     * @param freeAwardRealData
     * @param iconId
     * @param count
     * @param rotateState
     * @param specialAuxiliaryCfg
     * @param libList
     * @param index
     * @return
     * @throws Exception
     */
    protected T handAwardA(T lib,FreeAwardRealData freeAwardRealData,int iconId,int count,int rotateState,
                           SpecialAuxiliaryCfg specialAuxiliaryCfg,
                           List<T> libList,
                           int index) throws Exception {
        if(freeAwardRealData.getResultListA() == null || freeAwardRealData.getResultListA().isEmpty()){
            return lib;
        }
        outer:
        for (int[] daraArr : freeAwardRealData.getResultListA()) {
            AuxiliaryAwardType auxiliaryAwardType = AuxiliaryAwardType.getType(daraArr[0]);
            int data = daraArr[1];

            switch (auxiliaryAwardType) {
                case GOLD_PROP:
                    lib = triggerGoldProp(lib, count, rotateState,data,index);
                    break;
                case FREE_GAME_COUNT:
                    lib = triggerFreeGame(lib, freeAwardRealData.getResultListA(), specialAuxiliaryCfg, count, rotateState,libList);
                    break outer;
                case REWARD_MINI_GAME:
                    lib = triggerMiniGame(lib, iconId, data, rotateState,libList,index);
                    break;
                case SPIN_COUNT_AGAIN:
                    lib = triggerAgainGame(lib, freeAwardRealData.getResultListA(), specialAuxiliaryCfg, count, rotateState,libList);
                    break outer;
            }
        }
        return lib;
    }

    protected T handAwardC(T lib,FreeAwardRealData freeAwardRealData,int iconId,int count,int rotateState,
                           SpecialAuxiliaryCfg specialAuxiliaryCfg,
                           List<T> libList,
                           int index){
        return lib;
    }


    /**
     * 处理特殊游戏的随机奖励
     *
     * @param lib
     * @param cfg
     * @param count
     * @param rotateState
     * @return
     */
    public T handSpecialRandReward(T lib, SpecialAuxiliaryCfg cfg,int count, int rotateState,List<T> libList,int index) throws Exception {
        if (cfg.getFreeRandAward() == null || cfg.getFreeRandAward().isEmpty()) {
            return lib;
        }
        log.debug("找到随机 specialAuxiliary 的随机奖励 = {}", cfg.getId());

        PropAndAwardInfo<FreeRandAwardInfo> propAndAwardInfo = getPropAndAwardInfo(cfg.getId());
        if (propAndAwardInfo == null) {
            log.warn("没有找到该小游戏的 免费随机奖励配置 miniGameId = {}", cfg.getId());
            return lib;
        }

        if (cfg.getType() == SlotsConst.SpecialAuxiliary.TYPE_FREE_ROLL) {  //免费旋转
            List<Integer> idList = getRewardList(propAndAwardInfo,lib.getRollerMode());
            //处理奖励逻辑
            lib = handSpecialAuxiliaryAward(lib, idList, cfg, -1, count, rotateState,libList,index);
        }else if(cfg.getType() == SlotsConst.SpecialAuxiliary.TYPE_OPEN_BOX){  //开启宝箱

        }
        return lib;
    }

    /**
     * 获取奖励中的id
     * @param propAndAwardInfo
     * @param rollerMode
     * @return
     */
    public List<Integer> getRewardList(PropAndAwardInfo<FreeRandAwardInfo> propAndAwardInfo,int rollerMode) {
        if(propAndAwardInfo == null){
            return Collections.emptyList();
        }
        List<Integer> idList = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer,FreeRandAwardInfo>> en1 : propAndAwardInfo.getAwardMap2().entrySet()) {
            Map<Integer,FreeRandAwardInfo> tempMap = en1.getValue();
            for(Map.Entry<Integer,FreeRandAwardInfo> en2 : tempMap.entrySet()){
                FreeRandAwardInfo freeRandAwardInfo = en2.getValue();
                //检查模式id是否一致
                if (freeRandAwardInfo.getModelId() != rollerMode) {
                    continue;
                }
                idList.add(freeRandAwardInfo.getAwardId());
            }
        }
        return idList;
    }

    /**
     * 金币系数
     *
     * @param lib
     * @param count
     * @param rotateState
     * @return
     */
    protected T triggerGoldProp(T lib, int count, int rotateState,int awardData,int index) {
        return lib;
    }

    /**
     * 触发免费转
     *
     * @param lib
     * @param specialAuxiliaryCfg
     * @param count
     * @param rotateState
     * @return
     */
    protected T triggerFreeGame(T lib,List<int[]> specialAuxiliaryAwardDataList,
                                                   SpecialAuxiliaryCfg specialAuxiliaryCfg,
                                                   int count,
                                                   int rotateState,
                                                   List<T> libList) throws Exception {
        return lib;
    }

    protected A addAwardLineInfo(BaseLineCfg baseLineCfg,BaseElementRewardCfg rewardCfg,int sameCount,
                                 int baseIconId,List<Integer> lineList,int[] arr) {
        return null;
    }

    /**
     * 判断两个icon是否一样
     *
     * @param sameInfo
     * @param iconIdFront  前一个图标
     * @param iconIdBack  后一个图标
     * @return
     */
    protected SameInfo iconSame(SameInfo sameInfo, int iconIdFront, int iconIdBack) {
        //是不是普通图标
        boolean normal_Front = this.noralIconSet.contains(iconIdFront);
        boolean normal_Back = this.noralIconSet.contains(iconIdBack);

        //是不是wild
        boolean wild_Front = this.wildIconSet.contains(iconIdFront);
        boolean wild_Back = this.wildIconSet.contains(iconIdBack);

        if (wild_Front) {  //表示front是wild图标
            if (wild_Back) {  //均为wild，相同
                sameInfo.setSame(true);
//                log.debug("均为wild图标 iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
            } else {
                //如果2是普通图标
                if (normal_Back) {
                    if(sameInfo.getBaseIconId() > 0){
                        sameInfo.setSame(sameInfo.getBaseIconId() == iconIdBack);
//                        log.debug("front 为wild，back是普通图标a iconIdFront = {},iconIdBack = {},same = {}", iconIdFront, iconIdBack,sameInfo.isSame());
                    }else {
                        sameInfo.setSame(true);
                        sameInfo.setBaseIconId(iconIdBack);
//                        log.debug("front 为wild，back是普通图标b iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
                    }
                } else {
//                    log.debug("front为wild，back是非wild的特殊图标 iconIdFront = {},iconIdBack = {},same = false", iconIdFront, iconIdBack);
                }
            }
        } else if (normal_Front) {  //表示fornt是普通图标
            if (wild_Back) { //back是wild
                sameInfo.setSame(true);
                sameInfo.setBaseIconId(iconIdFront);
//                log.debug("front为普通，back是wild iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
            } else {
                //如果front是普通，back是非wild，则只有两者id相同
                if (iconIdFront == iconIdBack) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconIdFront);
//                    log.debug("均为普通图标 iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
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
        List<Integer> tmpRollerIdList = this.baseRollerModeCfg.getRollerMode().get(modeId);
        if (tmpRollerIdList == null || tmpRollerIdList.isEmpty()) {
            throw new IllegalArgumentException("未找到该游戏的滚轴id列表1,生成结果集失败 gameType = " + this.gameType + ",modeId = " + modeId);
        }

        //特殊滚轴id
        List<Integer> specialRollerIds = this.baseRollerModeCfg.getSpecialRollerId();
        List<Integer> rollerIdList = new ArrayList<>();
        for(int rid : tmpRollerIdList){
            if(specialRollerIds != null && specialRollerIds.contains(rid)){
                continue;
            }
            rollerIdList.add(rid);
        }

        if (rollerIdList.isEmpty()) {
            throw new IllegalArgumentException("未找到该游戏的滚轴id列表2,生成结果集失败 gameType = " + this.gameType + ",modeId = " + modeId);
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

    /**
     * 出现的元素种类数
     * @return
     */
    protected boolean checkIconTypes(int iconId,Map<Integer, Integer> iconShowCountMap){
        for(Map.Entry<Integer, Map<Integer, BaseLineFreeInfo>> en2 : this.baseLineFreeCfgMap.entrySet()){
            Map<Integer, BaseLineFreeInfo> freeInfoMap = en2.getValue();
            for(Map.Entry<Integer, BaseLineFreeInfo> en3 : freeInfoMap.entrySet()){
                BaseLineFreeInfo baseLineFreeInfo = en3.getValue();
                //是否等于主元素
                if(iconId != baseLineFreeInfo.getMainElementId()){
                    continue;
                }
                int types = 0;
                for(List<Integer> tmpList : baseLineFreeInfo.getElementGroupList()){
                    for(int tmpIconId : tmpList){
                        if(iconShowCountMap.containsKey(tmpIconId)){
                            types++;
                        }
                    }
                }

                if(types >= baseLineFreeInfo.getMinIconTypeMin()){
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    public void calTimes(T lib) throws Exception{

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
    protected void baseLineFreeConfig() {
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

                tempMap.put(baseLineFreeInfo.getMainElementId(), baseLineFreeInfo);
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

            //巨型块大小
            //允许去替换的格子id -> 元素id
            Map<Integer,Integer> girdMap = new HashMap<>();
            if(cfg.getBigBlockSize() != null && !cfg.getBigBlockSize().isEmpty()){
                specialPropInfo = new PropInfo();
                propInfo = new PropInfo();

                specialBegin = 0;
                specialEnd = 0;

                begin = 0;
                end = 0;

                Map<Integer, List<Integer>> iconColumMap = new HashMap<>();

                //巨型块大小
                for (List<String> list : cfg.getBigBlockSize()) {
                    int iconId = Integer.parseInt(list.get(0));
                    String[] arr = list.get(1).split("-");
                    int prop = Integer.parseInt(list.get(2));
                    for (String str : arr) {
                        //列
                        int columnId = Integer.parseInt(str);
                        int index = (columnId - 1) * this.baseInitCfg.getRows() + 1;
                        //计算这一列所有的格子id
                        for (int i = 0; i < this.baseInitCfg.getRows(); i++) {
                            girdMap.put(index + i, iconId);
                        }

                        iconColumMap.computeIfAbsent(iconId, k -> new ArrayList<>()).add(columnId);
                    }

                    begin = end;
                    specialBegin = specialEnd;
                    if(config.getSpecialIconId() == iconId){
                        specialEnd += prop;
                        specialPropInfo.addProp(iconId, specialBegin, specialEnd);
                    }else {
                        end += prop;
                        propInfo.addProp(iconId,begin,end);
                    }

                }

                propInfo.setSum(end);
                specialPropInfo.setSum(end);
                config.setBigBlockSizePropInfo(propInfo);
                config.setSpecialBigBlockSizePropInfo(specialPropInfo);
                config.setIconColumMap(iconColumMap);
            }


            //影响格子
            if(cfg.getAffectGird() != null && !cfg.getAffectGird().isEmpty()){
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
            }

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
     * 检查baseLineFree配置的出现的特殊元素，  线路玩法 -> 主元素id -> 出现总次数
     * @param iconShowCountMap
     * @return
     */
    protected Map<Integer, Map<Integer, Integer>> baseLineFreeShowId(Map<Integer, Integer> iconShowCountMap){
        Map<Integer, Map<Integer, Integer>> showIdMap = new HashMap<>();
        for (Map.Entry<Integer, Map<Integer, BaseLineFreeInfo>> en : this.baseLineFreeCfgMap.entrySet()) {
            //主元素id -> 出现总次数
            Map<Integer, BaseLineFreeInfo> freeInfoMap = en.getValue();
            for (Map.Entry<Integer, BaseLineFreeInfo> en2 : freeInfoMap.entrySet()) {
                BaseLineFreeInfo freeInfo = en2.getValue();
                Map<Integer, Integer> tempMap = showIdMap.computeIfAbsent(freeInfo.getPlayType(), k -> new HashMap<>());

                group:
                for (List<Integer> groupList : freeInfo.getElementGroupList()) {
                    for (int specialIcon : groupList) {
                        Integer count = iconShowCountMap.get(specialIcon);
                        if (count == null) {
                            tempMap.remove(freeInfo.getMainElementId());
                            continue group;
                        }
                        tempMap.merge(freeInfo.getMainElementId(), count, Integer::sum);
                    }
                }
            }

        }
        return showIdMap;
    }

    /**
     * 格子修改
     *
     * @param spinType
     * @param arr
     */
    public int[] girdUpdate(int modelId, int spinType, int rotatestate, int[] arr) {
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

            //修改出火车，要特殊写
            if(girdUpdateConfig.getId() == SlotsConst.SpecialGird.ID_TRAIN_UPDATE){
                PropInfo clonePropInfo = girdUpdateConfig.getOtherIconAffectGirdPropInfo().clone();
                PropInfo otherIconIdPropInfo = girdUpdateConfig.getOtherIconPropInfo().clone();
                //记录每个key出现的次数
                Map<Integer, Integer> existMap = new HashMap<>();
                //每个元素在每一列出现的次数
                Map<Integer,Set<Integer>> iconColumShowMap = new HashMap<>();
                for(int i=0;i<randCount;i++){
                    //获取一个其他特殊图标需要替换的格子id
                    Integer otherIconGirdId = clonePropInfo.getRandKey();
                    int maxShowLimit = clonePropInfo.getMaxShowLimit(otherIconGirdId);

                    Integer otherIconId = otherIconIdPropInfo.getRandKey();
                    if(otherIconGirdId > 0 && otherIconId > 0){
                        //将格子id，转化成所在列
                        int columId = otherIconGirdId / this.baseInitCfg.getRows();
                        if((otherIconGirdId % this.baseInitCfg.getRows()) > 0){
                            columId ++;
                        }

                        boolean exist = false;
                        Set<Integer> tempSet = iconColumShowMap.get(otherIconId);
                        if(tempSet != null){
                            exist = tempSet.contains(columId);
                        }

                        if(exist){
                            i--;
                            continue;
                        }

                        int afterCount = existMap.merge(otherIconGirdId, 1, Integer::sum);
                        if (afterCount >= maxShowLimit) {
                            //达到最大次数后，要去除，然后重新计算权重随机范围
                            clonePropInfo.removeKeyAndRecalculate(otherIconGirdId);
                        }

                        int oldIconId = arr[otherIconGirdId];
                        arr[otherIconGirdId] = otherIconId;
                        iconColumShowMap.computeIfAbsent(otherIconId, k -> new HashSet<>()).add(columId);
                        if(iconColumShowMap.get(otherIconId).size() >= this.baseInitCfg.getRows()){
                            otherIconIdPropInfo.removeKeyAndRecalculate(otherIconId);
                        }
                        log.debug("修改格子 girdId = {},oldIconId = {},newIconId = {}", otherIconGirdId,oldIconId, otherIconId);
                    }
                }
            }else {
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
                    if(otherIconGirdId > 0 && otherIconId != null && otherIconId > 0){
                        int oldIconId = arr[otherIconGirdId];
                        arr[otherIconGirdId] = otherIconId;
                        log.debug("修改格子 girdId = {},oldIconId = {},newIconId = {}", otherIconGirdId,oldIconId, otherIconId);
                    }
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

    public BaseInitCfg getBaseInitCfg() {
        return baseInitCfg;
    }

    public PropAndAwardInfo<FreeRandAwardInfo> getPropAndAwardInfo(int auxiliaryId){
        return this.specialAuxiliaryRandAwardPropMap.get(auxiliaryId);
    }

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(BaseInitCfg.EXCEL_NAME, this::baseInitCfg)
            .addChangeSampleFileObserveWithCallBack(BaseRollerModeCfg.EXCEL_NAME, this::baseRollerModeCfg)
            .addChangeSampleFileObserveWithCallBack(BaseElementCfg.EXCEL_NAME, this::baseElementConfig)
            .addChangeSampleFileObserveWithCallBack(BaseRollerCfg.EXCEL_NAME, this::baseRollerConfig)
            .addChangeSampleFileObserveWithCallBack(BaseLineCfg.EXCEL_NAME, this::baseLineConfig)
            .addChangeSampleFileObserveWithCallBack(BaseLineFreeCfg.EXCEL_NAME, this::baseLineFreeConfig)
            .addChangeSampleFileObserveWithCallBack(BaseElementRewardCfg.EXCEL_NAME, this::baseElementRewardConfig)
            .addChangeSampleFileObserveWithCallBack(SpecialAuxiliaryCfg.EXCEL_NAME, this::specialAuxiliaryCconfig)
            .addChangeSampleFileObserveWithCallBack(SpecialAuxiliaryAwardCfg.EXCEL_NAME, this::specialAuxiliaryAwardCconfig)
            .addChangeSampleFileObserveWithCallBack(SpecialGirdCfg.EXCEL_NAME, this::specialGirdConfig);
    }
}
