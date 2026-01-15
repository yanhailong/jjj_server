package com.jjg.game.table.baccarat;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BetAreaCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.baccarat.gamephase.BaccaratSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableBetPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableWaitReadyPhase;
import com.jjg.game.table.baccarat.message.BaccaratMessageBuilder;
import com.jjg.game.table.baccarat.message.resp.RespBaccaratTableInfo;
import com.jjg.game.table.common.BaseTableGameController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 百家乐游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BACCARAT, roomType = RoomType.BET_ROOM)
public class BaccaratGameController extends BaseTableGameController<BaccaratGameDataVo> {

    public BaccaratGameController(AbstractRoomController<Room_BetCfg, ? extends Room> roomController) {
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
    public void respRoomInitInfo(PlayerController playerController) {
        EGamePhase eGamePhase = getCurrentGamePhase();
        // 如果在结算阶段需要从缓存中读取数据
        RespBaccaratTableInfo baccaratTableInfo =
            BaccaratMessageBuilder.buildRespBaccaratTableInfo(this, gameDataVo, eGamePhase);
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
        gamePhases.add(new BaccaratTableWaitReadyPhase(this));
        // 押注阶段
        gamePhases.add(new BaccaratTableBetPhase(this));
        // 进入结算(发牌、亮牌、补牌、结算对服务端来说只有一个阶段)
        gamePhases.add(new BaccaratSettlementPhase(this));
        return gamePhases;
    }

    @Override
    protected BaccaratGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new BaccaratGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {

    }
    public long calculationEffectiveWaterFlow(Map<Integer, List<Integer>> playerBetInfo) {
        Map<Integer, Long> effectiveWaterFlow = new HashMap<>();
        Map<Integer, BetAreaCfg> cfgMap = GameDataManager.getBetAreaCfgList().stream()
                .filter(betAreaCfg -> betAreaCfg.getGameID() == EGameType.BACCARAT.getGameTypeId())
                .collect(Collectors.toMap(BetAreaCfg::getAreaID, cfg -> cfg));
        for (Map.Entry<Integer, List<Integer>> listEntry : playerBetInfo.entrySet()) {
            Integer key = listEntry.getKey();
            BetAreaCfg betAreaCfg = cfgMap.get(key);
            if (Objects.isNull(betAreaCfg)) {
                continue;
            }
            List<Integer> value = listEntry.getValue();
            int sum = value.stream().mapToInt(Integer::intValue).sum();
            //计算有效流水
            Long bet = effectiveWaterFlow.getOrDefault(betAreaCfg.getRepulsionID(), 0L);
            if (bet > 0) {
                effectiveWaterFlow.put(betAreaCfg.getRepulsionID(), sum - bet);
            } else {
                effectiveWaterFlow.put(betAreaCfg.getId(), (long) sum);
            }
        }
        return effectiveWaterFlow.values().stream().mapToLong(Math::abs).sum();
    }
}
