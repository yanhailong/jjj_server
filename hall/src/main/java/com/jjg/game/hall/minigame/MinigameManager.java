package com.jjg.game.hall.minigame;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.hall.minigame.constant.MinigameConstant;
import com.jjg.game.hall.minigame.event.MinigameReadyEvent;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ActivityConfigCfg;
import com.jjg.game.sampledata.bean.MiniGameListCfg;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 小游戏管理器
 */
@Component
public class MinigameManager implements ConfigExcelChangeListener {

    private final Logger log = LoggerFactory.getLogger(MinigameManager.class);

    private final RedisLock redisLock;

    private final RedissonClient redissonClient;

    private final ApplicationEventPublisher eventPublisher;

    public MinigameManager(RedisLock redisLock, RedissonClient redissonClient, ApplicationEventPublisher eventPublisher) {
        this.redisLock = redisLock;
        this.redissonClient = redissonClient;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 初始化
     */
    public void init() {
//        loadConfig();
//        redisLock.tryLockAndRun(MinigameConstant.RedisLock.MINIGAME_INIT_LOCK, () -> {
//            //加载配置
//            RMap<Integer, MiniGameListCfg> entries = redissonClient.getMap(MinigameConstant.RedisKey.MINIGAME_CONFIG);
//            if (entries.isEmpty()) {
//                GameDataManager.getMiniGameListCfgList()
//                        .forEach(miniGameListCfg -> entries.fastPut(miniGameListCfg.getId(), miniGameListCfg));
//            }
//            log.info("ready to init minigame server");
//            entries.forEach((key, cfg) -> {
//                if (cfg.getStatus() == 0) {
//                    MinigameReadyEvent event = new MinigameReadyEvent();
//                    event.setGameId(key);
//                    eventPublisher.publishEvent(event);
//                }
//            });
//        });
    }

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(MiniGameListCfg.EXCEL_NAME, this::loadConfig);
    }

    /**
     * 刷新配置
     */
    public void loadConfig() {
        log.debug("加载小游戏配置表");
        GameDataManager.getMiniGameListCfgMap().forEach((key,cfg) -> {
            if (cfg.getStatus() == 0) {
                MinigameReadyEvent event = new MinigameReadyEvent();
                event.setGameId(key);
                eventPublisher.publishEvent(event);
            }
        });
    }

    /**
     * 获取开启的游戏的id列表
     *
     * @return
     */
    public List<Integer> getOpenGameList() {
        List<Integer> openGameList = new ArrayList<>();
        GameDataManager.getMiniGameListCfgMap().forEach((k,v) -> {
            if(v.getStatus() == 0){
                openGameList.add(k);
            }
        });
        return openGameList;
    }

    /**
     * 判断指定游戏是否开启
     *
     * @return true 开启
     */
    public boolean isOpenGame(int gameId) {
        MiniGameListCfg cfg = GameDataManager.getMiniGameListCfgMap().get(gameId);
        if(cfg == null){
            return false;
        }

        return cfg.getStatus() == 0;
    }


}
