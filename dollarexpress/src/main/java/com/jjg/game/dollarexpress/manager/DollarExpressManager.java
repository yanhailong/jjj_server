package com.jjg.game.dollarexpress.manager;

import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.dao.DollarExpressPoolDao;
import com.jjg.game.dollarexpress.data.*;
import com.jjg.game.dollarexpress.sample.GameDataManager;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressControlCfg;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressResultWeightCfg;
import com.jjg.game.dollarexpress.sample.bean.DollarExpressWareHouseCfg;
import com.jjg.game.dollarexpress.service.DollarExpressPlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏逻辑处理器
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressManager {
    private Logger log = LoggerFactory.getLogger(getClass());


    @Autowired
    private DollarExpressPlayerService dollarExpressPlayerService;
    @Autowired
    private DollarExpressPoolDao dollarExpressPoolDao;
//    @Autowired
//    private DollarExpressLogger logger;

    //control表中的概率计算缓存
    public Map<Integer,PropInfo> controlPropMap = new HashMap<>();

    public int[] controlTrainPropMinMin = null;
    public int[] controlSafeBoxPropMinMin = null;
    public int[] controlFreePropMinMin = null;
    public int[] controlGoldTrainPropMinMin = null;


    public Map<Long, PlayerGameData> gameDataMap = new ConcurrentHashMap<>();

    /**
     * 初始化
     */
    public void init(){
        //奖池初始化
        dollarExpressPoolDao.initPool();
        //缓存control表计算后的一些数据
//        initControlConfigData();
//        //缓存show表计算后的一些数据
//        initShowConfigData();
//        //缓存resultShow表计算后的一些数据
//        initResultShowConfigData();
//        //缓存icon表的以及计算所需的数据
//        initIconConfigData();
    }

    /**
     * 开始游戏
     * @return
     */
    public GameRunInfo startGame(long playerId, long stake){
        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS, playerId);
        try{
            PlayerGameData playerGameData = gameDataMap.get(playerId);
            if(playerGameData == null){
                gameRunInfo.setCode(Code.ERROR_REQ);
                log.debug("未找到玩家游戏数据2 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
                return gameRunInfo;
            }

//            if(playerGameData.getSpecialType() > 0){
//                gameRunInfo.setCode(Code.ERROR_REQ);
//                log.debug("当前处于特殊模式 gameType = {},playerId:{},specialId={}", playerGameData.getGameType(),playerId,playerGameData.getSpecialType());
//                return gameRunInfo;
//            }

            //获取配置
//            DollarExpressWareHouseCfg wareHouseCfg = GameDataManager.getDollarExpressWareHouseCfg(playerGameData.getWareId());
//            if(wareHouseCfg == null || wareHouseCfg.getBetList() == null){
//                gameRunInfo.setCode(Code.NOT_FOUND);
//                log.debug("未找到对应的配置2 gameType = {},playerId:{}", playerGameData.getGameType(),playerId);
//                return gameRunInfo;
//            }
//
//            //检查stake是否为配置数据
//            boolean validStake = wareHouseCfg.getBetList().stream().anyMatch(b -> b == stake);
//            if(!validStake){
//                gameRunInfo.setCode(Code.PARAM_ERROR);
//                log.debug("stake参数不在配置项内 gameType = {},playerId:{}, wardId = {}, stake = {}", playerGameData.getGameType(),playerId,playerGameData.getWareId(),stake);
//                return gameRunInfo;
//            }
//
//            //是否应该选择免费类型
//            if(playerGameData.getCanChooseFreeType().get()){
//                gameRunInfo.setCode(Code.FORBID);
//                log.debug("此时应该选择免费游戏类型，不应该正常玩游戏 gameType = {},playerId:{}, wardId = {}, stake = {}", playerGameData.getGameType(),playerId,playerGameData.getWareId(),stake);
//                return gameRunInfo;
//            }
//
//            //从control表 获取 axle 或者 special 配置id
//            CommonResult<Integer> controlConfigAxleResult = getControlConfigAxle(playerGameData,wareHouseCfg);
//            if(!controlConfigAxleResult.success()){
//                gameRunInfo.setCode(controlConfigAxleResult.code);
//                return gameRunInfo;
//            }
//
//            //扣除金额
//            CommonResult<Player> result = dollarExpressPlayerService.addGold(playerId, -stake, "startDollarExpressGame");
//            if(!result.success()){
//                gameRunInfo.setCode(result.code);
//                log.debug("扣除玩家金币失败 gameType = {},playerId:{}, wardId = {}, stake = {}", playerGameData.getGameType(),playerId,playerGameData.getWareId(),stake);
//                return gameRunInfo;
//            }
//            log.debug("玩家扣除金额成功 playerId ={}",playerId);
//
//            Player player = result.data;
//
//            //下注金额 除以100
//            long betValue = stake / config.getMultiplier();
//
//            //根据配置出奖
//            String[] axleArr = controlConfigAxleResult.data.split("_");
//            int specialType = 0;
//            if(DollarExpressConst.Common.AXLE_PREFIX.equals(axleArr[0])){  //普通模式
//                int axleId = Integer.parseInt(axleArr[1]);
//                log.debug("进入普通中奖模式 gameType = {},playerId:{}, wardId = {}, stake = {},axleId = {}",playerGameData.getGameType(),playerId,playerGameData.getWareId(),stake,axleId);
//
//                Map<Integer, List<Integer>> tempMap = this.showConfigDataMap.get(axleId);
//                gameRunInfo = genNormalLottery(gameRunInfo,tempMap);
////                gameRunInfo = normalAward(gameRunInfo,playerGameData,betValue,config);
//            }else if(DollarExpressConst.Common.SPECIAL_PREFIX.equals(axleArr[0])){  //特殊模式
//                specialType = Integer.parseInt(axleArr[1]);
//                log.debug("进入特殊中奖模式 gameType = {},playerId:{}, wardId = {}, stake = {},specialType = {}",playerGameData.getGameType(),playerId,playerGameData.getWareId(),stake,specialType);
//
//                PropInfo<Integer> propInfo = this.resultShowConfigDataMap.get(specialType);
//                gameRunInfo = genSpecialLottery(gameRunInfo,propInfo);
//
////                gameRunInfo = specialAward(gameRunInfo,playerGameData,betValue,specialType);
//            }else {
//                log.debug("出奖配置错误 gameType = {},playerId:{}, wardId = {}, stake = {},axleArr[0] = {}",playerGameData.getGameType(),playerId,config.getSid(),stake, axleArr[0]);
//                gameRunInfo.setCode(Code.FAIL);
//                return gameRunInfo;
//            }
//
//            if(gameRunInfo.getIntArray() == null){
//                log.debug("出奖失败 gameType = {},playerId:{}, wardId = {}, betValue = {},specialType = {}",playerGameData.getGameType(),playerGameData.playerId(),playerGameData.getWareId(),betValue, specialType);
//                gameRunInfo.setCode(Code.FAIL);
//                return gameRunInfo;
//            }
//
//            gameRunInfo = checkAllWard(gameRunInfo,playerGameData,betValue,config,specialType);
            //发送日志
//            logger.gameResult(player,gameRunInfo);
        }catch (Exception e){
            log.error("",e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }
//
//    /**
//     * 检查所有的奖励
//     * @param gameRunInfo
//     * @param playerGameData
//     * @param betValue
//     * @param config
//     * @param specialType
//     * @return
//     */
//    public GameRunInfo checkAllWard(GameRunInfo gameRunInfo,PlayerGameData playerGameData,long betValue,DollarExpressWareHouseConfig config,int specialType){
//        gameRunInfo = normalAward(gameRunInfo,playerGameData,betValue,config);
//        if(specialType > 0){
//            gameRunInfo = specialAward(gameRunInfo,playerGameData,betValue,specialType);
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 开出特殊结果，检查中奖信息
//     * @param gameRunInfo
//     * @param playerGameData
//     * @param betValue
//     * @param specialType
//     * @return
//     */
//    private GameRunInfo specialAward(GameRunInfo gameRunInfo, PlayerGameData playerGameData,long betValue,int specialType){
//        log.debug("检查特殊中奖情况");
//        gameRunInfo.setSpecialType(specialType);
//        playerGameData.setLastBetValue(betValue);
//
//        if(gameRunInfo.getSpecialType() > 0){
//            playerGameData.setSpecialType(gameRunInfo.getSpecialType());
//            playerGameData.setResultShowId(gameRunInfo.getResultShowId());
//            //检查特殊中奖
//            gameRunInfo = checkSpecial(gameRunInfo,playerGameData,betValue);
//            playerGameData.setGoldTrainInFree(gameRunInfo.isGoldTrainInFree());
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 开出普通结果，检查中奖信息
//     * @param gameRunInfo
//     * @param playerGameData
//     * @param stake
//     * @param config
//     * @return
//     */
//    public GameRunInfo normalAward(GameRunInfo gameRunInfo, PlayerGameData playerGameData,long stake,DollarExpressWareHouseConfig config){
//        if(gameRunInfo.getIntArray() == null){
//            log.debug("出奖失败 gameType = {},playerId:{}, wardId = {}, stake = {}",playerGameData.getGameType(),playerGameData.playerId(),config.getSid(),stake);
//            gameRunInfo.setCode(Code.FAIL);
//            return gameRunInfo;
//        }
//
//        //下注金额 除以100
//        long betValue = stake / config.getMultiplier();
//        playerGameData.setLastBetValue(betValue);
//
//        //中保险箱的话，不计算中奖线
//        if(gameRunInfo.getSpecialType() != DollarExpressConst.ResultShow.WIN_SAFE_BOX){
//            //所有的中奖线信息
//            List<ResultLineInfo> resultLineInfoList = checkLineAward(betValue,gameRunInfo.getIntArray());
//            //组装返回结果
//            gameRunInfo.setResultLineInfoList(resultLineInfoList);
//        }else {
//            log.debug("保险箱不计算中奖线");
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 选择免费游戏类型
//     * @param playerId
//     * @param type
//     * @return
//     */
//    public GameRunInfo chooseFreeGameType(long playerId, int type){
//        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS, playerId);
//        try{
//            PlayerGameData playerGameData = gameDataMap.get(playerId);
//            if(playerGameData == null){
//                gameRunInfo.setCode(Code.ERROR_REQ);
//                log.debug("未找到玩家游戏数据，选择免费游戏失败 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
//                return gameRunInfo;
//            }
//
//            boolean flag = playerGameData.getCanChooseFreeType().compareAndSet(true,false);
//            if(!flag){
//                gameRunInfo.setCode(Code.FORBID);
//                log.debug("没有选择权限，选择免费游戏失败 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
//                return gameRunInfo;
//            }
//
//            if(type == DollarExpressConst.Common.TRAIN_TYPE_WHEN_WIN_FREE){
//                TrainInfo trainInfo = addTrainInfo(playerGameData.isGoldTrainInFree() ? DollarExpressConst.ResultShow.FREE_TO_WIN_GOLD_TRAIN_MOUDLE : DollarExpressConst.ResultShow.FREE_TO_WIN_NORMAL_TRAIN_MOUDLE, playerId, playerGameData.getLastBetValue());
//                gameRunInfo.setTrainInfo(trainInfo);
//            }else {
//                int freeCount = DollarExpressResultShowConfig.getDollarExpressResultShowConfig(playerGameData.getResultShowId()).getFreetime();
//                playerGameData.setFreeCount(freeCount);
//                gameRunInfo.setFreeCount(freeCount);
//            }
//        }catch (Exception e){
//            log.error("",e);
//            gameRunInfo.setCode(Code.EXCEPTION);
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 投资游戏
//     * @param playerId
//     * @param areaId1
//     * @param areaId2
//     * @param areaId3
//     * @return
//     */
//    public GameRunInfo investArea(long playerId,int areaId1,int areaId2,int areaId3){
//        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS, playerId);
//        try{
//            PlayerGameData playerGameData = gameDataMap.get(playerId);
//            if(playerGameData == null){
//                gameRunInfo.setCode(Code.ERROR_REQ);
//                log.debug("未找到玩家游戏数据，投资游戏失败 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
//                return gameRunInfo;
//            }
//
//            //TODO 校验3个id是否合法
//
//            PropInfo<Integer> propInfo = resultShowConfigDataMap.get(DollarExpressConst.ResultShow.INVEST_MOUDLE);
//            if(propInfo == null){
//                gameRunInfo.setCode(Code.ERROR_REQ);
//                log.debug("未找到resultshow表中关于投资模式的配置，投资游戏失败 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
//                return gameRunInfo;
//            }
//
//            //根据权重获取对应的特殊玩法的图标集合
//            int rand = RandomUtils.randomInt(propInfo.getSum());
//            PropData<Integer> propData = propInfo.getPropMap().values().stream()
//                    .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
//                    .findFirst()
//                    .orElse(null);
//            if(propData == null){
//                log.debug("根据权重没有找到对应的 propData,投资游戏失败， sum = {},rand = {}",propInfo.getSum(),rand);
//                return gameRunInfo;
//            }
//
//            Map<Integer, Integer> iconMap = this.resultShowIconMap.get(propData.getKey());
//            if(iconMap == null){
//                log.debug("根据权重没有找到对应的 iconMap， sid = {}",propData.getKey());
//                return gameRunInfo;
//            }
//
//            Integer times1 = iconMap.get(areaId1);
//            Integer times2 = iconMap.get(areaId2);
//            Integer times3 = iconMap.get(areaId3);
//
//            gameRunInfo.setInvestGold(times1,times2,times3,playerGameData.getShowDollarValueAve());
//        }catch (Exception e){
//            log.error("",e);
//            gameRunInfo.setCode(Code.EXCEPTION);
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 使用免费次数，玩免费游戏
//     */
//    public GameRunInfo freeGame(long playerId){
//        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS, playerId);
//        try{
//            PlayerGameData playerGameData = gameDataMap.get(playerId);
//            if(playerGameData == null){
//                gameRunInfo.setCode(Code.ERROR_REQ);
//                log.debug("未找到玩家游戏数据，免费游戏失败 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
//                return gameRunInfo;
//            }
//
//            int freeCount = playerGameData.addFreeCount(-1);
//            if(freeCount < 0){
//                gameRunInfo.setCode(Code.FORBID);
//                log.debug("没有免费次数，免费游戏失败 gameType = {},wareId = {},playerId:{}", playerGameData.getGameType(),playerGameData.getWareId(),playerId);
//                return gameRunInfo;
//            }
//
//            DollarExpressWareHouseConfig config = DollarExpressWareHouseConfig.getDollarExpressWareHouseConfig(playerGameData.getWareId());
//
//            PropInfo<Integer> propInfo = resultShowConfigDataMap.get(DollarExpressConst.ResultShow.FREE_MOUDLE);
//            gameRunInfo = genSpecialLottery(gameRunInfo, propInfo);
//            gameRunInfo = checkAllWard(gameRunInfo,playerGameData,playerGameData.getLastBetValue(),config,DollarExpressConst.ResultShow.FREE_MOUDLE);
//        }catch (Exception e){
//            log.error("",e);
//            gameRunInfo.setCode(Code.EXCEPTION);
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 免费模式-中火车
//     */
//    public void freeToTrain(){
//
//    }
//
//    /**
//     * 生成普通奖励
//     * @param map
//     */
//    private GameRunInfo genNormalLottery(GameRunInfo gameRunInfo,Map<Integer, List<Integer>> map){
//        log.debug("开始生成普通奖励 playerId = {}",gameRunInfo.getPlayerId());
//        int[] arr = new int[DollarExpressConst.Common.ALL_CION_COUNT];
//        int index = 0;
//        for(int i=1;i<=map.size();i++){
//            log.debug("开始生成第 {} 列图标",i);
//            List<Integer> temList = map.get(i);
//            //随机生成起始位置
//            int rand = RandomUtils.randomInt(temList.size());
//            for(int j = 0; j< DollarExpressConst.Common.COLUM_ICON_COUNT; j++){
//                int num = rand + j;
//                num = num % temList.size();
//
//                arr[index] = temList.get(num);
//                index++;
//            }
//        }
//        gameRunInfo.setIntArray(arr);
//        return gameRunInfo;
//    }
//
//    /**
//     * 生成特殊奖励
//     * @param propInfo
//     * @return
//     */
//    private GameRunInfo genSpecialLottery(GameRunInfo gameRunInfo,PropInfo<Integer> propInfo){
//        log.debug("开始生成特殊奖励 playerId = {}",gameRunInfo.getPlayerId());
//        //根据权重获取对应的特殊玩法的图标集合
//        int rand = RandomUtils.randomInt(propInfo.getSum());
//        PropData<Integer> propData = propInfo.getPropMap().values().stream()
//                .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
//                .findFirst()
//                .orElse(null);
//        if(propData == null){
//            log.debug("根据权重没有找到对应的 propData， sum = {},rand = {}",propInfo.getSum(),rand);
//            return gameRunInfo;
//        }
//
//        Map<Integer, Integer> iconMap = this.resultShowIconMap.get(propData.getKey());
//        if(iconMap == null){
//            log.debug("根据权重没有找到对应的 iconMap， sid = {}",propData.getKey());
//            return gameRunInfo;
//        }
//        gameRunInfo.setIntArray(fill(iconMap));
//        gameRunInfo.setResultShowId(propData.getKey());
//        log.debug("随机到resultshow中的id为 {}",gameRunInfo.getResultShowId());
//        return gameRunInfo;
//    }
//
//    /**
//     * 根据给定的位置图标，填补剩余位置图标，要求填补的位置不能有新的中奖
//     * @param iconMap
//     * @return
//     */
//    private int[] fill(Map<Integer, Integer> iconMap){
//        PropInfo<Integer> propInfo = iconNoWinMap.get(DollarExpressConst.Icon.NORMAL_TYPE);
//        if(propInfo == null){
//            return null;
//        }
//
//        //首先填充预制的图标
//        int[] arr = new int[DollarExpressConst.Common.ALL_CION_COUNT];
//        for(Map.Entry<Integer, Integer> en : iconMap.entrySet()){
//            arr[en.getKey()] = en.getValue();
//        }
//
//        //第一列已经出现的图标
//        Set<Integer> existIconSet = new HashSet<>();
//        //填充第一列
//        genByPropInfo(arr,1,propInfo,existIconSet);
//
//        PropInfo<Integer> colum2PropInfo = new PropInfo<>();
//        int begin = 0;
//        int end = 0;
//        //排除已经出现的图标后，重新计算权重
//        for(DollarExpressIconConfig c : DollarExpressIconConfig.factory.getAllSamples()){
//            if(c.getNoWinning() < 1){
//                continue;
//            }
//            if(c.getType() != DollarExpressConst.Icon.NORMAL_TYPE){
//                continue;
//            }
//            if(existIconSet.contains(c.getSid())){
//                continue;
//            }
//            begin = end;
//            end += c.getNoWinning();
//
//            PropData<Integer> tempPropData = new PropData<>(c.getSid(), begin, end);
//            colum2PropInfo.getPropMap().put(c.getSid(),tempPropData);
//            colum2PropInfo.setSum(tempPropData.getEnd());
//        }
//        //填充第二列
//        genByPropInfo(arr,2,colum2PropInfo,null);
//
//        //填充剩余列
//        for(int i = 3; i<= DollarExpressConst.Common.COLUMS_COUNT; i++){
//            genByPropInfo(arr,i,propInfo,null);
//        }
//        return arr;
//    }
//
//    /**
//     * 根据权重填充图标
//     * @param arr
//     * @param columId
//     * @param propInfo
//     */
//    private void genByPropInfo(int[] arr,int columId,PropInfo<Integer> propInfo,Set<Integer> existIconSet){
//        int locationBegin = (columId - 1) * DollarExpressConst.Common.COLUM_ICON_COUNT;
//        int locationEnd = locationBegin + DollarExpressConst.Common.COLUM_ICON_COUNT - 1;
//        for(int i=locationBegin;i<=locationEnd;i++){
//            //如果该位置上已经有图标，就不填充
//            if(arr[i] > 1){
//                continue;
//            }
//            //根据icon表的nowin权重填充
//            int rand = RandomUtils.randomInt(propInfo.getSum());
//            PropData<Integer> propData = propInfo.getPropMap().values().stream()
//                    .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
//                    .findFirst()
//                    .orElse(null);
//            if(propData == null){
//                continue;
//            }
//            if(existIconSet != null){
//                existIconSet.add(propData.getKey());
//            }
//            arr[i] = propData.getKey();
//        }
//    }
//
    /**
     * 根据奖池偏差获取轴配置
     * @param playerGameData
     * @return
     */
    private CommonResult<Integer> getControlConfigAxle(PlayerGameData playerGameData,DollarExpressWareHouseCfg wareHouseCfg){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);

        //获取奖池
        Number poolGoldNumber = dollarExpressPoolDao.getByWareId(playerGameData.getGameType(), playerGameData.getWareId());
        if(poolGoldNumber == null){
            log.debug("该奖池不存在 gameType = {},wareId = {},playerId = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId());
            result.code = Code.FAIL;
            return result;
        }

        long poolGold = poolGoldNumber.longValue();
        //计算差值
        long diff = poolGold - wareHouseCfg.getBasicWarehouse();
        DollarExpressControlCfg config = getByPoolDiff(diff);
        if(config == null){
            log.debug("该配置不存在 gameType = {},wareId = {},playerId = {},diff = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId(),diff);
            result.code = Code.NOT_FOUND;
            return result;
        }

        PropInfo propInfo = controlPropMap.get(config.getSid());
        if(propInfo == null){
            log.debug("该配置不存在2 gameType = {},wareId = {},playerId = {},diff = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId(),diff);
            result.code = Code.NOT_FOUND;
            return result;
        }

        //根据概率获取轴配置
        int rand = RandomUtils.randomInt(propInfo.getSum());

        //是否中奖普通
        for(Map.Entry<Integer,int[]> en : propInfo.getPropMap().entrySet()){
            int[] arr = en.getValue();
            if(rand >= arr[0] && rand <= arr[1]){
                result.data = en.getKey();
                return result;
            }
        }

        result.code = Code.NOT_FOUND;
        log.debug("随机无法获取的propData， gameType = {},wareId = {},playerId = {},rand = {}",playerGameData.getGameType(),playerGameData.getWareId(),playerGameData.playerId(),rand);
        return result;
    }

    /**
     * 根据奖池偏差值获取 DollarExpressControlConfig 配置
     * @param diff
     * @return
     */
    private DollarExpressControlCfg getByPoolDiff(long diff){
        for(Map.Entry<Integer,DollarExpressControlCfg> en : GameDataManager.getDollarExpressControlCfgMap().entrySet()){
            DollarExpressControlCfg cfg = en.getValue();
            if(diff >= cfg.getEntryConditionMin() && diff <= cfg.getEntryConditionMax()){
                return cfg;
            }
        }
        return null;
    }
//
//    /**
//     * 检查中奖情况
//     * @param ante 底注
//     * @param arr
//     */
//    private List<ResultLineInfo> checkLineAward(long ante, int[] arr){
//        List<ResultLineInfo> awardLineInfoList = new ArrayList<>();
//        log.debug("检查中奖线情况 ante = {},arr = {}",ante,arr);
//        for(DollarExpressLineConfig c : DollarExpressLineConfig.factory.getAllSamples()){
//            SameInfo sameInfo = new SameInfo();
//            sameInfo = iconSame(sameInfo,arr[c.getYLine1()],arr[c.getYLine2()]);
//
//            if(!sameInfo.isSame()){
//                continue;
//            }
//
//            sameInfo = iconSame(sameInfo,arr[c.getYLine2()],arr[c.getYLine3()]);
//            if(!sameInfo.isSame()){
//                //检查2连的倍率
//                DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(sameInfo.getBaseIconId());
//                log.debug("检测到2连 lineSid = {},iconId = {}",c.getSid(),sameInfo.getBaseIconId());
//                if(iconConfig != null && iconConfig.getPayout_2() > 0){
//                    addResultLineInfo(awardLineInfoList,c.getSid(),List.of(c.getYLine1(),c.getYLine2()),iconConfig.getPayout_2(),ante);
//                    log.debug("添加2连赔率 lineSid = {},iconSid = {},payout = {}",c.getSid(),iconConfig.getSid(),iconConfig.getPayout_2());
//                }
//                continue;
//            }
//
//            sameInfo = iconSame(sameInfo,arr[c.getYLine3()],arr[c.getYLine4()]);
//            if(!sameInfo.isSame()){
//                //检查3连的倍率
//                DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(sameInfo.getBaseIconId());
//                log.debug("检测到3连 lineSid = {},iconId = {}",c.getSid(),sameInfo.getBaseIconId());
//                if(iconConfig != null && iconConfig.getPayout_3() > 0){
//                    addResultLineInfo(awardLineInfoList,c.getSid(),List.of(c.getYLine1(),c.getYLine2(),c.getYLine3()),iconConfig.getPayout_3(),ante);
//                    log.debug("添加3连赔率 lineSid = {},iconSid = {},payout = {}",c.getSid(),iconConfig.getSid(),iconConfig.getPayout_3());
//                }
//                continue;
//            }
//
//            sameInfo = iconSame(sameInfo,arr[c.getYLine4()],arr[c.getYLine5()]);
//            if(!sameInfo.isSame()){
//                //检查4连的倍率
//                DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(sameInfo.getBaseIconId());
//                log.debug("检测到4连 lineSid = {},iconId = {}",c.getSid(),sameInfo.getBaseIconId());
//                if(iconConfig != null && iconConfig.getPayout_4() > 0){
//                    addResultLineInfo(awardLineInfoList,c.getSid(),List.of(c.getYLine1(),c.getYLine2(),c.getYLine3(),c.getYLine4()),iconConfig.getPayout_4(),ante);
//                    log.debug("添加4连赔率 lineSid = {},iconSid = {},payout = {}",c.getSid(),iconConfig.getSid(),iconConfig.getPayout_4());
//                }
//                continue;
//            }
//
//            //检查5连的倍率
//            DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(sameInfo.getBaseIconId());
//            log.debug("检测到5连 lineSid = {},iconId = {}",c.getSid(),sameInfo.getBaseIconId());
//            if(iconConfig != null && iconConfig.getPayout_5() > 0){
//                addResultLineInfo(awardLineInfoList,c.getSid(),List.of(c.getYLine1(),c.getYLine2(),c.getYLine3(),c.getYLine4(),c.getYLine5()),iconConfig.getPayout_5(),ante);
//                log.debug("添加5连赔率 lineSid = {},iconSid = {},payout = {}",c.getSid(),iconConfig.getSid(),iconConfig.getPayout_5());
//            }
//        }
//        return awardLineInfoList;
//    }
//
//    /**
//     * 检查特殊玩法
//     * @param gameRunInfo
//     * @return
//     */
//    private GameRunInfo checkSpecial(GameRunInfo gameRunInfo,PlayerGameData playerGameData,long betValue){
//        log.debug("检查特殊玩法");
//
//        log.debug("寻找美元图标");
//        //美元图标集合
//        Map<Integer,Integer> dollarIconMap = new HashMap<>();
//        //all_aboard图标个数
//        int aboardCount = 0;
//        List<Integer> trainTypeList = new ArrayList<>();
//
//        //找出里面所有的美金图标
//        for(int i=0;i<gameRunInfo.getIntArray().length;i++){
//            int iconId = gameRunInfo.getIntArray()[i];
//
//            //检查是不是 all_aboard 图标
//            if(iconId == DollarExpressConst.Icon.ALL_ABOARD_ID){
//                aboardCount++;
//            }
//
//            //火车类型列表
//            if(iconId == DollarExpressConst.Icon.RED_TRAIN_ID){
//                trainTypeList.add(DollarExpressConst.ResultShow.RED_TRAIN_MOUDLE);
//            }else if(iconId == DollarExpressConst.Icon.PURPLE_TRAIN_ID){
//                trainTypeList.add(DollarExpressConst.ResultShow.PURPLE_TRAIN_MOUDLE);
//            }else if(iconId == DollarExpressConst.Icon.BLUE_TRAIN_ID){
//                trainTypeList.add(DollarExpressConst.ResultShow.BLUE_TRAIN_MOUDLE);
//            }else if(iconId == DollarExpressConst.Icon.GREEN_TRAIN_ID){
//                trainTypeList.add(DollarExpressConst.ResultShow.GREEN_TRAIN_MOUDLE);
//            }
//
//            //美元图标
//            Integer times = this.dollarIconMap.get(iconId);
//            if(times != null && times > 0){
//                dollarIconMap.put(iconId,times);
//                playerGameData.addShowDollarCount(1);
//                playerGameData.addShowDollarValue(betValue);
//                log.debug("找到美金图标 iconId = {},index = {}",iconId,i);
//            }
//        }
//
//        //如果是中免费
//        if(gameRunInfo.getSpecialType() == DollarExpressConst.ResultShow.WIN_FREE){
//            log.debug("检查特殊玩法之免费次数");
//            gameRunInfo.setFreeCount(DollarExpressResultShowConfig.getDollarExpressResultShowConfig(gameRunInfo.getResultShowId()).getFreetime());
//            if(gameRunInfo.getFreeCount() > 0){
//                playerGameData.getCanChooseFreeType().compareAndSet(false,true);
//            }
//
//            //配置表中配置的次数限制
//            int v = Integer.parseInt(DollarExpressGolbalConfig.getDollarExpressGolbalConfig(DollarExpressConst.Global.FREE_TYPE_TO_GOLD_TRAIN_COUNT_ID).getValue());
//            if(aboardCount >= v){
//                gameRunInfo.setGoldTrainInFree(true);
//            }
//        }else if(gameRunInfo.getSpecialType() == DollarExpressConst.ResultShow.WIN_NORMAL_TRAIN){   //如果是中火车
//            log.debug("检查特殊玩法之中火车");
//            List<TrainInfo> trainInfoList = new ArrayList<>();
//            for(int type : trainTypeList){
//                TrainInfo trainInfo = addTrainInfo(type, gameRunInfo.getPlayerId(), betValue);
//                if(trainInfo == null){
//                    continue;
//                }
//                trainInfoList.add(trainInfo);
//            }
//            gameRunInfo.setTrainInfoList(trainInfoList);
//        }else if(gameRunInfo.getSpecialType() == DollarExpressConst.ResultShow.WIN_SAFE_BOX){  //如果是中保险箱
//            log.debug("检查特殊玩法之保险箱");
//
//            for(Map.Entry<Integer,Integer> en : dollarIconMap.entrySet()){
//                int index = en.getKey();
//                int times = en.getValue();
//
//                SafeBoxInfo safeBoxInfo = new SafeBoxInfo();
//                long gold = betValue * times;
//                gameRunInfo.addAllWinGold(gold);
//
//                safeBoxInfo.indexId = index;
//                safeBoxInfo.times = times;
//                safeBoxInfo.addGold = gold;
//                gameRunInfo.addSafeBoxInfo(safeBoxInfo);
//                log.debug("保险箱之添加美元图标金额 iconId = {},times = {},addGold = {}",gameRunInfo.getIntArray()[index],times,gold);
//            }
//
//        }else if(gameRunInfo.getSpecialType() == DollarExpressConst.ResultShow.WIN_GOLD_TRAIN){
//            TrainInfo trainInfo = addTrainInfo(DollarExpressConst.ResultShow.GOLD_TRAIN_MOUDLE, gameRunInfo.getPlayerId(), betValue);
//            if(trainInfo != null){
//                gameRunInfo.setTrainInfoList(List.of(trainInfo));
//            }
//        }
//        return gameRunInfo;
//    }
//
//    /**
//     * 添加火车信息
//     * @param resultShowTrainType
//     * @param playerId
//     * @param betValue
//     * @return
//     */
//    private TrainInfo addTrainInfo(int resultShowTrainType,long playerId,long betValue){
//        //在resultshow表中找到对应的火车应该显示的数值
//        PropInfo<Integer> propInfo = resultShowConfigDataMap.get(resultShowTrainType);
//        log.debug("寻找火车配置 playerId = {},resultShowTrainType = {}", playerId, resultShowTrainType);
//        if(propInfo == null){
//            log.debug("未在resultshow中找到该类型火车配置 playerId = {},resultShowTrainType = {}",playerId,resultShowTrainType);
//            return null;
//        }
//
//        int rand = RandomUtils.randomInt(propInfo.getSum());
//        PropData<Integer> propData = propInfo.getPropMap().values().stream()
//                .filter(tempPropData -> rand >= tempPropData.getBegin() && rand < tempPropData.getEnd())
//                .findFirst()
//                .orElse(null);
//        if(propData == null){
//            log.debug("中火车模式时，没有根据权重找到对应的火车模式id， playerId = {},rand = {}",playerId,rand);
//            return null;
//        }
//
//        log.debug("找到火车配置中的 propData，resultshow.getSid() = {}",propData.getKey());
//        //根据权重找到的sid，然后找到对应的配置图标id
//        Map<Integer, Integer> iconMap = resultShowIconMap.get(propData.getKey());
//        if(iconMap == null){
//            log.debug("中火车模式时，没有根据权重找到对应的火车模式id2， playerId = {},sid = {}",playerId,propData.getKey());
//            return null;
//        }
//
//        TrainInfo trainInfo = new TrainInfo();
//        trainInfo.type = resultShowTrainType;
//        trainInfo.goldList = new ArrayList<>();
//        for(Map.Entry<Integer, Integer> en : iconMap.entrySet()){
//            int iconId = en.getValue();
//            if(iconId < 1){
//                continue;
//            }
//            DollarExpressIconConfig iconConfig = DollarExpressIconConfig.getDollarExpressIconConfig(iconId);
//            if(iconConfig == null){
//                log.debug("中火车模式时，没有找到对应的图标， playerId = {},iconId = {}",playerId,iconId);
//                continue;
//            }
//
////            if(iconConfig.payout_1 < 1){
////                continue;
////            }
//
//            long goldValue = iconConfig.getPayout_1() * betValue;
//            trainInfo.goldList.add(goldValue);
//            log.debug("添加火车金币 playerId = {},betValue = {},iconId = {},payout_1 = {},goldValue = {}",playerId,betValue,iconId,iconConfig.getPayout_1(),goldValue);
//        }
//        return trainInfo;
//    }
//
//    /**
//     * 组装中奖线信息
//     * @param awardLineInfoList
//     * @param sid
//     * @param indexList
//     * @param payout
//     * @param ante
//     */
//    private void addResultLineInfo(List<ResultLineInfo> awardLineInfoList, int sid, List<Integer> indexList, int payout, long ante){
//        ResultLineInfo resultLineInfo = new ResultLineInfo();
//        resultLineInfo.id = sid;
//        resultLineInfo.indexList = indexList;
//        resultLineInfo.times = payout;
//        resultLineInfo.winGold = ante * resultLineInfo.times;
//        awardLineInfoList.add(resultLineInfo);
//    }
//
//    /**
//     * 判断两个图片是否相同，包含wild
//     * @param iconId1
//     * @param iconId2
//     * @return
//     */
//    private SameInfo iconSame(SameInfo sameInfo, int iconId1, int iconId2){
//        BigDecimal icon_1_times = wildIconMap.get(iconId1);
//        BigDecimal icon_2_times = wildIconMap.get(iconId2);
//
//        if(iconId1 == iconId2){
//            //icon_1_times如果为null，那么icon_2_times也肯定为null，表示均为普通图标，否则均为wild
//            if(icon_1_times == null){
//                sameInfo.setBaseIconId(iconId1);
//                log.debug("均为普通图标 iconId1 = {},iconId2 = {}, same = true",iconId1,iconId2);
//            }else {
//                sameInfo.addTimes(icon_1_times);
//                sameInfo.addTimes(icon_2_times);
//                log.debug("均为wild图标 iconId1 = {},iconId2 = {}, icon_1_times = {},icon_2_times = {},same = true",iconId1,iconId2,icon_1_times,icon_2_times);
//            }
//            sameInfo.setSame(true);
//            return sameInfo;
//        }
//
//        //如果两个图标不一样，则判断有没有wild
//        if(icon_1_times == null){
//            if(icon_2_times == null){
//                //俩都为null，表示都不是wild图标
//                sameInfo.setSame(false);
//
////                log.debug("均为普通图标 iconId1 = {},iconId2 = {}, same = false",iconId1,iconId2);
//            }else {
//                //icon_2_times不为null，表示iconId2是wild
//                sameInfo.setSame(true);
//                sameInfo.setBaseIconId(iconId1);
//                sameInfo.addTimes(icon_2_times);
//                log.debug("iconId1 = {} 是普通图标,iconId2 = {} 是wild图标, same = true",iconId1,iconId2);
//            }
//            return sameInfo;
//        }else {
//            sameInfo.setSame(true);
//            if(icon_2_times == null){
//                //1不为null，2为null，表示 1是wild，2是普通图标
//                sameInfo.addTimes(icon_1_times);
//                sameInfo.setBaseIconId(iconId2);
//                log.debug("iconId1 = {} 是wild图标,iconId2 = {} 是普通图标, same = true",iconId1,iconId2);
//            }else {
//                //俩都不为null，表示都是wild图标
//                sameInfo.addTimes(icon_1_times);
//                sameInfo.addTimes(icon_2_times);
//                log.debug("均为wild图标 iconId1 = {} ,iconId2 = {} , same = true",iconId1,iconId2);
//            }
//            return sameInfo;
//        }
//    }
//
//    /**
//     * 创建 GameData
//     * @param playerController
//     * @return
//     */
    public PlayerGameData createPlayerGameData(PlayerController playerController, PlayerSessionInfo playerSessionInfo){
        PlayerGameData playerGameData = gameDataMap.computeIfAbsent(playerController.playerId(), k -> new PlayerGameData(playerController));
        playerGameData.setGameType(playerSessionInfo.getGameType());
        playerGameData.setWareId(playerSessionInfo.getWareId());
        return playerGameData;
    }
//
//    /**
//     * 检查stake是否为配置数据
//     * @param config
//     * @param stake
//     * @return
//     */
//    private boolean checkStake(DollarExpressWareHouseConfig config,long stake){
//        if(config.getStake_1() == stake || config.getStake_2() == stake || config.getStake_3() == stake
//                || config.getStake_4() == stake || config.getStake_5() == stake || config.getStake_6() == stake
//                || config.getStake_7() == stake || config.getStake_8() == stake || config.getStake_9() == stake
//                || config.getStake_10() == stake){
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public void change(String className) {
//        if(className.equalsIgnoreCase(DollarExpressControlConfig.class.getSimpleName())){
//            initControlConfigData();
//        }else if(className.equalsIgnoreCase(DollarExpressShowConfig.class.getSimpleName())){
//            initShowConfigData();
//        }else if(className.equalsIgnoreCase(DollarExpressResultShowConfig.class.getSimpleName())){
//            initResultShowConfigData();
//        }else if(className.equalsIgnoreCase(DollarExpressIconConfig.class.getSimpleName())){
//            initIconConfigData();
//        }
//    }
//
//    /**
//     * 缓存control表计算后的一些数据
//     */
//    private void initControlConfigData(){
//        try{
//            //取出类似 axle_1 或者 special_1 这样的字段名
//            List<Field> controlConfigAxleFieldNameList = new ArrayList<>();
//            List<Field> controlConfigSpecialFieldNameList = new ArrayList<>();
//            Field[] declaredFields = DollarExpressControlConfig.class.getDeclaredFields();
//            for(Field field : declaredFields){
//                String fieldName = field.getName();
//                if(fieldName.startsWith(DollarExpressConst.Common.AXLE_PREFIX)){
//                    field.setAccessible(true);
//                    controlConfigAxleFieldNameList.add(field);
//                }else if(fieldName.startsWith(DollarExpressConst.Common.SPECIAL_PREFIX)){
//                    field.setAccessible(true);
//                    controlConfigSpecialFieldNameList.add(field);
//                }
//            }
//
//
//            //计算出每个元素在随机时所在的区间范围
//            Map<Integer,PropInfo<String>> tempControlPropDataMap = new HashMap<>();
//            for(DollarExpressControlConfig c : DollarExpressControlConfig.factory.getAllSamples()){
//                PropInfo<String> propInfo = tempControlPropDataMap.computeIfAbsent(c.getSid(), k -> new PropInfo<>());
//
//                Map<String, PropData<String>> cMap = propInfo.getPropMap();
//
//                //起始值
//                int begin = 0;
//                //结束值
//                int end = 0;
//
//                for(Field aField : controlConfigAxleFieldNameList){
//                    int value = (int)aField.get(c);
//                    String fieldName = aField.getName();
//
//                    cMap.put(fieldName,new PropData<>(fieldName,begin,end += value));
//                    begin = end;
//                }
//
//                for(Field sField : controlConfigSpecialFieldNameList){
//                    int value = (int)sField.get(c);
//                    String fieldName = sField.getName();
//
//                    cMap.put(fieldName,new PropData<>(fieldName,begin,end += value));
//                    begin = end;
//                }
//                propInfo.setSum(end);
//            }
//
//            this.controlPropDataMap = tempControlPropDataMap;
//            log.debug("已完成更新 control 配置缓存数据");
//        }catch (Exception e){
//            log.error("",e);
//        }
//    }
//
//    /**
//     * 缓存show表计算后的一些数据
//     */
//    private void initShowConfigData(){
//        try{
//            //取出类似 colum_1_1 这样格式的字段名
//            List<Field> showConfigColumFieldNameList = new ArrayList<>();
//
//            Field[] declaredFields = DollarExpressShowConfig.class.getDeclaredFields();
//            for(Field field : declaredFields){
//                String fieldName = field.getName();
//                if(fieldName.startsWith(DollarExpressConst.Common.COLUM_PREFIX)){
//                    field.setAccessible(true);
//                    showConfigColumFieldNameList.add(field);
//                }
//            }
//
//            Map<Integer,Map<Integer,List<Integer>>> tempShowConfigDataMap = new HashMap<>();
//            for(Field field : showConfigColumFieldNameList){
//                String name = field.getName();
//                String[] arr = name.split("_");
//
//                int axle = Integer.parseInt(arr[1]);
//                int columIndex = Integer.parseInt(arr[2]);
//
//                Map<Integer, List<Integer>> tempMap = tempShowConfigDataMap.computeIfAbsent(axle, k -> new HashMap<>());
//                List<Integer> tempList = tempMap.computeIfAbsent(columIndex, k -> new ArrayList<>());
//
//                for(DollarExpressShowConfig c : DollarExpressShowConfig.factory.getAllSamples()){
//                    int iconId = (int)field.get(c);
//                    tempList.add(iconId);
//                }
//            }
//
//            this.showConfigDataMap = tempShowConfigDataMap;
//            log.debug("已完成更新 show 配置缓存数据");
//        }catch (Exception e){
//            log.error("",e);
//        }
//    }
//
//    /**
//     * 缓存resultShow表计算后的一些数据
//     */
//    private void initResultShowConfigData(){
//        try{
//            //取出类似 icon_1 这样格式的字段名
//            List<Field> resultShowConfigIconFieldNameList = new ArrayList<>();
//
//            Field[] declaredFields = DollarExpressResultShowConfig.class.getDeclaredFields();
//            for(Field field : declaredFields){
//                String fieldName = field.getName();
//                if(fieldName.startsWith(DollarExpressConst.Common.ICON_PREFIX)){
//                    field.setAccessible(true);
//                    resultShowConfigIconFieldNameList.add(field);
//                }
//            }
//
//            //计算概率区间
//            Map<Integer,PropInfo<Integer>> tempResultShowConfigDataMap = new HashMap<>();
//            //缓存位置上的特殊图标
//            Map<Integer,Map<Integer,Integer>> tempResultShowIconMap = new HashMap<>();
//
//            for(DollarExpressResultShowConfig c : DollarExpressResultShowConfig.factory.getAllSamples()){
//                int begin = 0;
//
//                PropInfo<Integer> info = tempResultShowConfigDataMap.get(c.getType());
//                if(info == null){
//                    info = new PropInfo<>();
//                    tempResultShowConfigDataMap.put(c.getType(), info);
//                }else {
//                    begin = info.getSum();
//                }
//
//                info.setSum(info.getSum() + c.getWeight());
//                info.getPropMap().put(c.getSid(),new PropData<>(c.getSid(),begin,info.getSum()));
//                //------------------------------
//                //缓存iconId
//                for(Field f : resultShowConfigIconFieldNameList){
//                    int v = (int)f.get(c);
//                    int index = Integer.parseInt(f.getName().split("_")[1]);
//
//                    Map<Integer, Integer> tempMap = tempResultShowIconMap.computeIfAbsent(c.getSid(), k -> new HashMap<>());
//                    tempMap.put(index,v);
//                }
//            }
//            this.resultShowConfigDataMap = tempResultShowConfigDataMap;
//            this.resultShowIconMap = tempResultShowIconMap;
//            log.debug("已完成更新 resultShow 配置缓存数据");
//        }catch (Exception e){
//            log.error("",e);
//        }
//    }
//
//    /**
//     * 缓存icon表的以及计算所需的数据
//     */
//    private void initIconConfigData() {
//        try{
//            Map<Integer,PropInfo<Integer>> tempIconNoWinMap = new HashMap<>();
//            Map<Integer,BigDecimal> tempWildMap = new HashMap<>();
//            Map<Integer,Integer> tempDollarIconMap = new HashMap<>();
//            for(DollarExpressIconConfig c : DollarExpressIconConfig.factory.getAllSamples()){
//                if(c.getNoWinning() > 0){
//                    PropInfo<Integer> propInfo = tempIconNoWinMap.computeIfAbsent(c.getType(), k -> new PropInfo<>());
//
//                    PropData<Integer> propData = new PropData<>(c.getSid(), propInfo.getSum(), propInfo.getSum() + c.getNoWinning());
//                    propInfo.getPropMap().put(c.getSid(),propData);
//                    propInfo.setSum(propData.getEnd());
//                }
//
//                if(c.getType() == DollarExpressConst.Icon.WILD_TYPE){
//                    BigDecimal wildTimes = BigDecimal.valueOf(c.getDoubling()).divide(DollarExpressConst.Common.TEN_THOUSAND).setScale(2, BigDecimal.ROUND_HALF_UP);
//                    tempWildMap.put(c.getSid(),wildTimes);
//                }else if(c.getType() == DollarExpressConst.Icon.DOLLAR_TYPE){
//                    tempDollarIconMap.put(c.getSid(),c.getPayout_1());
//                }
//            }
//            this.iconNoWinMap = tempIconNoWinMap;
//            this.wildIconMap = tempWildMap;
//            this.dollarIconMap = tempDollarIconMap;
//            log.debug("已完成更新 icon 配置缓存数据");
//        }catch (Exception e){
//            log.error("",e);
//        }
//    }

    public void cacheConfigData(){
        cacheControlProp();
    }

    /**
     * 缓存control的概率信息
     */
    private void cacheControlProp(){
        Map<Integer,PropInfo> tempControlPropMap = new HashMap<>();

        for(Map.Entry<Integer,DollarExpressControlCfg> en : GameDataManager.getDollarExpressControlCfgMap().entrySet()){
            PropInfo propInfo = new PropInfo();
            DollarExpressControlCfg cfg = en.getValue();

            Map<Integer, int[]> propMap = new HashMap<>();

            int begin = 0;
            int end = 0;
            for(int i=0;i<cfg.getAxleList().size();i++){
                Integer prop = cfg.getAxleList().get(i);
                begin = end;
                end += prop;
                propMap.put(i,new int[]{begin,end});
            }

            end = addControlSpecialProp(begin,end,cfg.getTrain(),propMap,DollarExpressConst.Common.CONTROLL_SPECIAL_TRAIN_ID);

            end = addControlSpecialProp(begin,end,cfg.getSafeBox(),propMap,DollarExpressConst.Common.CONTROLL_SPECIAL_SAFE_BOX_ID);

            end = addControlSpecialProp(begin,end,cfg.getFree(),propMap,DollarExpressConst.Common.CONTROLL_SPECIAL_FREE_ID);

            end = addControlSpecialProp(begin,end,cfg.getGoldTrain(),propMap,DollarExpressConst.Common.CONTROLL_SPECIAL_GOLD_TRAIN_ID);

            propInfo.setPropMap(propMap);
            propInfo.setSum(end);
        }

        this.controlPropMap = tempControlPropMap;
    }
    
    private void cacheResultWeight(){
        for(Map.Entry<Integer, DollarExpressResultWeightCfg> en : GameDataManager.getDollarExpressResultWeightCfgMap().entrySet()){
            DollarExpressResultWeightCfg cfg = en.getValue();
            cfg.getWeights();
        }
    }

    private int addControlSpecialProp(int begin, int end,int prop,Map<Integer, int[]> propMap,int specialId){
        begin = end;
        end += prop;
        propMap.put(specialId,new int[]{begin,end});
        return end;
    }
}
