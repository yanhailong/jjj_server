package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.common.gamephase.WaitReadyPhase;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;
import com.jjg.game.table.loongtigerwar.room.manager.LoongTigerWarRoomGameController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author 2CL
 */
public class LoongTigerWarReadyPhase extends WaitReadyPhase<LoongTigerWarGameDataVo> {

    public LoongTigerWarReadyPhase(LoongTigerWarRoomGameController gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        gameDataVo.setPhaseEndTime(getPhaseRunTime() + System.currentTimeMillis());
    }

    @Override
    public void phaseFinish() {
        LoongTigerWarGameDataVo gameDataVo1 = getGameDataVo();
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
