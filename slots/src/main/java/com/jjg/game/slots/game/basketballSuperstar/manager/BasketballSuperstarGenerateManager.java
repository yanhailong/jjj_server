package com.jjg.game.slots.game.basketballSuperstar.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarAddFreeInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarAwardLineInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import com.jjg.game.slots.game.thor.ThorConstant;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class BasketballSuperstarGenerateManager extends AbstractSlotsGenerateManager<BasketballSuperstarAwardLineInfo, BasketballSuperstarResultLib> {
    public BasketballSuperstarGenerateManager() {
        super(BasketballSuperstarResultLib.class);
    }

    private BasketballSuperstarAddFreeInfo basketballSuperstarAddFreeInfo;


    @Override
    protected List<SpecialAuxiliaryInfo> overallDisperse(BasketballSuperstarResultLib lib) {
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

            if(lib.getJackpotId() < 1){
                lib.setJackpotId(cfg.getJackpotID());
            }
        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public BasketballSuperstarResultLib checkAward(int[] arr, BasketballSuperstarResultLib lib, boolean freeModel) throws Exception {
        if (freeModel) {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<BasketballSuperstarAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            calTimes(lib);
            return lib;
        } else {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);

            //检查满线图案
            List<BasketballSuperstarAwardLineInfo> fullLineInfoList = fullLine(lib);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

            calTimes(lib);

            return lib;
        }
    }

    @Override
    protected BasketballSuperstarAwardLineInfo addFullLineAwardInfo(Set<Integer> sameIconIndexSet, BaseElementRewardCfg cfg) {
        BasketballSuperstarAwardLineInfo info = new BasketballSuperstarAwardLineInfo();

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

        while (remainFreeCount > 0) {
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }

            BasketballSuperstarResultLib lib = generateFreeOne(specialModeType, specialAuxiliaryCfg, specialGroupGirdID);
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
    private int checkAddFreeCount(BasketballSuperstarResultLib lib) {
        if (this.basketballSuperstarAddFreeInfo.getLibType() != BasketballSuperstarConstant.SpecialMode.FREE) {
            return 0;
        }

        int addCount = 0;
        for (int i = 1; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            //是否出现了目标图标
            if (icon != this.basketballSuperstarAddFreeInfo.getTargetIcon()) {
                continue;
            }
            boolean flag = SlotsUtil.calProp(this.basketballSuperstarAddFreeInfo.getProp());
            if (flag) {
                addCount += this.basketballSuperstarAddFreeInfo.getAddFreeCount();
            }
        }
        return addCount;
    }


    @Override
    public void calTimes(BasketballSuperstarResultLib lib) throws Exception {
        if (!checkElement(lib)) {
            throw new IllegalArgumentException("检查结果有错误 lib = " + JSONObject.toJSONString(lib));
        }

        if (lib.getSpecialAuxiliaryInfoList() != null && !lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
                if (specialAuxiliaryInfo.getFreeGames() != null && !specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                    Set<Integer> libTypeSet = new HashSet<>();
                    libTypeSet.add(BasketballSuperstarConstant.SpecialMode.FREE);
                    lib.setLibTypeSet(libTypeSet);
                    break;
                }
            }
        }

        //中奖线
        lib.addTimes(calLineTimes(lib.getAwardLineInfoList()));
        //免费
        lib.addTimes(calFree(lib));
    }

    /**
     * 计算中奖线的倍数
     *
     * @param list
     * @return
     */
    public int calLineTimes(List<BasketballSuperstarAwardLineInfo> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }

        int times = 0;
        for (BasketballSuperstarAwardLineInfo awardLineInfo : list) {
            times += awardLineInfo.getBaseTimes();
        }
        return times;
    }

    /**
     * 计算免费游戏的总倍数
     *
     * @param lib
     * @return
     */
    private long calFree(BasketballSuperstarResultLib lib) throws Exception {
        if (lib.getSpecialAuxiliaryInfoList() == null || lib.getSpecialAuxiliaryInfoList().isEmpty()) {
            return 0;
        }

        long times = 0;
        for (SpecialAuxiliaryInfo specialAuxiliaryInfo : lib.getSpecialAuxiliaryInfoList()) {
            if (specialAuxiliaryInfo.getFreeGames() == null || specialAuxiliaryInfo.getFreeGames().isEmpty()) {
                continue;
            }

            for (JSONObject jsonObject : specialAuxiliaryInfo.getFreeGames()) {
                BasketballSuperstarResultLib tmpLib = JSON.parseObject(jsonObject.toJSONString(), BasketballSuperstarResultLib.class);
                calTimes(tmpLib);
                times += tmpLib.getTimes();
            }
        }
        return times;
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
    private void addShowAuxiliaryId(BasketballSuperstarResultLib lib, Set<Integer> set) {
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
    private boolean checkJackpool(BasketballSuperstarResultLib lib) {
        if(lib.getJackpotId() < 1){
            return false;
        }

        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ThorConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == ThorConstant.BaseElement.ID_MINI || icon == ThorConstant.BaseElement.ID_MINOR ||
                    icon == ThorConstant.BaseElement.ID_MAJOR || icon == ThorConstant.BaseElement.ID_GRAND) {
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
    private boolean checkTriggerFree(BasketballSuperstarResultLib lib) {
        int count = 0;
        for (int i = 0; i < lib.getIconArr().length; i++) {
            int icon = lib.getIconArr()[i];
            if (icon == ThorConstant.BaseElement.ID_SCATTER) {
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
    private boolean checkElement(BasketballSuperstarResultLib lib) {
        if (lib.getLibTypeSet() == null || lib.getLibTypeSet().isEmpty()) {
            return true;
        }

        //检查二选一
        if (lib.getLibTypeSet().contains(BasketballSuperstarConstant.SpecialMode.FREE)
                && !checkTriggerFree(lib)) {
            log.warn("检查免费触发局失败");
            return false;
        }

        //检查jackpool模式
        if (lib.getLibTypeSet().contains(BasketballSuperstarConstant.SpecialMode.JACKPOOL) && !checkJackpool(lib)) {
            log.warn("检查jackpool模式失败");
            return false;
        }

        return true;
    }

}
