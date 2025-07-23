package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.common.gamephase.WaitReadyPhase;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import com.jjg.game.table.redblackwar.room.manager.RedBlackWarRoomGameController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author 2CL
 */
public class RedBlackWarWaitReadyPhase extends WaitReadyPhase<RedBlackWarGameDataVo> {

    public RedBlackWarWaitReadyPhase(RedBlackWarRoomGameController gameController) {
        super(gameController);
    }


    @Override
    public void phaseFinish() {
        RedBlackWarGameDataVo gameDataVo1 = getGameDataVo();
        Map<Long, GamePlayer> gamePlayerMap = gameDataVo1.getGamePlayerMap();
        if (gamePlayerMap.isEmpty()) {
            return;
        }
        List<Long> playerIds = gamePlayerMap.values()
                .stream()
                .sorted(Comparator.comparing(GamePlayer::getGold))
                .map(GamePlayer::getId)
                .limit(6)
                .toList();
        gameDataVo1.getRedBlackWarPlayerInfos().clear();
        gameDataVo1.getRedBlackWarPlayerInfos().addAll(playerIds);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
