package com.jjg.game.hall.friendroom.services;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.IllegalNameCheckService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.hall.dao.HallRoomDao;
import com.jjg.game.hall.friendroom.dao.FriendRoomRedisDao;
import com.jjg.game.hall.friendroom.dao.RoomFriendDao;
import com.jjg.game.hall.friendroom.data.FriendRoomFollowBean;
import com.jjg.game.hall.friendroom.message.FriendRoomMessageBuilder;
import com.jjg.game.hall.friendroom.message.req.*;
import com.jjg.game.hall.friendroom.message.res.*;
import com.jjg.game.hall.friendroom.message.struct.BaseFriendRoomPlayerInfo;
import com.jjg.game.hall.friendroom.message.struct.FriendRoomBaseData;
import com.jjg.game.hall.friendroom.message.struct.RoomFriendEnum.ERoomFriendListOperate;
import com.jjg.game.hall.utils.HallDataUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友房服务
 *
 * @author 2CL
 */
@Service
public class FriendRoomServices {

    private static final Logger log = LoggerFactory.getLogger(FriendRoomServices.class);
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
    private FriendRoomRedisDao friendRoomRedisDao;

    /**
     * 创建好友房
     */
    public int createFriendRoom(PlayerController playerController, ReqCreateFriendsRoom reqCreateFriendsRoom) {
        Player player = corePlayerService.get(playerController.playerId());
        // 房间创建检查
        int checkResCode = checkCreateRoom(player, reqCreateFriendsRoom);
        if (checkResCode != Code.SUCCESS) {
            log.warn("请求创建好友房失败, 检查未通过 req = {}", reqCreateFriendsRoom);
            return checkResCode;
        }
        int invitationCode;
        if (player.getFriendRoomInvitationCode() == 0) {
            // 生成邀请码
            invitationCode = friendRoomRedisDao.genInvitationCode();
            if (invitationCode <= 0) {
                log.error("申请邀请失败，时间：{}", TimeHelper.getCurrentDateZeroMileTime());
                return Code.FAIL;
            }
        } else {
            invitationCode = 0;
        }
        int roomCfgId = reqCreateFriendsRoom.roomCfgId;
        // 扣除道具
        CommonResult<PlayerPack> removeItem =
            playerPackService.removeItem(
                player.getId(),
                new Item(reqCreateFriendsRoom.itemId, reqCreateFriendsRoom.itemNum),
                "create_friend_room"
            );
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
        // 保存邀请码
        if (invitationCode > 0) {
            // 保存玩家邀请码
            corePlayerService.doSave(player.getId(), (p) -> p.setFriendRoomInvitationCode(invitationCode));
            // 添加邀请码映射
            friendRoomRedisDao.addInvitationCode(invitationCode, player.getId());
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
    public void reqFriendPanelData(PlayerController playerController) {
        Player player = playerController.getPlayer();
        NotifyFriendRoomPanelData notifyFriendRoomPanelData = new NotifyFriendRoomPanelData();
        // 获取玩家创建的好友房数据
        List<FriendRoom> friendRoomList = hallRoomDao.getPlayerAllFriendRoom(player.getId());
        // 获取玩家关注列表
        List<FriendRoomFollowBean> friendRoomFollowBeans =
            roomFriendDao.getDefualtRoomFriendList(player.getId(), player.getFriendRoomInvitationCode());
        Map<Long, FriendRoomFollowBean> friendRoomFollowBeanMap =
            friendRoomFollowBeans.stream().collect(
                HashMap::new, (map, e) -> map.put(e.getFollowedPlayerId(), e), HashMap::putAll);
        List<Long> followedPlayerId =
            friendRoomFollowBeans.stream().map(FriendRoomFollowBean::getFollowedPlayerId).toList();
        List<Player> followedplayerList = corePlayerService.multiGetPlayer(followedPlayerId);
        // 好友信息
        List<BaseFriendRoomPlayerInfo> baseFriendRoomPlayerInfos = new ArrayList<>();
        for (Player followedPlayer : followedplayerList) {
            BaseFriendRoomPlayerInfo info = FriendRoomMessageBuilder.buildFriendRoomInfo(followedPlayer);
            long topUpTimeStamp = friendRoomFollowBeanMap.get(followedPlayer.getId()).getTopUpTimeStamp();
            info.isTopUp = topUpTimeStamp > 0;
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
        GlobalConfigCfg invitationResetGlobalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_RESET_TIMES);
        notifyFriendRoomPanelData.invitationCodeResetTotalTimes = invitationResetGlobalConfigCfg.getIntValue();
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
        // 邀请码重置使用次数
        int invitationCodeRestTimes = friendRoomRedisDao.getUseTimes(player.getId());
        // 邀请码剩余次数
        notifyFriendRoomPanelData.invitationCodeResetRemainingTimes =
            invitationCodeRestTimes >= invitationResetGlobalConfigCfg.getIntValue() ?
                0 :
                invitationResetGlobalConfigCfg.getIntValue() - invitationCodeRestTimes;
        // 发送数据
        playerController.send(notifyFriendRoomPanelData);
    }

    /**
     * 请求通过邀请码关注玩家
     */
    public void reqFollowedByInvitationCode(PlayerController playerController, int invitationCode) {
        Player player = playerController.getPlayer();
        // 先判断邀请码是否存在
        Long targetPlayerId = friendRoomRedisDao.getPlayerIdByInvitationCode(invitationCode);
        ResFollowByInvitationCode res = new ResFollowByInvitationCode(Code.FAIL);
        if (targetPlayerId == null || targetPlayerId == 0) {
            res.code = Code.ILLEGAL_FRIEND_ROOM_INVITATION_CODE;
            playerController.send(res);
            return;
        }
        // 通过邀请码添加关注好友
        roomFriendDao.addFriendByInvitationCode(player.getId(), targetPlayerId, invitationCode);
        Player targetPlayer = corePlayerService.get(targetPlayerId);
        res.playerInfo = FriendRoomMessageBuilder.buildFriendRoomInfo(targetPlayer);
        res.code = Code.SUCCESS;
        playerController.send(res);
    }

    /**
     * 请求操作关注列表
     */
    public void reqOperateFollowedFriendsList(PlayerController playerController, ReqOperateFollowedFriendsList req) {
        // 必须使用最新的player
        Player targetPlayer = corePlayerService.get(req.playerId);
        ResOperateFollowedFriendsList res = new ResOperateFollowedFriendsList(Code.FAIL);
        int targetPlayerInvitationCode = targetPlayer.getFriendRoomInvitationCode();
        // 如果目标玩家的邀请码为0则不可能被关注
        if (targetPlayerInvitationCode < 0) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        // 通过玩家ID，邀请码和目标玩家ID进行查找
        FriendRoomFollowBean friendRoomFollowBean =
            roomFriendDao.getRoomFriend(playerController.playerId(), req.playerId, targetPlayerInvitationCode);

        if (req.operate == ERoomFriendListOperate.TOP_UP) {
            friendRoomFollowBean.setTopUpTimeStamp(System.currentTimeMillis());
        } else if (req.operate == ERoomFriendListOperate.REMOVE) {
            friendRoomFollowBean.setRemoveTime(System.currentTimeMillis());
        }
        FriendRoomFollowBean updatedFriendRoomFollowBean =
            roomFriendDao.updateFriendRoomFollowBean(friendRoomFollowBean);
        if (updatedFriendRoomFollowBean != null) {
            res.code = Code.SUCCESS;
        }
        playerController.send(res);
    }

    /**
     * 请求关注的好友房间列表
     */
    public void reqFollowedFriendRoomList(PlayerController playerController, ReqFollowedFriendRoomList req) {
        ResFollowedFriendRoomList res = new ResFollowedFriendRoomList(Code.FAIL);
        // 判断当前操作的玩家是否是玩家关注的
        Player targetPlayer = corePlayerService.get(req.playerId);
        // 通过玩家ID，邀请码和目标玩家ID进行查找
        FriendRoomFollowBean friendRoomFollowBean =
            roomFriendDao.getRoomFriend(
                playerController.playerId(), req.playerId, targetPlayer.getFriendRoomInvitationCode());
        // 如果查找不到，有可能对方重新刷新了邀请码
        if (friendRoomFollowBean == null) {
            res.code = Code.NOT_FOLLOWED;
            playerController.send(res);
            return;
        }
        List<FriendRoom> friendRoomList = hallRoomDao.getPlayerAllFriendRoom(req.playerId);
        // 房间信息
        List<FriendRoomBaseData> friendRoomBaseDataList = new ArrayList<>();
        for (FriendRoom friendRoom : friendRoomList) {
            FriendRoomBaseData friendRoomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(friendRoom);
            friendRoomBaseDataList.add(friendRoomBaseData);
        }
        res.roomList = friendRoomBaseDataList;
        res.code = Code.SUCCESS;
        playerController.send(res);
    }

    /**
     * 请求刷新好友列表
     */
    public void reqRefreshFollowedFriendList(PlayerController playerController, ReqRefreshFollowedFriendList req) {
        ResRefreshFollowedFriendList res = new ResRefreshFollowedFriendList(Code.SUCCESS);
        if (req.pageSize < 0 || req.pageIdx < 0) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        Player player = corePlayerService.get(playerController.playerId());
        // 获取玩家关注列表
        List<FriendRoomFollowBean> friendRoomFollowBeans =
            roomFriendDao.getRoomFriendList(
                player.getId(), player.getFriendRoomInvitationCode(), req.pageIdx, req.pageSize);
        res.pageIdx = friendRoomFollowBeans.size() < req.pageSize ? -1 : req.pageIdx + 1;
        List<Long> followedPlayerId =
            friendRoomFollowBeans.stream().map(FriendRoomFollowBean::getFollowedPlayerId).toList();
        List<Player> followedplayerList = corePlayerService.multiGetPlayer(followedPlayerId);
        Map<Integer, Long> invitationCodeMap =
            friendRoomFollowBeans.stream().collect(
                HashMap::new, (map, e) -> map.put(e.getInvitationCode(), e.getId()), HashMap::putAll);
        List<Long> containsIds = new ArrayList<>();
        // 移除不包含的邀请码，过期的邀请码
        followedplayerList.removeIf(p -> {
                boolean checkRes = !invitationCodeMap.containsKey(p.getFriendRoomInvitationCode());
                if (!checkRes) {
                    containsIds.add(invitationCodeMap.get(p.getFriendRoomInvitationCode()));
                }
                return checkRes;
            }
        );
        invitationCodeMap.values().removeAll(containsIds);
        if (!invitationCodeMap.isEmpty()) {
            // 批量删除关注好友
            roomFriendDao.removeFollowedFriend(invitationCodeMap.values());
        }
        // 不包含的，或者过期的需要移除这部分的数据
        res.followedFriendList =
            followedplayerList.stream().map(FriendRoomMessageBuilder::buildFriendRoomInfo).toList();
    }

    /**
     * 请求刷新好友列表
     */
    public void reqOperateShieldPlayer(PlayerController playerController, ReqOperateShieldPlayer req) {

    }
}
