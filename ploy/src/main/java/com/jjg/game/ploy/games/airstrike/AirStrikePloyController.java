package com.jjg.game.ploy.games.airstrike;

import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.ploy.controller.AbstractMultiPloyController;
import com.jjg.game.ploy.games.airstrike.data.AirStrikePlayerPloyGameData;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/3/19
 */
@Component
public class AirStrikePloyController extends AbstractMultiPloyController<AirStrikePlayerPloyGameData> {
    public AirStrikePloyController() {
        super(LoggerFactory.getLogger(AirStrikePloyController.class), AirStrikePlayerPloyGameData.class);
    }

    @Override
    protected AbstractResponse buildResEnterGameMessage(int code, int gameType, int roomCfgId, AirStrikePlayerPloyGameData playerGameData) {
        return null;
    }

    @Override
    protected AbstractMessage buildResBetMessage(int code, AirStrikePlayerPloyGameData playerGameData, int oddsType) {
        return null;
    }
}
