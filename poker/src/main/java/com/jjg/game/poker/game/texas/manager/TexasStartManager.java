package com.jjg.game.poker.game.texas.manager;

import com.jjg.game.core.listener.GameSampleFileChangeListener;
import com.jjg.game.core.sample.SampleConfig;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author 11
 * @date 2025/6/27 17:53
 */
@Component
public class TexasStartManager implements IRoomStartListener, GameSampleFileChangeListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SampleConfig sampleConfig;

    @Override
    public int[] getGameTypes() {
        return TexasConstant.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void start() {
        log.info("正在启动红黑大战游戏...");

        loadGameDataConfig();
    }

    @Override
    public void shutdown() {
        log.info("正在关闭红黑大战游戏...");
    }

    @Override
    public void change(File changedExcelConfig) {

    }

    private void loadGameDataConfig() {
        try {
            String samplePath = sampleConfig.getSamplePath();
            if (samplePath == null) {
                return;
            }
//            GameDataManager.loadAllData(samplePath);
        } catch (Exception e) {
            log.error("加载配置表失败");
            throw new RuntimeException("加载配置表失败", e);
        }
    }
}
