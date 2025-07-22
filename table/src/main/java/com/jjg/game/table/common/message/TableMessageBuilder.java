package com.jjg.game.table.common.message;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.table.baccarat.data.BaccaratGameDataVo;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.res.NotifyTableRoomPlayerInfoChange;
import com.jjg.game.table.common.message.res.RespTablePlayerInfo;
import com.jjg.game.table.common.message.res.TablePlayerInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
            dataVo.getGamePlayerMap().values().stream().sorted(Comparator.comparingLong(Player::getGold).reversed()).toList();
        for (GamePlayer gamePlayer : sortedPlayersByGold) {
            TablePlayerInfo tablePlayerInfo = buildTablePlayerInfo(gamePlayer);
            infoChange.tableChangedPlayerInfos.add(tablePlayerInfo);
        }
        return infoChange;
    }
}
