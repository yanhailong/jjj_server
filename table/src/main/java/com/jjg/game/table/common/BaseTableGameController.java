package com.jjg.game.table.common;

import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.Room;
import com.jjg.game.core.data.RoomPlayer;
import com.jjg.game.core.pb.NotifyTableExitRoom;
import com.jjg.game.room.base.BaseGameTickTask;
import com.jjg.game.room.base.BaseGameTickTask.ETickTaskType;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.controller.AbstractPhaseGameController;
import com.jjg.game.room.controller.AbstractRoomController;
import com.jjg.game.room.data.robot.GameRobotPlayer;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.data.room.TablePlayerGameData;
import com.jjg.game.room.message.RoomMessageBuilder;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RobotCfg;
import com.jjg.game.sampledata.bean.Room_BetCfg;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.TableMessageBuilder;
import com.jjg.game.table.common.message.req.NotifyTableLongTimeNoOperate;
import com.jjg.game.table.common.message.res.NotifyTableRoomPlayerInfoChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * table类房间基类
 *
 * @author 2CL
 */
public abstract class BaseTableGameController<G extends TableGameDataVo> extends
        AbstractPhaseGameController<Room_BetCfg, G> {

    public BaseTableGameController(AbstractRoomController<Room_BetCfg, ? extends Room> roomController) {
        super(roomController);
    }

    @Override
    public <R extends Room> void initial(R room) {
        super.initial(room);
        // 玩家长时间未操作检查
        tickTaskMap.put(ETickTaskType.PLAYER_NO_OPERATE_CHECK,
                new BaseGameTickTask(TableConstant.PLAYER_NO_OPERATE_CHECK_INTERVAL) {
                    @Override
                    public void run(long triggeredTimestamp) {
                        checkPlayerNoOperateAlert();
                    }
                });
    }

    @Override
    protected void beforeEnterNextRound() {
        super.beforeEnterNextRound();
        // 检查机器人是否离开
        checkRobotGamePlayerExit();
    }

    @Override
    protected void nextRoundStart() {
        super.nextRoundStart();
        for (GamePlayer value : gameDataVo.getGamePlayerMapExceptRobot().values()) {
            // 玩家每轮进入时记录一下金币值，打点需要
            value.getTableGameData().setRoundStartPlayerGold(getItemNum(value.getId()));
        }
    }

    @Override
    public boolean canExitGame(long playerId) {
        return !(getCurrentGamePhase() == EGamePhase.BET && gameDataVo.getPlayerBetInfo().containsKey(playerId));
    }

    /**
     * 检查机器人是否需要离开房间
     */
    protected void checkRobotGamePlayerExit() {
        // 游戏下注列表
        List<Integer> betList = gameDataVo.getRoomCfg().getBetList();
        if (betList.isEmpty()) {
            return;
        }
        // 场上最大押注值
        int maxBetOnTable = betList.stream().max(Integer::compare).get();
        // 需要离开房间的机器人
        List<PlayerController> needExitRoomRobots = new ArrayList<>();
        for (Map.Entry<Long, GamePlayer> entry : gameDataVo.getGamePlayerMap().entrySet()) {
            if (entry.getValue() instanceof GameRobotPlayer gameRobotPlayer) {
                RobotCfg robotCfg = GameDataManager.getRobotCfg((int) gameRobotPlayer.getId());
                // 判断机器人是否需要离开房间
                boolean needExit = isNeedRobotPlayerExitRoom(gameRobotPlayer, robotCfg, maxBetOnTable);
                if (needExit) {
                    PlayerController playerController = roomController.getPlayerController(entry.getKey());
                    if (playerController != null) {
                        needExitRoomRobots.add(playerController);
                    }
                }
            }
        }
        if (!needExitRoomRobots.isEmpty()) {
            // 调用房间离开流程
            int exitCode = roomController.getRoomManager().robotPlayerExitRoom(needExitRoomRobots);
            if (exitCode != Code.SUCCESS) {
                log.error("机器人在 {} 中退出房间失败", gameDataVo.roomLogInfo());
            }
        }
    }

    /**
     * 是否需要机器人离开房间
     */
    protected boolean isNeedRobotPlayerExitRoom(GameRobotPlayer gameRobotPlayer, RobotCfg robotCfg, int maxBetOnTable) {
        boolean needExit = false;
        int exitMultiplier = robotCfg.getExitMultiplier();
        int playerMulti = (int) Math.floor(getItemNum(gameRobotPlayer.getId()) / (maxBetOnTable * 100.0));
        // 如果机器人当前携带的金币 / 押注游戏游戏 * 100 得出倍数小于了配置的倍数直接让机器人退出
        if (playerMulti < exitMultiplier) {
            needExit = true;
        } else if (RandomUtils.getRandomNumInt10000() < robotCfg.getExit()) {
            // 概率退出房间
            needExit = true;
        }
        return needExit;
    }

    @Override
    public GamePlayer onPlayerJoinRoom(PlayerController playerController, boolean gameStartStatus) {
        GamePlayer gamePlayer = super.onPlayerJoinRoom(playerController, gameStartStatus);
        gamePlayer.setTableGameData(new TablePlayerGameData());
        // 场上玩家重新排序
        resortPlayerOnTable();
        // 通知场上玩家加入
        NotifyTableRoomPlayerInfoChange playerInfoChange =
                TableMessageBuilder.buildNotifyTableRoomPlayerInfoChange(
                        this, playerController, TableConstant.ON_TABLE_PLAYER_NUM, gameDataVo);
        // 需要排除当前玩家，玩家刚进场给自己发送没有意义
        broadcastToPlayers(RoomMessageBuilder
                .newBuilder()
                .setData(playerInfoChange)
                .toAllPlayer()
                .exceptPlayer(playerController.playerId()));
        // 进入房间时需要更新操作时间
        gameDataVo.updatePlayerOperateTime(playerController.playerId());
        return gamePlayer;
    }

    @Override
    public void dispatchGamePhaseMsg(PlayerController playerController, PFMessage message) {
        super.dispatchGamePhaseMsg(playerController, message);
        // 更新玩家操作时间
        gameDataVo.updatePlayerOperateTime(playerController.playerId());
    }

    /**
     * 检查玩家未操作提示
     */
    protected void checkPlayerNoOperateAlert() {
        long currentTime = System.currentTimeMillis();
        // 获取真人玩家
        Map<Long, GamePlayer> gamePlayerMap = gameDataVo.getGamePlayerMapExceptRobot();
        if (gamePlayerMap.isEmpty()) {
            return;
        }
        // 长时间无操作触发提示时间
        int waitTime = gameDataVo.getRoomCfg().getWaitTime();
        // 长时间无操作触发提示语言ID
        int waitTimeTipLangId = gameDataVo.getRoomCfg().getTipText();
        // 长时间无操作退出触发时间
        int exitTime = gameDataVo.getRoomCfg().getEscTime() + waitTime;
        // 长时间无操作退出提示语言ID
        int exitTipLangId = gameDataVo.getRoomCfg().getEscTipText();
        for (Map.Entry<Long, GamePlayer> entry : gamePlayerMap.entrySet()) {
            GamePlayer gamePlayer = entry.getValue();
            // 检查当前玩家是否能在无操作的情况下退出
            if (!checkPlayerCanExitWhenNoOperate(gamePlayer)) {
                continue;
            }
            long playerLatestOperateTime = gamePlayer.getTableGameData().getPlayerLatestOperateTime();
            // 如果还没进入房间
            if (playerLatestOperateTime <= 0) {
                continue;
            }
            // 如果超过最大退出时间
            if (playerLatestOperateTime + exitTime < currentTime) {
                RoomPlayer roomPlayer = roomController.getRoomPlayer(gamePlayer.getId());
                if (roomPlayer == null || roomPlayer.isOnline()) {
                    NotifyTableExitRoom notifyTableExitRoom =
                            TableMessageBuilder.buildNotifyTableExitRoom(exitTipLangId);
                    broadcastToPlayers(RoomMessageBuilder.newBuilder()
                            .addPlayerId(entry.getKey()).setData(notifyTableExitRoom));
                } else {
                    roomController.getRoomManager().exitRoom(gamePlayer.getId());
                }
                continue;
            }
            // 如果超过最大操作等待时间
            if (playerLatestOperateTime + waitTime < currentTime && !gamePlayer.getTableGameData().isHasNotifyNoOperate()) {
                gamePlayer.getTableGameData().setHasNotifyNoOperate(true);
                NotifyTableLongTimeNoOperate notifyTableLongTimeNoOperate =
                        TableMessageBuilder.buildNotifyTableLongTimeNoOperate(waitTimeTipLangId);
                broadcastToPlayers(RoomMessageBuilder.newBuilder()
                        .addPlayerId(entry.getKey()).setData(notifyTableLongTimeNoOperate));
            }
        }
    }

    protected boolean checkPlayerCanExitWhenNoOperate(GamePlayer gamePlayer) {
        return true;
    }

    @Override
    public <R extends Room> CommonResult<R> onPlayerLeaveRoom(PlayerController playerController) {
        CommonResult<R> leaveRes = super.onPlayerLeaveRoom(playerController);
        // 场上玩家重新排序
        resortPlayerOnTable();
        // 通知场上玩家离开
        NotifyTableRoomPlayerInfoChange playerInfoChange =
                TableMessageBuilder.buildNotifyTableRoomPlayerInfoChange(
                        this, playerController, TableConstant.ON_TABLE_PLAYER_NUM, gameDataVo);
        // 需要排除当前玩家，因为给离开的玩家发送已经没有意义
        broadcastToPlayers(RoomMessageBuilder
                .newBuilder()
                .setData(playerInfoChange)
                .toAllPlayer()
                .exceptPlayer(playerController.playerId()));
        return leaveRes;
    }

    /**
     * 对场上玩家进行重新排序
     */
    private void resortPlayerOnTable() {
        List<GamePlayer> topGamePlayers =
                gameDataVo.getGamePlayerMap().values().stream()
                        .sorted((o1, o2) -> Long.compare(getItemNum(o2.getId()), getItemNum(o1.getId())))
                        .limit(TableConstant.ON_TABLE_PLAYER_NUM)
                        .toList();
        for (int i = 1; i <= topGamePlayers.size(); i++) {
            GamePlayer player = topGamePlayers.get(i - 1);
            player.getTableGameData().setSitNum(i);
        }
    }


}
