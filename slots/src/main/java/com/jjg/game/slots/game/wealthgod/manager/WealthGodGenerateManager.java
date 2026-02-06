package com.jjg.game.slots.game.wealthgod.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.wealthgod.data.WealthGodAwardLineInfo;
import com.jjg.game.slots.game.wealthgod.data.WealthGodResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class WealthGodGenerateManager extends AbstractSlotsGenerateManager<WealthGodAwardLineInfo, WealthGodResultLib> {

    public WealthGodGenerateManager() {
        super(WealthGodResultLib.class);
    }

    protected WealthGodAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount,
                                                      int baseIconId, List<Integer> lineList, int[] arr) {
        WealthGodAwardLineInfo awardLineInfo = new WealthGodAwardLineInfo();
        awardLineInfo.setSameCount(sameCount);
        awardLineInfo.setLineId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        return awardLineInfo;
    }

    @Override
    public void calTimes(WealthGodResultLib lib) throws Exception {
        if (triggerFreeLib(lib)) {
            calFree(lib, lib);
        } else {
            calLineTimes(lib, lib);
        }
    }

    /**
     * 计算中奖线的倍数
     */
    private void calLineTimes(WealthGodResultLib recordLib, WealthGodResultLib lib) {
        List<WealthGodAwardLineInfo> list = lib.getAwardLineInfoList();
        if (list == null || list.isEmpty()) {
            return;
        }
        for (WealthGodAwardLineInfo awardLineInfo : list) {
            recordLib.addTimes(awardLineInfo.getBaseTimes());
        }
    }

    /**
     * 计算免费游戏的倍数
     * 新逻辑：为每次旋转单独记录倍数，不再累加到外层总倍数
     * 这样可以清晰地看到每次旋转（包括嵌套免费游戏）的具体倍数贡献
     */
    private void calFree(WealthGodResultLib recordLib, WealthGodResultLib lib) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }
            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                WealthGodResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), WealthGodResultLib.class);
                calLineTimes(recordLib, tmpLib);
                // 递归处理嵌套的免费游戏
                calFree(recordLib, tmpLib);
            }
        }
    }

    @Override
    protected List<SpecialAuxiliaryInfo> assignPattern(WealthGodResultLib lib) {
        int[] arr = lib.getIconArr();
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();
        //获取指定图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_ASSIGN);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return specialAuxiliaryInfoList;
        }
        log.debug("检查指定图案");
        //小游戏
        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();
            //必须出现的图案
            int mustIconCount = 0;
            //条件图案
            Map<Integer, Integer> conditionIconsMap = new HashMap<>();
            for (int icon : arr) {
                //检查条件参数的图案
                if (icon == cfg.getRewardNum()) {
                    mustIconCount++;
                } else if (cfg.getElementId().contains(icon)) {
                    conditionIconsMap.merge(icon, 1, Integer::sum);
                }
            }
            //检查条件是否都满足
            if (mustIconCount < 1 || conditionIconsMap.isEmpty()) {
                continue;
            }

            //是否触发小游戏
            if (cfg.getFeatureTriggerId() == null || cfg.getFeatureTriggerId().isEmpty()) {
                continue;
            }

            conditionIconsMap.forEach((k, v) -> {
                for (int i = 0; i < v; i++) {
                    cfg.getFeatureTriggerId().forEach(miniGameId -> lib.getLibTypeSet().forEach(libType -> {
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(lib, libType, arr, miniGameId, lib.getSpecialGirdInfoList());
                        if (specialAuxiliaryInfo != null) {
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
                    }));
                }
            });
        }
        return specialAuxiliaryInfoList;
    }

    /**
     * 触发小游戏
     */
    public SpecialAuxiliaryInfo triggerMiniGame(WealthGodResultLib resultLib, int specialModeType, int[] arr, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
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
        triggerFree(resultLib, specialModeType, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo);
        //检查是否有额外奖励
        triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, specialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
        return specialAuxiliaryInfo;
    }

    /**
     * 检查免费旋转
     */
    protected void triggerFree(WealthGodResultLib lib, int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg,
                               SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig, SpecialAuxiliaryInfo specialAuxiliaryInfo) {
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

            WealthGodResultLib t = generateFreeOne(lib, specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(t));
        }
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(WealthGodResultLib lib) {
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = new ArrayList<>();
        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return specialAuxiliaryInfoList;
        }
        log.debug("检查全局分散");
        //小游戏
        int[] arr = lib.getSource();
        //记录每个图标出现的次数
        Map<Integer, Integer> iconCountMap = new HashMap<>();
        for (int icon : arr) {
            iconCountMap.merge(icon, 1, Integer::sum);
        }
        for (Map.Entry<Integer, BaseElementRewardCfg> en : normalRewardCfgMap.entrySet()) {
            BaseElementRewardCfg cfg = en.getValue();

            //检查出现的个数是否满足
            int elementsCount = 0;
            for (int iconId : cfg.getElementId()) {
                Integer count = iconCountMap.get(iconId);
                if (count != null) {
                    elementsCount += count;
                }
            }
            if (elementsCount != cfg.getRewardNum()) {
                continue;
            }

            //是否触发小游戏
            if (cfg.getFeatureTriggerId() != null && !cfg.getFeatureTriggerId().isEmpty()) {
                cfg.getFeatureTriggerId().forEach(miniGameId -> lib.getLibTypeSet().forEach(libType -> {
                    SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(lib, libType, arr, miniGameId, lib.getSpecialGirdInfoList());
                    if (specialAuxiliaryInfo != null) {
                        specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                    }
                }));
            }

            //触发jackpot
            if (cfg.getJackpotID() > 0) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;

    }

    @Override
    public SpecialGirdInfo gridUpdate(WealthGodResultLib lib, int cfgId, int[] arr) {
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

        Set<Integer> wildIconSet = this.iconsMap.get(SlotsConst.BaseElement.TYPE_WILD);

        //记录实际修改格子的次数
        int x = 0;
        int maxForCount = arr.length * 2;
        for (int i = 0; i < maxForCount; i++) {
            //获取一个需要替换的格子
            Integer girdId = cloneAffectGirdPropInfo.getRandKey();
            if (girdId == null) {
                log.debug("获取一个需要替换的格子失败");
                break;
            }
            girdShowMap.merge(girdId, 1, Integer::sum);

            //该格子上的图标id
            int icon = arr[girdId];
            //检查该格子是否不可替换
            if (specialGirdCfg.getNotReplaceEle() != null && specialGirdCfg.getNotReplaceEle().contains(icon)) {
                continue;
            }

            //随机一个需要出现的图标
            Integer newIcon = girdUpdatePropConfig.getShowIconPropInfo().getRandKey();
            if(newIcon == null){
                log.debug("随机一个需要出现的图标失败");
                break;
            }
            log.debug("修改格子 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);
            arr[girdId] = newIcon;
            //记录变成wild的图标的位置  每次免费和小游戏都需要变更为wild
            if (wildIconSet.contains(newIcon)) {
                lib.addChange(girdId, newIcon);
            }
            //赋值
            if (girdUpdatePropConfig.getValuePropInfo() != null) {
                Integer value = girdUpdatePropConfig.getValuePropInfo().getRandKey();
                if(value == null){
                    log.debug("修改图标后赋值失败 girdId = {}, oldIcon = {}, newIcon = {}", girdId, arr[girdId], newIcon);
                    break;
                }
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

    /**
     * 生成免费游戏的结果库。
     *
     * @param oldLib              旧的结果库，用于获取wild图标信息
     * @param specialModeType     特殊模式类型
     * @param specialAuxiliaryCfg 辅助配置
     * @param specialGroupGirdID  特殊格子组ID
     * @return 生成的免费游戏结果库，如果过程中发生错误则返回null
     */
    private WealthGodResultLib generateFreeOne(WealthGodResultLib oldLib, int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }
            //创建结果库对象
            WealthGodResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());
            lib.addLibType(specialModeType);

            //获取rollerMode
            int rollerMode = specialAuxiliaryCfg.getRollerMode();
            if (rollerMode < 1) {
                rollerMode = specialModeCfg.getRollerMode();
            }

            //生成所有的图标
            int[] arr = generateAllIcons(specialModeCfg.getRollerMode(), specialModeCfg.getCols(), specialModeCfg.getRows());
            if (arr == null) {
                return null;
            }

            Map<Integer, Integer> changeMap = oldLib.getAllIconChangeMap();
            //还原之前已经是wild的图标
            if (changeMap != null && !changeMap.isEmpty()) {
                changeMap.forEach((index, icon) -> {
                    arr[index] = icon;
                    lib.addAllIconChange(index, icon);
                });
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
            //替换财神图标
            int[] replaceArr = replaceWealthGod(lib, arr);
            //记录元数据
            lib.setSource(arr);
            //判断中奖，返回
            return checkAward(replaceArr, lib);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;

    }

    @Override
    public WealthGodResultLib generateOne(int libType) throws Exception {
        //获取模式配置
        SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(libType);
        if (specialModeCfg == null) {
            log.warn("生成图标时，specialModeCfg 配置为空 gameType = {},libType = {}", this.gameType, libType);
            return null;
        }

        //创建结果库对象
        WealthGodResultLib lib = createResultLib();
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
            SpecialGirdInfo specialGirdInfo = gridUpdate(lib, randKey, arr);
            if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                lib.addSpecialGirdInfo(specialGirdInfo);
            }
        }

        //修改格子
        if (specialModeCfg.getSpecialGirdID() != null && !specialModeCfg.getSpecialGirdID().isEmpty()) {
            for (int specialGirdCfgId : specialModeCfg.getSpecialGirdID()) {
                SpecialGirdInfo specialGirdInfo = gridUpdate(lib, specialGirdCfgId, arr);
                if (specialGirdInfo != null && !specialGirdInfo.emptyInfo()) {
                    lib.addSpecialGirdInfo(specialGirdInfo);
                }
            }
        }

        //替换财神图标
        int[] replaceArr = replaceWealthGod(lib, arr);

        //记录元数据
        lib.setSource(arr);

        //判断中奖，返回
        return checkAward(replaceArr, lib);
    }

    /**
     * 替换财神图标为wild
     */
    public int[] replaceWealthGod(WealthGodResultLib lib, int[] param) {
        int[] arr = Arrays.copyOf(param, param.length);

        //获取全局分散图案的配置
        Map<Integer, BaseElementRewardCfg> normalRewardCfgMap = this.baseElementRewardCfgMap.get(SlotsConst.BaseElementReward.LINE_TYPE_DISPERSE_GLOBAL);
        if (normalRewardCfgMap == null || normalRewardCfgMap.isEmpty()) {
            return null;
        }

        //获取每个图标出现的次数
        Map<Integer, Integer> showCountMap = checkIconShowCount(arr);

        log.debug("财神变wild");

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

            int miniGameId = cfg.getFeatureTriggerId().getFirst();

            //根据小游戏id去找相关配置
            SpecialAuxiliaryCfg specialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
            if (specialAuxiliaryCfg == null) {
                log.warn("未找到该小游戏的配置 miniGameId = {}", miniGameId);
                return arr;
            }

            SpecialAuxiliaryPropConfig specialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
            if (specialAuxiliaryPropConfig == null) {
                log.warn("未找到该小游戏小关的权重信息配置 miniGameId = {}", miniGameId);
                return arr;
            }
            //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
            Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
            if (freeCount == null || freeCount < 1) {
                return arr;
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
            }
        }
        return arr;
    }

}
