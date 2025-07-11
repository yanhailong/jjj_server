package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.slots.game.dollarexpress.data.GameRunInfo;
import com.jjg.game.slots.game.dollarexpress.dao.DollarExpressResultLibDao;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressPlayerGameData;
import com.jjg.game.slots.game.dollarexpress.data.DollarExpressResultLib;
import com.jjg.game.slots.game.dollarexpress.generate.DollarExpressGenerate;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.sample.bean.BaseRoomCfg;
import com.jjg.game.slots.sample.bean.SpecialResultLibCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 游戏逻辑处理器
 * @author 11
 * @date 2025/6/11 16:48
 */
@Component
public class DollarExpressGameManager extends AbstractSlotsGameManager<DollarExpressPlayerGameData> {

    @Autowired
    private DollarExpressResultLibDao libDao;

    public DollarExpressGameManager(){
        super(DollarExpressPlayerGameData.class);
    }

    @Override
    public void init(int gameType) {
        super.init(gameType);

        generateLib(10);
    }

    /**
     * 生成结果库
     * @param count
     */
    public void generateLib(int count){
        boolean flag = this.generate.compareAndSet(false, true);
        if(!flag){
            log.debug("当前正在生成结果库，请勿打扰....");
            return;
        }

        log.info("开始生成结果库，预期生成 {} 条",count);
        DollarExpressGenerate dollarExpressGenerate = new DollarExpressGenerate(CoreConst.GameType.DOLLAR_EXPRESS);

        String newDocName = this.libDao.getNewMongoLibName(CoreConst.GameType.DOLLAR_EXPRESS);

        List<DollarExpressResultLib> libList = new ArrayList<>();
        int i = 0;
        int saveCount = 0;

        int expectGenerateCount = count;
        int restCount = Math.min(count,100);

        while (count > 0){
            int reduceCount = 1;
            i++;
            try{
                dollarExpressGenerate.generateOne();

                for(Map.Entry<Integer, DollarExpressResultLib> en : dollarExpressGenerate.getBranchLibMap().entrySet()){
                    libList.add(en.getValue());
                }

                if(libList.size() >= restCount){
                    saveCount += libDao.batchSave(libList,newDocName);
                    libList = new ArrayList<>();
                }

                if((i % 2000) == 0){
                    Thread.sleep(500);
                }
            }catch (Exception e){
                log.error("",e);
            }finally {
                count -= reduceCount;
            }
        }

        //加载到redis
        this.libDao.moveToRedis(CoreConst.GameType.DOLLAR_EXPRESS,newDocName,this.resultLibSectionMap);
        this.generate.compareAndSet(true, false);
        log.info("生成结果库结束，预期 {} 条，成功保存到数据库 {} 条", expectGenerateCount, saveCount);
    }

    /**
     * 开始游戏
     * @param playerController
     * @param betValue
     * @return
     */
    public GameRunInfo startGame(PlayerController playerController, long betValue){
        GameRunInfo gameRunInfo = new GameRunInfo(Code.SUCCESS, playerController.playerId());
        try{
            //获取倍场配置
            BaseRoomCfg baseRoomCfg = getBaseRoomCfg(playerController.player.getWareId());
            if(baseRoomCfg == null){
                log.warn("获取倍场配置失败 playerId = {},gameType = {},wareId = {},betValue = {}",playerController.playerId(),playerController.player.getGameType(),playerController.player.getWareId(),betValue);
                gameRunInfo.setCode(Code.NOT_FOUND);
                return gameRunInfo;
            }

            //检查押分是否合法
            boolean match = baseRoomCfg.getLineBetScore().stream().anyMatch(bet -> bet == betValue);
            if(!match){
                log.warn("押分值不合法 playerId = {},gameType = {},wareId = {},betValue = {}",playerController.playerId(),playerController.player.getGameType(),playerController.player.getWareId(),betValue);
                gameRunInfo.setCode(Code.PARAM_ERROR);
                return gameRunInfo;
            }

            //获取玩家游戏数据
            DollarExpressPlayerGameData playerGameData = getPlayerGameData(playerController);
            CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
            if(!libCfgResult.success()){
                gameRunInfo.setCode(libCfgResult.code);
                return gameRunInfo;
            }

            //获取 specialResultLib 中的type
            CommonResult<Integer> resultLibTypeResult = getResultLibType(playerGameData.getGameType(), libCfgResult.data.getModelId());
            if(!resultLibTypeResult.success()){
                gameRunInfo.setCode(libCfgResult.code);
                return gameRunInfo;
            }

            //获取倍数区间
            CommonResult<int[]> resultLibSectionResult = getResultLibSection(playerGameData.getGameType(), libCfgResult.data.getModelId(), resultLibTypeResult.data);
            if(!resultLibSectionResult.success()){
                gameRunInfo.setCode(libCfgResult.code);
                return gameRunInfo;
            }


        }catch (Exception e){
            log.error("",e);
        }
        return gameRunInfo;
    }
}
