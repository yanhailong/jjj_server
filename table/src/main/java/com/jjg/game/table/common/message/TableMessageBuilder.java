package com.jjg.game.table.common.message;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.message.res.NotifyTableRoomPlayerInfoChange;
import com.jjg.game.table.common.message.res.RespTablePlayerInfo;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;

import java.util.*;

/**
 * 构建房间相关消息
 *
 * @author 2CL
 */
public class TableMessageBuilder {


    /**
     * 玩家数据列表
     */
    public static RespTablePlayerInfo buildTableAllPlayerInfo(TableGameDataVo dataVo) {
        RespTablePlayerInfo playerBetInfo = new RespTablePlayerInfo(Code.SUCCESS);
        playerBetInfo.tablePlayerInfo = new ArrayList<>();
        for (Map.Entry<Long, GamePlayer> entry : dataVo.getGamePlayerMap().entrySet()) {
            GamePlayer gamePlayer = entry.getValue();
            TablePlayerInfo tablePlayerInfo = buildTablePlayerInfo(gamePlayer);
            playerBetInfo.tablePlayerInfo.add(tablePlayerInfo);
        }
        // 按照金币给玩家排序
        playerBetInfo.tablePlayerInfo.sort(Comparator.comparingLong(o -> o.goldNum));
        return playerBetInfo;
    }


    /**
     * 构建游戏的玩家基础信息
     */
    public static List<TablePlayerInfo> buildTablePlayerInfo(List<Long> playerIds, TableGameDataVo tableGameDataVo) {
        Map<Long, GamePlayer> gamePlayerMap = tableGameDataVo.getGamePlayerMap();
        List<TablePlayerInfo> tablePlayerInfos = new ArrayList<>(playerIds.size());
        for (Long playerId : playerIds) {
            GamePlayer gamePlayer = gamePlayerMap.get(playerId);
            if (Objects.isNull(gamePlayer)) {
                continue;
            }
            tablePlayerInfos.add(buildTablePlayerInfo(gamePlayer));
        }
        return tablePlayerInfos;
    }

    /**
     * 构建游戏的前6玩家基础信息
     */
    public static List<TablePlayerInfo> buildTablePlayerInfo(TableGameDataVo tableGameDataVo) {
        List<GamePlayer> gamePlayers = tableGameDataVo.getGamePlayerMap()
                .values()
                .stream().sorted(Comparator.comparingLong(Player::getGold).reversed())
                .limit(7)
                .toList();
        List<TablePlayerInfo> tablePlayerInfos = new ArrayList<>(gamePlayers.size());
        for (GamePlayer gamePlayer : gamePlayers) {
            tablePlayerInfos.add(buildTablePlayerInfo(gamePlayer));
        }
        return tablePlayerInfos;
    }

    /**
     * 构建游戏的玩家基础信息
     */
    public static TablePlayerInfo buildTablePlayerInfo(GamePlayer gamePlayer) {
        TablePlayerInfo tablePlayerInfo = new TablePlayerInfo();
        tablePlayerInfo.playerId = gamePlayer.getId();
        tablePlayerInfo.playerName = gamePlayer.getNickName();
        tablePlayerInfo.local = gamePlayer.getIp();
        tablePlayerInfo.vipLevel = gamePlayer.getVipLevel();
        tablePlayerInfo.goldNum = gamePlayer.getGold();
        List<Pair<Boolean, Long>> betInfoList = gamePlayer.getTableGameData().getBetInfoList();
        long totalBet = 0;
        int winNum = 0;
        for (Pair<Boolean, Long> betInfo : betInfoList) {
            totalBet += betInfo.getSecond();
            if (betInfo.getFirst()) {
                winNum++;
            }
        }
        tablePlayerInfo.totalBet = totalBet;
        tablePlayerInfo.winCount = winNum;
        return tablePlayerInfo;
    }

    /**
     * 通知场上玩家信息有变化
     */
    public static NotifyTableRoomPlayerInfoChange buildNotifyTableRoomPlayerInfoChange(long changedPlayerId,
                                                                                       TableGameDataVo dataVo) {
        NotifyTableRoomPlayerInfoChange infoChange = new NotifyTableRoomPlayerInfoChange();
        infoChange.changedPlayerId = changedPlayerId;
        infoChange.tableChangedPlayerInfos = new ArrayList<>();
        List<GamePlayer> sortedPlayersByGold =
                dataVo.getGamePlayerMap().values().stream().sorted(Comparator.comparingLong(Player::getGold).reversed())
                        .limit(7).toList();
        infoChange.totalPlayerNum = sortedPlayersByGold.size();
        for (GamePlayer gamePlayer : sortedPlayersByGold) {
            TablePlayerInfo tablePlayerInfo = buildTablePlayerInfo(gamePlayer);
            infoChange.tableChangedPlayerInfos.add(tablePlayerInfo);
        }
        return infoChange;
    }

    /**
     * 获取玩家的结算信息
     *
     * @param playerGet 结算的玩家获得的金币
     */
    public static List<PlayerChangedGold> getPlayerSettleInfos(Map<Long, Long> playerGet) {
        List<PlayerChangedGold> settleInfoArrayList = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : playerGet.entrySet()) {
            PlayerChangedGold info = new PlayerChangedGold();
            info.playerWinGold = entry.getValue();
            info.playerId = entry.getKey();
            settleInfoArrayList.add(info);
        }
        return settleInfoArrayList;
    }
}
