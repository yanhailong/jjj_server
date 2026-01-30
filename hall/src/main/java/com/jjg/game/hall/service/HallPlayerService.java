package com.jjg.game.hall.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.PlayerExecutorGroupDisruptor;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.PlayerLastGameInfoDao;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.service.AbstractPlayerService;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.task.service.TaskService;
import com.jjg.game.hall.casino.service.PlayerBuildingService;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.pb.res.ResFunctionOpenList;
import com.jjg.game.hall.vip.service.VipService;
import com.jjg.game.sampledata.bean.GameFunctionCfg;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2025/5/26 16:49
 */
@Service
public class HallPlayerService extends AbstractPlayerService implements ConfigExcelChangeListener {

    @Autowired
    private PlayerLastGameInfoDao playerLastGameInfoDao;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private PlayerBuildingService playerBuildingService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private VipService vipService;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private GameFunctionService gameFunctionService;

    /**
     * 仅在登录时调用
     * 创建或保存  要记录登录时间
     *
     * @param playerId
     * @param cbk
     * @return
     */
    public CommonResult<Player> loginAndNewOrSave(long playerId, LoginQueryDataAction cbk) {
        CommonResult<Player> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        boolean lock = false;
        try {
            lock = redisLock.tryLockWithDefaultTime(key);
            if (!lock) {
                log.debug("获取锁失败 lockKey:{} playerId:{}", key, playerId);
                return result;
            }
            Player player = getFromAllDB(playerId);
            if (player == null) {
                player = new Player();
                player.setId(playerId);
                cbk.registerAction(player);
            } else {
                cbk.loginAction(player);
            }
            //记录登录时间
            playerLoginTimeDao.add(playerId, System.currentTimeMillis());
            redisTemplate.opsForHash().put(tableName, playerId, player);
            result.code = Code.SUCCESS;
            result.data = player;
            return result;
        } catch (Exception e) {
            log.warn("创建或保存对象异常 playerId={}", playerId, e);
        } finally {
            if (lock) {
                redisLock.tryUnlock(key);
            }

        }
        return result;
    }

    public interface LoginQueryDataAction {

        /**
         * 登录行为
         */
        void loginAction(Player player);

        /**
         * 注册行为
         */
        void registerAction(Player player);
    }

    /**
     * 清除过期的player数据
     */
    public void clean() {
        log.info("开始清除过期player数据");

        long now = System.currentTimeMillis();

        //获取一个时间
        long expireTime = now - TimeHelper.ONE_DAY_OF_MILLIS;

        Set<Object> loginSet = playerLoginTimeDao.getLoginSet(expireTime);
        if (loginSet == null || loginSet.isEmpty()) {
            return;
        }

        int index = 0;
        int finishNum = 0;
        for (Object o : loginSet) {
            try {
                if (index % 1000 == 0) {
                    log.info("已执行循环次数index={},成功次数={}", index, finishNum);
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                boolean clear = clean(Long.parseLong(o.toString()), expireTime);
                if (clear) {
                    finishNum++;
                }
                index++;
            } catch (Exception e) {
                log.error("清除player数据异常,playerId:{}", o, e);
            }
        }
        log.info("清除player数据完成,循环次数={},成功次数={},消耗时间={} ms", index, finishNum, System.currentTimeMillis() - now);
    }

    /**
     * 从redis删除过期的player数据,并且存储到mongodb
     *
     * @param playerId
     * @param expireTime
     * @return
     */
    private boolean clean(long playerId, long expireTime) {
        if (playerId < 1) {
            return false;
        }

        Double score = playerLoginTimeDao.score(playerId);
        //如果再次检测到玩家数据还没有过期,就不用保存
        if (score != null && score > expireTime) {
            return false;
        }

        Player player = getFromRedis(playerId);
        if (player != null) {
            //如果在线就暂时不保存到mongodb
            boolean online = playerSessionService.hasSession(playerId);
            if (online) {
                return false;
            }
            playerDao.save(player);
            redisTemplate.opsForHash().delete(tableName, playerId);
        }
        playerLoginTimeDao.remove(playerId);
        playerPackService.moveToMongo(playerId);
        playerBuildingService.moveToMongo(playerId);
        playerLastGameInfoDao.deleteById(playerId);
        noticeService.removeReadData(playerId);
        taskService.moveToMongo(playerId);
        vipService.moveToMongo(playerId);
        accountDao.moveToMongo(playerId);
        playerSessionTokenDao.delToken(playerId);
        return true;
    }

    public void sendFunctionReload() {
        for (PFSession pfSession : clusterSystem.getAllOnlinePlayerPFSession()) {
            if (pfSession.getPlayerId() > 0 && pfSession.getReference() instanceof PlayerController) {
                PlayerExecutorGroupDisruptor.getDefaultExecutor()
                        .tryPublish(pfSession.getWorkId(), HallConstant.MsgBean.REQ_FUNCTION_OPEN_LIST, new BaseHandler<String>() {
                            @Override
                            public void action() {
                                if (pfSession.getPlayerId() > 0 && pfSession.getReference() instanceof PlayerController playerController) {
                                    Player player = get(playerController.playerId());
                                    List<Integer> openedFuncIdList = gameFunctionService.getOpenedFuncIdList(player);
                                    ResFunctionOpenList res = new ResFunctionOpenList(Code.SUCCESS);
                                    res.openedFunctionIdList = openedFuncIdList;
                                    playerController.send(res);
                                    log.info("推送玩家获取功能开放列表 playerId = {},openedFuncIdList = {}", playerController.playerId(), openedFuncIdList);
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void initSampleCallbackCollector() {
        addChangeSampleFileObserveWithCallBack(GameFunctionCfg.EXCEL_NAME, this::sendFunctionReload);
    }
}
