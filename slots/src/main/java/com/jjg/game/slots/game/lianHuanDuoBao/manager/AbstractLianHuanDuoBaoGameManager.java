package com.jjg.game.slots.game.lianHuanDuoBao.manager;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseRoomCfg;
import com.jjg.game.sampledata.bean.SpecialResultLibCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.data.BetDivideInfo;
import com.jjg.game.slots.data.TestLibData;
import com.jjg.game.slots.game.lianHuanDuoBao.constant.LianHuanDuoBaoConstant;
import com.jjg.game.slots.game.lianHuanDuoBao.dao.LianHuanDuoBaoResultLibDao;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoChestReward;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoGameRunInfo;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoPlayerGameData;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoResultLib;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;

import java.util.List;

/**
 * 连环夺宝 GameManager 基类
 * <p>
 * 当前只实现：玩家状态机入口（normal/bonus）、按关卡选 modelId、lib 抽取。
 * 业务细节（钥匙累计 → 切层、宝箱开启顺序、龙珠收集、聚宝盆抽水、bonus 结算）
 * 在后续阶段补全。
 *
 * @author lm
 * @date 2026/6/2
 */
public abstract class AbstractLianHuanDuoBaoGameManager
        extends AbstractSlotsGameManager<LianHuanDuoBaoPlayerGameData, LianHuanDuoBaoResultLib, LianHuanDuoBaoGameRunInfo> {

    protected final LianHuanDuoBaoGameGenerateManager gameGenerateManager;
    protected final LianHuanDuoBaoResultLibDao resultLibDao;

    public AbstractLianHuanDuoBaoGameManager(LianHuanDuoBaoGameGenerateManager gameGenerateManager,
                                             LianHuanDuoBaoResultLibDao resultLibDao) {
        super(LianHuanDuoBaoPlayerGameData.class, LianHuanDuoBaoResultLib.class, LianHuanDuoBaoGameRunInfo.class);
        this.gameGenerateManager = gameGenerateManager;
        this.resultLibDao = resultLibDao;
    }

    @Override
    public void init() {
        log.info("启动连环夺宝游戏管理器...");
//        super.init();
    }

    @Override
    protected CommonResult<LianHuanDuoBaoResultLib> getLibFromDB(LianHuanDuoBaoPlayerGameData playerGameData, int libType) {
        CommonResult<LianHuanDuoBaoResultLib> result = new CommonResult<>(Code.SUCCESS);
        TestLibData testLibData = playerGameData.pollTestLibData();
        boolean gmLibType = false;
        LianHuanDuoBaoResultLib resultLib = null;
        if (testLibData != null) {
            libType = testLibData.getLibType();
            if (libType > 0) {
                gmLibType = true;
                log.debug("获取到测试数据 playerId = {},libType = {}", playerGameData.getPlayerId(), libType);
            } else if (testLibData.getData() != null) {
                resultLib = (LianHuanDuoBaoResultLib) testLibData.getData();
                log.debug("获取到测试数据 playerId = {},lib = {}", playerGameData.getPlayerId(), JSON.toJSONString(resultLib));
            }
        }
        if (resultLib == null) {
            BaseRoomCfg baseRoomCfg = GameDataManager.getBaseRoomCfg(playerGameData.getRoomCfgId());
            if (baseRoomCfg == null) {
                result.code = Code.NOT_FOUND;
                return result;
            }
            CommonResult<SpecialResultLibCfg> libCfgResult = getLibCfg(playerGameData, baseRoomCfg.getInitBasePool());
            if (!libCfgResult.success()) {
                if (libCfgResult.code == Code.AMOUNT_OF_RESERVES_IS_NOT_ENOUGHT) {
                    sendRoomAmountNotEnough(playerGameData.getPlayerId());
                }
                result.code = libCfgResult.code;
                return result;
            }
            if (!gmLibType && LianHuanDuoBaoConstant.SpecialMode.NORMAL_MAP.containsValue(libType)) {
                CommonResult<Integer> typeResult = getResultLibType(playerGameData, libCfgResult.data.getModelId(), playerGameData.getRoomType());
                if (!typeResult.success()) {
                    result.code = typeResult.code;
                    return result;
                }
                libType = typeResult.data;
            }
            for (int i = 0; i < SlotsConst.Common.GET_LIB_FAIL_RETRY_COUNT; i++) {
                resultLib = getLib(libCfgResult.data, libType, playerGameData);
                if (resultLib != null) {
                    if (resultLib.getJackpotIds() != null && !resultLib.getJackpotIds().isEmpty()) {
                        List<Integer> rewardPoolIds = checkLibPool(resultLib, playerGameData);
                        if (rewardPoolIds.isEmpty()) {
                            resultLib = afterForbidPoolLib(libCfgResult.data, resultLib, playerGameData);
                        }
                    }
                }
                if (resultLib == null) {
                    continue;
                }
                playerGameData.setLastModelId(libCfgResult.data.getModelId());
                break;
            }
        }
        if (resultLib == null) {
            int normalLibType = LianHuanDuoBaoConstant.SpecialMode.NORMAL_MAP.getOrDefault(playerGameData.getLayerNumber(), 0);
            resultLib = getResultLibDao().getLibBySectionIndex(normalLibType, 0, this.libClass);
            if (resultLib == null) {
                result.code = Code.FAIL;
                return result;
            }
        }
        result.data = resultLib;
        return result;
    }

    /**
     * 根据玩家当前关卡选 modelId 抽 lib
     */
    @Override
    protected LianHuanDuoBaoGameRunInfo normal(LianHuanDuoBaoGameRunInfo gameRunInfo,
                                               LianHuanDuoBaoPlayerGameData playerGameData,
                                               long betValue) {
        Integer model = LianHuanDuoBaoConstant.SpecialMode.NORMAL_MAP.get(playerGameData.getLayerNumber());
        if (model == null) {
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        CommonResult<Pair<LianHuanDuoBaoResultLib, BetDivideInfo>> libResult = normalGetLib(playerGameData, betValue, model);
        if (!libResult.success()) {
            gameRunInfo.setCode(libResult.code);
            return gameRunInfo;
        }
        LianHuanDuoBaoResultLib resultLib = libResult.data.getFirst();
        if (resultLib == null) {
            gameRunInfo.setCode(Code.FAIL);
            return gameRunInfo;
        }
        gameRunInfo.setBetDivideInfo(libResult.data.getSecond());
        normal(gameRunInfo, playerGameData, betValue, resultLib);
        return gameRunInfo;
    }

    @Override
    protected CommonResult<Integer> getResultLibType(LianHuanDuoBaoPlayerGameData playerGameData, int modelId, RoomType roomType) {
        CommonResult<Integer> resultLibType = super.getResultLibType(playerGameData, modelId, roomType);
        if (!resultLibType.success()) {
            return resultLibType;
        }
        //映射到当前关卡的 normal lib type
        if (LianHuanDuoBaoConstant.SpecialMode.NORMAL_MAP.containsValue(resultLibType.data)) {
            Integer realType = LianHuanDuoBaoConstant.SpecialMode.NORMAL_MAP.get(playerGameData.getLayerNumber());
            if (realType != null) {
                resultLibType.data = realType;
            }
        }
        return resultLibType;
    }

    /**
     * 玩家点旋转/开始游戏入口
     */
    @Override
    public LianHuanDuoBaoGameRunInfo startGame(PlayerController playerController,
                                               LianHuanDuoBaoPlayerGameData playerGameData,
                                               long betValue, boolean auto) {
        LianHuanDuoBaoGameRunInfo gameRunInfo = new LianHuanDuoBaoGameRunInfo(Code.SUCCESS, playerGameData.getPlayerId());
        try {
            gameRunInfo.setAuto(auto);
            Player player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(playerController.getPlayer().getRoomCfgId());
            gameRunInfo.setBeforeGold(getMoneyByItemId(warehouseCfg, player));

            int status = playerGameData.getStatus();
            if (status == LianHuanDuoBaoConstant.Status.NORMAL) {
                normal(gameRunInfo, playerGameData, betValue);
            } else if (status == LianHuanDuoBaoConstant.Status.BONUS) {
                //bonus 走单独的流程，后续阶段实现
                gameRunInfo.setCode(Code.FAIL);
                log.warn("bonus 模式还未实现 playerId={}", playerController.playerId());
                return gameRunInfo;
            } else {
                gameRunInfo.setCode(Code.FAIL);
                log.warn("当前状态错误 playerId = {},status = {}", playerController.playerId(), status);
                return gameRunInfo;
            }

            if (!gameRunInfo.success()) {
                return gameRunInfo;
            }

            gameRunInfo.setLayerNumber(playerGameData.getLayerNumber());
            gameRunInfo.setNextLayerNumber(playerGameData.getLayerNumber());
            //后续阶段：在这里做钥匙累计 / 关卡切换 / 龙珠收集 / 聚宝盆抽水

            rewardFromBigPool(gameRunInfo, playerGameData);
            gameRunInfo.addAllWinGold(gameRunInfo.getSmallPoolGold());
            triggerWinTask(playerController.getPlayer(), gameRunInfo, playerGameData, warehouseCfg.getTransactionItemId());

            player = slotsPlayerService.get(playerGameData.getPlayerId());
            playerController.setPlayer(player);
            gameRunInfo.setAfterGold(getMoneyByItemId(warehouseCfg, player));

            int times = calWinTimes(gameRunInfo, playerGameData);
            gameRunInfo.setBigShowId(getBigShowIdByTimes(times));

            if (!auto) {
                checkMarquee(playerGameData, gameRunInfo.getAllWinGold());
            }
            gameRunInfo.setData(playerGameData);
        } catch (Exception e) {
            log.error("", e);
        }
        return gameRunInfo;
    }

    @Override
    protected LianHuanDuoBaoGameRunInfo normal(LianHuanDuoBaoGameRunInfo gameRunInfo,
                                               LianHuanDuoBaoPlayerGameData playerGameData,
                                               long betValue, LianHuanDuoBaoResultLib resultLib) {
        //本局基础中奖倍数
        gameRunInfo.addBigPoolTimes(resultLib.getTimes());
        rewardFromSmallPool(gameRunInfo, playerGameData, resultLib.getJackpotIds());

        gameRunInfo.setIconArr(resultLib.getIconArr());
        gameRunInfo.setResultLib(resultLib);
        gameRunInfo.setStake(betValue);
        gameRunInfo.setStatus(playerGameData.getStatus());

        //--- 阶段 3：钥匙累计 → 关卡切换；cascade 送龙珠 ---
        //--- 阶段 4：钥匙触发宝箱开启 → 金币 + 概率掉龙珠 ---
        applyKeysAndDragonBalls(gameRunInfo, playerGameData, resultLib);

        //--- 阶段 5 待做：聚宝盆抽水 ---
        gameRunInfo.setDragonBallCount(playerGameData.getDragonBallCount());
        gameRunInfo.setTreasureBowlAmount(playerGameData.getTreasureBowlAmount());
        return gameRunInfo;
    }

    /**
     * 把 lib 算好的钥匙数 / 连续消除送龙珠累加到 playerGameData，并判断关卡切换 / bonus 触发。
     * 关卡规则（文档 [5][8][76]）：
     * <ul>
     *   <li>每关收集满 15 把钥匙进入下一关，剩余溢出钥匙带到下一关</li>
     *   <li>第三关凑满 15 把后进入 bonus 小游戏：状态切 BONUS，龙珠+5 保底</li>
     *   <li>从第三关回第一关时钥匙不继承（bonus 流程里完成）</li>
     * </ul>
     */
    private void applyKeysAndDragonBalls(LianHuanDuoBaoGameRunInfo gameRunInfo,
                                         LianHuanDuoBaoPlayerGameData playerGameData,
                                         LianHuanDuoBaoResultLib resultLib) {
        int keyCollected = resultLib.getKeyCollectionNum();
        int cascadeDragonBalls = resultLib.getCascadeDragonBalls();
        int currentLayer = playerGameData.getLayerNumber();
        gameRunInfo.setCollectedKeyNum(keyCollected);
        gameRunInfo.setLayerNumber(currentLayer);

        //=== 阶段 4：钥匙开宝箱 ===
        //每把消除掉的钥匙都开一个宝箱：金币奖励 + 概率掉龙珠
        //开启顺序按当前关卡 + 玩家在本关已开启数推进
        if (keyCollected > 0) {
            List<LianHuanDuoBaoChestReward> chestRewards = gameGenerateManager.openChests(
                    keyCollected, currentLayer, playerGameData.getOpenedChestCount());
            if (!chestRewards.isEmpty()) {
                gameRunInfo.setChestRewards(chestRewards);
                resultLib.setChestRewards(chestRewards);

                long oneBetScore = playerGameData.getOneBetScore();
                long chestGold = 0;
                int chestDragonBalls = 0;
                for (LianHuanDuoBaoChestReward r : chestRewards) {
                    chestGold += (long) r.getRewardTimes() * oneBetScore;
                    if (r.isDropDragonBall()) {
                        chestDragonBalls++;
                    }
                }
                //宝箱开出来的金币算到本局总赢分（用 bigPool times 累加：times = chestGold / oneBetScore = sum of rewardTimes）
                long chestTotalTimes = chestGold / Math.max(1, oneBetScore);
                gameRunInfo.addBigPoolTimes(chestTotalTimes);

                //宝箱开出来的龙珠累加到玩家
                if (chestDragonBalls > 0) {
                    playerGameData.setDragonBallCount(playerGameData.getDragonBallCount() + chestDragonBalls);
                }
                //推进本关已开启数
                playerGameData.setOpenedChestCount(playerGameData.getOpenedChestCount() + chestRewards.size());
                log.info("玩家 {} 第{}关本局开了 {} 个宝箱，金币 +{}, 龙珠 +{}",
                        playerGameData.getPlayerId(), currentLayer, chestRewards.size(), chestGold, chestDragonBalls);
            }
        }

        //连续消除送龙珠
        if (cascadeDragonBalls > 0) {
            playerGameData.setDragonBallCount(playerGameData.getDragonBallCount() + cascadeDragonBalls);
        }

        //=== 阶段 3：累计钥匙 → 关卡切换 ===
        int totalKey = playerGameData.getCollectedKeyNum() + keyCollected;
        int needCount = LianHuanDuoBaoConstant.Common.KEYS_PER_LAYER;

        if (totalKey >= needCount) {
            int overflow = totalKey - needCount;
            if (currentLayer >= LianHuanDuoBaoConstant.Common.MAX_LAYER) {
                //第三关凑齐 15 把 → bonus 小游戏：清钥匙、清宝箱开启数、状态切 BONUS、龙珠保底 +5
                playerGameData.setCollectedKeyNum(0);
                playerGameData.setOpenedChestCount(0);
                playerGameData.setStatus(LianHuanDuoBaoConstant.Status.BONUS);
                int extraDragonBalls = LianHuanDuoBaoConstant.Common.BONUS_MIN_DRAGON_BALL;
                playerGameData.setDragonBallCount(playerGameData.getDragonBallCount() + extraDragonBalls);
                //bonus 期间下一局还是在"第三关"上，bonus 结束后回到第一关由 bonus 流程处理
                gameRunInfo.setNextLayerNumber(currentLayer);
                gameRunInfo.setStatus(LianHuanDuoBaoConstant.Status.BONUS);
                log.info("玩家 {} 通关第三关进入 bonus，本局钥匙={}, 累计=15+{}, 龙珠+={} +保底{}",
                        playerGameData.getPlayerId(), keyCollected, overflow, cascadeDragonBalls, extraDragonBalls);
            } else {
                //进入下一关：钥匙溢出带过去，宝箱开启数重置（新关卡新宝箱）
                playerGameData.setLayerNumber(currentLayer + 1);
                playerGameData.setCollectedKeyNum(overflow);
                playerGameData.setOpenedChestCount(0);
                gameRunInfo.setNextLayerNumber(currentLayer + 1);
                log.info("玩家 {} 从第{}关进入第{}关，溢出钥匙={}",
                        playerGameData.getPlayerId(), currentLayer, currentLayer + 1, overflow);
            }
        } else {
            playerGameData.setCollectedKeyNum(totalKey);
            gameRunInfo.setNextLayerNumber(currentLayer);
        }
        gameRunInfo.setTotalKeyNum(playerGameData.getCollectedKeyNum());
        gameRunInfo.setDragonBallCount(playerGameData.getDragonBallCount());
    }

    @Override
    public int getGameType() {
        return CoreConst.GameType.LIAN_HUAN_DUO_BAO;
    }

    @Override
    protected LianHuanDuoBaoResultLibDao getResultLibDao() {
        return this.resultLibDao;
    }

    @Override
    protected LianHuanDuoBaoGameGenerateManager getGenerateManager() {
        return this.gameGenerateManager;
    }

    @Override
    public void shutdown() {
        try {
            super.shutdown();
            log.info("已关闭连环夺宝游戏管理器");
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
