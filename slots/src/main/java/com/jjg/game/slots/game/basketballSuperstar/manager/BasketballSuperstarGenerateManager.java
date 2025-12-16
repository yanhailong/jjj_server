package com.jjg.game.slots.game.basketballSuperstar.manager;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.config.ConfigManager;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.SpecialAuxiliaryInfo;
import com.jjg.game.slots.data.SpecialAuxiliaryPropConfig;
import com.jjg.game.slots.data.SpecialGirdInfo;
import com.jjg.game.slots.game.basketballSuperstar.BasketballSuperstarConstant;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarAddFreeInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarAwardLineInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarFreeStickyWildInfo;
import com.jjg.game.slots.game.basketballSuperstar.data.BasketballSuperstarResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGenerateManager;
import com.jjg.game.slots.utils.SlotsUtil;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @auBasketballSuperstar lihaocao
 * @date 2025/12/2 17:33
 */
@Component
public class BasketballSuperstarGenerateManager extends AbstractSlotsGenerateManager<BasketballSuperstarAwardLineInfo, BasketballSuperstarResultLib> {

    public BasketballSuperstarGenerateManager() {
        super(BasketballSuperstarResultLib.class);
    }

    private BasketballSuperstarAddFreeInfo basketballSuperstarAddFreeInfo;

    private BasketballSuperstarFreeStickyWildInfo basketballSuperstarFreeStickyWildInfo;

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

            if (lib.getJackpotId() < 1) {
                lib.setJackpotId(cfg.getJackpotID());
                log.info("lib.getJackpotId()  {}", lib.getJackpotId());
            }

        }
        return specialAuxiliaryInfoList;
    }

    @Override
    public BasketballSuperstarResultLib checkAward(int[] arr, BasketballSuperstarResultLib lib, boolean freeModel) throws Exception {
        if (freeModel) {
            lib.setGameType(this.gameType);
            lib.setIconArr(arr);
            //拷贝数组
            int[] newArr = new int[arr.length];
            System.arraycopy(arr, 0, newArr, 0, arr.length);
            for (Integer i : lib.getChangeStickyIconSet()) {
                newArr[i] = BasketballSuperstarConstant.BaseElement.ID_WILD;
            }
            //检查满线图案
            List<BasketballSuperstarAwardLineInfo> fullLineInfoList = fullLine(newArr);
            lib.addAllAwardLineInfo(fullLineInfoList);

            //检查全局分散图案
            List<SpecialAuxiliaryInfo> overallDisperseAuxiliaryInfoList = overallDisperse(lib);
            lib.addSpecialAuxiliaryInfo(overallDisperseAuxiliaryInfoList);

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

        //根据权重选取 变成wild 图标
        Integer stickyIcon = selectByWeight(this.basketballSuperstarFreeStickyWildInfo.getIconWeightMap());
        //检查是否有免费旋转次数，免费旋转的结果，通过specialMode生成
        Integer freeCount = specialAuxiliaryPropConfig.getTriggerCountPropInfo().getRandKey();
        if (freeCount == null || freeCount < 1) {
            return;
        }

        log.debug("增加免费游戏次数 addCount = {}", freeCount);

        int remainFreeCount = freeCount;
        BasketballSuperstarResultLib lastLib = null;
        while (remainFreeCount > 0) {
            log.debug("免费转 权重变成wild 图标 stickyIcon = {}", stickyIcon);
            //检查是否有修改图案策略组id
            int specialGroupGirdID = 0;
            if (specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo() != null) {
                Integer randKey = specialAuxiliaryPropConfig.getSpecialGroupGirdIDPropInfo().getRandKey();
                if (randKey != null && randKey > 0) {
                    specialGroupGirdID = randKey;
                }
            }
            BasketballSuperstarResultLib lib = generateFreeOneHaveLastLib(specialModeType, specialAuxiliaryCfg, specialGroupGirdID, lastLib, stickyIcon);
            int addCount = checkAddFreeCount(lib);
            lib.setAddFreeCount(addCount);
            lib.setStickyIcon(stickyIcon);
            remainFreeCount += addCount;
            specialAuxiliaryInfo.addFreeGame((JSONObject) JSON.toJSON(lib));
            log.debug("--------------{}------------", remainFreeCount);
            remainFreeCount--;
            lastLib = lib;
        }
    }

    /**
     * 生成一个免费结果（有上盘结果）
     *
     * @param specialAuxiliaryCfg
     * @return
     */
    public BasketballSuperstarResultLib generateFreeOneHaveLastLib(int specialModeType,
                                                                   SpecialAuxiliaryCfg specialAuxiliaryCfg,
                                                                   int specialGroupGirdID,
                                                                   BasketballSuperstarResultLib lastLib,
                                                                   int stickyIcon) {
        try {
            //获取模式配置
            SpecialModeCfg specialModeCfg = this.specialModeCfgMap.get(specialModeType);
            if (specialModeCfg == null) {
                log.warn("生成免费游戏图标时，specialModeCfg 配置为空 gameType = {},specialModeType = {}", this.gameType, specialModeType);
                return null;
            }

            //创建结果库对象
            BasketballSuperstarResultLib lib = createResultLib();
            lib.setId(RandomUtils.getUUid());
            lib.setRollerMode(specialModeCfg.getRollerMode());

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

            //权重随机出来需要改变位置的格子
            Set<Integer> icons = new HashSet<>();
            //遍历格子 ，把 是stickyIcon添加进去
            for (int i = 0; i < arr.length; i++) {
                //第一列不能为 改变wild的格子 0 1 2 3 4
                if (arr[i] == stickyIcon && i >= 5) {
                    icons.add(i);
                }
            }
            log.info("新增需要格子变成 wild{}", JSONObject.toJSONString(icons));

            //如果上一轮有修改图标记录 直接加入进来
            if (lastLib != null && lastLib.getChangeStickyIconSet() != null && !lastLib.getChangeStickyIconSet().isEmpty()) {
                icons.addAll(lastLib.getChangeStickyIconSet());
                log.info("需要格子变成 wild{}", JSONObject.toJSONString(icons));
            }
            lib.setChangeStickyIconSet(icons);


            //上盘结果  wild图标保存
//            SpecialGirdInfo lastInfo = gridUpdate(lastLib.getIconArr(), arr);
//            if (lastInfo.getValueMap() != null && !lastInfo.getValueMap().isEmpty()) {
//                lib.addSpecialGirdInfo(lastInfo);
//            }
//
//            //权重选取的图标 变成wild 图标
//            SpecialGirdInfo wildInfo = gridUpdate(lastLib.getStickyIcon(), arr);
//            if (wildInfo.getValueMap() != null && !wildInfo.getValueMap().isEmpty()) {
//                lib.addSpecialGirdInfo(wildInfo);
//            }

            //判断中奖，返回
            return checkAward(arr, lib, true);

        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }
//
//    /**
//     * 获取 修改棋子原因 上次是wild 粘性
//     *
//     * @return
//     */
//    private SpecialGirdInfo gridUpdate(int[] LastArr, int[] arr) {
//        SpecialGirdInfo info = new SpecialGirdInfo();
//
//        for (int i = 0; i < LastArr.length; i++) {
//            if (LastArr[i] == BasketballSuperstarConstant.BaseElement.ID_WILD && arr[i] != BasketballSuperstarConstant.BaseElement.ID_WILD) {
//                arr[i] = BasketballSuperstarConstant.BaseElement.ID_WILD;
//                info.addValue(i, BasketballSuperstarConstant.BaseElement.ID_WILD);
//            }
//        }
//        if(info.getValueMap()!=null && !info.getValueMap().isEmpty()){
//            log.debug("上盘是粘性wild 继续保留 修改后的图标 arr = {}", Arrays.toString(arr));
//        }
//
//        return info;
//    }
//
//    /**
//     * 获取 修改棋子原因 粘性图标变成wild
//     *
//     * @return
//     */
//    private SpecialGirdInfo gridUpdate(int stickyIcon, int[] arr) {
//
//        SpecialGirdInfo info = new SpecialGirdInfo();
//        for (int i = 0; i < arr.length; i++) {
//            if (arr[i] == stickyIcon) {
//                arr[i] = BasketballSuperstarConstant.BaseElement.ID_WILD;
//                info.addValue(i, BasketballSuperstarConstant.BaseElement.ID_WILD);
//            }
//        }
//        if(info.getValueMap()!=null && !info.getValueMap().isEmpty()){
//            log.debug("粘性图标变成wild arr = {}", Arrays.toString(arr));
//        }
//        return info;
//    }

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
        if (lib.getJackpotId() < 1) {
            return false;
        }
        int[] newArr = new int[lib.getIconArr().length];
        System.arraycopy(lib.getIconArr(), 0, newArr, 0, lib.getIconArr().length);
        if (lib.getChangeStickyIconSet() != null && !lib.getChangeStickyIconSet().isEmpty()) {
            for (Integer i : lib.getChangeStickyIconSet()) {
                newArr[i] = BasketballSuperstarConstant.BaseElement.ID_WILD;
            }
        }
        int count = 0;
        int jackpool = 0;
        for (int i = 0; i < newArr.length; i++) {
            int icon = newArr[i];
            if (icon == BasketballSuperstarConstant.BaseElement.ID_SCATTER) {
                count++;
            } else if (icon == BasketballSuperstarConstant.BaseElement.ID_MINI || icon == BasketballSuperstarConstant.BaseElement.ID_MINOR ||
                    icon == BasketballSuperstarConstant.BaseElement.ID_MAJOR || icon == BasketballSuperstarConstant.BaseElement.ID_GRAND) {
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
        //拷贝数组
        int[] newArr = new int[lib.getIconArr().length];
        System.arraycopy(lib.getIconArr(), 0, newArr, 0, lib.getIconArr().length);
        if (lib.getChangeStickyIconSet() != null && !lib.getChangeStickyIconSet().isEmpty()) {
            for (Integer i : lib.getChangeStickyIconSet()) {
                newArr[i] = BasketballSuperstarConstant.BaseElement.ID_WILD;
            }
        }
        int count = 0;
        for (int i = 0; i < newArr.length; i++) {
            int icon = newArr[i];
            if (icon == BasketballSuperstarConstant.BaseElement.ID_ADDFREEE) {
                count++;
            }
        }
        return count >= 0;
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

    @Override
    protected void specialPlayConfig() {
        for (Map.Entry<Integer, SpecialPlayCfg> en : GameDataManager.getSpecialPlayCfgMap().entrySet()) {
            SpecialPlayCfg cfg = en.getValue();
            if (cfg.getGameType() != this.gameType) {
                continue;
            }
            //免费游戏出现在2、3、4、5轴时变成百搭，并一直粘连，直至退出此模式
            if (cfg.getPlayType() == BasketballSuperstarConstant.SpecialPlay.TYPE_STICKY_WILD) {
                BasketballSuperstarFreeStickyWildInfo tmpBasketballSuperstarFreeStickyWildInfo = new BasketballSuperstarFreeStickyWildInfo();
                String[] modeArr = cfg.getValue().split(",");
                int mode = Integer.parseInt(modeArr[0]);
                tmpBasketballSuperstarFreeStickyWildInfo.setLibType(mode);
                String[] iconWeight = modeArr[1].split("\\|");
                Map<Integer, Integer> iconWeightMap = new HashMap<>();
                for (String s : iconWeight) {
                    String[] arr = s.split("_");
                    int icon = Integer.parseInt(arr[0]);
                    int weight = Integer.parseInt(arr[1]);
                    iconWeightMap.put(icon, weight);
                }
                tmpBasketballSuperstarFreeStickyWildInfo.setIconWeightMap(iconWeightMap);

                this.basketballSuperstarFreeStickyWildInfo = tmpBasketballSuperstarFreeStickyWildInfo;
            } else
                //增加免费次数
                if (cfg.getPlayType() == BasketballSuperstarConstant.SpecialPlay.TYPE_ADD_FREE_COUNT) {
                    BasketballSuperstarAddFreeInfo tmpBasketballSuperstarAddFreeInfo = new BasketballSuperstarAddFreeInfo();
                    String[] arr = cfg.getValue().split("_");

                    tmpBasketballSuperstarAddFreeInfo.setLibType(Integer.parseInt(arr[0]));
                    tmpBasketballSuperstarAddFreeInfo.setTargetIcon(Integer.parseInt(arr[1]));
                    tmpBasketballSuperstarAddFreeInfo.setAddFreeCount(Integer.parseInt(arr[2]));
                    tmpBasketballSuperstarAddFreeInfo.setProp(Integer.parseInt(arr[3]));

                    this.basketballSuperstarAddFreeInfo = tmpBasketballSuperstarAddFreeInfo;
                }
        }
    }

    /**
     * 根据权重随机选择一个图标
     *
     * @param iconWeightMap 图标权重映射，key为图标ID，value为权重
     * @return 随机选中的图标ID
     */
    private Integer selectByWeight(Map<Integer, Integer> iconWeightMap) {
        if (iconWeightMap == null || iconWeightMap.isEmpty()) {
            throw new IllegalArgumentException("权重映射不能为空");
        }

        // 计算总权重
        int totalWeight = 0;
        for (int weight : iconWeightMap.values()) {
            if (weight < 0) {
                throw new IllegalArgumentException("权重不能为负数: " + weight);
            }
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            throw new IllegalArgumentException("总权重不能为0");
        }

        // 生成随机数 [0, totalWeight)
        Random random = new Random();
        int randomWeight = random.nextInt(totalWeight);

        // 遍历查找对应的图标
        int cumulativeWeight = 0;
        for (Map.Entry<Integer, Integer> entry : iconWeightMap.entrySet()) {
            cumulativeWeight += entry.getValue();
            if (randomWeight < cumulativeWeight) {
                return entry.getKey();
            }
        }

        // 理论上不会执行到这里，但为了编译安全返回最后一个
        return iconWeightMap.keySet().iterator().next();
    }

}
