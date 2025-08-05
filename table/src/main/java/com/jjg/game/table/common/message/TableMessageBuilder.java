package com.jjg.game.table.common.message;

import com.jjg.game.common.proto.Pair;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.Player;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.room.data.room.GameDataVo;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.GlobalConfigCfg;
import com.jjg.game.table.common.TableConstant;
import com.jjg.game.table.common.data.TableGameDataVo;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;
import com.jjg.game.table.common.message.req.NotifyTableExitRoom;
import com.jjg.game.table.common.message.req.NotifyTableLongTimeNoOperate;
import com.jjg.game.table.common.message.res.NotifyPhaseChangInfo;
import com.jjg.game.table.common.message.res.NotifyTableRoomConf;
import com.jjg.game.table.common.message.res.NotifyTableRoomPlayerInfoChange;
import com.jjg.game.table.common.message.res.RespTablePlayerInfo;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;

import java.util.*;
import java.util.stream.Stream;

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
        Map<Long, GamePlayer> sortedGamePlayers = getSortedGamePlayer(dataVo, 0);
        for (GamePlayer gamePlayer : sortedGamePlayers.values()) {
            TablePlayerInfo tablePlayerInfo = buildTablePlayerInfo(gamePlayer);
            playerBetInfo.tablePlayerInfo.add(tablePlayerInfo);
        }
        // 按照金币给玩家排序
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
    public static List<TablePlayerInfo> buildPlayerInfoOnTable(GameDataVo<?> tableGameDataVo) {
        return buildTablePlayerInfo(tableGameDataVo, TableConstant.ON_TABLE_PLAYER_NUM);
    }

    /**
     * 构建游戏的前6玩家基础信息
     */
    public static List<TablePlayerInfo> buildTablePlayerInfo(GameDataVo<?> tableGameDataVo, int limit) {
        Map<Long, GamePlayer> sortedGamePlayers = getSortedGamePlayer(tableGameDataVo, limit);
        List<TablePlayerInfo> tablePlayerInfos = new ArrayList<>(sortedGamePlayers.size());
        for (GamePlayer gamePlayer : sortedGamePlayers.values()) {
            tablePlayerInfos.add(buildTablePlayerInfo(gamePlayer));
        }
        return tablePlayerInfos;
    }


    /**
     * 构建游戏的前7玩家基础信息,包含玩家自己的数据
     */
    public static List<TablePlayerInfo> buildTablePlayerInfo(
        long playerId, TableGameDataVo tableGameDataVo, int limit) {
        Map<Long, GamePlayer> sortedGamePlayer = getSortedGamePlayer(tableGameDataVo, limit);
        List<GamePlayer> gamePlayers = new ArrayList<>(sortedGamePlayer.values());
        if (!sortedGamePlayer.containsKey(playerId)) {
            if (gamePlayers.size() == limit) {
                gamePlayers.remove(gamePlayers.size() - 1);
            }
            gamePlayers.add(tableGameDataVo.getGamePlayer(playerId));
        }
        List<TablePlayerInfo> tablePlayerInfos = new ArrayList<>(gamePlayers.size());
        for (GamePlayer gamePlayer : gamePlayers) {
            tablePlayerInfos.add(buildTablePlayerInfo(gamePlayer));
        }
        return tablePlayerInfos;
    }


    /**
     * 获取根据金币从大到小排序的GamePlayer列表
     *
     * @param tableGameDataVo 数据源
     * @param limit           列表长度（小于等于0为全部）
     * @return 排序后的GamePlayer列表
     */
    private static Map<Long, GamePlayer> getSortedGamePlayer(GameDataVo<?> tableGameDataVo, int limit) {
        Stream<GamePlayer> sorted = tableGameDataVo.getGamePlayerMap()
            .values()
            .stream()
            .sorted(Comparator.comparingLong(Player::getGold).reversed());
        if (limit > 0) {
            sorted = sorted.limit(limit);
        }
        return sorted.collect(LinkedHashMap::new, (map, e) -> map.put(e.getId(), e), HashMap::putAll);
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

    public static NotifyPhaseChangInfo getNotifyPhaseChangInfo(EGamePhase gamePhase, long endTime) {
        NotifyPhaseChangInfo notifyPhaseChangInfo = new NotifyPhaseChangInfo();
        notifyPhaseChangInfo.gamePhase = gamePhase;
        notifyPhaseChangInfo.endTime = endTime;
        return notifyPhaseChangInfo;
    }

    /**
     * 通知场上玩家信息有变化
     */
    public static NotifyTableRoomPlayerInfoChange buildNotifyTableRoomPlayerInfoChange(
        long changedPlayerId, int sendSize, TableGameDataVo dataVo) {
        NotifyTableRoomPlayerInfoChange infoChange = new NotifyTableRoomPlayerInfoChange();
        infoChange.changedPlayerId = changedPlayerId;
        infoChange.tableChangedPlayerInfos = new ArrayList<>();
        Map<Long, GamePlayer> sortedGamePlayers = getSortedGamePlayer(dataVo, sendSize);
        List<GamePlayer> sortedPlayersByGold = new ArrayList<>(sortedGamePlayers.values());
        infoChange.totalPlayerNum = dataVo.getPlayerNum();
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
    public static List<PlayerChangedGold> getPlayerSettleInfos(
        Map<Long, DefaultKeyValue<Long, Long>> playerGet, TableGameDataVo gameDataVo) {
        List<PlayerChangedGold> settleInfoArrayList = new ArrayList<>();
        for (Map.Entry<Long, DefaultKeyValue<Long, Long>> entry : playerGet.entrySet()) {
            PlayerChangedGold info = new PlayerChangedGold();
            DefaultKeyValue<Long, Long> keyValue = entry.getValue();
            info.playerWinGold = keyValue.getValue() - keyValue.getKey();
            info.playerId = entry.getKey();
            info.playerBetGold = keyValue.getKey();
            GamePlayer gamePlayer = gameDataVo.getGamePlayer(entry.getKey());
            if (Objects.nonNull(gamePlayer)) {
                info.playerCurGold = gamePlayer.getGold();
            }
            settleInfoArrayList.add(info);
        }
        return settleInfoArrayList;
    }

    /**
     * 构建桌面押注信息
     */
    public static List<BetTableInfo> buildBetTableInfos(TableGameDataVo gameDataVo, boolean needPlayerBetGold) {
        Map<Long, Map<Integer, List<Integer>>> areaTotalBet = gameDataVo.getPlayerBetInfo();
        Map<Integer, BetTableInfo> baccaratTableInfoMap = new HashMap<>();
        for (Map<Integer, List<Integer>> value : areaTotalBet.values()) {
            for (Map.Entry<Integer, List<Integer>> entry : value.entrySet()) {
                if (!baccaratTableInfoMap.containsKey(entry.getKey())) {
                    baccaratTableInfoMap.put(entry.getKey(), new BetTableInfo());
                    baccaratTableInfoMap.get(entry.getKey()).betIdx = entry.getKey();
                }
                BetTableInfo betTableInfo = baccaratTableInfoMap.get(entry.getKey());
                betTableInfo.betIdxTotal = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                // 刚进入和断线重连时需要金币列表
                if (needPlayerBetGold) {
                    if (betTableInfo.betGoldList == null) {
                        betTableInfo.betGoldList = new ArrayList<>();
                    }
                    betTableInfo.betGoldList.addAll(entry.getValue());
                }
            }
        }
        return new ArrayList<>(baccaratTableInfoMap.values());
    }


    /**
     * 添加玩家下注区域的数据
     */
    public static List<BetTableInfo> buildPlayerBetInfo(
        List<BetTableInfo> betTableInfos, TableGameDataVo gameDataVo, long playerId) {
        Map<Integer, BetTableInfo> tableInfoMap =
            betTableInfos.stream().collect(HashMap::new, (map, e) -> map.put(e.betIdx, e), HashMap::putAll);
        Map<Integer, List<Integer>> playerBetInfo = gameDataVo.getPlayerBetInfo(playerId);
        if (playerBetInfo == null) {
            return betTableInfos;
        }
        // 玩家区域信息
        for (Map.Entry<Integer, List<Integer>> entry : playerBetInfo.entrySet()) {
            long areaTotal = entry.getValue().stream().mapToInt(Integer::intValue).sum();
            if (!tableInfoMap.containsKey(entry.getKey())) {
                BetTableInfo betTableInfo = new BetTableInfo();
                betTableInfo.betIdx = entry.getKey();
                betTableInfo.playerBetTotal = areaTotal;
                tableInfoMap.put(entry.getKey(), betTableInfo);
            } else {
                tableInfoMap.get(entry.getKey()).playerBetTotal = areaTotal;
            }
        }
        return tableInfoMap.values().stream().toList();
    }

    /**
     * 构建桌面长时间无操作通知
     */
    public static NotifyTableLongTimeNoOperate buildNotifyTableLongTimeNoOperate(int langId) {
        NotifyTableLongTimeNoOperate notify = new NotifyTableLongTimeNoOperate();
        notify.langId = langId;
        return notify;
    }

    /**
     * 构建桌面退出房间通知
     */
    public static NotifyTableExitRoom buildNotifyTableExitRoom(int langId) {
        NotifyTableExitRoom notify = new NotifyTableExitRoom();
        notify.langId = langId;
        return notify;
    }

    /**
     * 构建房间相关配置通知
     */
    public static NotifyTableRoomConf buildNotifyTableRoomConf() {
        GlobalConfigCfg globalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.MAX_CHIP_ON_TABLE);
        // 推送房间配置
        NotifyTableRoomConf notifyTableRoomConf = new NotifyTableRoomConf();
        notifyTableRoomConf.maxChipOnTable = globalConfigCfg.getIntValue();
        return notifyTableRoomConf;
    }
}
