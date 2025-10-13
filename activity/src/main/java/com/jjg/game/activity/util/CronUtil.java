package com.jjg.game.activity.util;

import com.jjg.game.common.proto.Pair;
import org.springframework.scheduling.support.CronExpression;

import java.time.LocalDateTime;

/**
 * @author lm
 * @date 2025/10/11 14:26
 */
public class CronUtil {
    /**
     * 获取下次活动开启的结束和开始时间
     *
     * @param startCronStr 开始的cron
     * @param endCronStr   结束的cron
     * @param offset       时间
     * @return 开始时间,结束时间
     */
    public static Pair<LocalDateTime, LocalDateTime> getNextOpenTime(String startCronStr, String endCronStr, LocalDateTime offset) {
        CronExpression startCron = CronExpression.parse(startCronStr);
        LocalDateTime start = startCron.next(offset);
        if (start != null) {
            CronExpression endCron = CronExpression.parse(endCronStr);
            LocalDateTime end = endCron.next(start);
            if (end != null) {
                return Pair.newPair(start, end);
            }
        }
        return null;
    }
}
