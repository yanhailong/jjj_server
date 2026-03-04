package com.jjg.game.table.russianlette;

import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.data.BetTableRoom;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.RoomType;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.BaseTableGameController;
import com.jjg.game.table.russianlette.data.RussianLetteGameDataVo;
import com.jjg.game.table.russianlette.gamephase.RussianLetteBetPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteDrawPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteSettlementPhase;
import com.jjg.game.table.russianlette.gamephase.RussianLetteTableWaitReadyPhase;
import com.jjg.game.table.russianlette.message.RussianLetteMessageBuilder;
import com.jjg.game.table.russianlette.message.resp.NotifyRussianLetteTableInfo;

import java.util.LinkedHashSet;

/**
 * 俄罗斯转盘游戏控制器
 * <p>
 * 四阶段循环（stageTime 配置索引）：
 * <pre>
 *   REST(stageTime[0]=4s) → BET(stageTime[1]=13s) → DRAW_ON(stageTime[2]=9s) → SETTLEMENT(stageTime[3]=5s) → REST ...
 * </pre>
 *
 * @author lhc
 */
@GameController(gameType = EGameType.RUSSIAN_ROULETTE, roomType = RoomType.BET_ROOM)
public class RussianLetteGameController extends BaseTableGameController<RussianLetteGameDataVo> {

    public RussianLetteGameController(AbstractRoomController<Room_BetCfg, BetTableRoom> roomController) {
        super(roomController);
    }

    /**
     * 玩家进入房间时推送初始化数据（桌面状态、历史记录、当前阶段信息）
     */
    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        NotifyRussianLetteTableInfo animalsTableInfo =
            RussianLetteMessageBuilder.notifyAnimalsTableInfo(playerController.playerId(), this, true);
        playerController.send(animalsTableInfo);
    }

    /**
     * 俄罗斯转盘为持续循环游戏，阶段结束后不结束房间
     */
    @Override
    protected boolean isGameOverAfterPhaseOver() {
        return false;
    }

    /**
     * 初始化四阶段游戏流程（LinkedHashSet 保证顺序，末尾阶段结束后循环回第一个）：
     * <ol>
     *   <li>{@link RussianLetteTableWaitReadyPhase}：REST 休闲阶段，清除上局数据</li>
     *   <li>{@link RussianLetteBetPhase}：BET 下注阶段，接受玩家押注</li>
     *   <li>{@link RussianLetteDrawPhase}：DRAW_ON 开奖阶段，生成随机号码并广播</li>
     *   <li>{@link RussianLetteSettlementPhase}：SETTLEMENT 结算阶段，计算并发放奖励</li>
     * </ol>
     */
    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> roomPhases = new LinkedHashSet<>();
        roomPhases.add(new RussianLetteTableWaitReadyPhase(this));  // REST        stageTime[0]
        roomPhases.add(new RussianLetteBetPhase(this));             // BET         stageTime[1]
        roomPhases.add(new RussianLetteDrawPhase(this));            // DRAW_ON     stageTime[2]
        roomPhases.add(new RussianLetteSettlementPhase(this));      // SETTLEMENT  stageTime[3]
        return roomPhases;
    }

    @Override
    protected RussianLetteGameDataVo createRoomDataVo(Room_BetCfg roomCfg) {
        return new RussianLetteGameDataVo(roomCfg);
    }

    @Override
    protected void phaseRunOver() {
        // 阶段运行结束钩子（无额外操作）
    }

    @Override
    public EGameType gameControlType() {
        return EGameType.RUSSIAN_ROULETTE;
    }
}
