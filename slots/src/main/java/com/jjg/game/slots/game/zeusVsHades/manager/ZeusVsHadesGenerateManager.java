package com.jjg.game.slots.game.zeusVsHades.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.*;
import com.jjg.game.slots.game.zeusVsHades.ZeusVsHadesConstant;
import com.jjg.game.slots.game.zeusVsHades.data.*;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @auZeusVsHades lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class ZeusVsHadesGenerateManager extends AbstractSlotsGenerateManager<ZeusVsHadesAwardLineInfo, ZeusVsHadesResultLib> {

    public ZeusVsHadesGenerateManager() {
        super(ZeusVsHadesResultLib.class);
    }

    private ZeusVsHadesFreeChooseInfo zeusVsHadesFreeChooseInfo;

    private Map<Integer, ZeusVsHadesNormalChooseInfo> zeusVsHadesNormalChooseInfoMap;

    @Override
    public ZeusVsHadesResultLib generateOne(int libType) throws Exception {
        try {
            ZeusVsHadesResultLib lib = super.generateOne(libType);
            if (libType == ZeusVsHadesConstant.SpecialMode.ZEUS || libType == ZeusVsHadesConstant.SpecialMode.HADES) {
                Set<Integer> libTypeSet = lib.getLibTypeSet();
                libTypeSet.remove(ZeusVsHadesConstant.SpecialMode.CHOOSE);
                lib.addLibType(libType);
            }
            return lib;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected ZeusVsHadesAwardLineInfo getAwardLineInfo() {
        return new ZeusVsHadesAwardLineInfo();
    }

    @Override
    protected ZeusVsHadesAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
        ZeusVsHadesAwardLineInfo info = super.addFullLineAwardInfo(sameIconIndexSet, cfg);
        info.setVsTime(1);
        info.setTotalTime(info.getBaseTimes() * info.getVsTime());
        return info;
    }

    @Override
    protected ZeusVsHadesAwardLineInfo addAwardLineInfo(BaseLineCfg baseLineCfg, BaseElementRewardCfg rewardCfg, int sameCount, int baseIconId, List<Integer> lineList, int[] arr) {
        ZeusVsHadesAwardLineInfo awardLineInfo = new ZeusVsHadesAwardLineInfo();
        Set<Integer> icons = new HashSet<>();
        List<Integer> posLocation = baseLineCfg.getPosLocation();
        int rewardNum = rewardCfg.getRewardNum();
        List<Integer> integers = posLocation.subList(0, rewardNum);
        icons.addAll(integers);
        awardLineInfo.setSameIconSet(icons);
        awardLineInfo.setSameIcon(rewardCfg.getElementId().getFirst());
        awardLineInfo.setLineId(baseLineCfg.getLineId());
        awardLineInfo.setBaseTimes(rewardCfg.getBet());
        awardLineInfo.setVsTime(1);
        awardLineInfo.setTotalTime(awardLineInfo.getBaseTimes() * awardLineInfo.getVsTime());
        return awardLineInfo;
    }

    private void resetLineRewardTimes(ZeusVsHadesResultLib lib, List<ZeusVsHadesAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        int vsTime = 0;
        Map<Integer, Integer> vsTimes = lib.getVsTimes();

        if (vsTimes == null || vsTimes.isEmpty()) {
            return;
        }

        for (Integer value : vsTimes.values()) {
            vsTime = vsTime + value;
        }

        for (int i = 0; i < list.size(); i++) {
            ZeusVsHadesAwardLineInfo zeusVsHadesAwardLineInfo = list.get(i);
            zeusVsHadesAwardLineInfo.setVsTime(vsTime);
            zeusVsHadesAwardLineInfo.setTotalTime(zeusVsHadesAwardLineInfo.getVsTime() * zeusVsHadesAwardLineInfo.getBaseTimes());
        }
    }

    protected List<SpecialAuxiliaryInfo> overallDisperse(ZeusVsHadesResultLib lib, int specialModeType) {
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

//        boolean isZeus = (RandomUtils.getRandomNumInt100() > 50) ? true : false;
        List<Boolean> winStatus = new ArrayList<>();
        winStatus.add(false);
        winStatus.add(false);

        AtomicBoolean isAdd = new AtomicBoolean(true);

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
//                        log.error("miniGameId:{}", miniGameId);
//                        if (miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_1_MINI_GAMEID &&
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_2_MINI_GAMEID &&
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_3_MINI_GAMEID &&
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_4_MINI_GAMEID &&
//
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_1_MINI_GAMEID &&
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_2_MINI_GAMEID &&
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_3_MINI_GAMEID &&
//                                miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_4_MINI_GAMEID
//                        ) {
                        SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(winStatus, isAdd, specialModeType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                        if (specialAuxiliaryInfo != null) {
                            showAuxiliaryIdSet.add(miniGameId);
                            specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                        }
//                        }
                    }
                });
            }
            //jackpot
            if (lib.jackpotEmpty()) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(ZeusVsHadesResultLib lib) {
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

//        boolean isZeus = (RandomUtils.getRandomNumInt100() > 50) ? true : false;
        List<Boolean> winStatus = new ArrayList<>();
        winStatus.add(false);
        winStatus.add(false);

        AtomicBoolean isAdd = new AtomicBoolean(true);

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
//                            log.error("miniGameId:{}", miniGameId);
//                            if (miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_1_MINI_GAMEID &&
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_2_MINI_GAMEID &&
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_3_MINI_GAMEID &&
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_HADES_4_MINI_GAMEID &&
//
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_1_MINI_GAMEID &&
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_2_MINI_GAMEID &&
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_3_MINI_GAMEID &&
//                                    miniGameId != ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_4_MINI_GAMEID
//                            ) {
                            SpecialAuxiliaryInfo specialAuxiliaryInfo = triggerMiniGame(winStatus, isAdd, libType, lib.getIconArr(), miniGameId, lib.getSpecialGirdInfoList());
                            if (specialAuxiliaryInfo != null) {
                                showAuxiliaryIdSet.add(miniGameId);
                                specialAuxiliaryInfoList.add(specialAuxiliaryInfo);
                            }
//                            }
                        });
                    }
                });
            }
            //jackpot
            if (lib.jackpotEmpty()) {
                lib.addJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public SpecialAuxiliaryInfo triggerMiniGame(int specialModeType, int[] arr, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
        List<Boolean> winStatus = new ArrayList<>();
        winStatus.add(false);
        winStatus.add(false);
        return triggerMiniGame(winStatus, new AtomicBoolean(true), specialModeType, arr, miniGameId, specialGirdInfoList);
    }

    /**
     * 触发小游戏
     *
     * @param miniGameId
     * @return
     */
    public SpecialAuxiliaryInfo triggerMiniGame(List<Boolean> winStatus, AtomicBoolean isAdd, int specialModeType, int[] arr, int miniGameId, List<SpecialGirdInfo> specialGirdInfoList) {
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

        ZeusVsHadesSpecialAuxiliaryInfo specialAuxiliaryInfo = new ZeusVsHadesSpecialAuxiliaryInfo();
        specialAuxiliaryInfo.setCfgId(miniGameId);
        if (specialModeType == ZeusVsHadesConstant.SpecialMode.NORMAL) {
            int auxiliaryId = specialAuxiliaryCfg.getId();
            ZeusVsHadesNormalChooseInfo zeusVsHadesNormalChooseInfo = zeusVsHadesNormalChooseInfoMap.get(auxiliaryId);
            if (zeusVsHadesNormalChooseInfo != null) {
                specialAuxiliaryInfo.setColumn(zeusVsHadesNormalChooseInfo.getColumn());
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
                boolean isZeus = (RandomUtils.getRandomNumInt100() > 50) ? true : false;
                if (isZeus) {
                    specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.ZEUS);
                } else {
                    specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.HADES);
                }
                //宙斯模式
                if (isZeus && !winStatus.get(0)) {
                    int zeusAuxiliaryId = zeusVsHadesNormalChooseInfo.getZeusAuxiliaryId();
                    //检查免费旋转
                    SpecialAuxiliaryCfg zeusSpecialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(zeusAuxiliaryId);
                    SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(zeusAuxiliaryId);
                    triggerFree(ZeusVsHadesConstant.SpecialMode.NORMAL, zeusSpecialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                    //检查是否有额外奖励
                    triggerAuxiliaryExtra(arr, zeusSpecialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                    winStatus.set(0, true);
                }
                //哈里斯模式
                else if (!isZeus && !winStatus.get(1)) {
                    specialAuxiliaryInfo.setHadesExchangeWildSet(setHadesWildIcon(arr, zeusVsHadesNormalChooseInfo.getColumn()));
                    winStatus.set(1, true);
                }
            }
        } else if (specialModeType == ZeusVsHadesConstant.SpecialMode.ZEUS) {
            int auxiliaryId = specialAuxiliaryCfg.getId();
            if (miniGameId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS) {
//                int zeusAuxiliaryId = zeusVsHadesNormalChooseInfo.getZeusAuxiliaryId();
                //检查免费旋转
                SpecialAuxiliaryCfg zeusSpecialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
                SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
                triggerFree(ZeusVsHadesConstant.SpecialMode.ZEUS, zeusSpecialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                //检查是否有额外奖励
                triggerAuxiliaryExtra(arr, zeusSpecialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                return specialAuxiliaryInfo;
                //触发免费宙斯模式
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_1) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.ZEUS);
                if (isAdd.getAndSet(false)) {
                    SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(auxiliaryId);
                    triggerFree(ZeusVsHadesConstant.SpecialMode.ZEUS, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                    //检查是否有额外奖励
                    triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                }
                specialAuxiliaryInfo.setColumn(0);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_2) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.ZEUS);
                if (isAdd.getAndSet(false)) {
                    SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(auxiliaryId);
                    triggerFree(ZeusVsHadesConstant.SpecialMode.ZEUS, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                    //检查是否有额外奖励
                    triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                }
                specialAuxiliaryInfo.setColumn(1);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_3) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.ZEUS);
                if (isAdd.getAndSet(false)) {
                    SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(auxiliaryId);
                    triggerFree(ZeusVsHadesConstant.SpecialMode.ZEUS, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                    //检查是否有额外奖励
                    triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                }
                specialAuxiliaryInfo.setColumn(2);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_4) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.ZEUS);
                if (isAdd.getAndSet(false)) {
                    SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(auxiliaryId);
                    triggerFree(ZeusVsHadesConstant.SpecialMode.ZEUS, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                    //检查是否有额外奖励
                    triggerAuxiliaryExtra(arr, specialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                }
                specialAuxiliaryInfo.setColumn(3);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            }

        } else if (specialModeType == ZeusVsHadesConstant.SpecialMode.HADES) {
            int auxiliaryId = specialAuxiliaryCfg.getId();
            if (miniGameId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES) {
                //检查免费旋转
                SpecialAuxiliaryCfg zeusSpecialAuxiliaryCfg = GameDataManager.getSpecialAuxiliaryCfg(miniGameId);
                SpecialAuxiliaryPropConfig zeusSpecialAuxiliaryPropConfig = this.specialAuxiliaryPropConfigMap.get(miniGameId);
                triggerFree(ZeusVsHadesConstant.SpecialMode.HADES, zeusSpecialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo);
                //检查是否有额外奖励
                triggerAuxiliaryExtra(arr, zeusSpecialAuxiliaryCfg, zeusSpecialAuxiliaryPropConfig, specialAuxiliaryInfo, specialGirdInfoList);
                return specialAuxiliaryInfo;
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_1) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.HADES);
                if (isAdd.getAndSet(false)) {
                    specialAuxiliaryInfo.setHadesExchangeWildSet(setHadesWildIcon(arr, 0));
                }
                specialAuxiliaryInfo.setColumn(0);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_2) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.HADES);
                if (isAdd.getAndSet(false)) {
                    specialAuxiliaryInfo.setHadesExchangeWildSet(setHadesWildIcon(arr, 1));
                }
                specialAuxiliaryInfo.setColumn(1);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_3) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.HADES);
                if (isAdd.getAndSet(false)) {
                    specialAuxiliaryInfo.setHadesExchangeWildSet(setHadesWildIcon(arr, 2));
                }
                specialAuxiliaryInfo.setColumn(2);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            } else if (auxiliaryId == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_4) {
                specialAuxiliaryInfo.setWildStatus(ZeusVsHadesConstant.WildStatus.HADES);
                if (isAdd.getAndSet(false)) {
                    specialAuxiliaryInfo.setHadesExchangeWildSet(setHadesWildIcon(arr, 3));
                }
                specialAuxiliaryInfo.setColumn(3);
                specialAuxiliaryInfo.setTime(randomTimes(specialAuxiliaryCfg.getAwardTypeC()));
            }
        }
        return specialAuxiliaryInfo;
    }

    private int randomTimes(List<List<Integer>> awardTypeC) {
        Map<Integer, Integer> map = new HashMap<>();
        for (List<Integer> list : awardTypeC) {
            map.put(list.get(0), list.get(1));
        }
        Set<Integer> randomByWeight = RandomUtils.getRandomByWeight(map, 1);
        Integer times = randomByWeight.iterator().next();
        return times;
    }

    //哈里斯模式 赋值 额外 wild
    private Set<Integer> setHadesWildIcon(int[] arr, int column) {
        BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
        int rows = baseInitCfg.getRows();
        //不是特殊图标
        Set<Integer> otherIconSet = new HashSet<>();
        for (int i = 0; i < arr.length; i++) {
            int icon = arr[i];
            if (icon <= ZeusVsHadesConstant.BaseElement.TYPE0_BIG && icon >= ZeusVsHadesConstant.BaseElement.TYPE0_SMALL && (i - 1) / rows != column) {
                otherIconSet.add(i);
            }
        }
        List<Integer> list = new ArrayList<>(otherIconSet);
        // 洗牌算法随机排序
        Collections.shuffle(list);

        int count = Math.min(2, otherIconSet.size());
        Set<Integer> wildIconSet = new HashSet<>();
        for (Integer i : list.subList(0, count)) {
            wildIconSet.add(i);
        }

        return wildIconSet;
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

            ZeusVsHadesResultLib lib;
            if (specialModeType == ZeusVsHadesConstant.SpecialMode.ZEUS) {
                lib = generateZeusOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            } else if (specialModeType == ZeusVsHadesConstant.SpecialMode.HADES) {
                lib = generateHadesOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            } else {
                lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
            }
            int addCount = 0;
            log.debug("免费转新加 {}", addCount);
            lib.setAddFreeCount(addCount);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            log.debug("--------------{}------------", remainFreeCount);
            remainFreeCount--;
        }
    }

    private ZeusVsHadesResultLib generateZeusOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            ZeusVsHadesResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());
            lib.addLibType(specialModeType);
//            Set<Integer> libTypeSet = new HashSet<>();
//            libTypeSet.add(specialModeType);
//            lib.setLibTypeSet(libTypeSet);
//            lib.setWildStatus(ZeusVsHadesConstant.WildStatus.ZEUS);
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
            //判断中奖，返回
            return checkAward(arr, lib, specialModeType);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    private ZeusVsHadesResultLib generateHadesOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            ZeusVsHadesResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());
            lib.addLibType(specialModeType);
//            Set<Integer> libTypeSet = new HashSet<>();
//            libTypeSet.add(specialModeType);
//            lib.setLibTypeSet(libTypeSet);
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
            //判断中奖，返回
            return checkAward(arr, lib, specialModeType);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    //特殊玩法的中奖
    public ZeusVsHadesResultLib checkAward(int[] arr, ZeusVsHadesResultLib lib, boolean freeModel) throws Exception {
//        if (freeModel) {
//            return checkAward(arr, lib, ZeusVsHadesConstant.SpecialMode.CHOOSE);
//        } else {
//            return checkAward(arr, lib, ZeusVsHadesConstant.SpecialMode.NORMAL);
//        }
        return checkAward(arr, lib, lib.getLibTypeSet().iterator().next());
    }

    //特殊玩法的中奖
    public ZeusVsHadesResultLib checkAward(int[] arr, ZeusVsHadesResultLib lib, int specialModeType) throws Exception {
        lib.setGameType(this.gameType);
        lib.setIconArr(arr);

        //检查指定图案
        List<SpecialAuxiliaryInfo> specialAuxiliaryInfoList = assignPattern(lib);
        lib.addSpecialAuxiliaryInfo(specialAuxiliaryInfoList);

        //检查满线图案_x连
        List<ZeusVsHadesAwardLineInfo> fullLineInfoList = fullLine(lib);
        lib.addAllAwardLineInfo(fullLineInfoList);

        //检查全局分散图案
        List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib, specialModeType);
        lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

        //检查满线图案_数量
        List<ZeusVsHadesAwardLineInfo> fullLineCountInfoList = fullLineCount(lib);
        lib.addAllAwardLineInfo(fullLineCountInfoList);

        //检查连线分散数量
        List<ZeusVsHadesAwardLineInfo> lineDispersionCount = lineDispersionCount(lib);
        lib.addAllAwardLineInfo(lineDispersionCount);

        //整理lib
        deallib(lib);

        //连线
        List<ZeusVsHadesAwardLineInfo> awardLineInfoList = winLines(lib, true);
        lib.addAllAwardLineInfo(awardLineInfoList);

        //重置中奖倍数
        resetLineRewardTimes(lib, lib.getAwardLineInfoList());

        //计算倍数
        calTimes(lib);
        return lib;
    }

    //整理lib
    private void deallib(ZeusVsHadesResultLib lib) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return;
        }
        Map<Integer, Set<Integer>> wildMap = new HashMap<>();
        Map<Integer, Integer> vsTimes = new HashMap<>();
        Map<Integer, Integer> vsStatus = new HashMap<>();
        Set<Integer> hadesExchangeWildSet = new HashSet<>();
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS
                    || specialAuxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES) {
                continue;
            }
            ZeusVsHadesSpecialAuxiliaryInfo auxiliaryInfo = (ZeusVsHadesSpecialAuxiliaryInfo) specialAuxiliaryInfo;
            if (auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.NORMAL_1
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.NORMAL_2
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.NORMAL_3
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.NORMAL_4
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_1
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_2
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_3
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_ZEUS_4
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_1
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_2
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_3
                    || auxiliaryInfo.getCfgId() == ZeusVsHadesConstant.SpecialAuxiliary.FREE_HADES_4
            ) {
                if (auxiliaryInfo.getColumn() != null && auxiliaryInfo.getTime() != null) {
                    vsTimes.put(auxiliaryInfo.getColumn(), auxiliaryInfo.getTime());
                    vsStatus.put(auxiliaryInfo.getColumn(), auxiliaryInfo.getWildStatus());
                    if (auxiliaryInfo.getHadesExchangeWildSet() != null && !auxiliaryInfo.getHadesExchangeWildSet().isEmpty()) {
                        hadesExchangeWildSet = auxiliaryInfo.getHadesExchangeWildSet();
                    }
                }
            }
        }

        lib.setVsTimes(vsTimes);
        vsTimes.forEach((key, value) -> {
            Set<Integer> rowsSet = wildMap.get(0);
            if (rowsSet == null) {
                rowsSet = new HashSet<>();
            }
            BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(this.gameType);
            for (int i = 1; i < lib.getIconArr().length; i++) {
                if ((i - 1) / baseInitCfg.getRows() == key) {
                    rowsSet.add(i);
                }
            }
            wildMap.put(0, rowsSet);
        });
        Set<Integer> exchangeWildSet = wildMap.get(0);

        while (!aDoesNotContainBStrict(hadesExchangeWildSet, exchangeWildSet)) {
            hadesExchangeWildSet = setHadesWildIcon(lib.getIconArr(), vsTimes.keySet().iterator().next());
        }

        wildMap.put(1, hadesExchangeWildSet);

        lib.setReplaceWildIndexs(wildMap);

        lib.setVsStatus(vsStatus);
    }


    public List<ZeusVsHadesAwardLineInfo> winLines(ZeusVsHadesResultLib lib, boolean freeModel) {
        int[] arr = lib.getIconArr();
        int[] newArr = new int[arr.length];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        if (lib.getReplaceWildIndexs() != null) {
            if (lib.getReplaceWildIndexs().get(0) != null && lib.getReplaceWildIndexs().get(0).size() > 0) {
                for (Integer index : lib.getReplaceWildIndexs().get(0)) {
                    newArr[index] = ZeusVsHadesConstant.BaseElement.ID_WILD;
                }
            }
            if (lib.getReplaceWildIndexs().get(1) != null && lib.getReplaceWildIndexs().get(1).size() > 0) {
                for (Integer index : lib.getReplaceWildIndexs().get(1)) {
                    newArr[index] = ZeusVsHadesConstant.BaseElement.ID_WILD;
                }
            }
        }
        return winLines(newArr, freeModel);
    }

    @Override
    public void calTimes(ZeusVsHadesResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            throw new IllegalArgumentException("检查结果有错误 lib = " + JSONObject.toJSONString(lib));
        }
        lib.setTimes(0L);
        if (triggerFreeLib(lib, ZeusVsHadesConstant.SpecialMode.CHOOSE)
                || triggerFreeLib(lib, ZeusVsHadesConstant.SpecialMode.HADES)
                || triggerFreeLib(lib, ZeusVsHadesConstant.SpecialMode.ZEUS)) {
            //免费
            lib.addTimes(calFree(lib));
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        } else {
            //中奖线
            lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        }
    }

    /**
     * 是否为免费触发局
     *
     * @param lib
     * @return
     */
    protected boolean triggerFreeLib(ZeusVsHadesResultLib lib, int freeModel) {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return false;
        }

        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() != null && !specialAuxiliaryInfo.getFreeGames().isEmpty()) {
//                if (freeModel > 0) {
////                    Set<Integer> libTypeSet = new HashSet<>();
////                    libTypeSet.add(freeModel);
////                    lib.setLibTypeSet(libTypeSet);
//                }
                return true;
            }
        }
        return false;
    }

    /**
     * 计算免费游戏的总倍数
     *
     * @param lib
     * @return
     */
    protected long calFree(ZeusVsHadesResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                ZeusVsHadesResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), this.resultLibClazz);
                calTimes(tmpLib);
//                calLineTimes(tmpLib.getAwardLineInfoList());
                times += tmpLib.getTimes();
            }
        }
        return times;
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<ZeusVsHadesAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (ZeusVsHadesAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getTotalTime();
        }
        log.info("中奖线倍数times = {} awardLineInfo ={}", times, JSON.toJSON(list));
        return times;
    }

    /**
     * 添加已经出现的小游戏id
     *
     * @param lib
     * @param set
     */
    private void addShowAuxiliaryId(ZeusVsHadesResultLib lib, Set<Integer> set) {
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
    private boolean checkJackpool(ZeusVsHadesResultLib lib) {
        if (lib.jackpotEmpty()) {
            return false;
        }

        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ZeusVsHadesConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == ZeusVsHadesConstant.BaseElement.ID_MINI || icon == ZeusVsHadesConstant.BaseElement.ID_MINOR || icon == ZeusVsHadesConstant.BaseElement.ID_MAJOR || icon == ZeusVsHadesConstant.BaseElement.ID_GRAND) {
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
    private boolean checkTriggerFree(ZeusVsHadesResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ZeusVsHadesConstant.BaseElement.ID_SCATTER) {
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
    private boolean checkElement(ZeusVsHadesResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

//        //检查二选一
//        if (lib.getLibTypeSet().contains(ZeusVsHadesConstant.SpecialMode.CHOOSE) && !checkTriggerFree(lib)) {
//            log.warn("检查选择触发局失败");
//            return false;
//        }

//        //检查jackpool模式
//        if (lib.getLibTypeSet().contains(ZeusVsHadesConstant.SpecialMode.JACKPOOL) && !checkJackpool(lib)) {
//            log.warn("检查jackpool模式失败");
//            return false;
//        }
//
//        //检查宙斯模式 vs 哈迪斯模式
//        if ((lib.getLibTypeSet().contains(ZeusVsHadesConstant.SpecialMode.ZEUS) || lib.getLibTypeSet().contains(ZeusVsHadesConstant.SpecialMode.HADES)) && !checkFreeModel(lib)) {
//            log.warn("检查模式失败");
//            return false;
//        }
        return true;
    }

    public boolean checkFreeModel(ZeusVsHadesResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ZeusVsHadesConstant.BaseElement.ID_SCATTER) {
                count++;
            }
        }
        return count >= 3;
    }


    /**
     * 生成一个免费结果
     *
     * @param specialAuxiliaryCfg
     * @return
     */
    @Override
    public ZeusVsHadesResultLib generateFreeOne(int specialModeType, SpecialAuxiliaryCfg specialAuxiliaryCfg, int specialGroupGirdID) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
//            if (specialModeType == ZeusVsHadesConstant.SpecialMode.ZEUSWILDFREE) {
//                specialModeCfg = this.specialModeCfgMap.get(2);
//            }

            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }
            //创建结果库对象
            ZeusVsHadesResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());
            lib.addLibType(specialModeType);
//            Set<Integer> libTypeSet = new HashSet<>();
//            libTypeSet.add(specialModeType);
//            lib.setLibTypeSet(libTypeSet);
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
            //判断中奖，返回
            return checkAward(arr, lib, true);
        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }

    @Override
    protected void specialPlayConfig() {
        ZeusVsHadesFreeChooseInfo tempZeusVsHadesFreeChooseInfo = new ZeusVsHadesFreeChooseInfo();
        Map<Integer, ZeusVsHadesNormalChooseInfo> tempZeusVsHadesNormalChooseInfoMap = new HashMap<>();
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }

            //选择还是 宙斯、哈里斯
            if (cfg.getPlayType() == ZeusVsHadesConstant.SpecialPlay.FREE_CHOOSE_ZEUS_OR_HADES) {
                String[] arr = cfg.getValue().split(",");
                String[] arr1 = arr[1].split("_");
                tempZeusVsHadesFreeChooseInfo.setRewardId(Integer.parseInt(arr[0]));
                tempZeusVsHadesFreeChooseInfo.setChooseZeus(Integer.parseInt(arr1[0]));
                tempZeusVsHadesFreeChooseInfo.setChooseHades(Integer.parseInt(arr1[1]));
            }

            //选择还是 宙斯、哈里斯
            //  30200003,30200007|30200008;30200004,30200007|30200008;30200005,30200007|30200006;30200006,30200007|30200008
            else if (cfg.getPlayType() == ZeusVsHadesConstant.SpecialPlay.NORMAL_CHOOSE_ZEUS_OR_HADES) {
                String[] arr = cfg.getValue().split(";");
                for (int i = 0; i < arr.length; i++) {
                    String[] arr2 = arr[i].split(",");
                    int auxiliaryId = Integer.parseInt(arr2[0]);
                    String[] arr3 = arr2[1].split("\\|");
                    ZeusVsHadesNormalChooseInfo tempZeusVsHadesNormalChooseInfo = new ZeusVsHadesNormalChooseInfo();
                    tempZeusVsHadesNormalChooseInfo.setAuxiliaryId(auxiliaryId);
                    tempZeusVsHadesNormalChooseInfo.setZeusAuxiliaryId(Integer.parseInt(arr3[0]));
                    tempZeusVsHadesNormalChooseInfo.setHadesAuxiliaryId(Integer.parseInt(arr3[1]));
                    tempZeusVsHadesNormalChooseInfo.setAuxiliaryId(auxiliaryId);
                    tempZeusVsHadesNormalChooseInfo.setColumn(i);
                    tempZeusVsHadesNormalChooseInfoMap.put(auxiliaryId, tempZeusVsHadesNormalChooseInfo);
                }
            }
        }
        this.zeusVsHadesFreeChooseInfo = tempZeusVsHadesFreeChooseInfo;
        this.zeusVsHadesNormalChooseInfoMap = tempZeusVsHadesNormalChooseInfoMap;
    }

    /**
     * 格子修改
     *
     * @param cfgId specialGirdCfg的配置id
     * @param arr   图标数组
     * @return
     */
    public SpecialGirdInfo gridUpdate(int cfgId, int[] arr) {
        log.debug("开始修改格子 specialGirdCfgId = {}", cfgId);

        if (cfgId == ZeusVsHadesConstant.MiniGameId.NORMAL_1_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.NORMAL_2_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.NORMAL_3_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.NORMAL_4_MINI_GAMEID ||

                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_HADES_1_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_HADES_2_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_HADES_3_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_HADES_4_MINI_GAMEID ||

                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_1_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_2_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_3_MINI_GAMEID ||
                cfgId == ZeusVsHadesConstant.MiniGameId.FREE_ZEUS_4_MINI_GAMEID) {
            log.debug("该配置不用修改格子 cfgId = {}", cfgId);
            return null;
        }
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
     * 判断集合a是否不包含集合b中的任何元素（更严格的版本）
     * 要求a和b都不为空
     *
     * @param a 第一个集合
     * @param b 第二个集合
     * @return 如果a不包含b中的任何元素，返回true；否则返回false
     */
    public static boolean aDoesNotContainBStrict(Set<Integer> a, Set<Integer> b) {
        if (a == null || b == null) {
            return true;  // 遍历完所有元素都没有找到相同的
        }

        // 遍历b中的每个元素，检查是否在a中存在
        for (Integer num : b) {
            if (a.contains(num)) {
                return false;  // 只要找到一个相同的元素，就返回false
            }
        }
        return true;  // 遍历完所有元素都没有找到相同的
    }
}
