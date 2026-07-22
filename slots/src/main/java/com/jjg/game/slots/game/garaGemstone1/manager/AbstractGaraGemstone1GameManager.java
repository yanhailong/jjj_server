package com.jjg.game.slots.game.garaGemstone1.manager;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseInitCfg;
import com.jjg.game.sampledata.bean.PoolCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.dao.SlotsPoolDao;
import com.jjg.game.slots.game.garaGemstone1.GaraGemstone1Constant;
import com.jjg.game.slots.game.garaGemstone1.dao.GaraGemstone1ResultLibDao;
import com.jjg.game.slots.game.garaGemstone1.data.*;
import com.jjg.game.slots.game.garaGemstone1.pb.GaraGemstone1WinIconInfo;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractGaraGemstone1GameManager extends AbstractSlotsGameManager<GaraGemstone1PlayerGameData, GaraGemstone1ResultLib, GaraGemstone1GameRunInfo> {
    @Autowired
    private GaraGemstone1ResultLibDao libDao;
    @Autowired
    private GaraGemstone1GenerateManager gameGenerateManager;

    public AbstractGaraGemstone1GameManager() {
        super(GaraGemstone1PlayerGameData.class, GaraGemstone1ResultLib.class, GaraGemstone1GameRunInfo.class);
    }

    @Override
    public void init() {
        log.info("启动伽罗宝石1游戏管理器...");
        super.init();
        addUpdatePoolEvent();
    }

    @Override
    protected GaraGemstone1GameRunInfo startGame(PlayerController playerController, GaraGemstone1PlayerGameData playerGameData, long stake, boolean auto) {
        GaraGemstone1GameRunInfo gameRunInfo = new GaraGemstone1GameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));
            int status = playerGameData.getStatus();
            if (status == GaraGemstone1Constant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, stake);
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.debug("开始游戏失败，检测到错误状态 playerId={},gameType={},roomCfgId={},status={}",
                        playerGameData.getPlayerId(), playerGameData.getGameType(), playerGameData.getRoomCfgId(), status);
                return gameRunInfo;
            }
            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }
            //从奖池扣除，并给玩家加钱
            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            //触发实际赢钱的task
            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());
            //玩家当前金币
            player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            //添加大奖展示id
            int times = calWinTimes(gameRunInfo, playerGameData);
            log.debug("计算出获奖倍数 times={}", times);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
        } catch (Exception e) {
            log.error("", e);
            gameRunInfo.setCode(Code.EXCEPTION);
        }
        return gameRunInfo;
    }

    @Override
    public GaraGemstone1GameRunInfo normal(GaraGemstone1GameRunInfo gameRunInfo, GaraGemstone1PlayerGameData playerGameData, long betValue, GaraGemstone1ResultLib resultLib) {
        // 游戏时动态生成第四轴图标，写入 resultLib.iconArr 并设置 multiplyAxisTimes/axisJackpotId
        gameGenerateManager.generateAxisIcons(resultLib);

        long lineTimes = gameGenerateManager.calLineTimes(resultLib.getAwardLineInfoList());
        long axisMultiplier = resultLib.getMultiplyAxisTimes() > 0 ? resultLib.getMultiplyAxisTimes() : 1;
        long addTimes = lineTimes * axisMultiplier;

        gameRunInfo.setStatus(GaraGemstone1Constant.Status.NORMAL);
        log.debug("id={},data={}", resultLib.getId(), JSON.toJSONString(resultLib));
        gameRunInfo.setIconArr(resultLib.getIconArr());
        if (gameRunInfo.getBigPoolTimes() < 1) {
            gameRunInfo.addBigPoolTimes(addTimes);
        }

        // 检查3×3散花触发的jackpot
        if (resultLib.getJackpotId() > 0) {
            PoolCfg poolCfg = GameDataManager.getPoolCfg(resultLib.getJackpotId());
            if (poolCfg != null) {
                CommonResult<Long> result = slotsPoolDao.rewardByRatioFromSmallPool(playerGameData.getPlayerId(), this.gameType, playerGameData.getRoomCfgId(),
                        poolCfg.getTruePool(), poolCfg.getId(), betValue, poolCfg.getMaxMultiple(), AddType.SLOTS_JACKPOT_REWARD);
                if (result.success()) {
                    gameRunInfo.addSmallPoolGold(result.data);
                }
            }
        }
        // 检查第四轴（倍数轴）奖金符号触发的奖池（JACKPOOL模式）
        rewardFromSmallPool2(gameRunInfo, playerGameData, resultLib.getJackpotIds());
        gameRunInfo.setMultiplyAxisTimes(resultLib.getMultiplyAxisTimes());
        gameRunInfo.setAwardLineInfos(transAwardLinePbInfo(resultLib.getAwardLineInfoList(), playerGameData.getOneBetScore()));
        gameRunInfo.setStake(betValue);
        gameRunInfo.setResultLib(resultLib);
        return gameRunInfo;
    }

    protected List<GaraGemstone1WinIconInfo> transAwardLinePbInfo(List<GaraGemstone1AwardLineInfo> infoList, long oneBetScore) {
        if (CollUtil.isEmpty(infoList)) {
            return null;
        }
        List<GaraGemstone1WinIconInfo> list = new ArrayList<>(infoList.size());
        for (GaraGemstone1AwardLineInfo lineInfo : infoList) {
            GaraGemstone1WinIconInfo resultLineInfo = new GaraGemstone1WinIconInfo();
            resultLineInfo.id = lineInfo.getId();
            resultLineInfo.iconIndexs = getIconIndexsByLineId(lineInfo.getId()).subList(0, lineInfo.getSameCount());
            resultLineInfo.winGold = oneBetScore * lineInfo.getBaseTimes();
            list.add(resultLineInfo);
        }
        return list;
    }

    /**
     * 获取奖池信息
     */
    public GaraGemstone1GameRunInfo getPoolValue(PlayerController playerController, long stake) {
        GaraGemstone1GameRunInfo gameRunInfo = new GaraGemstone1GameRunInfo(Code.SUCCESS, playerController.playerId());
        try {
            int roomCfgId = playerController.getPlayer().getRoomCfgId();
            long poolValue = getPoolValueByRoomCfgId(roomCfgId);
            // 若奖池仍为 0（如配置未初始化或尚无玩家下注），使用时间加权公式计算假奖池显示值
            if (poolValue <= 0 && stake > 0) {
                BaseInitCfg baseInitCfg = GameDataManager.getBaseInitCfg(playerController.getPlayer().getGameType());
                if (baseInitCfg != null && CollUtil.isNotEmpty(baseInitCfg.getPrizePoolIdList())) {
                    for (int poolId : baseInitCfg.getPrizePoolIdList()) {
                        PoolCfg poolCfg = GameDataManager.getPoolCfg(poolId);
                        if (poolCfg != null && CollUtil.isNotEmpty(poolCfg.getGrowthRate()) && poolCfg.getGrowthRate().size() >= 2) {
                            poolValue = calPoolValue(stake, poolCfg.getGrowthRate(), poolCfg.getFakePoolInitTimes(), poolCfg.getFakePoolMax());
                            break;
                        }
                    }
                }
            }
            gameRunInfo.setMajor(poolValue);
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected GaraGemstone1ResultLibDao getResultLibDao() {
        return this.libDao;
    }

    @Override
    protected GaraGemstone1GenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.GARA_GEMSTONE_1;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭伽罗宝石1游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
