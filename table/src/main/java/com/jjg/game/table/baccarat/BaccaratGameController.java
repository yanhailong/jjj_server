package com.jjg.game.table.baccarat;

import com.alibaba.fastjson.JSON;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.sample.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.gamephase.BaccaratSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableBetPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratWaitReadyPhase;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.NotifyBaccaratSettlementInfo;
import com.jjg.game.table.baccarat.message.resp.RespBaccaratTableInfo;
import com.jjg.game.table.common.BaseTableGameController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * 百家乐游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BACCARAT)
public class BaccaratGameController extends BaseTableGameController<BaccaratGameDataVo> {

    private static final Logger log = LoggerFactory.getLogger(BaccaratGameController.class);

    public BaccaratGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.BACCARAT;
    }

    @Override
    public void autoRunGamePhase() {
        super.autoRunGamePhase();
    }

    @Override
    public void sendRoomInitInfo(PlayerController playerController) {
        EGamePhase eGamePhase = getCurrentGamePhase();
        // 如果刚好处于等待阶段则直接设置为下注阶段
        if (eGamePhase == EGamePhase.WAIT_READY) {
            eGamePhase = EGamePhase.BET;
        }
        RespBaccaratTableInfo baccaratTableInfo = null;
        // 如果在结算阶段需要从缓存中读取数据
        if (eGamePhase == EGamePhase.GAME_ROUND_OVER_SETTLEMENT) {
            NotifyBaccaratSettlementInfo settlementInfo = gameDataVo.getBaccaratSettlementInfo();
            baccaratTableInfo =
                BaccaratMessageBuilder.buildRespBaccaratTableInfo(gameDataVo, eGamePhase, settlementInfo);
        } else if (eGamePhase == EGamePhase.BET) {
            baccaratTableInfo =
                BaccaratMessageBuilder.buildRespBaccaratTableInfo(gameDataVo, eGamePhase, null);
        }
        if (baccaratTableInfo == null) {
            log.error("玩家：{} 获取百家乐桌面数据为空 room: {} cfgId: {}",
                playerController.playerId(), gameDataVo.getRoomId(), gameDataVo.getRoomCfg().getId());
        }
        log.info("百家乐房间初始化数据：{} ", JSON.toJSONString(baccaratTableInfo));
        // send
        playerController.send(Objects.requireNonNullElseGet(baccaratTableInfo,
            () -> new RespBaccaratTableInfo(Code.FAIL)));
    }

    /**
     * 百家乐的房间不会停止
     *
     * @return 是否停止
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        // 初始等待
        gamePhases.add(new BaccaratWaitReadyPhase(this));
        // 押注阶段
        gamePhases.add(new BaccaratTableBetPhase(this));
        // 进入结算(发牌、亮牌、补牌、结算对服务端来说只有一个阶段)
        gamePhases.add(new BaccaratSettlementPhase(this));
        return gamePhases;
    }

    @Override
    protected BaccaratGameDataVo copyRoomDataVo(GameDataVo<Room_BetCfg> roomData) {
        return new BaccaratGameDataVo(roomData.getRoomCfg());
    }

    @Override
    protected void phaseRunOver() {

    }

    @Override
    public void initGame() {
        // TODO 初始化机器人
        // 初始化启动流程

    }
}
