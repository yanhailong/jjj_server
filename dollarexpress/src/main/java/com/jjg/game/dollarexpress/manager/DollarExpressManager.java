package com.jjg.game.dollarexpress.manager;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.dao.PoolDao;
import com.jjg.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.dollarexpress.data.PlayerGameData;
import com.jjg.game.dollarexpress.data.PropData;
import com.jjg.game.dollarexpress.data.PropInfo;
import com.jjg.game.sample.*;
import com.jjg.game.dollarexpress.data.*;
import com.jjg.game.dollarexpress.pb.ResultLineInfo;
import com.jjg.game.dollarexpress.service.PlayerService;
import com.jjg.game.sample.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏管理器
 *  保存bean
 *  保存该节点上的玩家数据
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressManager implements ConfigExcelChangeListener {
    private Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    private PlayerService playerService;
    @Autowired
    private PoolDao poolDao;

    public Map<Long, PlayerGameData> gameDataMap = new ConcurrentHashMap<>();

    //缓存的control表的概率数据， configSid -> PropInfo
    private Map<Integer, PropInfo<String>> controlPropDataMap = new HashMap<>();
    //缓存的show表计算后的一些数据 axle-> colum ->
    private Map<Integer,Map<Integer,List<Integer>>> showConfigDataMap = new HashMap<>();
    //缓存的resultShow表计算后的一些数据 specail.type -> PropInfo
    private Map<Integer,PropInfo<Integer>> resultShowConfigDataMap = new HashMap<>();
    //resultshow表 , sid -> Index -> iconId
    private Map<Integer,Map<Integer,Integer>> resultShowIconMap = new HashMap<>();
    //icon表中不中奖的权重数据
    private Map<Integer,PropInfo<Integer>> iconNoWinMap = new HashMap<>();

    /**
     * 初始化
     */
    public void init(){
        //奖池初始化
        poolDao.init();
        //缓存control表计算后的一些数据
        initControlConfigData();
        //缓存show表计算后的一些数据
        initShowConfigData();
        //缓存resultShow表计算后的一些数据
        initResultShowConfigData();
        //缓存icon表的以及计算所需的数据
        initIconConfigData();
    }

    /**
     * 选择场次
     * @param playerController
     * @param wareId
     * @return
     */
    public GameRunInfo chooseWare(PlayerController playerController, int wareId){
        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS,playerController.playerId());
        try{
            DollarExpressWareHouseConfig config = DollarExpressWareHouseConfig.factory.getSample(wareId);
            if(config == null){
                gameRunInfo.setCode(Code.NOT_FOUND);
                log.debug("未找到对应的配置 gameType = {},playerId:{}, wardId = {}", playerController.player.getGameType(),playerController.playerId(),wareId);
                return gameRunInfo;
            }

            PlayerGameData playerGameData = gameDataMap.computeIfAbsent(playerController.playerId(), k -> new PlayerGameData(playerController));
            playerGameData.setGameType(playerController.player.getGameType());
            playerGameData.setWareId(config.sid);

            List<Long> list = List.of(config.stake_1, config.stake_2, config.stake_3, config.stake_4, config.stake_5, config.stake_6, config.stake_7, config.stake_8, config.stake_9, config.stake_10);
            gameRunInfo.setLongList(list);
        }catch (Exception e){
            log.error("",e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 开始游戏
     * @param playerId
     * @param stake
     * @return
     */
    public GameRunInfo startGame(long playerId, long stake){
        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS, playerId);
        try{
            PlayerGameData playerGameData = gameDataMap.get(playerId);
            if(playerGameData == null){
                gameRunInfo.setCode(Code.ERROR_REQ);
                log.debug("未找到玩家游戏数据2 gameType = {},playerId:{}", playerGameData.getGameType(),playerId);
                return gameRunInfo;
            }

            //获取配置
            DollarExpressWareHouseConfig config = DollarExpressWareHouseConfig.getDollarExpressWareHouseConfig(playerGameData.getWareId());
            if(config == null){
                gameRunInfo.setCode(Code.NOT_FOUND);
                log.debug("未找到对应的配置2 gameType = {},playerId:{}", playerGameData.getGameType(),playerId);
                return gameRunInfo;
            }

            //检查stake是否为配置数据
            boolean validStake = checkStake(config, stake);
            if(!validStake){
                gameRunInfo.setCode(Code.PARAM_ERROR);
                log.debug("stake参数不在配置项内 gameType = {},playerId:{}, wardId = {}, stake = {}", playerGameData.getGameType(),playerId,config.sid,stake);
                return gameRunInfo;
            }

            //从control表 获取 axle 或者 special 配置id
            CommonResult<String> controlConfigAxleResult = getControlConfigAxle(playerGameData,config);
            if(!controlConfigAxleResult.success()){
                gameRunInfo.setCode(controlConfigAxleResult.code);
                return gameRunInfo;
            }

            //扣除金额
            CommonResult<Player> result = playerService.addGold(playerId, -stake, "startDollarExpressGame");
            if(!result.success()){
                gameRunInfo.setCode(result.code);
                log.debug("扣除玩家金币失败 gameType = {},playerId:{}, wardId = {}, stake = {}", playerGameData.getGameType(),playerId,config.sid,stake);
                return gameRunInfo;
            }

            //根据配置出奖
            String[] axleArr = controlConfigAxleResult.data.split("_");
            int[] resultArr = null;
            if(DollarExpressConst.COMMON.AXLE_PREFIX.equals(axleArr[0])){
                Map<Integer, List<Integer>> tempMap = this.showConfigDataMap.get(Integer.parseInt(axleArr[1]));
                resultArr = genNormalLottery(tempMap);
            }else if(DollarExpressConst.COMMON.SPECIAL_PREFIX.equals(axleArr[0])){
                int specialId = Integer.parseInt(axleArr[1]);
                PropInfo<Integer> propInfo = this.resultShowConfigDataMap.get(specialId);
                resultArr = genSpecialLottery(propInfo);
                gameRunInfo.setSpecialId(specialId);
            }else {
                log.debug("出奖配置错误 gameType = {},playerId:{}, wardId = {}, stake = {},axleArr[0] = {}",playerGameData.getGameType(),playerId,config.sid,stake, axleArr[0]);
                gameRunInfo.setCode(Code.FAIL);
                return gameRunInfo;
            }

            if(resultArr == null){
                log.debug("出奖失败 gameType = {},playerId:{}, wardId = {}, stake = {},axleArr[0] = {}",playerGameData.getGameType(),playerId,config.sid,stake, axleArr[0]);
                gameRunInfo.setCode(Code.FAIL);
                return gameRunInfo;
            }

            //检查中奖情况
            List<ResultLineInfo> resultLineInfoList = checkAward(stake / config.multiplier,resultArr);

            //组装返回结果
            gameRunInfo.setResultLineInfoList(resultLineInfoList);
            gameRunInfo.setIntArray(resultArr);
        }catch (Exception e){
            log.error("",e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    /**
     * 生成普通奖励
     * @param map
     */
    private int[] genNormalLottery(Map<Integer, List<Integer>> map){
        int[] arr = new int[DollarExpressConst.COMMON.ALL_CION_COUNT];
        int index = 0;
        for(int i=1;i<=map.size();i++){
            List<Integer> temList = map.get(i);
            //随机生成起始位置
            int rand = RandomUtils.randomInt(temList.size());
            for(int j=0;j<DollarExpressConst.COMMON.COLUM_ICON_COUNT;j++){
                int num = rand + j;
                num = num % temList.size();

                arr[index] = temList.get(num);
                index++;
            }
        }
        return arr;
    }

    /**
     * 生成特殊奖励
     * @param propInfo
     * @return
     */
    private int[] genSpecialLottery(PropInfo<Integer> propInfo){
        //根据权重获取对应的特殊玩法的图标集合
        int rand = RandomUtils.randomInt(propInfo.getSum());
        PropData<Integer> propData = propInfo.getPropMap().values().stream()
                .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
                .findFirst()
                .orElse(null);
        if(propData == null){
            log.debug("根据权重没有找到对应的 propData， sum = {},rand = {}",propInfo.getSum(),rand);
            return null;
        }

        Map<Integer, Integer> iconMap = this.resultShowIconMap.get(propData.getKey());
        if(iconMap == null){
            log.debug("根据权重没有找到对应的 iconMap， sid = {}",propData.getKey());
            return null;
        }
        return fill(iconMap);
    }

    /**
     * 根据给定的位置图标，填补剩余位置图标，要求填补的位置不能有新的中奖
     * @param iconMap
     * @return
     */
    private int[] fill(Map<Integer, Integer> iconMap){
        PropInfo<Integer> propInfo = iconNoWinMap.get(DollarExpressConst.COMMON.ICON_NORMAL_TYPE);
        if(propInfo == null){
            return null;
        }

        //首先填充预制的图标
        int[] arr = new int[DollarExpressConst.COMMON.ALL_CION_COUNT];
        for(Map.Entry<Integer, Integer> en : iconMap.entrySet()){
            arr[en.getKey()] = en.getValue();
        }

        //第一列已经出现的图标
        Set<Integer> existIconSet = new HashSet<>();
        //填充第一列
        genByPropInfo(arr,1,propInfo,existIconSet);

        PropInfo<Integer> colum2PropInfo = new PropInfo<>();
        int begin = 0;
        int end = 0;
        //排除已经出现的图标后，重新计算权重
        for(DollarExpressIconConfig c : DollarExpressIconConfig.factory.getAllSamples()){
            if(c.noWinning < 1){
                continue;
            }
            if(c.type != DollarExpressConst.COMMON.ICON_NORMAL_TYPE){
                continue;
            }
            if(existIconSet.contains(c.sid)){
                continue;
            }
            begin = end;
            end += c.noWinning;

            PropData<Integer> tempPropData = new PropData<>(c.sid, begin, end);
            colum2PropInfo.getPropMap().put(c.sid,tempPropData);
            colum2PropInfo.setSum(tempPropData.getEnd());
        }
        //填充第二列
        genByPropInfo(arr,2,colum2PropInfo,null);

        //填充剩余列
        for(int i=3;i<=DollarExpressConst.COMMON.COLUMS_COUNT;i++){
            genByPropInfo(arr,i,propInfo,null);
        }
        return arr;
    }

    /**
     * 根据权重填充图标
     * @param arr
     * @param columId
     * @param propInfo
     */
    private void genByPropInfo(int[] arr,int columId,PropInfo<Integer> propInfo,Set<Integer> existIconSet){
        int locationBegin = (columId - 1) * DollarExpressConst.COMMON.COLUM_ICON_COUNT;
        int locationEnd = locationBegin + DollarExpressConst.COMMON.COLUM_ICON_COUNT - 1;
        for(int i=locationBegin;i<=locationEnd;i++){
            //如果该位置上已经有图标，就不填充
            if(arr[i] > 1){
                continue;
            }
            //根据icon表的nowin权重填充
            int rand = RandomUtils.randomInt(propInfo.getSum());
            PropData<Integer> propData = propInfo.getPropMap().values().stream()
                    .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
                    .findFirst()
                    .orElse(null);
            if(propData == null){
                continue;
            }
            if(existIconSet != null){
                existIconSet.add(propData.getKey());
            }
            arr[i] = propData.getKey();
        }
    }

    /**
     * 根据奖池偏差获取轴配置
     * @param playerGameData
     * @return
     */
    private CommonResult<String> getControlConfigAxle(PlayerGameData playerGameData,DollarExpressWareHouseConfig wareHouseConfig){
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);

        //获取奖池
        Long poolGold = poolDao.getByWareId(playerGameData.getWareId());
        if(poolGold == null){
            log.debug("该奖池不存在 gameType = {},wareId = {},playerId = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId());
            result.code = Code.FAIL;
            return result;
        }

        //计算差值
        long diff = poolGold - wareHouseConfig.basicWarehouse;
        DollarExpressControlConfig config = getByPoolDiff(diff);
        if(config == null){
            log.debug("该配置不存在 gameType = {},wareId = {},playerId = {},diff = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId(),diff);
            result.code = Code.NOT_FOUND;
            return result;
        }


        //根据概率获取轴配置
        PropInfo<String> propInfo = controlPropDataMap.get(config.sid);
        int rand = RandomUtils.randomInt(propInfo.getSum());

        PropData<String> propData = propInfo.getPropMap().values().stream()
                .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
                .findFirst()
                .orElse(null);

        if(propData == null){
            log.debug("随机无法获取的propData， gameType = {},wareId = {},playerId = {},rand = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId(),rand);
            result.code = Code.NOT_FOUND;
            return result;
        }

        result.data = propData.getKey();
        return result;
    }

    /**
     * 根据奖池偏差值获取 DollarExpressControlConfig 配置
     * @param diff
     * @return
     */
    private DollarExpressControlConfig getByPoolDiff(long diff){
        for(DollarExpressControlConfig c : DollarExpressControlConfig.factory.getAllSamples()){
            if(diff >= c.entryConditionMin && diff <= c.entryConditionMax){
                return c;
            }
        }
        return null;
    }

    /**
     * 检查中奖情况
     * @param ante 底注
     * @param arr
     */
    private List<ResultLineInfo> checkAward(long ante,int[] arr){
        List<ResultLineInfo> awardLineInfoList = new ArrayList<>();
        for(DollarExpressLineConfig c : DollarExpressLineConfig.factory.getAllSamples()){
            if(arr[c.yLine1] != arr[c.yLine2]){
                continue;
            }

            int iconId = arr[c.yLine2];
            if(arr[c.yLine2] != arr[c.yLine3]){
                //检查2连的倍率
                DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(iconId);
                if(iconConfig != null && iconConfig.payout_2 > 0){
                    ResultLineInfo resultLineInfo = new ResultLineInfo();
                    resultLineInfo.id = c.sid;
                    resultLineInfo.times = iconConfig.payout_2;
                    resultLineInfo.indexList = List.of(c.yLine1,c.yLine2,c.yLine3,c.yLine4,c.yLine5);
                    resultLineInfo.winGold = ante * resultLineInfo.times;
                    awardLineInfoList.add(resultLineInfo);
                }
                continue;
            }

            if(arr[c.yLine3] != arr[c.yLine4]){
                //检查3连的倍率
                DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(iconId);
                if(iconConfig != null && iconConfig.payout_3 > 0){
                    ResultLineInfo resultLineInfo = new ResultLineInfo();
                    resultLineInfo.id = c.sid;
                    resultLineInfo.times = iconConfig.payout_3;
                    resultLineInfo.indexList = List.of(c.yLine1,c.yLine2,c.yLine3,c.yLine4,c.yLine5);
                    resultLineInfo.winGold = ante * resultLineInfo.times;
                    awardLineInfoList.add(resultLineInfo);
                }
                continue;
            }

            if(arr[c.yLine4] != arr[c.yLine5]){
                //检查4连的倍率
                DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(iconId);
                if(iconConfig != null && iconConfig.payout_4 > 0){
                    ResultLineInfo resultLineInfo = new ResultLineInfo();
                    resultLineInfo.id = c.sid;
                    resultLineInfo.times = iconConfig.payout_4;
                    resultLineInfo.indexList = List.of(c.yLine1,c.yLine2,c.yLine3,c.yLine4,c.yLine5);
                    resultLineInfo.winGold = ante * resultLineInfo.times;
                    awardLineInfoList.add(resultLineInfo);
                }
                continue;
            }

            //检查5连的倍率
            DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(iconId);
            if(iconConfig != null && iconConfig.payout_5 > 0){
                ResultLineInfo resultLineInfo = new ResultLineInfo();
                resultLineInfo.id = c.sid;
                resultLineInfo.times = iconConfig.payout_5;
                resultLineInfo.indexList = List.of(c.yLine1,c.yLine2,c.yLine3,c.yLine4,c.yLine5);
                resultLineInfo.winGold = ante * resultLineInfo.times;
                awardLineInfoList.add(resultLineInfo);
            }
        }
        return awardLineInfoList;
    }


    /**
     * 创建 GameData
     * @param playerController
     * @return
     */
    public PlayerGameData createPlayerGameData(PlayerController playerController){
        return gameDataMap.computeIfAbsent(playerController.playerId(), k -> new PlayerGameData(playerController));
    }

    /**
     * 检查stake是否为配置数据
     * @param config
     * @param stake
     * @return
     */
    private boolean checkStake(DollarExpressWareHouseConfig config,long stake){
        if(config.stake_1 == stake || config.stake_2 == stake || config.stake_3 == stake
                || config.stake_4 == stake || config.stake_5 == stake || config.stake_6 == stake
                || config.stake_7 == stake || config.stake_8 == stake || config.stake_9 == stake
                || config.stake_10 == stake){
            return true;
        }
        return false;
    }

    @Override
    public void change(String className) {
        if(className.equalsIgnoreCase(DollarExpressControlConfig.class.getSimpleName())){
            initControlConfigData();
        }else if(className.equalsIgnoreCase(DollarExpressShowConfig.class.getSimpleName())){
            initShowConfigData();
        }else if(className.equalsIgnoreCase(DollarExpressResultShowConfig.class.getSimpleName())){
            initResultShowConfigData();
        }else if(className.equalsIgnoreCase(DollarExpressIconConfig.class.getSimpleName())){
            initIconConfigData();
        }
    }

    /**
     * 缓存control表计算后的一些数据
     */
    private void initControlConfigData(){
        try{
            //取出类似 axle_1 或者 special_1 这样的字段名
            List<Field> controlConfigAxleFieldNameList = new ArrayList<>();
            List<Field> controlConfigSpecialFieldNameList = new ArrayList<>();
            Field[] declaredFields = DollarExpressControlConfig.class.getDeclaredFields();
            for(Field field : declaredFields){
                String fieldName = field.getName();
                if(fieldName.startsWith(DollarExpressConst.COMMON.AXLE_PREFIX)){
                    field.setAccessible(true);
                    controlConfigAxleFieldNameList.add(field);
                }else if(fieldName.startsWith(DollarExpressConst.COMMON.SPECIAL_PREFIX)){
                    field.setAccessible(true);
                    controlConfigSpecialFieldNameList.add(field);
                }
            }


            //计算出每个元素在随机时所在的区间范围
            Map<Integer,PropInfo<String>> tempControlPropDataMap = new HashMap<>();
            for(DollarExpressControlConfig c : DollarExpressControlConfig.factory.getAllSamples()){
                PropInfo<String> propInfo = tempControlPropDataMap.computeIfAbsent(c.getSid(), k -> new PropInfo<>());

                Map<String, PropData<String>> cMap = propInfo.getPropMap();

                //起始值
                int begin = 0;
                //结束值
                int end = 0;

                for(Field aField : controlConfigAxleFieldNameList){
                    int value = (int)aField.get(c);
                    String fieldName = aField.getName();

                    cMap.put(fieldName,new PropData<>(fieldName,begin,end += value));
                    begin = end;
                }

                for(Field sField : controlConfigSpecialFieldNameList){
                    int value = (int)sField.get(c);
                    String fieldName = sField.getName();

                    cMap.put(fieldName,new PropData<>(fieldName,begin,end += value));
                    begin = end;
                }
                propInfo.setSum(end);
            }

            this.controlPropDataMap = tempControlPropDataMap;
            log.debug("已完成更新 control 配置缓存数据");
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 缓存show表计算后的一些数据
     */
    private void initShowConfigData(){
        try{
            //取出类似 colum_1_1 这样格式的字段名
            List<Field> showConfigColumFieldNameList = new ArrayList<>();

            Field[] declaredFields = DollarExpressShowConfig.class.getDeclaredFields();
            for(Field field : declaredFields){
                String fieldName = field.getName();
                if(fieldName.startsWith(DollarExpressConst.COMMON.COLUM_PREFIX)){
                    field.setAccessible(true);
                    showConfigColumFieldNameList.add(field);
                }
            }

            Map<Integer,Map<Integer,List<Integer>>> tempShowConfigDataMap = new HashMap<>();
            for(Field field : showConfigColumFieldNameList){
                String name = field.getName();
                String[] arr = name.split("_");

                int axle = Integer.parseInt(arr[1]);
                int columIndex = Integer.parseInt(arr[2]);

                Map<Integer, List<Integer>> tempMap = tempShowConfigDataMap.computeIfAbsent(axle, k -> new HashMap<>());
                List<Integer> tempList = tempMap.computeIfAbsent(columIndex, k -> new ArrayList<>());

                for(DollarExpressShowConfig c : DollarExpressShowConfig.factory.getAllSamples()){
                    int iconId = (int)field.get(c);
                    tempList.add(iconId);
                }
            }

            this.showConfigDataMap = tempShowConfigDataMap;
            log.debug("已完成更新 show 配置缓存数据");
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 缓存resultShow表计算后的一些数据
     */
    private void initResultShowConfigData(){
        try{
            //取出类似 icon_1 这样格式的字段名
            List<Field> resultShowConfigIconFieldNameList = new ArrayList<>();

            Field[] declaredFields = DollarExpressResultShowConfig.class.getDeclaredFields();
            for(Field field : declaredFields){
                String fieldName = field.getName();
                if(fieldName.startsWith(DollarExpressConst.COMMON.ICON_PREFIX)){
                    field.setAccessible(true);
                    resultShowConfigIconFieldNameList.add(field);
                }
            }

            //计算概率区间
            Map<Integer,PropInfo<Integer>> tempResultShowConfigDataMap = new HashMap<>();
            //缓存位置上的特殊图标
            Map<Integer,Map<Integer,Integer>> tempResultShowIconMap = new HashMap<>();

            for(DollarExpressResultShowConfig c : DollarExpressResultShowConfig.factory.getAllSamples()){
                int begin = 0;

                PropInfo<Integer> info = tempResultShowConfigDataMap.get(c.type);
                if(info == null){
                    info = new PropInfo<>();
                    tempResultShowConfigDataMap.put(c.type, info);
                }else {
                    begin = info.getSum();
                }

                info.setSum(info.getSum() + c.weight);
                info.getPropMap().put(c.sid,new PropData<>(c.sid,begin,info.getSum()));
                //------------------------------
                //缓存大于0的iconId
                for(Field f : resultShowConfigIconFieldNameList){
                    int v = (int)f.get(c);
                    if(v > 0){
                        int index = Integer.parseInt(f.getName().split("_")[1]);

                        Map<Integer, Integer> tempMap = tempResultShowIconMap.computeIfAbsent(c.sid, k -> new HashMap<>());
                        tempMap.put(index,v);
                    }
                }
            }
            this.resultShowConfigDataMap = tempResultShowConfigDataMap;
            this.resultShowIconMap = tempResultShowIconMap;
            log.debug("已完成更新 resultShow 配置缓存数据");
        }catch (Exception e){
            log.error("",e);
        }
    }

    /**
     * 缓存icon表的以及计算所需的数据
     */
    private void initIconConfigData() {
        try{
            Map<Integer,PropInfo<Integer>> tempIconNoWinMap = new HashMap<>();
            for(DollarExpressIconConfig c : DollarExpressIconConfig.factory.getAllSamples()){
                if(c.noWinning > 0){
                    PropInfo<Integer> propInfo = tempIconNoWinMap.computeIfAbsent(c.type, k -> new PropInfo<>());

                    PropData<Integer> propData = new PropData<>(c.sid, propInfo.getSum(), propInfo.getSum() + c.noWinning);
                    propInfo.getPropMap().put(c.sid,propData);
                    propInfo.setSum(propData.getEnd());
                }
            }
            this.iconNoWinMap = tempIconNoWinMap;
            log.debug("已完成更新 icon 配置缓存数据");
        }catch (Exception e){
            log.error("",e);
        }
    }
}
