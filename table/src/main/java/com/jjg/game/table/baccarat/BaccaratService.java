package com.jjg.game.table.baccarat;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 百家乐管理器
 *
 * @author 2CL
 */
@Service
public class BaccaratService implements IRoomStartListener {

    private static final Logger log = LoggerFactory.getLogger(BaccaratService.class);

    @Autowired
    private BaccaratSampleManager baccaratSampleManager;

    @Override
    public int[] getGameTypes() {
        return new int[]{CoreConst.GameType.BACCARAT};
    }

    @Override
    public void start() {
        try {
            // 配置表初始化
            baccaratSampleManager.init();
        } catch (Exception e) {
            log.error("==========>初始化百家乐游戏失败!<==========\n", e);
            throw new RuntimeException("==========>初始化百家乐游戏失败!<==========\n", e);
        }
    }

    @Override
    public void shutdown() {
        // 清除房间内的数据
    }
}
