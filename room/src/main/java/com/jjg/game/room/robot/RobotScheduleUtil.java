package com.jjg.game.room.robot;

import cn.hutool.core.lang.WeightRandom;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.common.concurrent.BaseHandler;
import com.jjg.game.common.utils.WheelTimerUtil;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ChessRobotCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 机器人定时工具类
 *
 * @author lm
 * @date 2025/9/30 10:12
 */
public class RobotScheduleUtil {

    /**
     * 获取机器人延迟执行时间
     *
     * @param id ChessRobotCfg配置ID
     * @return 延迟时间
     */
    public static int getChessExecutionDelay(int id) {
        ChessRobotCfg chessRobotCfg = GameDataManager.getChessRobotCfg(id);
        List<List<Integer>> delayTime = chessRobotCfg.getDelayTime();
        WeightRandom<Integer> random = new WeightRandom<>();
        for (int i = 0; i < delayTime.size(); i++) {
            List<Integer> list = delayTime.get(i);
            if (list.size() != 3) {
                continue;
            }
            random.add(i, list.getFirst());
        }
        Integer nextTime = random.next();
        if (nextTime == null) {
            return 1500;
        }
        List<Integer> list = delayTime.get(nextTime);
        return RandomUtil.randomInt(list.get(1), list.getLast());
    }

    /**
     * 机器人定时任务，会转到房间绑定的线程中
     *
     * @param roomController 房间控制器
     * @param processor      执行的任务
     * @param delayTime      延迟时间
     */
    public static void schedule(AbstractRoomController<?, ?> roomController, BaseHandler<?> processor, int delayTime) {
        WheelTimerUtil.schedule(() -> roomController.getRoomProcessor().tryPublish(0, processor), delayTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 周期性任务（固定间隔）
     *
     * @param initialDelay 首次延迟
     * @param period       间隔时间
     * @param unit         时间单位
     */
    public static void scheduleAtFixedRate(AbstractRoomController<?, ?> roomController, BaseHandler<?> processor,
                                           long initialDelay,
                                           long period,
                                           TimeUnit unit) {
        WheelTimerUtil.scheduleAtFixedRate(() -> roomController.getRoomProcessor().tryPublish(0, processor), initialDelay, period, unit);
    }
}
