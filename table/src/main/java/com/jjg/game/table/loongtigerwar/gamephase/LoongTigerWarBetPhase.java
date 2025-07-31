package com.jjg.game.table.loongtigerwar.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.bean.ReqBetBean;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.res.NotifyPhaseChangInfo;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;
import com.jjg.game.table.loongtigerwar.manager.LoongTigerWarSampleManager;
import com.jjg.game.table.loongtigerwar.room.data.LoongTigerWarGameDataVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 下注
 *
 * @author 2CL
 */
public class LoongTigerWarBetPhase extends BaseTableBetPhase<LoongTigerWarGameDataVo> {
    public LoongTigerWarBetPhase(AbstractGameController<Room_BetCfg, LoongTigerWarGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        broadcastMsgToRoom(TableMessageBuilder.getNotifyPhaseChangInfo(EGamePhase.BET, gameDataVo.getPhaseEndTime()));
    }


}
