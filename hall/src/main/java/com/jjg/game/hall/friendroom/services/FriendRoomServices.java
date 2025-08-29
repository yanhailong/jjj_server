package com.jjg.game.hall.friendroom.services;

import com.jjg.game.common.baselogic.IConsoleReceiver;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst.GameMajorType;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedissonLock;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.dao.room.AbstractFriendRoomDao.CreateFriendsRoom;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao.GameBillResult;
import com.jjg.game.core.data.*;
import com.jjg.game.core.rpc.HallRoomBridge;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.IllegalNameCheckService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.hall.friendroom.constant.FriendRoomConstant;
import com.jjg.game.hall.friendroom.dao.FriendRoomFollowDao;
import com.jjg.game.hall.friendroom.dao.FriendRoomRedisDao;
import com.jjg.game.hall.friendroom.dao.HallFriendRoomDao;
import com.jjg.game.hall.friendroom.data.FriendRoomFollowBean;
import com.jjg.game.hall.friendroom.message.FriendRoomMessageBuilder;
import com.jjg.game.hall.friendroom.message.req.*;
import com.jjg.game.hall.friendroom.message.res.*;
import com.jjg.game.hall.friendroom.message.struct.*;
import com.jjg.game.hall.friendroom.message.struct.RoomFriendEnum.ERoomFriendListOperate;
import com.jjg.game.hall.room.HallRoomService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuple2;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 好友房服务
 *
 * @author 2CL
 */
@Service
public class FriendRoomServices implements IConsoleReceiver {

    private static final Logger log = LoggerFactory.getLogger(FriendRoomServices.class);
    @Autowired
    private FriendRoomFollowDao friendRoomFollowDao;
    @Autowired
    private IllegalNameCheckService illegalNameCheckService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private HallFriendRoomDao friendRoomDao;
    @Autowired
    private FriendRoomRedisDao friendRoomRedisDao;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private FriendRoomBillHistoryDao billHistoryDao;
    @ClusterRpcReference()
    private HallRoomBridge hallRoomBridge;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private HallRoomService hallRoomService;

    /**
     * 创建好友房
     */
    public int createFriendRoom(PlayerController playerController, ReqCreateFriendsRoom req) {
        Player player = corePlayerService.get(playerController.playerId());
        // 房间创建检查
        int checkResCode = checkCreateRoom(player, req);
        if (checkResCode != Code.SUCCESS) {
            log.warn("请求创建好友房失败, 检查未通过 req = {}", req);
            return checkResCode;
        }
        int invitationCode = player.getFriendRoomInvitationCode();
        if (invitationCode == 0) {
            // 生成邀请码
            invitationCode = friendRoomRedisDao.genInvitationCode();
            if (invitationCode <= 0) {
                log.error("创建房间时，申请邀请码失败，时间：{}", TimeHelper.getCurrentDateZeroMileTime());
                return Code.FAIL;
            }
        }
        int roomCfgId = req.roomCfgId;
        // 扣除道具
        CommonResult<PackChangeResult> removeItem =
            playerPackService.removeItem(
                player.getId(),
                new Item(req.itemId, req.itemNum), "create_friend_room"
            );
        // 移除道具失败
        if (!removeItem.success()) {
            return removeItem.code;
        }
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        // 获取一个游戏类型的随机节点
        MarsNode targetNode =
            nodeManager.getGameNodeByWeight(warehouseCfg.getGameType(), player.getId(), player.getIp());
        RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(req.timeOfOpenRoom);
        if (roomExpendCfg == null) {
            return Code.PARAM_ERROR;
        }
        // 开启时长，毫秒
        int openTime = roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        // 创建房间
        FriendRoom friendRoom = this.friendRoomDao.createBetFriendRoom(
            player.getId(),
            targetNode.getNodePath(),
            warehouseCfg,
            new CreateFriendsRoom(req.itemId, req.roomCfgId, openTime, req.autoRenewal,
                req.predictCostGoldNum, req.roomAliasName, req.timeOfOpenRoom));
        if (friendRoom == null) {
            return Code.PARAM_ERROR;
        }
        // 保存邀请码
        if (invitationCode != player.getFriendRoomInvitationCode()) {
            final int newInvitationCode = invitationCode;
            // 保存玩家邀请码
            corePlayerService.doSave(player.getId(), (p) -> p.setFriendRoomInvitationCode(newInvitationCode));
            // 添加邀请码映射
            friendRoomRedisDao.addInvitationCode(invitationCode, player.getId());
        }
        RespCreateFriendsRoom res = new RespCreateFriendsRoom(Code.SUCCESS);
        // 向游戏节点发送初始化房间指令
        initFriendRoomInGameNode(roomCfgId, friendRoom);
        // 构建好友房基础数据
        res.roomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(friendRoom);
        playerController.send(res);
        // 通知前端房间创建
        return Code.SUCCESS;
    }

    /**
     * 向游戏节点发送初始化好友房创建指令
     */
    private void initFriendRoomInGameNode(int roomCfgId, FriendRoom friendRoom) {
        ClusterClient client = clusterSystem.getClusterByPath(friendRoom.getPath());
        try {
            GameRpcContext.getContext().withReqParameterBuilder(
                RpcReqParameterBuilder.create()
                    .addClusterClient(client)
                    .setTryMillisPerClient(200));
            // 向目标节点发送，创建好友房指令
            hallRoomBridge.createFriendRoom(roomCfgId, friendRoom.getId());
        } finally {
            GameRpcContext.getContext().clearRpcBuilderData();
        }
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
            .filter(cfg -> cfg.getDurationtype() == 1 && cfg.getId() == reqCreateFriendsRoom.timeOfOpenRoom)
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
        long playerRoomSize = friendRoomDao.getPlayerRoomSize(player.getId());
        if (playerRoomSize >= playerLevelConfigCfg.getRoomNum()) {
            return Code.CREATE_ROOM_TO_LIMIT;
        }
        // 准备金扣费检查
        PlayerPack playerPack = playerPackService.redisGet(player.getId());
        ItemCfg goldItemCfg =
            GameDataManager.getItemCfgList().stream().filter(cfg -> cfg.getType() == GameConstant.Item.TYPE_DIAMOND).findFirst().get();
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
     * 获取玩家最大房间数量
     */
    private int playerMaxRoomNum(int playerLevel) {
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(playerLevel);
        return playerLevelConfigCfg.getRoomNum();
    }

    /**
     * 请求加入好友房
     */
    public void reqJoinFriendRoom(PlayerController playerController, ReqJoinFriendRoom req) {
        ResJoinFriendRoom resJoinFriendRoom = new ResJoinFriendRoom(Code.PARAM_ERROR);
        if (req.playerId <= 0 || req.roomId <= 0) {
            playerController.send(resJoinFriendRoom);
            return;
        }
        // 查找好友房
        FriendRoom friendRoom = friendRoomDao.getFriendRoomById(req.playerId, req.roomId);
        if (friendRoom == null) {
            log.warn("玩家：{} 请求加入好友房：{} 但好友房数据为空", playerController.playerId(), req.roomId);
            playerController.send(resJoinFriendRoom);
            return;
        }
        // 如果不是玩家自己需要检查
        if (playerController.playerId() != req.playerId) {
            // 需要查询好友关系是否存在
            Player targetPlayer = corePlayerService.get(req.playerId);
            int targetPlayerCode = targetPlayer.getFriendRoomInvitationCode();
            FriendRoomFollowBean friendRoomFollowBean =
                friendRoomFollowDao.getRoomFriend(playerController.playerId(), req.playerId, targetPlayerCode);
            // 好友关系不存在
            if (friendRoomFollowBean == null) {
                resJoinFriendRoom.code = Code.FRIEND_NOT_FOLLOWED;
                playerController.send(resJoinFriendRoom);
                return;
            }
        }
        // 进入房间
        int code = hallRoomService.joinFriendRoom(playerController, req.roomId, friendRoom.getGameType());
        if (code != Code.SUCCESS) {
            resJoinFriendRoom.code = code;
            playerController.send(resJoinFriendRoom);
            return;
        }
        resJoinFriendRoom.code = Code.SUCCESS;
        playerController.send(resJoinFriendRoom);
    }

    /**
     * 请求好友房数据panel
     */
    public void reqFriendPanelData(PlayerController playerController) {
        Player player = playerController.getPlayer();
        NotifyFriendRoomPanelData notifyFriendRoomPanelData = new NotifyFriendRoomPanelData();
        // 获取玩家创建的好友房数据
        List<FriendRoom> friendRoomList = friendRoomDao.getPlayerAllFriendRoom(player.getId());
        // 获取玩家关注列表
        List<FriendRoomFollowBean> friendRoomFollowBeans =
            friendRoomFollowDao.getDefualtRoomFriendList(player.getId(), player.getFriendRoomInvitationCode());
        Map<Long, FriendRoomFollowBean> friendRoomFollowBeanMap =
            friendRoomFollowBeans.stream().collect(
                HashMap::new, (map, e) -> map.put(e.getFollowedPlayerId(), e), HashMap::putAll);
        List<Long> followedPlayerId =
            friendRoomFollowBeans.stream().map(FriendRoomFollowBean::getFollowedPlayerId).toList();
        List<Player> followedplayerList = corePlayerService.multiGetPlayer(followedPlayerId);
        // 好友信息
        Map<Long, BaseFriendRoomPlayerInfo> baseFriendRoomPlayerInfos = new HashMap<>();
        for (Player followedPlayer : followedplayerList) {
            BaseFriendRoomPlayerInfo info = FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(followedPlayer);
            FriendRoomFollowBean friendRoomFollowBean = friendRoomFollowBeanMap.get(followedPlayer.getId());
            long topUpTimeStamp = friendRoomFollowBean.getTopUpTimeStamp();
            info.isTopUp = topUpTimeStamp > 0;
            info.isLostFriendRelationship = friendRoomFollowBean.getRemoveTime() > 0;
            info.topUpTime = friendRoomFollowBean.getTopUpTimeStamp();
            info.addTime = friendRoomFollowBean.getFollowedTimeStamp();
            info.maxRoomNum = playerMaxRoomNum(followedPlayer.getLevel());
            baseFriendRoomPlayerInfos.put(followedPlayer.getId(), info);
        }
        // 获取当前所有好友的当前房间数量
        Map<Long, Integer> friendRoomNumMap = friendRoomDao.getPlayerFriendRoomNum(followedPlayerId);
        for (Map.Entry<Long, BaseFriendRoomPlayerInfo> entry : baseFriendRoomPlayerInfos.entrySet()) {
            entry.getValue().curRoomNum = friendRoomNumMap.getOrDefault(entry.getKey(), 0);
        }
        notifyFriendRoomPanelData.roomFriendInfos = baseFriendRoomPlayerInfos.values().stream().toList();
        // 房间信息
        List<FriendRoomBaseData> friendRoomBaseDataList = new ArrayList<>();
        for (FriendRoom friendRoom : friendRoomList) {
            FriendRoomBaseData friendRoomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(friendRoom);
            friendRoomBaseDataList.add(friendRoomBaseData);
        }
        notifyFriendRoomPanelData.roomBaseDataList = friendRoomBaseDataList;
        notifyFriendRoomPanelData.invitationCode = playerController.getPlayer().getFriendRoomInvitationCode();
        GlobalConfigCfg invitationResetGlobalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
        notifyFriendRoomPanelData.invitationCodeResetTotalTimes = invitationResetGlobalConfigCfg.getIntValue();
        notifyFriendRoomPanelData.playerNumOnTable =
            friendRoomBaseDataList.stream().map(a -> a.onlinePlayerNum).mapToInt(Long::intValue).sum();
        notifyFriendRoomPanelData.curTableNum =
            friendRoomBaseDataList.stream().map(a -> a.maxPlayerNum).mapToInt(a -> a).sum();
        notifyFriendRoomPanelData.maxTableNum =
            friendRoomBaseDataList.stream().map(data -> {
                Tuple2<Integer, Integer> tuple =
                    SampleDataUtils.getRoomMaxLimit(GameDataManager.getWarehouseCfg(data.gameId));
                return tuple.getT2();
            }).mapToInt(a -> a).sum();
        notifyFriendRoomPanelData.maxPlayerNumOnTable =
            GameDataManager.getPlayerLevelConfigCfg(player.getLevel()).getRoomNum();
        // 邀请码重置使用次数
        int invitationCodeRestTimes = friendRoomRedisDao.getInvitationCodeResetUseTimes(player.getId());
        // 邀请码剩余次数
        notifyFriendRoomPanelData.invitationCodeResetRemainingTimes =
            invitationCodeRestTimes >= invitationResetGlobalConfigCfg.getIntValue() ?
                0 :
                invitationResetGlobalConfigCfg.getIntValue() - invitationCodeRestTimes;
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        notifyFriendRoomPanelData.maxFollowedLimit = playerLevelConfigCfg.getFriendsNum();
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
        // 判断玩家好友是否达到上限
        long roomFriendSize = friendRoomFollowDao.countRoomFriendSize(player.getId());
        PlayerLevelConfigCfg levelCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        int maxFriendsNum = levelCfg.getFriendsNum();
        if (roomFriendSize >= maxFriendsNum) {
            res.code = Code.MAX_FOLLOWED_FRIENDS;
            playerController.send(res);
            return;
        }
        Player targetPlayer = corePlayerService.get(targetPlayerId);
        // 如果目标玩家为空
        if (targetPlayer == null) {
            playerController.send(res);
            return;
        }
        // 通过邀请码添加关注好友
        friendRoomFollowDao.addFriendByInvitationCode(player.getId(), targetPlayerId, invitationCode);
        res.playerInfo = FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(targetPlayer);
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
            friendRoomFollowDao.getRoomFriend(playerController.playerId(), req.playerId, targetPlayerInvitationCode);
        if (friendRoomFollowBean == null) {
            res.code = Code.NOT_FOUND;
            playerController.send(res);
            return;
        }
        if (req.operate == ERoomFriendListOperate.TOP_UP) {
            friendRoomFollowBean.setTopUpTimeStamp(System.currentTimeMillis());
        } else if (req.operate == ERoomFriendListOperate.REMOVE) {
            friendRoomFollowBean.setRemoveTime(System.currentTimeMillis());
        }
        FriendRoomFollowBean updatedFriendRoomFollowBean =
            friendRoomFollowDao.updateFriendRoomFollowBean(friendRoomFollowBean);
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
            friendRoomFollowDao.getRoomFriend(
                playerController.playerId(), req.playerId, targetPlayer.getFriendRoomInvitationCode());
        // 如果查找不到，有可能对方重新刷新了邀请码
        if (friendRoomFollowBean == null) {
            res.code = Code.NOT_FOLLOWED;
            playerController.send(res);
            return;
        }
        List<FriendRoom> friendRoomList = friendRoomDao.getPlayerAllFriendRoom(req.playerId);
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
        if (req.pageSize < FriendRoomConstant.PAGE_SIZE || req.pageIdx < 0) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        Player player = corePlayerService.get(playerController.playerId());
        // 获取玩家关注列表
        List<FriendRoomFollowBean> friendRoomFollowBeans =
            friendRoomFollowDao.getRoomFriendList(
                player.getId(), player.getFriendRoomInvitationCode(), req.pageIdx, req.pageSize);
        res.pageSize = req.pageSize;
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
            friendRoomFollowDao.removeFollowedFriend(invitationCodeMap.values());
        }
        // 不包含的，或者过期的需要移除这部分的数据
        res.followedFriendList =
            followedplayerList.stream().map(FriendRoomMessageBuilder::buildFriendRoomPlayerInfo).toList();
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        res.maxFollowLimit = playerLevelConfigCfg.getFriendsNum();
        playerController.send(res);
    }

    /**
     * 请求操作屏蔽玩家
     */
    public void reqOperateShieldPlayer(PlayerController playerController, ReqOperateShieldPlayer req) {
        ResOperateShieldPlayer res = new ResOperateShieldPlayer(Code.PARAM_ERROR);
        Player player = corePlayerService.get(playerController.playerId());
        if (req.operateCode < 1 || req.operateCode > 3) {
            playerController.send(res);
            return;
        }
        PlayerLevelConfigCfg levelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        // 黑名单数量
        int blockListNum = levelConfigCfg.getBlockListNum();
        List<Long> playerBlackList = friendRoomRedisDao.getPlayerBlackList(playerController.playerId());
        switch (req.operateCode) {
            case 1: {
                playerBlackList.addAll(req.playerId);
                // 如果添加的黑名单数量大于了配置值
                if (playerBlackList.size() > blockListNum) {
                    res.code = Code.ADD_BLACK_LIST_PLAYER_TO_LIMIT;
                    playerController.send(res);
                    return;
                }
                break;
            }
            case 2: {
                if (playerBlackList.isEmpty()) {
                    // 非法操作
                    res.code = Code.PARAM_ERROR;
                    playerController.send(res);
                    return;
                }
                // 移除黑名单
                playerBlackList.removeAll(req.playerId);
                break;
            }
            case 3: {
                if (playerBlackList.isEmpty()) {
                    // 非法操作
                    res.code = Code.PARAM_ERROR;
                    playerController.send(res);
                    return;
                }
                playerBlackList.clear();
            }
            default:
                break;
        }
        // 更新玩家黑名单
        friendRoomRedisDao.updatePlayerBlackList(playerController.playerId(), playerBlackList);
        res.code = Code.SUCCESS;
        playerController.send(res);
    }

    /**
     * 请求黑名单玩家
     */
    public void reqPlayerBlackList(PlayerController playerController) {
        List<Long> playerBlackList = friendRoomRedisDao.getPlayerBlackList(playerController.playerId());
        ResShieldPlayerList res = new ResShieldPlayerList(Code.SUCCESS);
        Player player = corePlayerService.get(playerController.playerId());
        PlayerLevelConfigCfg levelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        // 黑名单数量
        res.maxLimit = levelConfigCfg.getBlockListNum();
        List<Player> blackListPlayers = corePlayerService.multiGetPlayer(playerBlackList);
        res.shieldPlayerList =
            blackListPlayers.stream().map(FriendRoomMessageBuilder::buildFriendRoomPlayerInfo).toList();
        playerController.send(res);
    }

    /**
     * 修改好友房名字
     */
    public int reqUpdateFriendRoomName(PlayerController playerController, ReqUpdateFriendRoom updateFriendRoom) {
        ResManageFriendRoom res = new ResManageFriendRoom(Code.ILLEGAL_NAME);
        Player player = corePlayerService.get(playerController.playerId());
        // 检查房间更新
        int checkRes = checkUpdateRoom(player, updateFriendRoom);
        if (checkRes != Code.SUCCESS) {
            return checkRes;
        }
        FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerController.playerId(), updateFriendRoom.roomId);
        if (friendRoom == null) {
            return Code.PARAM_ERROR;
        }
        // 添加时间
        int addTime;
        if (updateFriendRoom.timeOfOpenRoom != 0) {
            RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(updateFriendRoom.timeOfOpenRoom);
            List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
            // 扣除道具
            CommonResult<PackChangeResult> removeItem =
                playerPackService.removeItem(
                    player.getId(),
                    new Item(requiredMoney.getFirst(), requiredMoney.get(1)), "manage_friend_room"
                );
            // 移除道具失败
            if (!removeItem.success()) {
                return removeItem.code;
            }
            // 开启时长，毫秒
            addTime = roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        } else {
            addTime = 0;
        }
        friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {
            }

            @Override
            public Boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setAliasName(updateFriendRoom.roomAliasName);
                dataEntity.setPredictCostGoldNum(dataEntity.getPredictCostGoldNum() + updateFriendRoom.predictCostGoldNum);
                dataEntity.setAutoRenewal(updateFriendRoom.autoRenewal);
                dataEntity.setOverdueTime(dataEntity.getOverdueTime() + addTime);
                return true;
            }
        });
        res.code = Code.SUCCESS;
        playerController.send(res);
        return Code.SUCCESS;
    }

    /**
     * 检查房间更新
     */
    private int checkUpdateRoom(Player player, ReqUpdateFriendRoom updateFriendRoom) {
        // 屏蔽字检查
        if (illegalNameCheckService.illegalNameCheck(updateFriendRoom.roomAliasName)) {
            return Code.ILLEGAL_NAME;
        }
        if (updateFriendRoom.timeOfOpenRoom != 0) {
            // 牌局时长ID合法性检查
            RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(updateFriendRoom.timeOfOpenRoom);
            if (roomExpendCfg == null) {
                return Code.PARAM_ERROR;
            }
        }
        // 准备金扣费检查 需要扣除的金币数量
        long needDeductGold = updateFriendRoom.predictCostGoldNum;
        if (needDeductGold > 0 && player.getGold() < needDeductGold) {
            return Code.NOT_ENOUGH;
        }
        return Code.SUCCESS;
    }

    /**
     * 请求好友房历史账单
     */
    public void reqFriendRoomBillHistory(PlayerController playerController, ReqFriendRoomBillHistory req) {
        ResFriendRoomBillHistory res = new ResFriendRoomBillHistory(Code.PARAM_ERROR);
        if (req.pageIdx < 0 || req.pageSize < FriendRoomConstant.PAGE_SIZE) {
            playerController.send(res);
            return;
        }
        List<GameBillResult> gameBillResults =
            billHistoryDao.pageFriendRoomBillByGameType(playerController.playerId(), req.pageIdx, req.pageSize);
        List<GameBillInfo> gameBillInfos = new ArrayList<>();
        for (GameBillResult gameBillResult : gameBillResults) {
            GameBillInfo gameBillInfo = new GameBillInfo();
            gameBillInfo.gameType = gameBillResult.getId();
            gameBillInfo.totalRound = gameBillResult.getTotalRound();
            gameBillInfo.canTakeIncome = gameBillResult.getTotalIncomeCanTake();
            gameBillInfo.totalIncome = gameBillResult.getTotalIncome();
            gameBillInfo.totalFlow = gameBillResult.getTotalWin();
            gameBillInfos.add(gameBillInfo);
        }
        res.gameBillInfos = gameBillInfos;
        res.canTakeIncome = gameBillInfos.stream().mapToLong(g -> g.totalIncome).sum();
        res.code = Code.SUCCESS;
        res.pageSize = req.pageSize;
        res.pageIdx = gameBillInfos.size() < req.pageSize ? -1 : req.pageIdx + 1;
        playerController.send(res);
    }

    /**
     * 请求好友房详细账单历史数据
     */
    public void reqFriendRoomDetailBillHistory(PlayerController playerController, ReqFriendRoomDetailBillHistory req) {
        ResFriendRoomDetailBillHistory res = new ResFriendRoomDetailBillHistory(Code.PARAM_ERROR);
        if (req.pageSize < FriendRoomConstant.PAGE_SIZE || req.pageIdx < 0) {
            playerController.send(res);
            return;
        }
        List<FriendRoomBillHistoryBean> pageFriendRoomBillHistory =
            billHistoryDao.pageFriendRoomBillHistory(playerController.playerId(), req.pageIdx, req.pageSize);
        // 按月分的好友房账单历史
        Map<Integer, List<FriendRoomBillHistory>> friendRoomBillOfMonth = new HashMap<>();
        // 构建好友房账单历史
        for (FriendRoomBillHistoryBean friendRoomBillHistoryBean : pageFriendRoomBillHistory) {
            FriendRoomBillHistory friendRoomBillHistory = new FriendRoomBillHistory();
            friendRoomBillHistory.id = friendRoomBillHistoryBean.getId();
            friendRoomBillHistory.createdTime = friendRoomBillHistoryBean.getCreatedAt();
            friendRoomBillHistory.partInNum = friendRoomBillHistoryBean.getPartInPlayerIncome().size();
            friendRoomBillHistory.totalIncome =
                friendRoomBillHistoryBean.isHasTookIncome() ? 0 : friendRoomBillHistoryBean.getTotalIncome();
            friendRoomBillHistory.totalWin =
                friendRoomBillHistoryBean.getPartInPlayerIncome().values().stream().mapToLong(a -> a).sum();
            Calendar calendar = Calendar.getInstance();
            Date date = new Date(friendRoomBillHistory.createdTime);
            calendar.setTime(date);
            friendRoomBillOfMonth.computeIfAbsent(calendar.get(Calendar.MONTH), k -> new ArrayList<>()).add(friendRoomBillHistory);
        }
        List<FriendRoomBillHistoryMonth> friendRoomBillHistoryMonths = new ArrayList<>();
        for (Map.Entry<Integer, List<FriendRoomBillHistory>> entry : friendRoomBillOfMonth.entrySet()) {
            FriendRoomBillHistoryMonth friendRoomBillHistoryMonth = new FriendRoomBillHistoryMonth();
            friendRoomBillHistoryMonth.month = entry.getKey();
            friendRoomBillHistoryMonth.billHistories = entry.getValue();
            friendRoomBillHistoryMonth.totalIncome =
                entry.getValue().stream().map(f -> f.totalIncome).mapToLong(a -> a).sum();
            friendRoomBillHistoryMonth.totalOfMatches =
                entry.getValue().stream().map(f -> f.totalWin).mapToLong(a -> a).sum();
            ;
            friendRoomBillHistoryMonths.add(friendRoomBillHistoryMonth);
        }
        res.monthBillList = friendRoomBillHistoryMonths;
        res.pageSize = req.pageSize;
        res.pageIdx = pageFriendRoomBillHistory.size() < req.pageSize ? -1 : req.pageIdx + 1;
        res.code = Code.SUCCESS;
        playerController.send(res);
    }

    /**
     * 请求好友房中账单历史中玩家信息
     */
    public void reqFriendRoomBillPlayerInfo(PlayerController playerController, ReqFriendRoomBillPlayerInfo req) {
        ResFriendRoomBillPlayerInfo res = new ResFriendRoomBillPlayerInfo(Code.PARAM_ERROR);
        if (req.id <= 0) {
            playerController.send(res);
            return;
        }
        // 获取单个好友房间账单信息
        FriendRoomBillHistoryBean historyBean = billHistoryDao.getOneFriendRoomBillInfo(req.id);
        if (historyBean.getPartInPlayerIncome().isEmpty()) {
            playerController.send(new ResFriendRoomBillPlayerInfo(Code.SUCCESS));
            return;
        }
        // 获取所有加入的玩家
        Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(historyBean.getPartInPlayerIncome().keySet());
        List<FriendRoomBillPlayerInfo> playerInfos = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : historyBean.getPartInPlayerIncome().entrySet()) {
            if (!playerMap.containsKey(entry.getKey())) {
                continue;
            }
            FriendRoomBillPlayerInfo playerInfo = new FriendRoomBillPlayerInfo();
            playerInfo.billFlow = entry.getValue();
            playerInfo.baseFriendRoomPlayerInfo =
                FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(playerMap.get(entry.getKey()));
            playerInfos.add(playerInfo);
        }
        res.playerInfos = playerInfos;
        res.code = Code.SUCCESS;
        playerController.send(res);
    }

    /**
     * 请求一键领取所有的房间收益,为了避免更新 新加入的账单数据，所以需要对领取方法进行加锁
     */
    @RedissonLock(key = "FriendRoomBillUpdate:#playerController.playerId()", waitTime = 20, timeUnit = TimeUnit.SECONDS)
    public void reqTakeFriendRoomIncomeReward(@Param("playerController") PlayerController playerController) {
        ResTakeFriendRoomBillIncome res = new ResTakeFriendRoomBillIncome(Code.SUCCESS);
        // 查询所有收益
        long playerAllReward = billHistoryDao.getPlayerAllReward(playerController.playerId());
        if (playerAllReward == 0) {
            playerController.send(res);
            return;
        }
        // 更新所有领奖状态
        billHistoryDao.updateAllHistoryRewardTook(playerController.playerId());
        // 给玩家添加金币
        corePlayerService.addGold(playerController.playerId(), playerAllReward, "friend_room_income_take_all");
        // 发送消息
        playerController.send(res);
    }

    /**
     * 请求操作好友房
     */
    public void reqOperateFriendRoom(PlayerController playerController, ReqOperateFriendRoom req) {
        ResOperateFriendRoom res = new ResOperateFriendRoom(Code.PARAM_ERROR);
        if (req.operateCode < 1 || req.operateCode > 3) {
            playerController.send(res);
            return;
        }
        long playerId = playerController.playerId();
        FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerId, req.roomId);
        // 房间不存在
        if (friendRoom == null) {
            res.code = Code.ROOM_NOT_FOUND;
            playerController.send(res);
            return;
        }
        switch (req.operateCode) {
            // 暂停
            case 1:
                if (friendRoom.getStatus() != 0) {
                    log.warn("房间状态不为默认开启状态，却还在请求暂停！player: {}, roomId: {} status: {}",
                        playerId, req.roomId, friendRoom.getStatus());
                    res.code = Code.ERROR_REQ;
                    playerController.send(res);
                    return;
                }
                // 重新开始
            case 2:
                if (friendRoom.getStatus() != 1) {
                    // 房间不为暂停，但是却还是在请求恢复
                    log.warn("房间状态不为暂停状态，却还在请求恢复！player: {}, roomId: {}, status: {}",
                        playerId, req.roomId, friendRoom.getStatus());
                    res.code = Code.ERROR_REQ;
                    playerController.send(res);
                    return;
                }
                // 解散
            case 3:
                ClusterClient client = clusterSystem.getClusterByPath(friendRoom.getPath());
                // 被操作的房间不能为空
                if (client != null) {
                    // 操作方房间
                    operateFriendRoom(playerController, client, req);
                } else {
                    // 房间对应的节点找不到
                    res.code = Code.FAIL;
                    playerController.send(res);
                }
                break;
            default:
                break;
        }
    }

    /**
     * 请求操作好友房
     */
    private void operateFriendRoom(
        PlayerController playerController, ClusterClient client, ReqOperateFriendRoom req) {
        ResOperateFriendRoom res = new ResOperateFriendRoom(Code.FAIL);
        try {
            // 单房间，直接等返回
            GameRpcContext.getContext().setReqParameterBuilder(
                RpcReqParameterBuilder.create()
                    .addClusterClient(client)
                    .setTryMillisPerClient(1000));
            // 操作房间
            hallRoomBridge.operateFriendRoom(playerController.playerId(), req.roomId, req.operateCode);
            // 操作完成后再获取
            FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerController.playerId(), req.roomId);
            res.roomStatus = friendRoom.getStatus();
            GlobalConfigCfg globalConfigCfg =
                GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
            int intervalTime = globalConfigCfg.getIntValue() * TimeHelper.ONE_MINUTE_OF_MILLIS;
            long curTime = System.currentTimeMillis();
            // 更新下次可操作的时间
            res.nextPauseBtnOverdueTime =
                friendRoom.getPauseTime() + intervalTime > curTime ?
                    friendRoom.getPauseTime() + intervalTime : 0;
            res.code = Code.SUCCESS;
            playerController.send(res);
        } catch (Exception e) {
            playerController.send(res);
            log.error("操作好友房发生异常：{}", e.getMessage(), e);
        } finally {
            GameRpcContext.getContext().clearRpcBuilderData();
        }
    }

    /**
     * 请求重置邀请码
     */
    public void reqResetInvitationCode(PlayerController playerController) {
        Player player = corePlayerService.get(playerController.playerId());
        ResResetInvitationCode res = new ResResetInvitationCode(Code.PARAM_ERROR);
        int oldInvitationCode = player.getFriendRoomInvitationCode();
        if (oldInvitationCode == 0) {
            // 玩家邀请码为0但是还在请求重置
            playerController.send(res);
            return;
        }
        GlobalConfigCfg invitationResetGlobalConfigCfg =
            GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
        // 检查次数
        Integer resetTimes = friendRoomRedisDao.getInvitationCodeResetUseTimes(player.getId());
        if (resetTimes != null && resetTimes >= invitationResetGlobalConfigCfg.getIntValue()) {
            res.code = Code.INVITATION_CODE_RESET_TIMES_NOT_ENOUGH;
            playerController.send(res);
            return;
        }
        int newInvitationCode = friendRoomRedisDao.genInvitationCode();
        if (newInvitationCode <= 0) {
            log.error("重置邀请码时，生成邀请码失败，时间：{}", TimeHelper.getCurrentDateZeroMileTime());
            res.code = Code.FAIL;
            playerController.send(res);
            return;
        }
        // 重置邀请码
        friendRoomRedisDao.resetInvitationCode(oldInvitationCode, newInvitationCode, player.getId());
        // 删除好友映射关系
        friendRoomFollowDao.deleteMappingRelateByInvitationCode(oldInvitationCode);
        // 保存玩家邀请码
        corePlayerService.doSave(player.getId(), (p) -> p.setFriendRoomInvitationCode(newInvitationCode));
        res.resetTimes = resetTimes == null ? 0 : resetTimes + 1;
        res.invitationCode = newInvitationCode;
        playerController.send(res);
    }

    @Override
    public void doCommand(String command, List<String> params) {
        switch (command) {
            case "RpcCallDemo": {
                try {
                    long playerId = Long.parseLong(params.get(0));
                    long roomId = Long.parseLong(params.get(1));
                    int operateCode = Integer.parseInt(params.get(2));
                    GameRpcContext.getContext().withReqParameterBuilder(RpcReqParameterBuilder.create()
                        .setRetryTimesPerClient(10)
                        .setTryMillisPerClient(200)
                        .addProviderNodeType(NodeType.GAME)
                        .addGameMajorType(GameMajorType.TABLE)
                        .setAllFinishedCallback(() -> log.info("全部结束"))
                        .setAllSuccessCallback(() -> log.info("全部成功"))
                        .addClientFilter((c) -> !c.nodeConfig.getName().contains("CCL"))
                    );
                    hallRoomBridge.operateFriendRoom(playerId, roomId, operateCode);
                    FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerId, roomId);
                    log.info("操作结果: {}", friendRoom.getStatus());
                } catch (Exception e) {
                    log.error("{}", e.getMessage(), e);
                } finally {
                    GameRpcContext.getContext().clearRpcBuilderData();
                }
                break;
            }
            default:
                break;
        }
    }

    @Override
    public List<String> needHandleCommands() {
        return List.of("RpcCallDemo");
    }
}
