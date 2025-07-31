package com.jjg.game.table.redblackwar.gamephase;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.betsample.sample.bean.BetAreaCfg;
import com.jjg.game.table.common.gamephase.BaseTableBetPhase;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.ReqBetBean;
import com.jjg.game.table.common.message.req.ReqBet;
import com.jjg.game.table.common.message.res.NotifyPlayerBet;
import com.jjg.game.table.redblackwar.manager.RedBlackWarSampleManager;
import com.jjg.game.table.redblackwar.room.data.RedBlackWarGameDataVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 下注
 *
 * @author 2CL
 */
public class RedBlackWarBetPhase extends BaseTableBetPhase<RedBlackWarGameDataVo> {


    public RedBlackWarBetPhase(AbstractGameController<Room_BetCfg, RedBlackWarGameDataVo> gameController) {
        super(gameController);
    }

    @Override
    public void phaseDoAction() {
        super.phaseDoAction();
        broadcastMsgToRoom(TableMessageBuilder.getNotifyPhaseChangInfo(EGamePhase.BET, gameDataVo.getPhaseEndTime()));
    }

}
