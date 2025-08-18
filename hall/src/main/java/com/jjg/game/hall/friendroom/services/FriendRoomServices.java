package com.jjg.game.hall.friendroom.services;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.IllegalNameCheckService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.friendroom.dao.FriendRoomInvitationCodeDao;
import com.jjg.game.hall.friendroom.dao.RoomFriendDao;
import com.jjg.game.hall.friendroom.data.FriendRoomFollowBean;
import com.jjg.game.hall.friendroom.message.FriendRoomMessageBuilder;
import com.jjg.game.hall.friendroom.message.req.ReqCreateFriendsRoom;
import com.jjg.game.hall.friendroom.message.res.NotifyFriendRoomPanelData;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;
import com.jjg.game.hall.utils.HallDataUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友房服务
 *
 * @author 2CL
 */
@Service
public class FriendRoomServices {

    @Autowired
    private RoomFriendDao roomFriendDao;
    @Autowired
    private IllegalNameCheckService illegalNameCheckService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private HallRoomDao hallRoomDao;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private FriendRoomInvitationCodeDao friendRoomInvitationCodeDao;

    /**
     * 创建好友房
     */
    public int createFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        Player player = corePlayerService.get(playerController.playerId());
        // 房间创建检查
        int checkResCode = checkCreateRoom(player, reqCreateFriendsRoom);
        if (checkResCode != Code.SUCCESS) {
            return checkResCode;
        }
        int roomCfgId = reqCreateFriendsRoom.roomCfgId;
        // 扣除道具
        CommonResult<PlayerPack> removeItem =
            playerPackService.removeItem(
                player.getId(), reqCreateFriendsRoom.itemId, reqCreateFriendsRoom.itemNum);
        // 移除道具失败
        if (!removeItem.success()) {
            return removeItem.code;
        }
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        // 创建房间
        FriendRoom friendRoom = hallRoomDao.createBetFriendRoom(
            player.getId(), warehouseCfg.getGameID(), roomCfgId, warehouseCfg.getEnterMax(), reqCreateFriendsRoom);
        if (friendRoom == null) {
            return Code.PARAM_ERROR;
        }
        // 通知前端房间创建
        return Code.SUCCESS;
    }

    /**
     * 检查是否能创建房间
     */
    private int checkCreateRoom(Player player, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        // 准备金检查
        if (reqCreateFriendsRoom.predictCostGoldNum < 0) {
            return Code.PARAM_ERROR;
        }
        // 房间名检查
        if (!illegalNameCheckService.illegalNameCheck(reqCreateFriendsRoom.roomAliasName)) {
            return Code.ILLEGAL_NAME;
        }
        // 检查场次是否存在
        RoomCfg roomCfg = GameDataManager.getRoomCfg(reqCreateFriendsRoom.roomCfgId);
        if (roomCfg != null) {
            return Code.PARAM_ERROR;
        }
        // 牌局时长合法性检查
        List<RoomExpendCfg> roomExpendCfgs = GameDataManager.getRoomExpendCfgList();
        RoomExpendCfg roomExpendCfg = roomExpendCfgs.stream()
            .filter(cfg -> cfg.getDurationtype() == 1 && cfg.getDurationTime() == reqCreateFriendsRoom.timeOfOpenRoom)
            .findAny()
            .orElse(null);
        if (roomExpendCfg == null) {
            return Code.PARAM_ERROR;
        }
        int moneyId = roomExpendCfg.getRequiredMoney().isEmpty() ? 0 : roomExpendCfg.getRequiredMoney().getFirst();
        int itemId = roomExpendCfg.getRequiredItem().isEmpty() ? 0 : roomExpendCfg.getRequiredItem().getFirst();
        // 检查道具ID合法性
        if (moneyId != reqCreateFriendsRoom.itemId && itemId != reqCreateFriendsRoom.itemId) {
            return Code.PARAM_ERROR;
        }
        // 房间数量检查
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        long playerRoomSize = hallRoomDao.getPlayerRoomSize(player.getId());
        if (playerRoomSize >= playerLevelConfigCfg.getRoomNum()) {
            return Code.CREATE_ROOM_TO_LIMIT;
        }
        // 准备金扣费检查
        PlayerPack playerPack = playerPackService.redisGet(player.getId());
        ItemCfg goldItemCfg =
            GameDataManager.getItemCfgList().stream().filter(cfg -> cfg.getType() == GameConstant.Item.TYPE_MONEY).findFirst().get();
        boolean useGold = itemId == goldItemCfg.getId();
        if (playerPack == null && !useGold) {
            return Code.NOT_ENOUGH;
        } else if (playerPack != null) {
            CommonResult<Long> removeRes = playerPack.removeItem(itemId, reqCreateFriendsRoom.itemNum);
            if (!removeRes.success()) {
                return removeRes.code;
            }
        }
        // 需要扣除的金币数量
        long needDeductGold = reqCreateFriendsRoom.predictCostGoldNum;
        if (useGold) {
            needDeductGold += reqCreateFriendsRoom.itemNum;
        }
        // 金币扣除检查
        if (player.getGold() < needDeductGold) {
            return Code.NOT_ENOUGH;
        }
        return Code.SUCCESS;
    }

    /**
     * 请求好友房数据panel
     */
    private void reqFriendPanelData(PlayerController playerController) {
        Player player = playerController.getPlayer();
        NotifyFriendRoomPanelData notifyFriendRoomPanelData = new NotifyFriendRoomPanelData();
        // 获取玩家创建的好友房数据
        List<FriendRoom> friendRoomList = hallRoomDao.getPlayerAllFriendRoom(player.getId());
        // 获取玩家关注列表
        List<FriendRoomFollowBean> friendRoomFollowBeans = roomFriendDao.getRoomFriendList(player.getId());
        List<Long> followedPlayerId =
            friendRoomFollowBeans.stream().map(FriendRoomFollowBean::getFollowedPlayerId).toList();
        List<Player> followedplayerList = corePlayerService.multiGetPlayer(followedPlayerId);
        // 好友信息
        List<BaseFriendRoomPlayerInfo> baseFriendRoomPlayerInfos = new ArrayList<>();
        for (Player followedPlayer : followedplayerList) {
            BaseFriendRoomPlayerInfo info = FriendRoomMessageBuilder.buildFriendRoomInfo(followedPlayer);
            baseFriendRoomPlayerInfos.add(info);
        }
        notifyFriendRoomPanelData.roomFriendInfos = baseFriendRoomPlayerInfos;
        // 房间信息
        List<FriendRoomBaseData> friendRoomBaseDataList = new ArrayList<>();
        for (FriendRoom friendRoom : friendRoomList) {
            FriendRoomBaseData friendRoomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(friendRoom);
            friendRoomBaseDataList.add(friendRoomBaseData);
        }
        notifyFriendRoomPanelData.roomBaseDataList = friendRoomBaseDataList;
        notifyFriendRoomPanelData.invitationCode = playerController.getPlayer().getFriendRoomInvitationCode();
        GlobalConfigCfg globalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_RESET_TIMES);
        notifyFriendRoomPanelData.invitationCodeResetTotalTimes = globalConfigCfg.getIntValue();
        notifyFriendRoomPanelData.playerNumOnTable =
            friendRoomBaseDataList.stream().map(a -> a.onlinePlayerNum).mapToInt(Long::intValue).sum();
        notifyFriendRoomPanelData.curTableNum =
            friendRoomBaseDataList.stream().map(a -> a.maxPlayerNum).mapToInt(a -> a).sum();
        notifyFriendRoomPanelData.maxTableNum =
            friendRoomBaseDataList.stream().map(data -> {
                Tuple2<Integer, Integer> tuple =
                    HallDataUtils.getRoomMaxLimit(GameDataManager.getWarehouseCfg(data.gameId));
                return tuple.getT2();
            }).mapToInt(a -> a).sum();
        notifyFriendRoomPanelData.maxPlayerNumOnTable =
            GameDataManager.getPlayerLevelConfigCfg(player.getLevel()).getRoomNum();
        notifyFriendRoomPanelData.invitationCodeResetRemainingTimes =
            friendRoomInvitationCodeDao.getUseTimes(player.getId());
        // 发送数据
        playerController.send(notifyFriendRoomPanelData);
    }
}
