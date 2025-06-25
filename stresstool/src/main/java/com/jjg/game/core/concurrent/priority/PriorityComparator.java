package com.jjg.game.core.concurrent.priority;

import com.jjg.game.core.concurrent.BaseHandler;
import com.jjg.game.utils.LoggerUtils;

import java.util.Comparator;

/**
 * 优先级队列比较器
 *
 * @author 2CL ou.yangceng
 */
public class PriorityComparator {
  public static Comparator<Runnable> comparator =
      new Comparator<Runnable>() {
        @Override
        public int compare(Runnable o1, Runnable o2) {
          BasePriorityHandlerRunnable basePriorityHandlerRunnable1 =
              (BasePriorityHandlerRunnable) o1;
          BasePriorityHandlerRunnable basePriorityHandlerRunnable2 =
              (BasePriorityHandlerRunnable) o2;
          PlayerPriority p1 = basePriorityHandlerRunnable1.getPlayerPriority();
          PlayerPriority p2 = basePriorityHandlerRunnable2.getPlayerPriority();
          // 先比较优先级,按优先级倒序排序
          int pr1 = (p1 == null ? 0 : p1.getPriority());
          int pr2 = (p2 == null ? 0 : p2.getPriority());

          // (return pr2 - pr1);

          if (pr1 < pr2) {
            return 1;
          } else if (pr1 > pr2) {
            return -1;
          } else {
            // 如果优先级相同,再比较时间,按入队时间正序排序
            BaseHandler handler1 = basePriorityHandlerRunnable1.getHandler();
            BaseHandler handler2 = basePriorityHandlerRunnable2.getHandler();
            if (handler1 == null) {
              LoggerUtils.LOGGER.error(
                  "handler1 is null," + basePriorityHandlerRunnable1.getClass().getSimpleName());
            }
            if (handler2 == null) {
              LoggerUtils.LOGGER.error(
                  "handler2 is null," + basePriorityHandlerRunnable2.getClass().getSimpleName());
            }
            // 升序
            return (int) (handler1.getTime() - handler2.getTime());

            //            if (time1 > time2) {
            //              return 1;
            //            } else if (time1 < time2) {
            //              return -1;
            //            } else {
            //              return 0;
            //            }
          }
        }
      };
}
