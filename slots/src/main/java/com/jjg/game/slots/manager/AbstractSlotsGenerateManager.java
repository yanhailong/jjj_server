package com.jjg.game.slots.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;

import com.jjg.game.slots.utils.SlotsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.*;

/**
 * 结果集生成器
 *
 * @author 11
 * @date 2025/7/2 13:54
 */
public class AbstractSlotsGenerateManager<A extends AwardLineInfo, T extends SlotsResultLib<A>> implements ConfigExcelChangeListener {
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected Class<T> resultLibClazz;
    //游戏类型
    protected int gameType;

    //type -> iconSet
    protected Map<Integer, Set<Integer>> iconsMap = null;
    //图标替换
    protected Map<Integer, Integer> replaceIconMap = null;
    //rollerGroup -> column -> cfg
    protected Map<Integer, Map<Integer, BaseRollerCfg>> baseRollerCfgMap = null;
    //lineId -> cfg
    protected Map<Integer, BaseLineCfg> baseLineCfgMap = null;
    //lineId -> 主元素id - > cfg
    protected Map<Integer, Map<Integer, BaseLineFreeInfo>> baseLineFreeCfgMap = null;
    //普通图标 lineType -> sid -> cfg
    protected Map<Integer, Map<Integer, BaseElementRewardCfg>> baseElementRewardCfgMap = null;

    //特殊模式配置表
    protected Map<Integer, SpecialModeCfg> specialModeCfgMap = null;
    //特殊模式配置表,修改图案策略组权重信息  type -> propInfo
    protected Map<Integer, PropInfo> specialModeGroupGirdPropMap = null;
    //小游戏相关的权重信息
    protected Map<Integer, SpecialAuxiliaryPropConfig> specialAuxiliaryPropConfigMap = null;
    //格子修改相关的权重信息
    protected Map<Integer, GirdUpdatePropConfig> specialGirdCfgMap = null;

    //specialResultLib表的一些权重等等信息
    protected SpecialResultLibCacheData specialResultLibCacheData = null;

    public AbstractSlotsGenerateManager(Class<T> resultLibClazz) {
        this.resultLibClazz = resultLibClazz;
    }

    public void init(int gameType) {
        this.gameType = gameType;
        initConfig();
    }

    public void initConfig() {
        baseRollerCfg();
        baseElementConfig();
        baseElementRewardConfig();
        baseLineConfig();

        specialModeConfig();
        specialAuxiliaryConfig();
        specialGirdConfig();
        specialPlayConfig();
        specialResultLibConfig();
    }

    protected T createResultLib() throws Exception {
        Constructor<T> constructor = this.resultLibClazz.getConstructor();
        return constructor.newInstance();
    }

    /**
     * 生成一个结果
     *
     * @param libType
     * @return
     */
    public T generateOne(int libType) throws Exception {
        //获取模式配置
        SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(libType);
        if (specialModeCfg == null) {
            log.warn("生成图标时，specialModeCfg 配置为空 gameType = {},libType = {}", this.gameType, libType);
            return null;
        }

        //创建结果库对象
        T lib = createResultLib();
        lib.setId(RandomUtils.getUUid());
        lib.setRollerMode(specialModeCfg.getRollerMode());
        lib.addLibType(libType);

        //生成所有的图标
        int[] arr = generateAllIcons(specialModeCfg.getRollerMode(), specialModeCfg.getCols(), specialModeCfg.getRows());
        if (arr == null) {
            return null;
        }

        log.debug("生成图标 arr = {}", Arrays.toString(arr));

        //修改格子策略组
        PropInfo propInfo = this.specialModeGroupGirdPropMap.get(libType);
        if (propInfo != null) {
            Integer randKey = propInfo.getRandKey();
            if (randKey == null) {
                randKey = 0;
            }
            SpecialGirdInfo specialGirdInfo = girdUpdate(randKey, arr);
            if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                lib.addSpecialGirdInfo(specialGirdInfo);
            }
        }

        //修改格子
        if (specialModeCfg.getSpecialGirdID() != null && !specialModeCfg.getSpecialGirdID().isEmpty()) {
            for (int specialGirdCfgId : specialModeCfg.getSpecialGirdID()) {
                SpecialGirdInfo specialGirdInfo = girdUpdate(specialGirdCfgId, arr);
                if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                    lib.addSpecialGirdInfo(specialGirdInfo);
                }
            }
        }

        //判断中奖，返回
        return checkAward(arr, lib);
    }

    /**
     * 生成一个免费结果
     *
     * @param specialAuxiliaryCfg
     * @return
     */
    public T generateFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            T lib = createResultLib();
            lib.setRollerMode(specialModeCfg.getRollerMode());

            //生成所有的图标
            int[] arr = generateAllIcons(specialModeCfg.getRollerMode(), specialModeCfg.getCols(), specialModeCfg.getRows());
            if (arr == null) {
                return null;
            }

            log.debug("生成免费游戏图标 arr = {}", Arrays.toString(arr));

            //修改格子策略组
            if (specialGroupGirdID > 0) {
                SpecialGirdInfo specialGirdInfo = girdUpdate(specialGroupGirdID, arr);
                if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                    lib.addSpecialGirdInfo(specialGirdInfo);
                }
            }

            //修改格子
            if (specialAuxiliaryCfg.getSpecialGirdID() != null && !specialAuxiliaryCfg.getSpecialGirdID().isEmpty()) {
                for (int specialGirdCfgId : specialAuxiliaryCfg.getSpecialGirdID()) {
                    SpecialGirdInfo specialGirdInfo = girdUpdate(specialGirdCfgId, arr);
                    if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                        lib.addSpecialGirdInfo(specialGirdInfo);
                    }
                }
            }

            //判断中奖，返回
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
    public T checkAward(int[] arr, T lib) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查连线
        List<A> awardLineInfoList = winLines(lib, SlotsConst.BaseElementReward.LINE_TYPE_NORMAL);
        lib.setAwardLineInfoList(awardLineInfoList);

        //检查指定图案
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = assignPattern(lib);
        lib.addSpecialAuxiliaryInfo(specialAuxiliaryInfoList);

        //检查满线图案_x连
        List<A> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //检查满线图案_数量
        List<A> fullLineCountInfoList = fullLineCount(lib);
        lib.addAllAwardLineInfo(fullLineCountInfoList);

        //计算倍数
        calTimes(lib);
        return lib;
    }

    /**
     * 按照配置生成所有的图标元素
     *
     * @return
     */
    public int[] generateAllIcons(int rollerMode, int cols, int rows) {
        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.get(rollerMode);
        if (rollerCfgMap == null) {
            log.warn("生成图标时，rollerCfgMap 配置为空 rollerMode={}", rollerMode);
            return null;
        }

        int[] arr = new int[cols * rows + 1];

        for (Map.Entry<Integer, BaseRollerCfg> en : rollerCfgMap.entrySet()) {
            BaseRollerCfg cfg = en.getValue();
            if (cfg.getAxleCountScope() == null || cfg.getAxleCountScope().isEmpty()) {
                log.warn("没有该滚轴的范围,生成结果集失败 gameType = {},rollerCfgId = {}", this.gameType, cfg.getId());
                return null;
            }

            int iconIndex = (cfg.getColumn() - 1) * rows + 1;

            //区间范围的第一个下标
            int first = cfg.getAxleCountScope().get(0) - 1;
            //区间范围的最后一个下标
            int last = cfg.getAxleCountScope().get(1) - 1;

            //随机生成一个起始位置
            int scopeIndex = RandomUtils.randomMinMax(first, last);
            for (int i = 0; i < rows; i++) {
                //首尾相连
                if (scopeIndex > last) {
                    scopeIndex = first;
                }

                int elementId = cfg.getElements().get(scopeIndex);
                arr[iconIndex] = elementId;
                iconIndex++;
                scopeIndex++;
            }
        }
        return arr;
    }

    /**
     * 根据滚轴id，生成一列的图标
     *
     * @param rollerMode
     * @param rows
     * @return
     */
    public int[] generateColumnIcons(int rollerMode, int rows, int colId) {
        Map<Integer, BaseRollerCfg> rollerCfgMap = this.baseRollerCfgMap.get(rollerMode);
        if (rollerCfgMap == null) {
            log.warn("生成1列图标时，rollerCfgMap 配置为空 rollerMode={}", rollerMode);
            return null;
        }

        BaseRollerCfg cfg = rollerCfgMap.get(colId);
        if (cfg == null) {
            log.warn("生成1列图标时，baseRollerCfg 配置为空 rollerMode={},colId = {}", rollerMode, colId);
            return null;
        }

        if (cfg.getAxleCountScope() == null || cfg.getAxleCountScope().isEmpty()) {
            log.warn("没有该滚轴的范围,生成1列图标失败 gameType = {},rollerCfgId = {}", this.gameType, cfg.getId());
            return null;
        }

        int[] arr = new int[rows];

        //区间范围的第一个下标
        int first = cfg.getAxleCountScope().get(0) - 1;
        //区间范围的最后一个下标
        int last = cfg.getAxleCountScope().get(1) - 1;

        //随机生成一个起始位置
        int scopeIndex = RandomUtils.randomMinMax(first, last);

        for (int i = 0; i < rows; i++) {
            //首尾相连
            if (scopeIndex > last) {
                scopeIndex = first;
            }
            int elementId = cfg.getElements().get(scopeIndex);
            arr[i] = elementId;
            scopeIndex++;
        }
        return arr;
    }

    /**
     * 检查中奖线
     *
     * @return
     */
    public List<A> winLines(T lib, int lineType) {
        return winLines(lib.getIconArr(), lineType);
    }

    /**
     * 检查中奖线
     *
     * @return
     */
    public List<A> winLines(int[] arr, int lineType) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        if (baseInitCfg.getLineType() != SlotsConst.BaseInit.NEED_BASE_LINE) {
            return null;
        }
        log.debug("开始检查中奖线信息 lineType = {}", lineType);
        List<A> awardLineInfoList = new ArrayList<>();

        for (Map.Entry<Integer, BaseLineCfg> en : this.baseLineCfgMap.entrySet()) {
            BaseLineCfg cfg = en.getValue();
            List<Integer> lineList = cfg.getPosLocation();

            SameInfo sameInfo = new SameInfo();

            int last = lineList.size() - 1;

            for (int direction : cfg.getDirection()) {
                //标记是否连线
                int sameCount = 0;
                for (int i = 0; i < last; i++) {
                    int index1;
                    int index2;
                    //检查方向算法
                    if (direction == SlotsConst.BaseLine.DIRECTION_LEFT) {
                        index1 = lineList.get(i);
                        index2 = lineList.get(i + 1);
                    } else if (direction == SlotsConst.BaseLine.DIRECTION_RIGHT) {
                        index1 = lineList.get(last - i);
                        index2 = lineList.get(last - i - 1);
                    } else {
                        index1 = lineList.get(i);
                        index2 = lineList.get(i + 1);
                    }

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
                    log.debug("sameInfo = {}", JSON.toJSONString(sameInfo));
                    Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_NORMAL);
                    for (Map.Entry<Integer, BaseElementRewardCfg> rewardEn : normalRewardCfgMap.entrySet()) {
                        BaseElementRewardCfg rewardCfg = rewardEn.getValue();
                        //线类型
                        if (lineType != SlotsConst.BaseElementReward.LINE_TYPE_ALL && rewardCfg.getLineType() != lineType) {
                            continue;
                        }

                        //匹配连线的元素id和个数
                        if (!rewardCfg.getElementId().contains(sameInfo.getBaseIconId()) || sameCount != rewardCfg.getRewardNum()) {
                            continue;
                        }

                        A info = addAwardLineInfo(cfg, rewardCfg, sameCount, sameInfo.getBaseIconId(), lineList, arr);
                        awardLineInfoList.add(info);
                        break;
                    }
                }
            }

        }
        return awardLineInfoList;
    }

    /**
     * 检查指定图案
     */
    protected List<SpecialAuxiliaryInfo> assignPattern(T lib) {
        return assignPattern(lib.getLibTypeSet(), lib.getIconArr(), lib.getSpecialGirdInfoList());
    }

    /**
     * 检查指定图案
     */
    protected List<SpecialAuxiliaryInfo> assignPattern(Set<Integer> libTypeSet, int[] arr, List<SpecialGirdInfo> specialGirdInfoList) {
        //获取指定图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_ASSIGN);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(arr);

        log.debug("检查指定图案");

        //小游戏
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();
        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            //必须出现的图案
            Integer mustIconCount = showCountMap.get(cfg.getRewardNum());
            //条件图案总数量
            int elementAllCount = 0;
            for (int iconId : cfg.getElementId()) {
                Integer count = showCountMap.get(iconId);
                if (count != null) {
                    elementAllCount++;
                }
            }

            //检查条件是否都满足
            if (mustIconCount == null || mustIconCount < 1 || elementAllCount < 1) {
                continue;
            }

            //是否触发小游戏
            if (cfg.getFeatureTriggerId() == null || cfg.getFeatureTriggerId().isEmpty()) {
                continue;
            }

            for (int i = 0; i < elementAllCount; i++) {
                cfg.getFeatureTriggerId().forEach(miniGameId -> {
                    libTypeSet.forEach(libType -> {
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, arr, miniGameId, specialGirdInfoList);
                        if (specialAuxiliaryInfo != null) {
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
                    });
                });
            }
        }
        return specialAuxiliaryInfoList;
    }

    /**
     * 检查满线图案
     */
    public List<A> fullLine(T lib) {
        return fullLine(lib.getIconArr());
    }

    /**
     * 检查满线图案
     *
     * @param arr
     */
    public List<A> fullLine(int[] arr) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> fullRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_FULL);
        if (fullRewardCfgMap == null || fullRewardCfgMap.isEmpty()) {
            return null;
        }
        log.debug("检查满线图案");

        List<A> list = new ArrayList<>();
        for (Map.Entry<Integer, BaseElementRewardCfg> en : fullRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            Map<Integer, Set<Integer>> sameMap = fullColSame(arr, cfg);
            if (!sameMap.isEmpty()) {
                list.add(addFullLineAwardInfo(sameMap, cfg));
                log.debug("满线中奖 sameMap = {},times = {}", sameMap, cfg.getBet());
            }
        }
        return list;
    }

    /**
     * 全局分散
     */
    protected List<SpecialAuxiliaryInfo> overallDisperse(T lib) {
        return overallDisperse(lib.getLibTypeSet(), lib.getIconArr(), lib.getSpecialGirdInfoList());
    }

    /**
     * 全局分散
     */
    protected List<SpecialAuxiliaryInfo> overallDisperse(Set<Integer> libTypeSet, int[] arr, List<SpecialGirdInfo> specialGirdInfoList) {
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(arr);

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
            if (cfg.getFeatureTriggerId() == null || cfg.getFeatureTriggerId().isEmpty()) {
                continue;
            }

            cfg.getFeatureTriggerId().forEach(miniGameId -> {
                libTypeSet.forEach(libType -> {
                    SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(libType, arr, miniGameId, specialGirdInfoList);
                    if (specialAuxiliaryInfo != null) {
                        specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                    }
                });

            });
        }
        return specialAuxiliaryInfoList;
    }

    /**
     * 检查满线图案_数量
     *
     * @param lib
     * @return
     */
    protected List<A> fullLineCount(T lib) {
        return fullLineCount(lib.getIconArr());
    }

    /**
     * 检查满线图案_数量
     *
     * @param arr
     * @return
     */
    protected List<A> fullLineCount(int[] arr) {
        //获取满线图案_数量的配置
        Map<Integer, BaseElementRewardCfg> fullLineCountCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_FULL_COUNT);
        if (fullLineCountCfgMap == null || fullLineCountCfgMap.isEmpty()) {
            return null;
        }

        List<A> awardInfoList = new ArrayList<>();

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
            int icon = arr[i];
            firstColIcons.merge(icon, 1, Integer::sum);
        }

        //然后从第二列开始检查，是否出现了第一列的图标
        for (Map.Entry<Integer, Integer> en : firstColIcons.entrySet()) {
            //第一列的图标和数量
            int icon = en.getKey();
            int count = en.getValue();

            int allCount = count;

            boolean firstNormal = normalIconSet.contains(icon);

            //从第2列开始，检查每一个图标
            for (int col = 2; col <= baseInitCfg.getCols(); col++) {
                int beginIndex = (col - 1) * baseInitCfg.getRows() + 1;

                boolean flag = false;

                for (int i = 0; i < baseInitCfg.getRows(); i++) {
                    int index = beginIndex + i;
                    int tmpIcon = arr[index];

                    boolean wild = false;
                    if (wildIconSet != null && wildIconSet.contains(tmpIcon)) {
                        wild = true;
                    }

                    if (wild && firstNormal) {
                        allCount++;
                        flag = true;
                    } else if (icon == tmpIcon) {
                        allCount++;
                        flag = true;
                    }
                }

                //这一列遍历结束后，检查图标个数是否增长
                if (!flag) { //如果没有增长，表示这一列中没有出现第一列的图标，所以中断
                    break;
                }
            }

            //找到baseElementReward表中关于该图标的配置
            for (Map.Entry<Integer, BaseElementRewardCfg> rewardCfgEn : fullLineCountCfgMap.entrySet()) {
                BaseElementRewardCfg cfg = rewardCfgEn.getValue();
                //配置表中的图标是否包含该图标
                if (!cfg.getElementId().contains(icon)) {
                    continue;
                }

                if (allCount < cfg.getRewardNum()) {
                    continue;
                }

                A rewardInfo = addFullLineCountAwardInfo(icon, count, cfg);
                awardInfoList.add(rewardInfo);
            }
        }

        return awardInfoList;
    }

    protected A addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        return null;
    }

    protected A addFullLineAwardInfo(Map<Integer, Set<Integer>> map, BaseElementRewardCfg cfg) {
        return null;
    }

    protected A addFullLineCountAwardInfo(int icon, int count, BaseElementRewardCfg cfg) {
        return null;
    }


    /**
     * 触发小游戏
     *
     * @param miniGameId
     * @return
     */
    public SpecialAuxiliaryInfo triggerMiniGame(int specialModeType, int[] arr, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
        log.debug("触发小游戏 miniGameId = {}", miniGameId);
        //根据小游戏id去找相关配置
        SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
        if (specialAuxiliaryCfg == null) {
            log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
        if (specialAuxiliaryPropConfig == null) {
            log.warn("未找到该小游戏小关的权重信息配置 miniGameId = {}", miniGameId);
            return null;
        }

        SpecialAuxiliaryInfo specialAuxiliaryInfo = new SpecialAuxiliaryInfo();
        specialAuxiliaryInfo.setCfgId(miniGameId);

        //检查免费旋转
        triggerFree(specialModeType, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo);
        //检查是否有额外奖励
        triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
        return specialAuxiliaryInfo;
    }

    /**
     * 检查免费旋转
     *
     * @param specialModeType
     * @param specialAuxiliaryCfg
     * @param specialAuxiliaryPropConfig
     * @param specialAuxiliaryInfo
     */
    protected void triggerFree(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
        if (specialAuxiliaryPropConfig.getTriggerCountPropInfo() == null) {
            return;
        }

        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        for (int i = 0; i < freeCount; i++) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            T t = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(t));
            log.debug("--------------{}------------", i);
        }
    }

    /**
     * 检查是否有额外奖励
     *
     * @param specialAuxiliaryCfg
     * @param specialAuxiliaryPropConfig
     * @param specialAuxiliaryInfo
     */
    protected void triggerAuxiliaryExtra(int[] arr, SpecialAuxiliaryCfg specialAuxiliaryCfg, SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo, List<SpecialGirdInfo> specialGirdInfoList) {
        if (specialAuxiliaryPropConfig.getRandCountPropInfo() == null) {
            return;
        }

        Integer randCount = specialAuxiliaryPropConfig.getRandCountPropInfo().getRandKey();
        if (randCount == null || randCount < 1) {
            return;
        }

        //找到修改格子后的配置
        SpecialGirdInfo sgInfo = null;
        if (specialGirdInfoList != null && !specialGirdInfoList.isEmpty()) {
            sgInfo = specialGirdInfoList.stream().filter(info -> info.getCfgId() == specialAuxiliaryCfg.getId()).findFirst().orElse(null);
        }

        SpecialAuxiliaryAwardInfo specialAuxiliaryAwardInfo = new SpecialAuxiliaryAwardInfo();
        specialAuxiliaryAwardInfo.setRandCount(randCount);

        //开始触发额外奖励
        for (int i = 0; i < randCount; i++) {
            //获取奖励A
            if (specialAuxiliaryCfg.getAwardTypeA() != null && !specialAuxiliaryCfg.getAwardTypeA().isEmpty() && sgInfo != null && sgInfo.getValueMap() != null && !sgInfo.getValueMap().isEmpty()) {
                //指定图标
                int icon = specialAuxiliaryCfg.getAwardTypeA().get(0);
                //指定图标的金额万分比
                int prop = specialAuxiliaryCfg.getAwardTypeA().get(1);

                for (Map.Entry<Integer, Integer> en : sgInfo.getValueMap().entrySet()) {
                    int girdId = en.getKey();
                    //检查是不是配置的图标
                    if (arr[girdId] == icon) {
                        int newValue = SlotsUtil.calProp(prop, en.getValue());
                        sgInfo.getValueMap().put(girdId, newValue);
                    }
                }
            }

            //获取奖励c
            if (specialAuxiliaryPropConfig.getAwardTypeCPropInfo() != null) {
                Integer times = specialAuxiliaryPropConfig.getAwardTypeCPropInfo().getRandKey();
                if (times != null) {
                    specialAuxiliaryAwardInfo.addAwardC(times);
                }
            }
        }
        specialAuxiliaryInfo.addAwardInfo(specialAuxiliaryAwardInfo);
    }

    /**
     * 判断两个icon是否一样
     *
     * @param sameInfo
     * @param iconIdFront 前一个图标
     * @param iconIdBack  后一个图标
     * @return
     */
    protected SameInfo iconSame(SameInfo sameInfo, int iconIdFront, int iconIdBack) {
        Set<Integer> noralIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_NORMAL);
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        //是不是普通图标
        boolean normal_Front = noralIconSet.contains(iconIdFront);
        boolean normal_Back = noralIconSet.contains(iconIdBack);

        //是不是wild
        boolean wild_Front = wildIconSet.contains(iconIdFront);
        boolean wild_Back = wildIconSet.contains(iconIdBack);

        if (wild_Front) {  //表示front是wild图标
            if (wild_Back) {  //均为wild，相同
                sameInfo.setSame(true);
                log.debug("均为wild图标 iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
            } else {
                //如果2是普通图标
                if (normal_Back) {
                    if (sameInfo.getBaseIconId() > 0) {
                        sameInfo.setSame(sameInfo.getBaseIconId() == iconIdBack);
                        log.debug("front 为wild，back是普通图标a iconIdFront = {},iconIdBack = {},same = {}", iconIdFront, iconIdBack, sameInfo.isSame());
                    } else {
                        sameInfo.setSame(true);
                        sameInfo.setBaseIconId(iconIdBack);
                        log.debug("front 为wild，back是普通图标b iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
                    }
                } else {
                    log.debug("front为wild，back是非wild的特殊图标 iconIdFront = {},iconIdBack = {},same = false", iconIdFront, iconIdBack);
                }
            }
        } else if (normal_Front) {  //表示fornt是普通图标
            if (wild_Back) { //back是wild
                sameInfo.setSame(true);
                sameInfo.setBaseIconId(iconIdFront);
                log.debug("front为普通，back是wild iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
            } else {
                //如果front是普通，back是非wild，则只有两者id相同
                if (iconIdFront == iconIdBack) {
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconIdFront);
                    log.debug("均为普通图标 iconIdFront = {},iconIdBack = {},same = true", iconIdFront, iconIdBack);
                }
            }
        } else {  //表示1是非wild的特殊图标,则无论2为什么，都不可能相同

        }
        return sameInfo;
    }

    /**
     * 满线图案中，比较两列中相同的图标
     */
    protected Map<Integer, Set<Integer>> fullColSame(int[] arr, BaseElementRewardCfg cfg) {
        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        List<Integer> iconsList = cfg.getElementId();

        // 记录所有连续列段
        List<List<Integer>> allSequences = new ArrayList<>();
        List<Integer> currentSequence = new ArrayList<>();
        Map<Integer, Set<Integer>> sameMap = new HashMap<>();

        //遍历所有图标
        for (int colIndex = 1; colIndex <= baseInitCfg.getCols(); colIndex++) {
            boolean hasValidElement = false;

            int beginIndex = (colIndex - 1) * baseInitCfg.getRows();
            for (int rowIndex = 1; rowIndex <= baseInitCfg.getRows(); rowIndex++) {
                int index = beginIndex + rowIndex;

                //检查是否出现了配置中的图标
                int iconId = arr[index];
                if ((wildIconSet != null && wildIconSet.contains(iconId)) || iconsList.contains(iconId)) {
                    sameMap.computeIfAbsent(colIndex, k -> new HashSet<>()).add(index);
                    hasValidElement = true;
                }
            }

            if (hasValidElement) {
                currentSequence.add(colIndex);
            } else {
                if (!currentSequence.isEmpty()) {
                    allSequences.add(new ArrayList<>(currentSequence));
                    currentSequence.clear();
                }
            }
        }

        // 添加最后一段连续列
        if (!currentSequence.isEmpty()) {
            allSequences.add(new ArrayList<>(currentSequence));
        }

        // 找出最长的连续列段
        List<Integer> longestSequence = allSequences.stream().max(Comparator.comparingInt(List::size)).orElse(Collections.emptyList());

        // 只保留最长连续列的记录
        sameMap.entrySet().removeIf(en -> !longestSequence.contains(en.getKey()));
        if (sameMap.size() == cfg.getRewardNum()) {
            return sameMap;
        }
        return Collections.emptyMap();
    }

    public void calTimes(T lib) throws Exception {

    }


    /********************************************配置相关***********************************************************/

    protected void baseRollerCfg() {
        Map<Integer, Map<Integer, BaseRollerCfg>> tmpBaseRollerCfgMap = new HashMap<>();

        GameDataManager.getBaseRollerCfgMap().forEach((k, v) -> {
            if (v.getGameType() == this.gameType) {
                tmpBaseRollerCfgMap.computeIfAbsent(v.getRollerGroup(), x -> new HashMap<>()).put(v.getColumn(), v);
            }
        });

        this.baseRollerCfgMap = tmpBaseRollerCfgMap;
    }

    /**
     * 元素相关
     */
    protected void baseElementConfig() {
        Map<Integer, Set<Integer>> tmpIconMap = new HashMap<>();
        Map<Integer, Integer> tmpReplaceIconMap = new HashMap<>();

        for (Map.Entry<Integer, BaseElementCfg> en : GameDataManager.getBaseElementCfgMap().entrySet()) {
            BaseElementCfg cfg = en.getValue();
            if (cfg.getGameId() != this.gameType) {
                continue;
            }
            tmpIconMap.computeIfAbsent(cfg.getType(), k -> new HashSet<>()).add(cfg.getElementId());
            if (cfg.getPostChangeElementId() > 0) {
                tmpReplaceIconMap.put(cfg.getElementId(), cfg.getPostChangeElementId());
            }
        }
        this.iconsMap = tmpIconMap;
        this.replaceIconMap = tmpReplaceIconMap;
    }

    /**
     * 中奖线相关
     */
    protected void baseLineConfig() {
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
            return;
        }
        this.baseLineCfgMap = tmpBaseLineCfgMap;
    }

    protected void baseElementRewardConfig() {
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
            return;
        }
        this.baseElementRewardCfgMap = tmpBaseElementRewardCfgMap;
    }

    /**
     * 特殊模式表
     */
    protected void specialModeConfig() {
        Map<Integer, SpecialModeCfg> tmpSpecialModeCfgMap = new HashMap<>();
        Map<Integer, PropInfo> tmpSpecialModeGroupGirdPropMap = new HashMap<>();

        GameDataManager.getSpecialModeCfgMap().forEach((k, v) -> {
            if (v.getGameType() == this.gameType) {
                tmpSpecialModeCfgMap.put(v.getType(), v);

                if (v.getSpecialGroupGirdID() != null && !v.getSpecialGroupGirdID().isEmpty()) {
                    tmpSpecialModeGroupGirdPropMap.put(v.getType(), SlotsUtil.converMapToPropInfo(v.getSpecialGroupGirdID()));
                }
            }
        });
        this.specialModeCfgMap = tmpSpecialModeCfgMap;
        this.specialModeGroupGirdPropMap = tmpSpecialModeGroupGirdPropMap;
    }


    /**
     * 小游戏
     */
    protected void specialAuxiliaryConfig() {
        Map<Integer, SpecialAuxiliaryPropConfig> tmpSpecialAuxiliaryPropConfigMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialAuxiliaryCfg> en : GameDataManager.getSpecialAuxiliaryCfgMap().entrySet()) {
            SpecialAuxiliaryCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            SpecialAuxiliaryPropConfig config = new SpecialAuxiliaryPropConfig();
            config.setId(cfg.getId());

            //免费旋转
            config.setTriggerCountPropInfo(SlotsUtil.converMapToPropInfo(cfg.getTriggerCount()));
            //随机次数
            config.setRandCountPropInfo(SlotsUtil.converMapToPropInfo(cfg.getRandCount()));
            //修改图案策略组
            config.setSpecialGroupGirdIDPropInfo(SlotsUtil.converMapToPropInfo(cfg.getSpecialGroupGirdID()));
            //奖励c
            config.setAwardTypeCPropInfo(SlotsUtil.converMapToPropInfo(cfg.getAwardTypeC()));

            tmpSpecialAuxiliaryPropConfigMap.put(config.getId(), config);
        }

        this.specialAuxiliaryPropConfigMap = tmpSpecialAuxiliaryPropConfigMap;
    }

    /**
     * 格子修改配置
     */
    protected void specialGirdConfig() {
        Map<Integer, GirdUpdatePropConfig> tmpSpecialGirdCfgMap = new HashMap<>();

        for (Map.Entry<Integer, SpecialGirdCfg> en : GameDataManager.getSpecialGirdCfgMap().entrySet()) {
            SpecialGirdCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            GirdUpdatePropConfig config = new GirdUpdatePropConfig();
            config.setId(cfg.getId());

            //需要出现的元素
            config.setShowIconPropInfo(SlotsUtil.converMapToPropInfo(cfg.getElement()));
            //影响格子
            config.setAffectGirdPropInfo(SlotsUtil.converMapToLimitPropInfo(cfg.getAffectGird()));
            //随机次数
            config.setRandCountPropInfo(SlotsUtil.converMapToPropInfo(cfg.getRandCount()));
            //成功后赋值
            config.setValuePropInfo(SlotsUtil.converMapToPropInfo(cfg.getValue()));


            tmpSpecialGirdCfgMap.put(cfg.getId(), config);
        }

        this.specialGirdCfgMap = tmpSpecialGirdCfgMap;
    }

    /**
     * 特殊玩法
     */
    protected void specialPlayConfig() {

    }

    /**
     * 结果库配置
     */
    protected void specialResultLibConfig() {
        List<SpecialResultLibCfg> cfgList = new ArrayList<>();
        for (Map.Entry<Integer, SpecialResultLibCfg> en : GameDataManager.getSpecialResultLibCfgMap().entrySet()) {
            SpecialResultLibCfg cfg = en.getValue();
            if (cfg.getGameType() != gameType) {
                continue;
            }
            cfgList.add(cfg);
        }

        this.specialResultLibCacheData = calSpecialResultLibCacheData(cfgList);
        log.info("计算分析缓存 specialResultLib 配置成功 gameType = {}", gameType);
    }

    /**
     * 计算分析specialResultLib表
     */
    public SpecialResultLibCacheData calSpecialResultLibCacheData(List<SpecialResultLibCfg> cfgList) {
        if (cfgList == null || cfgList.isEmpty()) {
            return null;
        }
        Map<Integer, SpecialResultLibCfg> tempLibCfgMap = new HashMap<>();
        Map<Integer, PropInfo> tempResultLibTypePropInfoMap = new HashMap<>();

        Map<Integer, Map<Integer, PropInfo>> tempResultLibSectionPropMap = new HashMap<>();

        boolean addSection = true;
        Map<Integer, Map<Integer, int[]>> tempResultLibSectionMap = new HashMap<>();

        int tmpDefaultRewardSectionIndex = -1;

        for (SpecialResultLibCfg cfg : cfgList) {
            tempLibCfgMap.put(cfg.getModelId(), cfg);

            //计算typeProp
            if (cfg.getTypeProp() != null && !cfg.getTypeProp().isEmpty()) {
                PropInfo propInfo = new PropInfo();

                int begin = 0;
                int end = 0;
                for (Map.Entry<Integer, Integer> en2 : cfg.getTypeProp().entrySet()) {
                    begin = end;
                    end += en2.getValue();
                    propInfo.addProp(en2.getKey(), begin, end);
                }
                propInfo.setSum(end);
                tempResultLibTypePropInfoMap.put(cfg.getModelId(), propInfo);
            }

            //计算sectionProp
            if (cfg.getSectionProp() != null && !cfg.getSectionProp().isEmpty()) {
                Map<Integer, PropInfo> typeSectionPropMap = tempResultLibSectionPropMap.computeIfAbsent(cfg.getModelId(), k -> new HashMap<>());

                for (Map.Entry<Integer, List<String>> en2 : cfg.getSectionProp().entrySet()) {
                    PropInfo propInfo = new PropInfo();

                    int type = en2.getKey();

                    Map<Integer, int[]> sectionMap = null;
                    if (addSection) {
                        sectionMap = tempResultLibSectionMap.computeIfAbsent(type, k -> new HashMap<>());
                    }

                    List<String> propList = en2.getValue();

                    int begin = 0;
                    int end = 0;
                    for (int i = 0; i < propList.size(); i++) {

                        String prop = propList.get(i);
                        String[] arr = prop.split("-");
                        String[] arr2 = arr[0].split("&");

                        begin = end;
                        end += Integer.parseInt(arr[1]);
                        propInfo.addProp(i, begin, end);

                        //倍数区间
                        if (sectionMap != null) {
                            int[] tmpArr = new int[]{Integer.parseInt(arr2[0]), Integer.parseInt(arr2[1])};
                            sectionMap.put(i, tmpArr);
                            if (tmpArr[0] == 0) {
                                tmpDefaultRewardSectionIndex = i;
                            }
                        }

                    }
                    propInfo.setSum(end);

                    typeSectionPropMap.put(type, propInfo);
                }
                addSection = false;
            }
        }

        if (tempLibCfgMap.isEmpty() || tempResultLibTypePropInfoMap.isEmpty() || tempResultLibSectionPropMap.isEmpty() || tempResultLibSectionMap.isEmpty()) {
            throw new IllegalArgumentException("该游戏specialResultLib 为空,初始化失败 gameType = " + gameType);
        }

        if (tmpDefaultRewardSectionIndex < 0) {
            throw new IllegalArgumentException("该游戏specialResultLib 中没有配置0倍区间 gameType = " + gameType);
        }

        SpecialResultLibCacheData data = new SpecialResultLibCacheData();
        data.setDefaultRewardSectionIndex(tmpDefaultRewardSectionIndex);
        data.setResultLibMap(tempLibCfgMap);
        data.setResultLibTypePropInfoMap(tempResultLibTypePropInfoMap);
        data.setResultLibSectionPropMap(tempResultLibSectionPropMap);
        data.setResultLibSectionMap(tempResultLibSectionMap);
        return data;
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
            arr[girdId] = newIcon;

            //赋值
            if (girdUpdatePropConfig.getValuePropInfo() != null) {
                int value = girdUpdatePropConfig.getValuePropInfo().getRandKey();
                info.addValue(girdId, value);
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

    @Override
    public void changeSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(BaseInitCfg.EXCEL_NAME, this::baseRollerCfg).addChangeSampleFileObserveWithCallBack(BaseElementCfg.EXCEL_NAME, this::baseElementConfig).addChangeSampleFileObserveWithCallBack(BaseElementRewardCfg.EXCEL_NAME, this::baseElementRewardConfig).addChangeSampleFileObserveWithCallBack(BaseLineCfg.EXCEL_NAME, this::baseLineConfig)

                .addChangeSampleFileObserveWithCallBack(SpecialModeCfg.EXCEL_NAME, this::specialModeConfig).addChangeSampleFileObserveWithCallBack(SpecialAuxiliaryCfg.EXCEL_NAME, this::specialAuxiliaryConfig).addChangeSampleFileObserveWithCallBack(SpecialGirdCfg.EXCEL_NAME, this::specialGirdConfig).addChangeSampleFileObserveWithCallBack(SpecialResultLibCfg.EXCEL_NAME, this::specialResultLibConfig);
    }

    public void setSpecialResultLibCacheData(SpecialResultLibCacheData specialResultLibCacheData) {
        this.specialResultLibCacheData = specialResultLibCacheData;
    }

    public SpecialResultLibCacheData getSpecialResultLibCacheData() {
        return specialResultLibCacheData;
    }


    /**
     * 根据区间来分割结果库
     *
     * @param countMap libType -> sectionIndex -> count
     * @return
     */
    public Map<Integer, Map<Integer, Integer>> splitLibBySection(Map<Integer, Integer> countMap) {
        Map<Integer, PropInfo> sectionPropInfoMap = this.specialResultLibCacheData.getResultLibSectionPropMap().get(1);

        Map<Integer, Map<Integer, Integer>> sectionCountMap = new HashMap<>();
        for (Map.Entry<Integer, PropInfo> en : sectionPropInfoMap.entrySet()) {
            int libType = en.getKey();
            PropInfo propInfo = en.getValue();
            Integer libAllCount = countMap.get(libType);
            if (libAllCount == null || libAllCount < 1) {
                continue;
            }
            BigDecimal libAllCountBigDecimal = BigDecimal.valueOf(libAllCount);
            BigDecimal sumBigDecimal = BigDecimal.valueOf(propInfo.getSum());

            Map<Integer, Integer> tempCountMap = new HashMap<>();

//            log.info("sum = {}", propInfo.getSum());

            for (Map.Entry<Integer, int[]> en2 : propInfo.getPropMap().entrySet()) {
                int index = en2.getKey();
                int prop = en2.getValue()[1] - en2.getValue()[0];

                BigDecimal divide = BigDecimal.valueOf(prop).divide(sumBigDecimal, 9, BigDecimal.ROUND_HALF_UP);
                int count = libAllCountBigDecimal.multiply(divide).intValue();

//                log.info("libType = {}, i = {},count = {}", libType, index, count);

                tempCountMap.put(index, count);
            }

            sectionCountMap.put(libType, tempCountMap);
        }
        return sectionCountMap;
    }

    /**
     * 生成结果时删除条数为0的
     *
     * @param tmpExceptGenCountMap
     */
    public void removeCount0(Map<Integer, Map<Integer, Integer>> tmpExceptGenCountMap) {
        tmpExceptGenCountMap.entrySet().removeIf(en1 -> {
            en1.getValue().entrySet().removeIf(en2 -> en2.getValue() < 1);
            if (en1.getValue().isEmpty()) {
                return true;
            }
            return false;
        });
    }

    /**
     * 计算每个图标出现的次数
     *
     * @param arr
     * @return
     */
    protected Map<Integer, Integer> checkIconShowCount(int[] arr) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 1; i < arr.length; i++) {
            int icon = arr[i];
            map.merge(icon, 1, Integer::sum);
        }
        return map;
    }
}
