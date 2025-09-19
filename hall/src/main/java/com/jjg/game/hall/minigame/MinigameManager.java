package com.jjg.game.hall.minigame;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.hall.minigame.constant.MinigameConstant;
import com.jjg.game.hall.minigame.event.MinigameReadyEvent;
import com.jjg.game.sampledata.GameDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 小游戏管理器
 */
@Component
public class MinigameManager {

    private final Logger log = LoggerFactory.getLogger(MinigameManager.class);

    private final RedisLock redisLock;

    private final RedisTemplate redisTemplate;

    private final ApplicationEventPublisher eventPublisher;

    public MinigameManager(RedisLock redisLock, RedisTemplate redisTemplate, ApplicationEventPublisher eventPublisher) {
        this.redisLock = redisLock;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 初始化
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        redisLock.tryLockAndRun(MinigameConstant.RedisLock.MINIGAME_INIT_LOCK, () -> {
            //加载配置
            Map<Integer, Integer> entries = redisTemplate.opsForHash().entries(MinigameConstant.RedisKey.MINIGAME_CONFIG);
            if (entries.isEmpty()) {
                GameDataManager.getMiniGameListCfgList()
                        .forEach(miniGameListCfg -> entries.put(miniGameListCfg.getId(), miniGameListCfg.getStatus()));
                redisTemplate.opsForHash().putAll(MinigameConstant.RedisKey.MINIGAME_STATUS, entries);
                long now = System.currentTimeMillis();
                //重置记录的开服时间
                redisTemplate.opsForValue().set(MinigameConstant.RedisKey.MINIGAME_OPEN_SERVER_TIME, now);
                log.info("ready to init minigame server");
                eventPublisher.publishEvent(new MinigameReadyEvent());
            }
        });
    }

    /**
     * 刷新配置
     */
    public void refreshConfig() {

    }

    /**
     * 获取开启的游戏的id列表
     *
     * @return
     */
    public List<Integer> getOpenGameList() {
        List<Integer> openGameList = new ArrayList<>();
        Map<Integer, Integer> entries = redisTemplate.opsForHash().entries(MinigameConstant.RedisKey.MINIGAME_STATUS);
        if (!entries.isEmpty()) {
            entries.forEach((k, v) -> {
                if (v == 1) {
                    openGameList.add(k);
                }
            });
        }
        return openGameList;
    }

}
