package com.jjg.game.dollarexpress.manager;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.dao.DollarExpressResultDao;
import com.jjg.game.dollarexpress.data.*;
import com.jjg.game.dollarexpress.sample.GameDataManager;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressIconCfg;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressLineCfg;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressShowCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author 11
 * @date 2025/6/23 10:31
 */
@Component
public class DollarExpressGenerateManager {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private DollarExpressResultDao dollarExpressResultDao;

    /**
     * 初始化
     */
    public void init(){
        //缓存icon表的以及计算所需的数据
    }

    /**
     * 生成结果集
     * @param count
     */
    public void generate(int count) {
        try{
            this.dollarExpressResultDao.removeTable();
            List<DollarExpressResult> list = new ArrayList<>();
            for(int i=0;i<count;i++) {
                DollarExpressResult result = generateOne(i);
                list.add(result);

                dollarExpressResultDao.saveAll(list);

                if(list.size() > 1000){
                    list = new ArrayList<>();
                    log.debug("已插入 DollarExpressResult 条数 {}",count);
                }
            }
        }catch (Exception e){
            log.error("",e);
        }


    }

    /**
     * 生成一条结果
     */
    private DollarExpressResult generateOne(int id) {
        try{
            DollarExpressResult result = new DollarExpressResult();
            result.setId(id);

            //随机生成icon集合
            int[] iconArr = genIconList();
            if(iconArr == null){
                log.warn("生成icon列表集合失败 i = {}",id);
                return null;
            }

            result.setIconArr(iconArr);
            //检查奖励
            result = checkAllAward(result);
            return result;
        }catch (Exception e){
            log.error("",e);
        }
        return null;
    }


    /**
     * 随机生成的icon结果
     * @return
     */
    private int[] genIconList(){
        try{
            int[] arr = new int[DollarExpressConst.Common.ALL_CION_COUNT];
            int index = 0;

            for(Map.Entry<Integer, DollarExpressShowCfg> en : GameDataManager.getDollarExpressShowCfgMap().entrySet()){
                DollarExpressShowCfg cfg = en.getValue();
                if(cfg.getIcons() == null || cfg.getIcons().isEmpty()){
                    log.warn("第 {} 轴的icon配置为空，生成失败",en.getKey());
                    continue;
                }

                //随机生成起始位置
                int rand = RandomUtils.randomInt(cfg.getIcons().size());
                for(int j = 0; j< DollarExpressConst.Common.COLUM_ICON_COUNT; j++){
                    int num = rand + j;
                    num = num % cfg.getIcons().size();

                    arr[index] = cfg.getIcons().get(num);
                    index++;
                }

            }
            return arr;
        }catch (Exception e){
            log.error("",e);
        }
        return null;
    }


    /**
     * 检查所有的奖励
     */
    private DollarExpressResult checkAllAward(DollarExpressResult result){
        //先检查特殊中奖
        result = checkSpecialAward(result);

        //如果是保险箱，就不检查中奖线
        if(result.getSpecialType() != DollarExpressConst.Common.SPECIAL_TYPE_SAFE_BOX){
            //中奖线
            List<AwardLineInfo> awardLineInfoList = checkLineAward(result.getIconArr());
            if(!awardLineInfoList.isEmpty()){
                result.setAwardList(awardLineInfoList);
            }
        }

        return result;
    }

    /**
     * 检查特殊奖励
     */
    private DollarExpressResult checkSpecialAward(DollarExpressResult result){
        //免费图标个数
        int freeIconCount = 0;
        //美元图标个数
        int dollarIconCount = 0;

        //检查第0-3轴
        for(int i=DollarExpressConst.Common.COLUM_0_ID_BEGIN;i<DollarExpressConst.Common.COLUM_3_ID_END;i++){
            int iconId = result.getIconArr()[i];

            if(isTrainId(iconId)){ //检查火车
                result.setSpecialType(DollarExpressConst.Common.SPECIAL_TYPE_TRAIN);
            }else if(iconId == DollarExpressConst.Icon.DOLLAR_ID){  //检查美元图标
                dollarIconCount++;
            }else if(iconId == DollarExpressConst.Icon.ALL_ABOARD_ID){ //免费
                freeIconCount++;
            }
        }

        //如果前面出现了美元图标
        if(dollarIconCount > 0){
            boolean addDollar = false;
            //检查第4轴
            for(int i = DollarExpressConst.Common.COLUM_4_ID_BEGIN;i<DollarExpressConst.Common.COLUM_4_ID_END;i++){
                int iconId = result.getIconArr()[i];
                //是否有保险箱
                if(iconId == DollarExpressConst.Icon.SAFE_BOX_ID){
                    addDollar = true;
                }

                //是否有金火车
                if(iconId == DollarExpressConst.Icon.GOLD_TRAIN_ID){
                    result.setSpecialType(DollarExpressConst.Common.SPECIAL_TYPE_GOLD_TRAIN);
                }
            }

            if(addDollar){
                //dollar icon的配置
                DollarExpressIconCfg dollarIconCfg = GameDataManager.getDollarExpressIconCfg(DollarExpressConst.Icon.DOLLAR_ID);
                if(dollarIconCfg != null){
                    result.setAwardTimes(result.getAwardTimes() + dollarIconCount * dollarIconCfg.getDoubling());
                }
            }
        }

        //大于3个才会触发免费游戏
        if(freeIconCount >= 3){
            result.setSpecialType(DollarExpressConst.Common.SPECIAL_TYPE_FREE);
        }

        return result;
    }

    /**
     * 检查中奖情况
     * @param arr
     */
    private List<AwardLineInfo> checkLineAward(int[] arr){
        List<AwardLineInfo> awardLineInfoList = new ArrayList<>();
        log.debug("检查中奖线情况 arr = {}",arr);

        for(Map.Entry<Integer,DollarExpressLineCfg> en : GameDataManager.getDollarExpressLineCfgMap().entrySet()){
            //中奖线配置
            DollarExpressLineCfg lineCfg = en.getValue();
            if(lineCfg.getYLine() == null || lineCfg.getYLine().isEmpty()){
                continue;
            }

            SameInfo sameInfo = new SameInfo();

            int last = lineCfg.getYLine().size()-1;

            //标记是否中奖
            boolean award = false;
            //中奖线中最后一个图标的坐标
            int lastAwardIconIndex = 0;
            for(int i=0;i<last;i++){
                int index1 = lineCfg.getYLine().get(i);
                int index2 = lineCfg.getYLine().get(i+1);

                sameInfo = iconSame(sameInfo,arr[index1],arr[index2]);

                if(sameInfo.isSame()){
                    //如果最后一个图标也相同，才计算中奖
                    if(i == lineCfg.getYLine().size()-1){
                        award = true;
                        lastAwardIconIndex = i;
                        break;
                    }
                }else {
                    //i > 0表示最低都是2连
                    if(i > 0){
                        award = true;
                        lastAwardIconIndex = i;
                        break;
                    }
                }
            }

            //如果这条线中奖
            if(award){
                DollarExpressIconCfg iconCfg = GameDataManager.getDollarExpressIconCfg(sameInfo.getBaseIconId());
                if(iconCfg != null && iconCfg.getPayout() != null && !iconCfg.getPayout().isEmpty()){
                    //获取对应的倍数
                    Integer times = iconCfg.getPayout().get(lastAwardIconIndex);
                    if(times == null || times < 1){
                        continue;
                    }
                    addResultLineInfo(awardLineInfoList,lineCfg.getSid(),lineCfg.getYLine().subList(0,lastAwardIconIndex),times);
                    log.debug("添加{}连赔率 lineSid = {},iconSid = {},payout = {}",lastAwardIconIndex+1,lineCfg.getSid(),iconCfg.getSid(),times);
                }
            }
        }
        return awardLineInfoList;
    }


    /**
     * 判断两个图片是否相同，包含wild
     * @param iconId1
     * @param iconId2
     * @return
     */
    private SameInfo iconSame(SameInfo sameInfo, int iconId1, int iconId2){
        BigDecimal icon_1_times = getWildTimes(iconId1);
        BigDecimal icon_2_times = getWildTimes(iconId2);

        boolean normal1 = iconId1 <= DollarExpressConst.Icon.NORMAL_TYPE_MAX_ID;
        boolean normal2 = iconId2 <= DollarExpressConst.Icon.NORMAL_TYPE_MAX_ID;

        if(iconId1 == iconId2){
            //icon_1_times如果为null，那么icon_2_times也肯定为null，表示均为普通图标，否则均为wild
            if(icon_1_times == null){
                if(normal1){
                    sameInfo.setBaseIconId(iconId1);
                    log.debug("均为普通图标 iconId1 = {},iconId2 = {}, same = true",iconId1,iconId2);
                }
            }else {
                sameInfo.addTimes(icon_1_times);
                sameInfo.addTimes(icon_2_times);
                log.debug("均为wild图标 iconId1 = {},iconId2 = {}, icon_1_times = {},icon_2_times = {},same = true",iconId1,iconId2,icon_1_times,icon_2_times);
            }
            sameInfo.setSame(true);
            return sameInfo;
        }

        //如果两个图标不一样，则判断有没有wild
        if(icon_1_times == null){
            if(icon_2_times == null){
                //俩都为null，表示都不是wild图标
                sameInfo.setSame(false);
//                log.debug("均为普通图标 iconId1 = {},iconId2 = {}, same = false",iconId1,iconId2);
            }else {
                //icon_2_times不为null，表示iconId2是wild
                //如果 1 是普通图标
                if(normal1){
                    sameInfo.setSame(true);
                    sameInfo.setBaseIconId(iconId1);
                    sameInfo.addTimes(icon_2_times);
                    log.debug("iconId1 = {} 是普通图标,iconId2 = {} 是wild图标, same = true",iconId1,iconId2);
                }
            }
            return sameInfo;
        }else {
            if(icon_2_times == null){
                if(normal2){
                    //1不为null，2为null，表示 1是wild，2是普通图标
                    sameInfo.addTimes(icon_1_times);
                    sameInfo.setBaseIconId(iconId2);
                    sameInfo.setSame(true);
                    log.debug("iconId1 = {} 是wild图标,iconId2 = {} 是普通图标, same = true",iconId1,iconId2);
                }
            }else {
                //俩都不为null，表示都是wild图标
                sameInfo.addTimes(icon_1_times);
                sameInfo.addTimes(icon_2_times);
                sameInfo.setSame(true);
                log.debug("均为wild图标 iconId1 = {} ,iconId2 = {} , same = true",iconId1,iconId2);
            }
            return sameInfo;
        }
    }

    /**
     * 判断是不是火车icon
     * @param iconId
     * @return
     */
    private boolean isTrainId(int iconId){
        return iconId == DollarExpressConst.Icon.RED_TRAIN_ID ||
                iconId == DollarExpressConst.Icon.GREEN_TRAIN_ID ||
                iconId == DollarExpressConst.Icon.BLUE_TRAIN_ID ||
                iconId == DollarExpressConst.Icon.PURPLE_TRAIN_ID;
    }

    /**
     * 判断是不是wild，如果是就返回它的倍率
     * @param iconId
     * @return
     */
    private BigDecimal getWildTimes(int iconId){
        if(iconId == DollarExpressConst.Icon.WILD_1_ID || iconId == DollarExpressConst.Icon.WILD_2_ID || iconId == DollarExpressConst.Icon.WILD_5_ID){
            DollarExpressIconCfg iconCfg = GameDataManager.getDollarExpressIconCfg(iconId);
            if(iconCfg != null){
                return BigDecimal.valueOf(iconCfg.getDoubling());
            }
        }
        return null;
    }

    /**
     * 组装中奖线信息
     * @param awardLineInfoList
     * @param sid
     * @param indexList
     * @param payout
     */
    private void addResultLineInfo(List<AwardLineInfo> awardLineInfoList, int sid, List<Integer> indexList, int payout){
        AwardLineInfo awardLineInfo = new AwardLineInfo();
        awardLineInfo.setId(sid);
        awardLineInfo.setTimes(payout);
        awardLineInfo.setIndexList(indexList);
        awardLineInfoList.add(awardLineInfo);
    }
}
