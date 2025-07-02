package com.jjg.game.common.concurrent.priority;

import com.jjg.game.common.concurrent.BaseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * 优先级队列比较器
 *
 * @author 2CL
 */
public class PriorityComparator {

    private static final Logger logger = LoggerFactory.getLogger(PriorityComparator.class);

    public static Comparator<BasePriorityHandlerRunnable> comparator =
        (o1, o2) -> {
            PlayerPriority p1 = o1.getPlayerPriority();
            PlayerPriority p2 = o2.getPlayerPriority();
            // 先比较优先级,按优先级倒序排序
            int pr1 = (p1 == null ? 0 : p1.getPriority());
            int pr2 = (p2 == null ? 0 : p2.getPriority());

            if (pr1 < pr2) {
                return 1;
            } else if (pr1 > pr2) {
                return -1;
            } else {
                // 如果优先级相同,再比较时间,按入队时间正序排序
                BaseHandler handler1 = o1.getHandler();
                BaseHandler handler2 = o2.getHandler();
                if (handler1 == null) {
                    logger.error("handler1 is null,{}", o1.getClass().getSimpleName());
                }
                if (handler2 == null) {
                    logger.error("handler2 is null,{}", o2.getClass().getSimpleName());
                }
                // 升序
                assert handler1 != null;
                assert handler2 != null;
                return (int) (handler1.getTime() - handler2.getTime());
            }
        };
}
