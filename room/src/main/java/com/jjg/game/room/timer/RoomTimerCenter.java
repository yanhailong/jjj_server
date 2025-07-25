package com.jjg.game.room.timer;

import com.jjg.game.common.cluster.ClusterProcessorExecutors;
import com.jjg.game.common.concurrent.BaseFuncProcessor;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.concurrent.BaseProcessor;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.common.timer.BaseTimerCenter;
import com.jjg.game.core.data.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 房间内的timer,和房间线程绑定=玩家和房间交互的逻辑线程
 *
 * @author 2CL
 */
public class RoomTimerCenter extends BaseTimerCenter<RoomTimerEvent<IProcessorHandler, Room>> {

    private static final Logger log = LoggerFactory.getLogger(RoomTimerCenter.class);

    /**
     * 当前房间的线程池
     */
    private final ClusterProcessorExecutors executors;

    public RoomTimerCenter(String timerName, ClusterProcessorExecutors clusterProcessorExecutors) {
        super(timerName);
        this.executors = clusterProcessorExecutors;
    }

    /**
     * 给房间添加定时任务
     */
    @Override
    public void add(RoomTimerEvent<IProcessorHandler, Room> event) {
        super.add(event);
    }

    @Override
    protected void begin() {
        active = true;
    }

    @Override
    public void collate() {
    }

    @Override
    public void fire(long time) {
        // 如果游戏线程没有绑定、没有设置或者线程已经停止
        for (RoomTimerEvent<?, Room> event : array) {
            if (event == null || event.getCount() <= 0 || !event.getEnabled()) {
                array.remove(event);
                continue;
            }
            // 跳过还在执行中的任务
            if (event.isInFire()) {
                continue;
            }
            long roomId = event.getRoomId();
            // 如果房间销毁则不执行定时任务
            if (roomId <= 0) {
                log.warn("房间退出了，还在执行房间定时任务: {}", event);
                continue;
            }
            BaseFuncProcessor baseProcessor = executors.getProcessorById(roomId);
            if (time >= event.getNextTime()) {
                event.setInFire(true);
                baseProcessor.executeHandler(new BaseHandler() {
                    @Override
                    public void action() {
                        event.run();
                    }
                });
            }
        }
    }
}
