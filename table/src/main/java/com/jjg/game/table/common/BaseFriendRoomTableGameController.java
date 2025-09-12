package com.jjg.game.table.common;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.data.FriendRoom;
import com.jjg.game.core.data.FriendRoomBillHistoryBean;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.room.base.ERoomItemReason;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.room.FriendRoomBillHistoryHelper;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.SettlementData;
import com.jjg.game.room.friendroom.AbstractFriendRoomController;
import com.jjg.game.room.friendroom.FriendRoomSampleUtils;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;

import java.util.HashMap;
import java.util.Map;

/**
 * 好友房百人游戏控制器
 *
 * @author 2CL
 */
public abstract class BaseFriendRoomTableGameController<G extends TableGameDataVo> extends BaseTableGameController<G> {

    public BaseFriendRoomTableGameController(AbstractRoomController<Room_BetCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    protected boolean checkRoomCanNextRound() {
        boolean checkRes = super.checkRoomCanNextRound();
        if (checkRes) {
            // 进入下一个回合之前需要判断，庄家是否连续坐庄N次
            if (roomController instanceof AbstractFriendRoomController<?, ?> friendRoomController) {
                // 获取当前庄家ID
                long roomBankerId = friendRoomController.getRoom().roomBankerId();
                // 如果走到此处，庄家应该不会出现为0的情况
                if (roomBankerId > 0) {
                    boolean cancelBeBankerSuccess = false;
                    // 如果当前庄家已经申请过下庄
                    if (gameDataVo.getApplyCancelBeBankerPlayer() > 0) {
                        // 下庄，下庄之后，下一个自动成为庄家
                        int code =
                            friendRoomController.cancelBeBanker(
                                roomBankerId, ERoomItemReason.FRIEND_ROOM_CONTINUES_BANKER_ADD_GOLD);
                        if (code != Code.SUCCESS) {
                            log.error("申请庄家下庄时失败, 当前庄家ID：{}, err code: {}", roomBankerId, code);
                        } else {
                            cancelBeBankerSuccess = true;
                            gameDataVo.setApplyCancelBeBankerPlayer(0);
                            log.info("玩家：{} 申请下庄成功", roomBankerId);
                        }
                    }
                    // 如果庄家没有申请下庄
                    if (!cancelBeBankerSuccess) {
                        int maxRoundBeBanker =
                            SampleDataUtils.getIntGlobalData(GlobalSampleConstantId.BE_BANKER_MAX_ROUND);
                        // 如果连续坐庄次数超过限制，需要手动下庄
                        if (gameDataVo.getBeBankerTimes() >= maxRoundBeBanker) {
                            // 下庄，下庄之后，下一个自动成为庄家
                            int code =
                                friendRoomController.cancelBeBanker(
                                    roomBankerId, ERoomItemReason.FRIEND_ROOM_CONTINUES_BANKER_ADD_GOLD);
                            if (code != Code.SUCCESS) {
                                log.error("检查庄家自动下庄时失败, 当前庄家ID：{}, err code: {}", roomBankerId, code);
                            } else {
                                cancelBeBankerSuccess = true;
                                log.info("玩家：{} 上庄次数达到上限，自动下庄", roomBankerId);
                            }
                        }
                    }
                    // 如果玩家连续坐庄次数没有达到上限，继续判断
                    if (!cancelBeBankerSuccess) {
                        // 如果庄家准备金不够也需要自动下庄
                        int minBankerAmount =
                            FriendRoomSampleUtils.getRoomMinBankerAmount(gameDataVo.getRoomCfg().getId());
                        long resetGold = friendRoomController.getRoom().roomBankerResetGold();
                        if (resetGold < minBankerAmount) {
                            // 下庄，下庄之后，下一个自动成为庄家
                            int code =
                                friendRoomController.cancelBeBanker(
                                    roomBankerId, ERoomItemReason.FRIEND_ROOM_PREDICATE_GOLD_NOT_ENOUGH);
                            if (code != Code.SUCCESS) {
                                log.error("检查庄家剩余准备金时，自动下庄失败, 当前庄家ID：{}, err code: {}", roomBankerId, code);
                            } else {
                                log.info("庄家：{} 准备金不足，自动下庄", roomBankerId);
                            }
                        }
                    }
                } else {
                    log.error("游戏：{} 进入下一轮之前，庄家为空", gameDataVo.roomLogInfo());
                    checkRes = false;
                }
                if (checkRes) {
                    // 如果没有庄家则不能继续，需要通过玩家申请上庄重新启动游戏
                    checkRes = friendRoomController.getRoom().hasBanker();
                }
                // 如果检查的结果是不能继续
                if (!checkRes) {
                    friendRoomController.pauseGame();
                }
            }
        }
        return checkRes;
    }

    @Override
    public void dealBankerFlowing(long bankerFlowing, Map<Long, SettlementData> settlementDataMap) {
        if (roomController instanceof AbstractFriendRoomController<?, ?> friendRoomController) {
            if (bankerFlowing < 0) {
                // 扣除庄家的金币
                friendRoomController.deductBankerGold(bankerFlowing);
            } else {
                // 给庄家添加金币
                long roomBankerId = friendRoomController.getRoom().roomBankerId();
                // 给庄家添加金币
                addItem(roomBankerId, bankerFlowing,
                    ERoomItemReason.FRIEND_ROOM_ADD_ROOM_CREATOR_RATIO.withCfgId(getRoom().getRoomCfgId()));
            }
            // 需要记录
            FriendRoomBillHistoryDao dao = roomController.getRoomManager().getFriendRoomBillHistoryDao();
            if (!settlementDataMap.isEmpty()) {
                long roomTotalIncome =
                    settlementDataMap.values().stream()
                        .filter(s -> s.getBetWin() > 0)
                        .mapToLong(SettlementData::getBetWin)
                        .sum();
                if (roomTotalIncome <= 0) {
                    return;
                }
                // 构建基础历史数据bean
                FriendRoomBillHistoryBean historyBean = FriendRoomBillHistoryHelper.buildFriendRoom(getRoom());
                long roomCreatorTotalIncome =
                    settlementDataMap.values().stream()
                        .filter(s -> s.getRoomCreatorIncome() > 0)
                        .mapToLong(SettlementData::getRoomCreatorIncome)
                        .sum();
                historyBean.setTotalIncome(roomCreatorTotalIncome);
                long totalFlowing =
                    settlementDataMap.values().stream()
                        .filter(s -> s.getBetWin() > 0)
                        .mapToLong(SettlementData::getBetWin)
                        .sum();
                historyBean.setTotalFlowing(totalFlowing);
                historyBean.setItemId(getGameTransactionItemId());
                historyBean.setMonth(TimeHelper.getMonthNumerical());
                historyBean.setPartInPlayerIncome(
                    settlementDataMap.entrySet().stream()
                        .collect(HashMap::new,
                            (map, e)
                                -> map.put(
                                e.getKey(),
                                e.getValue().getBetWin() > 0
                                    ? e.getValue().getBetWin()
                                    : -1 * e.getValue().getBetTotal()),
                            HashMap::putAll));
                // 添加历史
                dao.addFriendRoomBillHistory(historyBean);
            }
        }
    }

    @Override
    public void respRoomInitInfo(PlayerController playerController) {
        if (roomController instanceof AbstractFriendRoomController<?, ?> friendRoomController) {
            friendRoomController.broadFriendRoomChange();
        }
    }

    @Override
    protected boolean checkPlayerCanExitWhenNoOperate(GamePlayer gamePlayer) {
        // 庄家不能被踢出
        FriendRoom room = (FriendRoom) roomController.getRoom();
        long roomBankerId = room.roomBankerId();
        return roomBankerId != gamePlayer.getId();
    }

    @Override
    protected void nextRoundStart() {
        super.nextRoundStart();
        // 添加坐庄次数
        gameDataVo.addBeBankerTimes();
    }
}
