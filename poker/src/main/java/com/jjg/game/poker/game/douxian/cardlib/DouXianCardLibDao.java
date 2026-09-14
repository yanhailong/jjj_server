package com.jjg.game.poker.game.douxian.cardlib;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.poker.game.common.cardlib.AbstractCardLibDao;
import org.springframework.stereotype.Component;

@Component
public class DouXianCardLibDao extends AbstractCardLibDao<DouXianCardLib> {

    public DouXianCardLibDao() {
        super(DouXianCardLib.class, CoreConst.GameType.DOU_XIAN);
    }

    public int getPlayerWinStreak(long playerId) {
        Object value = redisTemplate.opsForHash().get(getStreakKey(), String.valueOf(playerId));
        return value == null ? 0 : Integer.parseInt(value.toString());
    }

    public void setPlayerWinStreak(long playerId, int streak) {
        redisTemplate.opsForHash().put(getStreakKey(), String.valueOf(playerId), String.valueOf(streak));
    }
}
