package com.jjg.game.hall.friendroom.services;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.data.DataSaveCallback;
import com.jjg.game.common.redis.RedissonLock;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GlobalSampleConstantId;
import com.jjg.game.core.dao.room.AbstractFriendRoomDao.CreateFriendsRoom;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao.GameBillResult;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.SnowflakeManager;
import com.jjg.game.core.rpc.HallRoomBridge;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.IllegalNameCheckService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RobotUtil;
import com.jjg.game.core.utils.SampleDataUtils;
import com.jjg.game.hall.friendroom.constant.FriendRoomConstant;
import com.jjg.game.hall.friendroom.constant.FriendRoomErrorCode;
import com.jjg.game.hall.friendroom.dao.FriendRoomFollowDao;
import com.jjg.game.hall.friendroom.dao.FriendRoomRedisDao;
import com.jjg.game.hall.friendroom.dao.HallFriendRoomDao;
import com.jjg.game.hall.friendroom.dao.RoomSlotsPoolDao;
import com.jjg.game.hall.friendroom.data.FriendRoomFollowBean;
import com.jjg.game.hall.friendroom.message.FriendRoomMessageBuilder;
import com.jjg.game.hall.friendroom.message.req.*;
import com.jjg.game.hall.friendroom.message.res.*;
import com.jjg.game.hall.friendroom.message.struct.*;
import com.jjg.game.hall.friendroom.message.struct.RoomFriendEnum.ERoomFriendListOperate;
import com.jjg.game.hall.room.HallRoomService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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
public class FriendRoomServices {

    private static final Logger log = LoggerFactory.getLogger(FriendRoomServices.class);
    @Autowired
    private IllegalNameCheckService illegalNameCheckService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CorePlayerService corePlayerService;
    @ClusterRpcReference()
    private HallRoomBridge hallRoomBridge;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private HallRoomService hallRoomService;
    // 玩家好友关系记录查询
    @Autowired
    private FriendRoomFollowDao friendRoomFollowDao;
    // 好友房房间数据查询
    @Autowired
    private HallFriendRoomDao friendRoomDao;
    // 好友房相关redis数据查询，邀请码重置次数，邀请码对应玩家记录，黑名单
    @Autowired
    private FriendRoomRedisDao friendRoomRedisDao;
    // 账单历史查询
    @Autowired
    private FriendRoomBillHistoryDao billHistoryDao;
    @Autowired
    private GameFunctionService gameFunctionService;
    @Autowired
    private RobotUtil robotUtil;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private RoomSlotsPoolDao roomSlotsPoolDao;
    @Lazy
    @Autowired
    protected SnowflakeManager snowflakeManager;

    /**
     * 创建好友房
     */
    public int createFriendRoom(PlayerController playerController, ReqCreateFriendsRoom req) {
        Player player = corePlayerService.get(playerController.playerId());
        // 房间创建检查
        int checkResCode = checkCreateRoom(player, req);
        if (checkResCode != Code.SUCCESS) {
            log.warn("请求创建好友房失败, 检查未通过 res: {} req = {}", checkResCode, JSON.toJSONString(req));
            return checkResCode;
        }
        int invitationCode = player.getFriendRoomInvitationCode();
        if (invitationCode == 0) {
            // 生成邀请码
            invitationCode = friendRoomRedisDao.genInvitationCode();
            if (invitationCode <= 0) {
                log.error("创建房间时，申请邀请码失败，时间：{}", TimeHelper.getCurrentDateZeroMilliTime());
                return Code.FAIL;
            }
        }
        RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(req.timeOfOpenRoom);
        if (roomExpendCfg == null) {
            return Code.PARAM_ERROR;
        }
        // 金币道具ID
        Item reqItem = getCreateRoomItem(roomExpendCfg, req.itemId);
        if (reqItem == null) {
            return Code.SAMPLE_ERROR;
        }
        int roomCfgId = req.roomCfgId;
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        // 获取一个游戏类型的随机节点
        MarsNode targetNode =
                nodeManager.getGameNodeByWeight(warehouseCfg.getGameID(), player.getId(), player.getIp());
        if (targetNode == null) {
            return Code.FAIL;
        }
        // 扣除道具
        CommonResult<ItemOperationResult> removeItem;
        Map<Integer, Long> itemMap;
        //随机房间号
        long roomId = snowflakeManager.nextId();

        String roomIdStr = String.valueOf(roomId);
        if (req.predictCostGoldNum != 0) {
            itemMap = ItemUtils.mergeItemsOnCreate(Map.of(reqItem.getId(), reqItem.getItemCount()),
                    Map.of(ItemUtils.getDiamondItemId(), req.predictCostGoldNum));
            removeItem = playerPackService.removeItems(player, itemMap, AddType.CREATE_FRIEND_ROOM, roomIdStr);
        } else {
            itemMap = Map.of(reqItem.getId(), reqItem.getItemCount());
            removeItem = playerPackService.removeItem(player, Collections.singletonList(new Item(reqItem.getId(), reqItem.getItemCount())), AddType.CREATE_FRIEND_ROOM, roomIdStr);
        }

        // 移除道具失败
        if (!removeItem.success()) {
            return removeItem.code;
        }
        // 开启时长，毫秒
        int openTime = roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        // 创建房间
        FriendRoom friendRoom = this.friendRoomDao.createBetFriendRoom(
                player.getId(),
                roomId,
                targetNode.getNodePath(),
                warehouseCfg,
                new CreateFriendsRoom(req.itemId, req.roomCfgId, openTime, req.autoRenewal,
                        req.predictCostGoldNum, req.roomAliasName, req.timeOfOpenRoom));
        if (friendRoom == null) {
            //创建房间失败，将消耗的道具回退
            playerPackService.addItems(player.getId(), itemMap, AddType.CREATE_FRIEND_FAIL_CALLBACK, roomIdStr);
            return Code.PARAM_ERROR;
        }
        RespCreateFriendsRoom res = new RespCreateFriendsRoom(Code.SUCCESS);
        // 保存邀请码
        if (invitationCode != player.getFriendRoomInvitationCode()) {
            final int newInvitationCode = invitationCode;
            // 保存玩家邀请码
            corePlayerService.doSave(player.getId(), (p) -> p.setFriendRoomInvitationCode(newInvitationCode));
            // 添加邀请码映射
            friendRoomRedisDao.addInvitationCode(invitationCode, player.getId());
            res.invitationCode = invitationCode;
        }
        // 向游戏节点发送初始化房间指令
        initFriendRoomInGameNode(roomCfgId, friendRoom);
        // 构建好友房基础数据
        res.roomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(friendRoom);
        playerController.send(res);
        log.info("玩家：{} 创建好友房成功：{}", player.getId(), JSON.toJSONString(res));
        coreLogger.roomOperate(friendRoom, 1, roomExpendCfg.getDurationTime(), itemMap, removeItem.data);
        // 通知前端房间创建
        return Code.SUCCESS;
    }

    /**
     * 向游戏节点发送初始化好友房创建指令
     */
    private void initFriendRoomInGameNode(int roomCfgId, FriendRoom friendRoom) {
        ClusterClient client = clusterSystem.getClusterByPath(friendRoom.getPath());
        log.debug("client: {}", (client == null ? "null" : friendRoom));
        try {
            GameRpcContext.getContext().withReqParameterBuilder(
                    RpcReqParameterBuilder.create()
                            .addClusterClient(client)
                            .setTryMillisPerClient(1000));
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
        // 检查游戏功能是否开放
        if (!gameFunctionService.checkGameFunctionOpen(player, EFunctionType.FRIEND_ROOM)) {
            return Code.FORBID;
        }
        // 房间名检查
        if (checkRoomName(reqCreateFriendsRoom.roomAliasName) != Code.SUCCESS) {
            return Code.ILLEGAL_NAME;
        }
        // 检查场次是否存在
        RoomCfg roomCfg = GameDataManager.getRoomCfg(reqCreateFriendsRoom.roomCfgId);
        if (roomCfg != null) {
            return Code.PARAM_ERROR;
        }
        // 牌局时长合法性检查
        RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(reqCreateFriendsRoom.timeOfOpenRoom);
        if (roomExpendCfg == null) {
            return Code.PARAM_ERROR;
        }
        Item reqItem = getCreateRoomItem(roomExpendCfg, reqCreateFriendsRoom.itemId);
        if (reqItem == null) {
            return Code.PARAM_ERROR;
        }
        // 房间数量检查
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        long playerRoomSize = friendRoomDao.getPlayerRoomSize(player.getId());
        if (playerRoomSize >= playerLevelConfigCfg.getRoomNum()) {
            return Code.CREATE_ROOM_TO_LIMIT;
        }
        // 道具检查
        int checkRes = playerPackService.checkHasItems(player, Collections.singletonList(reqItem));
        if (checkRes != Code.SUCCESS) {
            return checkRes;
        }
        // 需要扣除的金币数量,
        long needDeductGold = reqCreateFriendsRoom.predictCostGoldNum;
        // 金币扣除检查
        if (needDeductGold != 0 && player.getDiamond() < needDeductGold) {
            return Code.NOT_ENOUGH;
        }
        return Code.SUCCESS;
    }

    /**
     * 检查房间名字
     *
     * @param aliasName 房间名
     * @return 错误码
     */
    private int checkRoomName(String aliasName) {
        boolean baseNameCheck = StringUtils.isEmpty(aliasName) || aliasName.length() > 32;
        // 房间名检查
        if (baseNameCheck || !illegalNameCheckService.illegalNameCheck(aliasName)) {
            return Code.ILLEGAL_NAME;
        }
        return Code.SUCCESS;
    }

    /**
     * 获取创建房间的道具ID
     */
    private Item getCreateRoomItem(RoomExpendCfg roomExpendCfg, int reqItemId) {
        int moneyId = roomExpendCfg.getRequiredMoney().isEmpty() ? 0 : roomExpendCfg.getRequiredMoney().getFirst();
        int itemId = roomExpendCfg.getRequiredItem().isEmpty() ? 0 : roomExpendCfg.getRequiredItem().getFirst();
        // 检查道具ID合法性
        if (moneyId != reqItemId && itemId != reqItemId) {
            return null;
        }
        long itemNum;
        if (moneyId == reqItemId) {
            itemNum = roomExpendCfg.getRequiredMoney().get(1);
        } else {
            itemNum = roomExpendCfg.getRequiredItem().get(1);
        }
        return new Item(reqItemId, itemNum);
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
            resJoinFriendRoom.code = Code.ROOM_NOT_FOUND;
            log.warn("玩家：{} 请求加入好友房：{} 但好友房数据为空", playerController.playerId(), req.roomId);
            playerController.send(resJoinFriendRoom);
            return;
        }
        // 检查房间是否可以加入
        int checkRes = checkJoinRoom(playerController.playerId(), req.playerId, friendRoom, false);
        if (checkRes != Code.SUCCESS) {
            log.warn("{} code: {} 请求进入好友房：{} 失败, room: {} ",
                    playerController.playerId(), checkRes, req.roomId, JSON.toJSONString(friendRoom));
            resJoinFriendRoom.code = checkRes;
            playerController.send(resJoinFriendRoom);
            return;
        }
        // 进入房间
        int code = hallRoomService.joinFriendRoom(playerController, req.roomId, friendRoom.getGameType());
        if (code != Code.SUCCESS) {
            log.warn("玩家：{} 请求加入好友房：{} 失败", playerController.playerId(), friendRoom.logStr());
            resJoinFriendRoom.code = code;
            playerController.send(resJoinFriendRoom);
            return;
        }
        log.info("玩家：{} 请求加入好友房：{} 成功", playerController.playerId(), friendRoom.logStr());
        resJoinFriendRoom.code = Code.SUCCESS;
        resJoinFriendRoom.roomCfgId = friendRoom.getRoomCfgId();
        playerController.send(resJoinFriendRoom);
    }

    /**
     * 检查加入房间条件
     */
    public int checkJoinRoom(long playerId, long friendPlayerId, FriendRoom friendRoom, boolean isReconnect) {
        if (friendPlayerId <= 0) {
            return Code.PARAM_ERROR;
        }
        // 检查房间状态
        int roomStatus = friendRoom.getStatus();
        // 如果是断线重连，房间处于任何状态都可以进入
        if (!isReconnect) {
            if (roomStatus != 1 && roomStatus != 2) {
                return Code.ROOM_CANT_JOIN;
            }
        }
        // 检查房间过期时间
        long roomResetTime = FriendRoomMessageBuilder.getRoomResetTime(friendRoom);
        if (roomResetTime <= 0) {
            return FriendRoomErrorCode.ROOM_TIME_NOT_ENOUGH;
        }
        // 检查人数是否已满
        if (friendRoom.getRoomPlayers().size() >= friendRoom.getMaxLimit()) {
            return Code.ROOM_FULL;
        }
        // 检查黑名单
        List<Long> targetFriendBlackList = friendRoomRedisDao.getPlayerBlackList(friendPlayerId);
        if (targetFriendBlackList != null && targetFriendBlackList.contains(playerId)) {
            return Code.BAN_CAUSE_BLACK_LIST;
        }
        // 如果不是玩家自己需要检查
        if (playerId != friendPlayerId) {
            // 需要查询好友关系是否存在
            Player targetPlayer = corePlayerService.get(friendPlayerId);
            int targetPlayerCode = targetPlayer.getFriendRoomInvitationCode();
            FriendRoomFollowBean friendRoomFollowBean =
                    friendRoomFollowDao.getRoomFriend(playerId, friendPlayerId, targetPlayerCode);
            // 好友关系不存在
            if (friendRoomFollowBean == null) {
                return Code.FRIEND_NOT_FOLLOWED;
            }
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
        List<FriendRoom> friendRoomList = friendRoomDao.getPlayerAllFriendRoom(player.getId());
        // 获取玩家关注列表
        List<FriendRoomFollowBean> friendRoomFollowBeans = friendRoomFollowDao.getDefualtRoomFriendList(player.getId());
        Map<Long, FriendRoomFollowBean> friendRoomFollowBeanMap =
                friendRoomFollowBeans.stream().collect(
                        LinkedHashMap::new, (map, e) -> map.put(e.getFollowedPlayerId(), e), HashMap::putAll);
        List<Long> followedPlayerId =
                friendRoomFollowBeans.stream().map(FriendRoomFollowBean::getFollowedPlayerId).toList();
        List<Player> followedplayerList = corePlayerService.multiGetPlayer(followedPlayerId);
        // 好友信息
        notifyFriendRoomPanelData.roomFriendInfos =
                buildFriendRoomPlayerInfoList(followedPlayerId, followedplayerList, friendRoomFollowBeanMap);
        // 房间信息
        friendRoomList.sort((o1, o2) -> Long.compare(o2.getCreateTime(), o1.getCreateTime()));
        //构建房间基础数据
        List<FriendRoomBaseData> friendRoomBaseDataList = buildFriendRoomInfos(friendRoomList, true);

        notifyFriendRoomPanelData.roomBaseDataList = friendRoomBaseDataList;
        notifyFriendRoomPanelData.invitationCode = playerController.getPlayer().getFriendRoomInvitationCode();
        GlobalConfigCfg globalConfigCfg =
                GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
        notifyFriendRoomPanelData.invitationCodeResetTotalTimes = globalConfigCfg.getIntValue();
        notifyFriendRoomPanelData.playerNumOnTable =
                friendRoomBaseDataList.stream().map(a -> a.onlinePlayerNum).mapToInt(Long::intValue).sum();
        notifyFriendRoomPanelData.curTableNum = friendRoomBaseDataList.size();
        notifyFriendRoomPanelData.maxTableNum =
                GameDataManager.getPlayerLevelConfigCfg(player.getLevel()).getRoomNum();
        notifyFriendRoomPanelData.maxPlayerNumOnTable =
                friendRoomBaseDataList.stream().map(data -> {
                    Tuple2<Integer, Integer> tuple =
                            SampleDataUtils.getRoomMaxLimit(GameDataManager.getWarehouseCfg(data.gameId));
                    return tuple.getT1();
                }).mapToInt(a -> a).sum();
        // 邀请码重置使用次数
        Integer icRestTimes = friendRoomRedisDao.getInvitationCodeResetUseTimes(player.getId());
        icRestTimes = icRestTimes == null ? 0 : icRestTimes;
        // 邀请码剩余次数
        notifyFriendRoomPanelData.invitationCodeResetRemainingTimes =
                icRestTimes >= globalConfigCfg.getIntValue() ? 0 : globalConfigCfg.getIntValue() - icRestTimes;
        notifyFriendRoomPanelData.invitationCodeResetRemainingTimes =
                Math.max(notifyFriendRoomPanelData.invitationCodeResetRemainingTimes, 0);
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        notifyFriendRoomPanelData.maxFollowedLimit = playerLevelConfigCfg.getFriendsNum();
        log.debug("好友面板数据：{} ", JSON.toJSONString(notifyFriendRoomPanelData));
        // 发送数据
        playerController.send(notifyFriendRoomPanelData);
    }

    private List<FriendRoomBaseData> buildFriendRoomInfos(List<FriendRoom> friendRoomList, boolean isSelf) {
        Map<Long, Long> bigPoolByRoomIds = getSlotsRoomPool(friendRoomList);
        List<FriendRoomBaseData> dataArrayList = new ArrayList<>(friendRoomList.size());
        for (FriendRoom friendRoom : friendRoomList) {
            FriendRoomBaseData friendRoomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(friendRoom);
            if (friendRoom instanceof SlotsFriendRoom) {
                friendRoomBaseData.predictCostGoldNum = bigPoolByRoomIds.getOrDefault(friendRoom.getId(), 0L);
            }
            dataArrayList.add(friendRoomBaseData);
            // 检查房间的自动续费
            if (friendRoom.getStatus() != 3 && isSelf) {
                autoRenewalRoomInHall(friendRoom);
            }
        }
        return dataArrayList;
    }

    /**
     * 获取slotsroom的奖池
     *
     * @param friendRoomList 好友房列表
     * @return 奖池Mmap
     */
    private Map<Long, Long> getSlotsRoomPool(List<FriendRoom> friendRoomList) {
        List<Object> slotsRoomList = new ArrayList<>(friendRoomList.size());
        for (FriendRoom friendRoom : friendRoomList) {
            if (friendRoom instanceof SlotsFriendRoom) {
                slotsRoomList.add(friendRoom.getId());
            }
        }
        if (slotsRoomList.isEmpty()) {
            return Map.of();
        }
        return roomSlotsPoolDao.getBigPoolByRoomIds(slotsRoomList);
    }

    /**
     * 在大厅给房间自动续费
     */
    private void autoRenewalRoomInHall(FriendRoom friendRoom) {
        ClusterClient client = getRoomNode(friendRoom);
        if (client == null) {
            return;
        }
        MarsNode node = client.marsNode;
        //待关服的不管
        if (node == null || node.getNodeConfig().waitClose()) {
            return;
        }
        // 如果房间不在游戏中或者没有开启自动续费
        if (friendRoom.isInGaming() || !friendRoom.isAutoRenewal()) {
            return;
        }
        // 被暂停的房间不管
        if (friendRoom.getPauseTime() != 0) {
            return;
        }
        // 查看房间剩余时间
        long resetTime = FriendRoomMessageBuilder.getRoomResetTime(friendRoom);
        // 还没到时间
        if (resetTime > 0) {
            return;
        }
        // 自动续费，检查玩家金币是否足够
        RoomExpendCfg roomExpendCfg = getAutoRenewalCfg();
        if (roomExpendCfg == null || roomExpendCfg.getRequiredMoney().size() < 2) {
            return;
        }
        List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
        int itemNum = requiredMoney.get(1);
        // 时长，毫秒
        long durationTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        // 从房间底庄中扣除金币，如果不足直接暂停游戏
        if (itemNum > friendRoom.getPredictCostGoldNum() || itemNum <= 0) {
            // 自动续费失败，房间准备金不足
            log.info("自动续费失败，房间准备金不足: roomId: {},gameType = {},need: {} rest: {}", friendRoom.getId(), friendRoom.getGameType(), itemNum, friendRoom.getPredictCostGoldNum());
            return;
        }
        long overdueTime = friendRoom.getOverdueTime();
        long curTime = System.currentTimeMillis();
        long totalTake = 0;
        while (itemNum < friendRoom.getPredictCostGoldNum()) {
            friendRoom.setPredictCostGoldNum(friendRoom.getPredictCostGoldNum() - itemNum);
            totalTake += itemNum;
            overdueTime += durationTime;
            if (overdueTime > curTime) {
                break;
            }
        }
        // 如果时间没有改变，说明准备金不足
        if (overdueTime == friendRoom.getOverdueTime()) {
            return;
        }
        log.info("玩家：{} 房间: {} 在大厅进行自动续费，续费时长：{} 消耗准备金：{}",
                friendRoom.getCreator(), friendRoom.getId(), overdueTime - friendRoom.getOverdueTime(), totalTake);
        long finalOverdueTime = overdueTime;
        friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(), new DataSaveCallback<>() {
            @Override
            public void updateData(FriendRoom dataEntity) {

            }

            @Override
            public boolean updateDataWithRes(FriendRoom dataEntity) {
                dataEntity.setPredictCostGoldNum(friendRoom.getPredictCostGoldNum());
                dataEntity.setOverdueTime(finalOverdueTime);
                return true;
            }
        });
        Map<Integer, Long> itemMap = Map.of(requiredMoney.getFirst(), (long) itemNum);
        ItemOperationResult itemOperationResult = new ItemOperationResult();
        itemOperationResult.setDiamond(friendRoom.getPredictCostGoldNum());
        coreLogger.roomOperate(friendRoom, 2, roomExpendCfg.getDurationTime(), itemMap, itemOperationResult);
    }

    /**
     * 房间自动续费时获取续费配置
     */
    private RoomExpendCfg getAutoRenewalCfg() {
        for (RoomExpendCfg roomExpendCfg : GameDataManager.getRoomExpendCfgList()) {
            if (roomExpendCfg.getDurationtype() == 1) {
                return roomExpendCfg;
            }
        }
        return null;
    }

    /**
     * 请求通过邀请码关注玩家
     */
    public void reqFollowedByInvitationCode(PlayerController playerController, int invitationCode) {
        Player player = playerController.getPlayer();
        // 先判断邀请码是否存在
        Number numberTargetPlayerId = friendRoomRedisDao.getPlayerIdByInvitationCode(invitationCode);
        ResFollowByInvitationCode res = new ResFollowByInvitationCode(Code.FAIL);
        if (numberTargetPlayerId == null || numberTargetPlayerId.longValue() == 0) {
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
        long targetPlayerId = numberTargetPlayerId.longValue();
        Player targetPlayer = corePlayerService.get(targetPlayerId);
        // 如果目标玩家为空
        if (targetPlayer == null) {
            playerController.send(res);
            return;
        }
        // 检查是否是重复添加
        FriendRoomFollowBean friendRoomFollowBean =
                friendRoomFollowDao.getRoomFriend(player.getId(), targetPlayerId, invitationCode);
        if (friendRoomFollowBean != null) {
            res.code = Code.REPEAT_OP;
            playerController.send(res);
            return;
        }
        // 通过邀请码添加关注好友
        friendRoomFollowDao.addFriendByInvitationCode(player.getId(), targetPlayerId, invitationCode);
        BaseFriendRoomPlayerInfo roomPlayerInfo = FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(targetPlayer);
        roomPlayerInfo.maxRoomNum = playerMaxRoomNum(targetPlayer.getLevel());
        // 获取当前所有好友的当前房间数量
        Map<Long, Integer> friendRoomNumMap =
                friendRoomDao.getPlayerFriendRoomNum(Collections.singletonList(targetPlayerId));
        roomPlayerInfo.curRoomNum = friendRoomNumMap.getOrDefault(targetPlayerId, 0);
        res.playerInfo = roomPlayerInfo;
        res.code = Code.SUCCESS;
        log.info("{} 通过邀请码：{} 成功添加好友: {}", player.getId(), invitationCode, targetPlayer.getId());
        playerController.send(res);
    }

    /**
     * 请求操作关注列表
     */
    public void reqOperateFollowedFriendsList(PlayerController playerController, ReqOperateFollowedFriendsList req) {
        ResOperateFollowedFriendsList res = new ResOperateFollowedFriendsList(Code.PARAM_ERROR);
        if (req.playerId <= 0 || StringUtils.isEmpty(req.operate)) {
            playerController.send(res);
            return;
        }
        // 必须使用最新的player
        Player targetPlayer = corePlayerService.get(req.playerId);
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
        ERoomFriendListOperate code = null;
        for (ERoomFriendListOperate value : ERoomFriendListOperate.values()) {
            if (req.operate.equalsIgnoreCase(value.name())) {
                code = value;
                break;
            }
        }
        if (code == null) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        log.info("玩家：{} 请求操作好友：{} 列表, code: {}",
                playerController.playerId(), req.playerId, code.name());
        switch (code) {
            case TOP_UP -> friendRoomFollowBean.setTopUpTimeStamp(System.currentTimeMillis());
            case REMOVE -> friendRoomFollowBean.setRemoveTime(System.currentTimeMillis());
            case CANCEL_TOP_UP -> friendRoomFollowBean.setTopUpTimeStamp(0);
        }
        FriendRoomFollowBean updatedFriendRoomFollowBean =
                friendRoomFollowDao.updateFriendRoomFollowBean(friendRoomFollowBean);
        if (updatedFriendRoomFollowBean != null) {
            res.operateCode = req.operate;
            res.operatedPlayerId = req.playerId;
            res.code = Code.SUCCESS;
        }
        playerController.send(res);
    }

    /**
     * 请求关注的好友房间列表
     */
    public void reqFollowedFriendRoomList(PlayerController playerController, ReqFollowedFriendRoomList req) {
        ResFollowedFriendRoomList res = new ResFollowedFriendRoomList(Code.FAIL);
        if (req.playerId <= 0) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            return;
        }
        // 判断当前操作的玩家是否是玩家关注的
        Player targetPlayer = corePlayerService.get(req.playerId);
        if (targetPlayer == null) {
            res.code = Code.PARAM_ERROR;
            playerController.send(res);
            log.error("通过玩家ID：{} 刷新好友房列表异常", req.playerId);
            return;
        }
        if (playerController.playerId() != req.playerId) {
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
        }
        List<FriendRoom> friendRoomList = friendRoomDao.getPlayerAllFriendRoom(req.playerId);
        friendRoomList.sort((o1, o2) -> Long.compare(o2.getCreateTime(), o1.getCreateTime()));
        // 房间信息
        boolean isSelf = req.playerId == playerController.playerId();
        res.roomList = buildFriendRoomInfos(friendRoomList, isSelf);
        res.code = Code.SUCCESS;
        res.playerId = req.playerId;
        log.debug("返回好友房列表： {}", JSON.toJSONString(res));
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
                friendRoomFollowDao.getRoomFriendList(player.getId(), req.pageIdx, req.pageSize);
        res.pageSize = req.pageSize;
        res.pageIdx = friendRoomFollowBeans.size() < req.pageSize ? -1 : req.pageIdx + 1;
        List<Long> followedPlayerId =
                friendRoomFollowBeans.stream().map(FriendRoomFollowBean::getFollowedPlayerId).toList();
        List<Player> followedplayerList = corePlayerService.multiGetPlayer(followedPlayerId);
        Map<Long, FriendRoomFollowBean> friendRoomFollowBeanMap =
                friendRoomFollowBeans.stream().collect(
                        LinkedHashMap::new, (map, e) -> map.put(e.getFollowedPlayerId(), e), HashMap::putAll);
        // 不包含的，或者过期的需要移除这部分的数据
        res.followedFriendList = buildFriendRoomPlayerInfoList(followedPlayerId, followedplayerList,
                friendRoomFollowBeanMap);
        PlayerLevelConfigCfg playerLevelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        res.maxFollowLimit = playerLevelConfigCfg.getFriendsNum();
        log.debug("请求刷新好友关注列表: {}", JSON.toJSONString(res));
        playerController.send(res);
    }

    /**
     * 构建好友数据
     */
    private List<BaseFriendRoomPlayerInfo> buildFriendRoomPlayerInfoList(
            List<Long> followedPlayerId, List<Player> followedPlayerList, Map<Long, FriendRoomFollowBean> followBeanMap) {
        // 好友信息
        Map<Long, BaseFriendRoomPlayerInfo> baseFriendRoomPlayerInfos = new LinkedHashMap<>();
        Map<Long, Player> followedPlayerMap = followedPlayerList.stream()
                .collect(HashMap::new, (map, e) -> {
                    map.put(e.getId(), e);
                }, HashMap::putAll);
        for (Map.Entry<Long, FriendRoomFollowBean> entry : followBeanMap.entrySet()) {
            Player followedPlayer = followedPlayerMap.get(entry.getKey());
            if (followedPlayer != null) {
                BaseFriendRoomPlayerInfo info = FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(followedPlayer);
                FriendRoomFollowBean friendRoomFollowBean = followBeanMap.get(followedPlayer.getId());
                long topUpTimeStamp = friendRoomFollowBean.getTopUpTimeStamp();
                info.isTopUp = topUpTimeStamp > 0;
                info.isLostFriendRelationship = friendRoomFollowBean.getRemoveTime() > 0;
                info.topUpTime = topUpTimeStamp;
                info.addTime = friendRoomFollowBean.getFollowedTimeStamp();
                info.maxRoomNum = playerMaxRoomNum(followedPlayer.getLevel());
                baseFriendRoomPlayerInfos.put(followedPlayer.getId(), info);
            }
        }
        // 获取当前所有好友的当前房间数量
        Map<Long, Integer> friendRoomNumMap = friendRoomDao.getPlayerFriendRoomNum(followedPlayerId);
        for (Map.Entry<Long, BaseFriendRoomPlayerInfo> entry : baseFriendRoomPlayerInfos.entrySet()) {
            entry.getValue().curRoomNum = friendRoomNumMap.getOrDefault(entry.getKey(), 0);
        }
        return baseFriendRoomPlayerInfos.values().stream().toList();
    }

    /**
     * 请求操作屏蔽玩家
     */
    public void reqOperateShieldPlayer(PlayerController playerController, ReqOperateShieldPlayer req) {
        ResOperateShieldPlayer res = new ResOperateShieldPlayer(Code.PARAM_ERROR);
        Player player = corePlayerService.get(playerController.playerId());
        if (req.operateCode < 1 || req.operateCode > 3 || req.playerId == null || req.playerId.isEmpty()) {
            playerController.send(res);
            return;
        }
        PlayerLevelConfigCfg levelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        // 黑名单数量
        int blockListNum = levelConfigCfg.getBlockListNum();
        List<Long> playerBlackList = friendRoomRedisDao.getPlayerBlackList(playerController.playerId());
        log.info("玩家：{} 请求操作屏蔽好友：{} {} {}", player.getId(), req.playerId, req.operateCode, playerBlackList);
        playerBlackList = playerBlackList == null ? new ArrayList<>() : playerBlackList;
        if (new HashSet<>(playerBlackList).containsAll(req.playerId) && req.operateCode == 1) {
            res.code = FriendRoomErrorCode.FRIEND_ROOM_REPEAT_SHIELD;
            playerController.send(res);
            return;
        } else if (req.operateCode != 1) {
            if (!new HashSet<>(playerBlackList).containsAll(req.playerId)) {
                // 移除非法好友列表
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                return;
            }
        }
        switch (req.operateCode) {
            case 1: {
                playerBlackList.addAll(req.playerId);
                // 如果添加的黑名单数量大于了配置值
                if (playerBlackList.size() > blockListNum) {
                    res.code = Code.ADD_BLACK_LIST_PLAYER_TO_LIMIT;
                    playerController.send(res);
                    return;
                }
                log.info("玩家：{} 请求将玩家：{} 添加到黑名单", player.getId(), JSON.toJSONString(req.playerId));
                break;
            }
            case 2: {
                if (playerBlackList.isEmpty()) {
                    // 非法操作
                    res.code = Code.SUCCESS;
                    playerController.send(res);
                    return;
                }
                log.info("玩家：{} 请求将玩家：{} 移除黑名单", player.getId(), JSON.toJSONString(req.playerId));
                // 移除黑名单
                playerBlackList.removeIf(p -> req.playerId.contains(p));
                break;
            }
            case 3: {
                if (playerBlackList.isEmpty()) {
                    // 非法操作
                    res.code = Code.SUCCESS;
                    playerController.send(res);
                    return;
                }
                playerBlackList.clear();
                log.info("玩家：{} 请求清空黑名单", player.getId());
            }
            default:
                break;
        }
        // 检查玩家是否存在
        List<Player> players = corePlayerService.multiGetPlayer(req.playerId);
        if (players.isEmpty() || players.size() != req.playerId.size()) {
            res.code = FriendRoomErrorCode.FRIEND_ROOM_SHIELD_NONE_PLAYER;
            playerController.send(res);
            return;
        }
        // 更新玩家黑名单
        friendRoomRedisDao.updatePlayerBlackList(playerController.playerId(), playerBlackList);
        res.code = Code.SUCCESS;
        res.operateCode = req.operateCode;
        res.playerId = req.playerId;
        log.debug("更新后的屏蔽列表 {}", playerBlackList);
        playerController.send(res);
    }

    /**
     * 请求黑名单玩家
     */
    public void reqPlayerBlackList(PlayerController playerController) {
        List<Long> playerBlackList = friendRoomRedisDao.getPlayerBlackList(playerController.playerId());
        ResShieldPlayerList res = new ResShieldPlayerList(Code.SUCCESS);
        if (playerBlackList == null || playerBlackList.isEmpty()) {
            playerController.send(res);
            return;
        }
        Player player = corePlayerService.get(playerController.playerId());
        PlayerLevelConfigCfg levelConfigCfg = GameDataManager.getPlayerLevelConfigCfg(player.getLevel());
        // 黑名单数量
        res.maxLimit = levelConfigCfg.getBlockListNum();
        Map<Long, Player> blackListPlayers = corePlayerService.multiGetPlayerMap(playerBlackList);
        List<BaseFriendRoomPlayerInfo> blackPlayerInfoList = new ArrayList<>();
        for (Long playerId : playerBlackList) {
            BaseFriendRoomPlayerInfo playerInfo;
            if (blackListPlayers.containsKey(playerId)) {
                playerInfo = FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(blackListPlayers.get(playerId));
            } else {
                playerInfo = FriendRoomMessageBuilder.buildFriendRoomRobotPlayerInfo(robotUtil, playerId);
            }
            blackPlayerInfoList.add(playerInfo);
        }
        res.shieldPlayerList = blackPlayerInfoList;
        log.debug("屏蔽好友列表 {}", JSON.toJSONString(res));
        playerController.send(res);
    }

    /**
     * 更新好友房信息
     */
    public int reqUpdateFriendRoomData(PlayerController playerController, ReqUpdateFriendRoom updateFriendRoom) {
        ResManageFriendRoom res = new ResManageFriendRoom(Code.ILLEGAL_NAME);
        Player player = corePlayerService.get(playerController.playerId());
        // 检查房间更新
        int checkRes = checkUpdateRoom(player, updateFriendRoom);
        if (checkRes != Code.SUCCESS) {
            return checkRes;
        }
        FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerController.playerId(), updateFriendRoom.roomId);
        if (friendRoom == null) {
            return Code.ROOM_DESTROYED;
        }
        if (!checkOverdueTime(friendRoom, updateFriendRoom.timeOfOpenRoom)) {
            return Code.ROOM_RENEW_TIME_LIMIT;
        }

        ClusterClient client;
        if (StringUtils.isEmpty(friendRoom.getPath())) {
            client = randomNode(playerController, friendRoom, playerController.playerId());
        } else {
            client = clusterSystem.getClusterByPath(friendRoom.getPath());
            if (client == null) {
                client = randomNode(playerController, friendRoom, playerController.playerId());
            }
        }
        // 被操作的房间不能为空
        if (client == null) {
            // 房间对应的节点找不到
            res.code = Code.FAIL;
            return Code.ROOM_NOT_FOUND;
        }
        if (client.marsNode.getNodeConfig().waitClose()) {
            return Code.WAIT_CLOSE_NOT_MODIFICATION;
        }


        RoomExpendCfg roomExpendCfg = null;
        Map<Integer, Long> itemMap = null;
        CommonResult<ItemOperationResult> removeItem = null;
        // 添加时间
        int addTime = 0;
        if (updateFriendRoom.timeOfOpenRoom != 0 || updateFriendRoom.predictCostGoldNum > 0) {
            itemMap = new HashMap<>();
            roomExpendCfg = GameDataManager.getRoomExpendCfg(updateFriendRoom.timeOfOpenRoom);
            if (roomExpendCfg != null) {
                List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
                itemMap.put(requiredMoney.getFirst(), Long.valueOf(requiredMoney.get(1)));
            }
            if (updateFriendRoom.predictCostGoldNum > 0) {
                int diamondItemId = ItemUtils.getDiamondItemId();
                itemMap.put(diamondItemId,
                        itemMap.getOrDefault(diamondItemId, 0L) + updateFriendRoom.predictCostGoldNum);
            }
            if (CollectionUtil.isNotEmpty(itemMap)) {
                // 扣除道具
                removeItem = playerPackService.removeItems(player, itemMap,
                        AddType.MANAGE_FRIEND_ROOM);
                // 移除道具失败
                if (!removeItem.success()) {
                    return removeItem.code;
                }
            }
            if (roomExpendCfg != null) {
                // 开启时长，毫秒
                addTime = roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
            }
        }
        int finalAddTime = addTime;
        boolean isSlotsRoom = friendRoom instanceof SlotsFriendRoom;
        CommonResult<FriendRoom> result;
        if (!isSlotsRoom) {
            result = friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(),
                    new DataSaveCallback<>() {
                        @Override
                        public void updateData(FriendRoom dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(FriendRoom dataEntity) {
                            if (!StringUtils.isEmpty(updateFriendRoom.roomAliasName)) {
                                dataEntity.setAliasName(updateFriendRoom.roomAliasName);
                            }
                            dataEntity.setPredictCostGoldNum(dataEntity.getPredictCostGoldNum() + updateFriendRoom.predictCostGoldNum);
                            dataEntity.setPool(dataEntity.getPool() + updateFriendRoom.predictCostGoldNum);
                            dataEntity.setAutoRenewal(updateFriendRoom.autoRenewal);
                            if (finalAddTime > 0) {
                                long curTime = System.currentTimeMillis();
                                // 不管时间是否暂停，都只需要给原有的过期时间加上增量时间
                                if (dataEntity.getOverdueTime() < curTime) {
                                    // 房间已经过期，续时间
                                    dataEntity.setOverdueTime(curTime + finalAddTime);
                                } else {
                                    // 房间未过期，续时间
                                    dataEntity.setOverdueTime(dataEntity.getOverdueTime() + finalAddTime);
                                }
                            }
                            return true;
                        }
                    });
            if (!result.success()) {
                return result.code;
            }
            if (result.success() && updateFriendRoom.predictCostGoldNum > 0 && friendRoom instanceof BetFriendRoom) {
                friendRoomDao.modifyRoomPool(result.data.getGameType(), result.data.getId(), updateFriendRoom.predictCostGoldNum);
            }
            if ((addTime > 0 || updateFriendRoom.predictCostGoldNum > 0) && friendRoom.isInGaming()) {
                try {
                    GameRpcContext.getContext().setReqParameterBuilder(
                            RpcReqParameterBuilder.create()
                                    .addClusterClient(client)
                                    .setTryMillisPerClient(1000));
                    // 单房间，直接等返回
                    // 请求尝试开启游戏，如果游戏处于暂停状态，可以考虑异步请求开启游戏
                    hallRoomBridge.operateFriendRoom(player.getId(), friendRoom.getId(), 1, friendRoom.getRoomCfgId());
                } catch (Exception e) {
                    log.error("reqUpdateFriendRoomData operateFriendRoom", e);
                    return Code.EXCEPTION;
                } finally {
                    GameRpcContext.getContext().clearRpcBuilderData();
                }
            }
        } else {
            try {
                GameRpcContext.getContext().setReqParameterBuilder(
                        RpcReqParameterBuilder.create()
                                .addClusterClient(client)
                                .setTryMillisPerClient(1000));
                result = hallRoomBridge.updateFriendRoom(player.getId(), friendRoom.getRoomCfgId(), friendRoom.getId(), addTime,
                        updateFriendRoom.autoRenewal, updateFriendRoom.predictCostGoldNum, updateFriendRoom.roomAliasName);
            } catch (Exception e) {
                log.error("reqUpdateFriendRoomData updateFriendRoom", e);
                return Code.EXCEPTION;
            } finally {
                GameRpcContext.getContext().clearRpcBuilderData();
            }
        }
        if (result == null) {
            return Code.UNKNOWN_ERROR;
        }
        if (!result.success()) {
            return result.code;
        }
        res.code = Code.SUCCESS;
        res.roomBaseData = FriendRoomMessageBuilder.buildFriendRoomBaseData(result.data);
        playerController.send(res);
        log.info("请求更新房间数据成功，req: {} roomData: {}",
                JSON.toJSONString(updateFriendRoom), JSON.toJSONString(result.data));

        if (addTime > 0) {
            coreLogger.roomOperate(friendRoom, 3, roomExpendCfg.getDurationTime(), itemMap, removeItem == null ? null : removeItem.data);
        }
        return Code.SUCCESS;
    }

    /**
     * 续费时长限制
     *
     * @param friendRoom     房间数据
     * @param timeOfOpenRoom 时间变化
     * @return true 能增加时间
     */
    private boolean checkOverdueTime(FriendRoom friendRoom, int timeOfOpenRoom) {
        long currentTimeMillis = System.currentTimeMillis();
        if (timeOfOpenRoom == 0 || friendRoom.getOverdueTime() <= currentTimeMillis) {
            return true;
        }
        RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(timeOfOpenRoom);
        if (roomExpendCfg == null) {
            return false;
        }
        long addTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        //最大小时数
        long maxHour = 365 * 24;
        //续费时间计算
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(114);
        if (globalConfigCfg != null) {
            maxHour = globalConfigCfg.getIntValue();
        }
        long remainMillis = friendRoom.getOverdueTime() + addTime - currentTimeMillis;
        return remainMillis <= maxHour * TimeHelper.ONE_HOUR_OF_MILLIS;
    }

    /**
     * 检查房间更新
     */
    private int checkUpdateRoom(Player player, ReqUpdateFriendRoom updateFriendRoom) {
        // 屏蔽字检查
        if (!StringUtils.isEmpty(updateFriendRoom.roomAliasName)) {
            if (!illegalNameCheckService.illegalNameCheck(updateFriendRoom.roomAliasName)) {
                return Code.ILLEGAL_NAME;
            }
            if (updateFriendRoom.roomAliasName.length() > 16) {
                return Code.ILLEGAL_NAME;
            }
        }
        Map<Integer, Long> itemMap = new HashMap<>();
        if (updateFriendRoom.timeOfOpenRoom != 0) {
            // 牌局时长ID合法性检查
            RoomExpendCfg roomExpendCfg = GameDataManager.getRoomExpendCfg(updateFriendRoom.timeOfOpenRoom);
            if (roomExpendCfg == null) {
                return Code.PARAM_ERROR;
            }
            List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
            itemMap.put(requiredMoney.getFirst(), Long.valueOf(requiredMoney.get(1)));
        }
        // 准备金扣费检查 需要扣除的金币数量
        long needDeductGold = updateFriendRoom.predictCostGoldNum;
        if (needDeductGold < 0 || updateFriendRoom.roomId <= 0) {
            return Code.PARAM_ERROR;
        }
        // 钻石判断
        if (needDeductGold > 0) {
            int diamondItemId = ItemUtils.getDiamondItemId();
            itemMap.put(diamondItemId, itemMap.getOrDefault(diamondItemId, 0L) + needDeductGold);
        }
        // 道具检查
        if (!playerPackService.checkHasItems(player, itemMap)) {
            return Code.NOT_ENOUGH;
        }
        return Code.SUCCESS;
    }

    /**
     * 请求好友房历史账单
     */
    public void reqFriendRoomBillHistory(PlayerController playerController, ReqFriendRoomBillHistory req) {
        ResFriendRoomBillHistory res = new ResFriendRoomBillHistory(Code.PARAM_ERROR);
        if (req.pageIdx < 0 || req.pageSize < 0) {
            playerController.send(res);
            return;
        }
        List<GameBillResult> gameBillResults =
                billHistoryDao.pageFriendRoomBillByGameType(playerController.playerId(), req.pageIdx, req.pageSize);
        List<GameBillInfo> gameBillInfos = new ArrayList<>();
        for (GameBillResult gameBillResult : gameBillResults) {
            GameBillInfo gameBillInfo = new GameBillInfo();
            gameBillInfo.gameType = gameBillResult.getGameType();
            gameBillInfo.totalRound = gameBillResult.getTotalRound();
            gameBillInfo.canTakeIncome = gameBillResult.getTotalIncomeCanTake();
            gameBillInfo.totalIncome = gameBillResult.getTotalIncome();
            gameBillInfo.totalFlow = gameBillResult.getTotalWin();
            gameBillInfos.add(gameBillInfo);
        }
        // TODO.2CL 后续按照收益排序
        res.gameBillInfos = gameBillInfos.stream().sorted(Comparator.<GameBillInfo>comparingLong(o -> o.canTakeIncome).reversed()).toList();
        res.canTakeIncome = gameBillInfos.stream().mapToLong(g -> g.canTakeIncome).sum();
        res.code = Code.SUCCESS;
        res.pageSize = req.pageSize;
        res.pageIdx = gameBillInfos.size() < req.pageSize ? -1 : req.pageIdx + 1;
        log.debug("好友房历史账单 res: {}", JSON.toJSONString(res));
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

        //获取历史账单数据
        List<FriendRoomBillHistoryBean> pageFriendRoomBillHistory =
                billHistoryDao.pageFriendRoomBillHistory(
                        playerController.playerId(), req.gameType, req.pageIdx, req.pageSize);

        // 按月分的好友房账单历史
        Map<Integer, List<FriendRoomBillHistory>> friendRoomBillOfMonth = new HashMap<>();

        // 构建好友房账单历史
        if (CommonUtil.getMajorTypeByGameType(req.gameType) == CoreConst.GameMajorType.SLOTS) {  //slots类
            for (FriendRoomBillHistoryBean friendRoomBillHistoryBean : pageFriendRoomBillHistory) {
                if (friendRoomBillHistoryBean.getPartInPlayerBetScore() == null || friendRoomBillHistoryBean.getPartInPlayerBetScore().isEmpty()) {
                    continue;
                }

                for (Map.Entry<Long, Long> en : friendRoomBillHistoryBean.getPartInPlayerBetScore().entrySet()) {
                    FriendRoomBillHistory friendRoomBillHistory = new FriendRoomBillHistory();
                    friendRoomBillHistory.id = friendRoomBillHistoryBean.getId();
                    friendRoomBillHistory.createdTime = friendRoomBillHistoryBean.getCreatedAt();
                    friendRoomBillHistory.playerId = en.getKey();
                    friendRoomBillHistory.totalWin = en.getValue();
                    friendRoomBillHistory.partInNum = 1;
                    friendRoomBillHistory.totalIncome = friendRoomBillHistoryBean.getPlayerInCome(friendRoomBillHistory.playerId);

                    friendRoomBillOfMonth.computeIfAbsent(friendRoomBillHistoryBean.getMonth(), k -> new ArrayList<>()).add(friendRoomBillHistory);
                }
            }
        } else {  //百人和押注类
            for (FriendRoomBillHistoryBean friendRoomBillHistoryBean : pageFriendRoomBillHistory) {
                FriendRoomBillHistory friendRoomBillHistory = new FriendRoomBillHistory();
                friendRoomBillHistory.id = friendRoomBillHistoryBean.getId();
                friendRoomBillHistory.createdTime = friendRoomBillHistoryBean.getCreatedAt();
                friendRoomBillHistory.partInNum = friendRoomBillHistoryBean.getPartInPlayerIncome().size();
                friendRoomBillHistory.totalIncome =
                        friendRoomBillHistoryBean.isHasTookIncome() ? 0 : friendRoomBillHistoryBean.getTotalIncome();
                friendRoomBillHistory.totalWin =
                        friendRoomBillHistoryBean.getPartInPlayerIncome().values().stream().mapToLong(a -> a).sum();
                friendRoomBillOfMonth.computeIfAbsent(friendRoomBillHistoryBean.getMonth(), k -> new ArrayList<>()).add(friendRoomBillHistory);
            }
        }

        //月份统计
        List<FriendRoomBillHistoryDao.MonthStatisticsDto> monthStatistic =
                billHistoryDao.monthStatistic(
                        playerController.playerId(), req.gameType, friendRoomBillOfMonth.keySet().stream().toList());
        Map<Integer, Integer> monthCount =
                monthStatistic.stream().collect(HashMap::new, (map, e) -> map.put(e.month, e.count), HashMap::putAll);
        List<FriendRoomBillHistoryMonth> friendRoomBillHistoryMonths = new ArrayList<>();
        for (Map.Entry<Integer, List<FriendRoomBillHistory>> entry : friendRoomBillOfMonth.entrySet()) {
            FriendRoomBillHistoryMonth friendRoomBillHistoryMonth = new FriendRoomBillHistoryMonth();
            friendRoomBillHistoryMonth.month = entry.getKey() % 100;
            friendRoomBillHistoryMonth.billHistories = entry.getValue();
            friendRoomBillHistoryMonth.totalIncome =
                    entry.getValue().stream().map(f -> f.totalIncome).mapToLong(a -> a).sum();
            friendRoomBillHistoryMonth.totalOfMatches =
                    monthCount.getOrDefault(entry.getKey(), entry.getValue().size());
            friendRoomBillHistoryMonths.add(friendRoomBillHistoryMonth);
        }
        res.monthBillList =
                friendRoomBillHistoryMonths.stream().sorted((o1, o2) -> Integer.compare(o2.month, o1.month)).toList();
        res.pageSize = req.pageSize;
        res.pageIdx = pageFriendRoomBillHistory.size() < req.pageSize ? -1 : req.pageIdx + 1;
        res.gameType = req.gameType;
        res.code = Code.SUCCESS;
        log.debug("ReqFriendRoomDetailBillHistory : {} ", JSON.toJSONString(res));
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
        if (historyBean == null) {
            res.code = Code.NOT_FOUND;
            playerController.send(res);
            return;
        }
        if (historyBean.getPartInPlayerIncome().isEmpty()) {
            playerController.send(new ResFriendRoomBillPlayerInfo(Code.SUCCESS));
            return;
        }

        if (req.playerId > 0) {
            if (historyBean.getPartInPlayerBetScore().isEmpty()) {
                playerController.send(new ResFriendRoomBillPlayerInfo(Code.SUCCESS));
                return;
            }

            List<Long> playerBlackList = friendRoomRedisDao.getPlayerBlackList(playerController.playerId());
            Long betScore = historyBean.getPartInPlayerBetScore().get(req.playerId);

            FriendRoomBillPlayerInfo playerInfo = new FriendRoomBillPlayerInfo();
            playerInfo.baseFriendRoomPlayerInfo =
                    FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(corePlayerService.get(req.playerId));

            playerInfo.status = playerBlackList != null && playerBlackList.contains(req.playerId) ? 2 : 1;
            playerInfo.billFlow = betScore == null ? 0 : betScore;
            res.playerInfos = List.of(playerInfo);
        } else {
            // 获取所有加入的玩家
            Map<Long, Player> playerMap = corePlayerService.multiGetPlayerMap(historyBean.getPartInPlayerIncome().keySet());
            List<Long> playerBlackList = friendRoomRedisDao.getPlayerBlackList(playerController.playerId());
            List<FriendRoomBillPlayerInfo> playerInfos = new ArrayList<>();
            for (Map.Entry<Long, Long> entry : historyBean.getPartInPlayerIncome().entrySet()) {
                FriendRoomBillPlayerInfo playerInfo = new FriendRoomBillPlayerInfo();
                // 玩家ID不存在
                if (playerMap.containsKey(entry.getKey())) {
                    playerInfo.baseFriendRoomPlayerInfo =
                            FriendRoomMessageBuilder.buildFriendRoomPlayerInfo(playerMap.get(entry.getKey()));
                } else {
                    // 尝试获取机器人的玩家数据
                    playerInfo.baseFriendRoomPlayerInfo =
                            FriendRoomMessageBuilder.buildFriendRoomRobotPlayerInfo(robotUtil, entry.getKey());
                }
                playerInfo.billFlow = entry.getValue();
                playerInfo.status = playerBlackList != null && playerBlackList.contains(entry.getKey()) ? 2 : 1;
                playerInfos.add(playerInfo);
            }
            res.playerInfos = playerInfos;
        }

        res.code = Code.SUCCESS;
        log.debug("reqFriendRoomBillPlayerInfo: {}", JSON.toJSONString(res));
        playerController.send(res);
    }

    /**
     * 请求一键领取所有的房间收益,为了避免更新 新加入的账单数据，所以需要对领取方法进行加锁
     */
    @RedissonLock(key = "'FriendRoomBillUpdate:'+#playerId", waitTime = 10, timeUnit = TimeUnit.SECONDS)
    public int reqTakeFriendRoomIncomeReward(@Param("playerId") long playerId) {
        // 查询所有收益
        List<FriendRoomRewardItem> playerAllReward = billHistoryDao.getPlayerAllReward(playerId);
        if (playerAllReward.isEmpty()) {
            log.warn("该玩家可收益为空 playerId = {}", playerId);
            return Code.SUCCESS;
        }

        // 更新所有领奖状态
        boolean update = billHistoryDao.updateAllHistoryRewardTook(playerId, playerAllReward);
        if (!update) {
            return Code.FAIL;
        }

        Map<Integer, Long> addItmsMap = new HashMap<>();
        for (FriendRoomRewardItem fri : playerAllReward) {
            addItmsMap.merge(fri.getItemId(), fri.getCount(), Long::sum);
        }

        // 给玩家添加收益道具
        playerPackService.addItems(playerId, addItmsMap, AddType.FRIEND_ROOM_INCOME_TAKE_ALL);
        log.info("玩家一键领取好友房收益 playerId = {},playerAllReward = {}", playerId, JSON.toJSONString(playerAllReward));
        // 发送消息
        return Code.SUCCESS;
    }

    /**
     * 请求操作好友房
     */
    public void reqOperateFriendRoom(PlayerController playerController, ReqOperateFriendRoom req) {
        ResOperateFriendRoom res = new ResOperateFriendRoom(Code.PARAM_ERROR);
        log.debug("请求操作好友房 playerId = {},req = {} ", playerController.playerId(), JSON.toJSONString(req));
        if (req.operateCode < 1 || req.operateCode > 3 || req.roomId <= 0) {
            playerController.send(res);
            return;
        }
        long playerId = playerController.playerId();
        FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerId, req.roomId);
        // 房间不存在
        if (friendRoom == null) {
            res.code = Code.ROOM_DESTROYED;
            playerController.send(res);
            return;
        }
        long timeMillis = System.currentTimeMillis();
        if (req.operateCode != 3 && friendRoom.getOperationCoolingTime() > timeMillis) {
            res.code = Code.ERROR_REQ;
            playerController.send(res);
            return;
        }
        ClusterClient client;
        if (StringUtils.isEmpty(friendRoom.getPath())) {
            client = randomNode(playerController, friendRoom, playerId);
        } else {
            client = clusterSystem.getClusterByPath(friendRoom.getPath());
            if (client == null) {
                client = randomNode(playerController, friendRoom, playerId);
            }
        }
        // 被操作的房间不能为空
        if (client == null) {
            // 房间对应的节点找不到
            res.code = Code.FAIL;
            playerController.send(res);
            return;
        }
        long coolDownTime = getCoolDownTime();
        boolean isNotSlotsRoom = !(friendRoom instanceof SlotsFriendRoom);
        switch (req.operateCode) {
            // 开始
            case 1:
                if (friendRoom.getStatus() != 2) {
                    log.warn("房间状态不为默认开启状态，却还在请求暂停！player: {}, roomId: {} status: {}",
                            playerId, req.roomId, friendRoom.getStatus());
                    res.code = Code.ERROR_REQ;
                    playerController.send(res);
                    return;
                }
                if (isNotSlotsRoom) {
                    friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(), new DataSaveCallback<>() {
                        @Override
                        public void updateData(FriendRoom dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(FriendRoom dataEntity) {
                            dataEntity.setStatus(1);
                            dataEntity.setOperationCoolingTime(coolDownTime);
                            return true;
                        }
                    });
                }
                log.info("玩家：{} 请求开启房间：{}", playerId, req.roomId);
                // 操作房间
                operateFriendRoom(playerController, client, req, friendRoom);
                break;
            // 暂停
            case 2:
                if (friendRoom.getStatus() != 1) {
                    // 房间不为暂停，但是却还是在请求恢复
                    log.warn("房间状态不为暂停状态，却还在请求恢复！player: {}, roomId: {}, status: {}",
                            playerId, req.roomId, friendRoom.getStatus());
                    res.code = Code.ERROR_REQ;
                    playerController.send(res);
                    return;
                }
                if (isNotSlotsRoom) {
                    // 暂停游戏
                    friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(), new DataSaveCallback<>() {
                        @Override
                        public void updateData(FriendRoom dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(FriendRoom dataEntity) {
                            dataEntity.setStatus(2);
                            dataEntity.setPauseTime(System.currentTimeMillis());
                            dataEntity.setOperationCoolingTime(coolDownTime);
                            return true;
                        }
                    });
                }
                log.info("玩家：{} 请求暂停房间：{}", playerId, req.roomId);
                // 操作房间
                operateFriendRoom(playerController, client, req, friendRoom);
                break;
            // 解散
            case 3:
                // 解散房间，如果房间不在游戏中，直接删除
                if (isNotSlotsRoom) {
                    friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(), new DataSaveCallback<>() {
                        @Override
                        public void updateData(FriendRoom dataEntity) {
                        }

                        @Override
                        public boolean updateDataWithRes(FriendRoom dataEntity) {
                            dataEntity.setStatus(3);
                            dataEntity.setPauseTime(System.currentTimeMillis());
                            return true;
                        }
                    });
                }
                log.info("玩家：{} 请求解散房间：{}", playerId, req.roomId);
                // 操作房间
                operateFriendRoom(playerController, client, req, friendRoom);
                break;
            default:
                break;
        }
    }

    private ClusterClient randomNode(PlayerController playerController, FriendRoom friendRoom, long playerId) {
        String path;
        MarsNode targetNode = nodeManager.getGameNodeByWeight(friendRoom.getGameType(), playerId, playerController.getPlayer().getIp());
        if (targetNode != null) {
            path = targetNode.getNodePath();
            String finalPath = path;
            CommonResult<FriendRoom> result = friendRoomDao.doSave(friendRoom, new DataSaveCallback<>() {
                @Override
                public void updateData(FriendRoom dataEntity) {
                }

                @Override
                public boolean updateDataWithRes(FriendRoom dataEntity) {
                    dataEntity.setPath(finalPath);
                    return true;
                }
            });
            if (!result.success()) {
                log.error("未找到节点时，随机节点保存失败 playerId:{} roomId:{}", playerId, friendRoom.getId());
                return null;
            }
            log.info("随机节点创建房间 playerId:{} roomId:{} oldPath:{} path:{}", playerId, friendRoom.getId(), friendRoom.getPath(), path);
            friendRoom.setPath(result.data.getPath());
            initFriendRoomInGameNode(friendRoom.getRoomCfgId(), friendRoom);
            return clusterSystem.getClusterByPath(path);
        }
        return null;
    }

    /**
     * 请求操作好友房
     */
    private void operateFriendRoom(
            PlayerController playerController, ClusterClient client, ReqOperateFriendRoom req, FriendRoom oldRoom) {
        ResOperateFriendRoom res = new ResOperateFriendRoom(Code.FAIL);
        try {
            // 单房间，直接等返回
            GameRpcContext.getContext().setReqParameterBuilder(
                    RpcReqParameterBuilder.create()
                            .addClusterClient(client)
                            .setTryMillisPerClient(1000));
            // 操作房间
            hallRoomBridge.operateFriendRoom(playerController.playerId(), req.roomId, req.operateCode, oldRoom.getRoomCfgId());
            // 操作完成后再获取
            FriendRoom friendRoom = friendRoomDao.getFriendRoomById(playerController.playerId(), req.roomId);
            res.roomStatus = friendRoom == null ? 3 : friendRoom.getStatus();
            res.nextPauseBtnOverdueTime = friendRoom == null ? 0 : friendRoom.getOperationCoolingTime();
            res.roomId = req.roomId;
            res.code = Code.SUCCESS;
            playerController.send(res);
        } catch (Exception e) {
            playerController.send(res);
            log.error("操作好友房发生异常：{}", e.getMessage(), e);
        } finally {
            GameRpcContext.getContext().clearRpcBuilderData();
        }
    }

    public long getCoolDownTime() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GlobalSampleConstantId.INVITATION_REFRESH_INTERVAL);
        long timeMillis = System.currentTimeMillis();
        if (globalConfigCfg != null) {
            return timeMillis + (long) globalConfigCfg.getIntValue() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        }
        return timeMillis + TimeHelper.ONE_MINUTE_OF_MILLIS;
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
        int configuredTimes = invitationResetGlobalConfigCfg.getIntValue();
        if (resetTimes != null && resetTimes >= configuredTimes) {
            res.code = Code.INVITATION_CODE_RESET_TIMES_NOT_ENOUGH;
            playerController.send(res);
            return;
        }
        int newInvitationCode = friendRoomRedisDao.genInvitationCode();
        if (newInvitationCode <= 0) {
            log.error("重置邀请码时，生成邀请码失败，时间：{}", TimeHelper.getCurrentDateZeroMilliTime());
            res.code = Code.FAIL;
            playerController.send(res);
            return;
        }
        // 重置邀请码并增加使用次数
        friendRoomRedisDao.resetInvitationCode(oldInvitationCode, newInvitationCode, player.getId());
        // 删除好友映射关系
        friendRoomFollowDao.deleteMappingRelateByInvitationCode(player.getId(), oldInvitationCode);
        // 保存玩家邀请码
        corePlayerService.doSave(player.getId(), (p) -> p.setFriendRoomInvitationCode(newInvitationCode));
        // 重置成功后需要-1
        res.resetTimes = resetTimes == null ? configuredTimes - 1 : configuredTimes - resetTimes - 1;
        res.resetTimes = Math.max(res.resetTimes, 0);
        res.invitationCode = newInvitationCode;
        res.code = Code.SUCCESS;
        log.info("玩家：{} 请求重置邀请码次数：{}", player.getId(), res.resetTimes);
        playerController.send(res);
    }

    /**
     * 获取房间节点
     *
     * @param friendRoom
     * @return
     */
    public ClusterClient getRoomNode(FriendRoom friendRoom) {
        if (friendRoom.getPath() != null) {
            ClusterClient client = clusterSystem.getClusterByPath(friendRoom.getPath());
            if (client != null) {
                return client;
            }
        }

        //TODO 押注类和百人场是否要做同样的处理
        if (CommonUtil.getMajorTypeByGameType(friendRoom.getGameType()) != CoreConst.GameMajorType.SLOTS) {
            return null;
        }

        List<ClusterClient> clusterList = clusterSystem.getNodesByType(NodeType.GAME, friendRoom.getGameType());
        if (clusterList == null || clusterList.isEmpty()) {
            return null;
        }

        for (ClusterClient cc : clusterList) {
            if (cc.nodeConfig.waitClose()) {
                continue;
            }
            if (cc.nodeConfig.getWhiteIpList() != null && cc.nodeConfig.getWhiteIpList().length > 0) {
                continue;
            }
            if (cc.nodeConfig.getWhiteIdList() != null && cc.nodeConfig.getWhiteIdList().length > 0) {
                continue;
            }
            friendRoomDao.doSave(friendRoom.getGameType(), friendRoom.getId(), new DataSaveCallback<>() {
                @Override
                public void updateData(FriendRoom dataEntity) {
                }

                @Override
                public boolean updateDataWithRes(FriendRoom dataEntity) {
                    dataEntity.setPath(cc.marsNode.getNodePath());
                    return true;
                }
            });
            friendRoom.setPath(cc.marsNode.getNodePath());
            return cc;
        }
        return null;
    }
}
