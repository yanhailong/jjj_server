package com.jjg.game.table.baccarat;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.*;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.IRoomPhase;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.controller.GameController;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.room.friendroom.FriendRoomSampleUtils;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.baccarat.gamephase.BaccaratFriendSettlementPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableBetPhase;
import com.jjg.game.table.baccarat.gamephase.BaccaratTableWaitReadyPhase;

import java.util.LinkedHashSet;

/**
 * 百家乐好友房游戏控制器
 *
 * @author 2CL
 */
@GameController(gameType = EGameType.BACCARAT, roomType = RoomType.BET_TEAM_UP_ROOM)
public class BaccaratFriendGameController extends BaccaratGameController {

    public BaccaratFriendGameController(AbstractRoomController<Room_BetCfg, FriendRoom> roomController) {
        super(roomController);
    }

    @Override
    protected LinkedHashSet<IRoomPhase> initGamePhaseConf() {
        LinkedHashSet<IRoomPhase> gamePhases = new LinkedHashSet<>();
        // 初始等待
        gamePhases.add(new BaccaratTableWaitReadyPhase(this));
        // 押注阶段
        gamePhases.add(new BaccaratTableBetPhase(this));
        // 进入结算(发牌、亮牌、补牌、结算对服务端来说只有一个阶段)
        gamePhases.add(new BaccaratFriendSettlementPhase(this));
        return gamePhases;
    }

    @Override
    protected boolean checkRoomCanNextRound() {
        boolean checkRes = super.checkRoomCanNextRound();
        if (checkRes) {
            // 进入下一个回合之前需要判断，庄家是否连续坐庄N次
            if (roomController instanceof AbstractFriendRoomController<?> friendRoomController) {
                // 获取当前庄家ID
                long roomBankerId = friendRoomController.getRoom().roomBankerId();
                // 如果走到此处，庄家应该不会出现为0的情况
                if (roomBankerId != 0) {
                    int maxRoundBeBanker
                        = SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.BE_BANKER_MAX_ROUND);
                    // 如果超过限制，需要手动下庄
                    if (gameDataVo.getBeBankerTimes() >= maxRoundBeBanker) {
                        // 下庄，下庄之后，下一个自动成为庄家
                        int code = friendRoomController.cancelBeBanker(roomBankerId);
                        if (code != Code.SUCCESS) {
                            log.error("检查庄家自动下庄时失败, 当前庄家ID：{}, err code: {}", roomBankerId, code);
                        }
                    }
                    // 如果庄家准备金不够也需要自动下庄
                    int minBankerAmount = FriendRoomSampleUtils.getRoomMinBankerAmount(gameDataVo.getRoomCfg().getId());
                    long resetGold = friendRoomController.getRoom().roomBankerResetGold();
                    if (resetGold < minBankerAmount) {
                        // 下庄，下庄之后，下一个自动成为庄家
                        int code = friendRoomController.cancelBeBanker(roomBankerId);
                        if (code != Code.SUCCESS) {
                            log.error("检查庄家剩余准备金时，自动下庄失败, 当前庄家ID：{}, err code: {}", roomBankerId, code);
                        }
                    }
                } else {
                    log.error("游戏：{} 进入下一轮之前，庄家为空", gameDataVo.roomLogInfo());
                    checkRes = false;
                }
                if (checkRes) {
                    checkRes = friendRoomController.getRoom().hasBanker();
                }
            }
        }
        return checkRes;
    }

    @Override
    public <R extends Room> CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
        return super.onPlayerLeaveRoom(playerController);
    }

    @Override
    protected void nextRoundStart() {
        super.nextRoundStart();
        // 添加坐庄次数
        gameDataVo.addBeBankerTimes();
    }
}
