package com.jjg.game.slots.manager;

import com.google.common.util.concurrent.Striped;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.rpc.RpcCallSetting;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.room.FriendRoomBillHistoryDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.rpc.HallRoomBridge;
import com.jjg.game.core.service.MailService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.RoomExpendCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import com.jjg.game.slots.controller.SlotsRoomController;
import com.jjg.game.slots.dao.FriendRoomSlotsBillHistoryDao;
import com.jjg.game.slots.dao.RoomSlotsPoolDao;
import com.jjg.game.slots.dao.SlotsFriendRoomDao;
import com.jjg.game.slots.logger.SlotsLogger;
import com.jjg.game.slots.pb.ResSlotsStatus;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

@Component
public class SlotsRoomManager implements HallRoomBridge {
    private final Logger log = LoggerFactory.getLogger(SlotsRoomManager.class);

    @Autowired
    private SlotsFriendRoomDao slotsFriendRoomDao;
    @Autowired
    private RoomSlotsPoolDao roomSlotsPoolDao;
    @Autowired
    private FriendRoomSlotsBillHistoryDao friendRoomSlotsBillHistoryDao;
    @Autowired
    private FriendRoomBillHistoryDao friendRoomBillHistoryDao;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private MailService mailService;
    @Autowired
    private SlotsLogger slotsLogger;
    @Autowired
    private MarsCurator marsCurator;
    private final Striped<Lock> roomLocks = Striped.lock(1024);
    private final ConcurrentHashMap<Long, SlotsRoomController> roomControllers = new ConcurrentHashMap<>();

    private final Map<Long, Long> poolMap = new ConcurrentHashMap<>();

    public void init() {
    }

    /**
     * 玩家进入slots 好友房
     *
     * @param playerController
     * @return
     */
    public SlotsRoomController enterRoom(PlayerController playerController) {
        SlotsRoomController slotsRoomController = roomControllers.get(playerController.roomId());
        if (slotsRoomController != null) {
            return slotsRoomController;
        }
        SlotsFriendRoom room = null;
        Lock lock = roomLocks.get(playerController.roomId());
        lock.lock();
        try {
            room = slotsFriendRoomDao.getRoom(playerController.getPlayer().getGameType(), playerController.roomId());
            if (room == null) {
                log.warn("进入好友房失败,未找到房间信息 playerId = {},gameType = {},roomId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.roomId());
                return null;
            }
            if (!this.marsCurator.nodePath.equals(room.getPath())) {
                return null;
            }

            final SlotsFriendRoom finalRoom = room;
            slotsRoomController = roomControllers.computeIfAbsent(playerController.roomId(), k -> new SlotsRoomController(finalRoom));

            Number number = roomSlotsPoolDao.getBigPoolByRoomId(playerController.roomId());
            poolMap.put(playerController.roomId(), number == null ? 0 : number.longValue());

            slotsRoomController.addPlayer(playerController);
            slotsFriendRoomDao.save(room);
        } catch (Exception e) {
            log.error("enterRoom error", e);
        } finally {
            lock.unlock();
        }
        if (room == null) {
            return null;
        }
        return slotsRoomController;
    }

    /**
     * 退出房间
     *
     * @param playerController
     */
    public void exitRoom(PlayerController playerController) {
        if (playerController.getScene() == null) {
            return;
        }

        if (playerController.getScene() instanceof SlotsRoomController slotsRoomController) {
            slotsRoomController.exitRoom(playerController);
            slotsFriendRoomDao.save(slotsRoomController.getRoom());
        }
    }

    /**
     * 创建slots好友房
     *
     * @param roomCfgId
     * @param roomId    房间ID
     */
    @Override
    public void createFriendRoom(int roomCfgId, long roomId) {
        try {
            SlotsFriendRoom room = slotsFriendRoomDao.getRoomByCfgId(roomCfgId, roomId, true);
            if (room == null) {
                log.warn("创建好友房失败,未找到房间信息 roomCfgId = {},roomId = {}", roomCfgId, roomId);
                return;
            }
            room.setInGaming(true);
            roomControllers.computeIfAbsent(roomId, k -> new SlotsRoomController(room));
            roomSlotsPoolDao.initRoomPool(roomId, room.getPredictCostGoldNum());
            updatePoolValue(roomId, room.getPredictCostGoldNum());
            log.info("slots初始化房间成功 roomCfgId = {},roomId = {}", roomCfgId, roomId);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    @Override
    @RpcCallSetting(processorModKey = "#arg1")
    public void operateFriendRoom(long playerId, long roomId, int operateCode, int roomCfgId) {
        try {
            if (operateCode < 1 || operateCode > 3) {
                return;
            }

            SlotsRoomController slotsRoomController = roomControllers.get(roomId);
            if (slotsRoomController == null) {
                SlotsFriendRoom room = slotsFriendRoomDao.getRoomByCfgId(roomCfgId, roomId, false);
                if (room != null) {
                    slotsRoomController = roomControllers.computeIfAbsent(roomId, k -> new SlotsRoomController(room));
                } else {
                    log.warn("操作房间失败,roomController为空 playerId = {},roomId = {},operateCode = {} ", playerId, roomId, operateCode);
                    return;
                }
            }

            // 房主
            long roomCreator = slotsRoomController.getRoom().getCreator();
            if (roomCreator != playerId) {
                log.error("操作异常，玩家：{} 请求操作房间，但房间房主ID为：{}", playerId, roomCreator);
                return;
            }

            switch (operateCode) {
                case 2:
                    log.info("收到请求暂停房间：{} 的请求", roomId);
                    // 暂停房间
//                    slotsRoomController.pauseGame();
                    pause(slotsRoomController);
                    break;
                case 1:
                    log.info("收到请求继续房间：{} 的请求", roomId);
                    // 继续游戏
//                    slotsRoomController.tryContinueGame();
                    tryContinueGame(slotsRoomController);
                    break;
                case 3:
                    log.info("收到请求解散房间：{} 的请求", roomId);
                    dissbandRoom(slotsRoomController);
//                    destoryRoom(slotsRoomController);
                    break;
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 暂停游戏
     *
     * @param slotsRoomController
     */
    private void pause(SlotsRoomController slotsRoomController) {
        slotsRoomController.pauseGame();
    }

    /**
     * 房间继续
     *
     * @param slotsRoomController
     */
    private void tryContinueGame(SlotsRoomController slotsRoomController) {
        autoRenewal(slotsRoomController);
    }

    /**
     * 自动续费
     *
     * @param slotsRoomController
     */
    public boolean autoRenewal(SlotsRoomController slotsRoomController) {
        long now = System.currentTimeMillis();
        //检查到期时间
        if (slotsRoomController.getRoom().getOverdueTime() >= now) {
            return false;
        }

        // 如果时间到期且没有开启自动续费，先暂停游戏
        long roomId = slotsRoomController.getRoom().getId();
        if (!slotsRoomController.getRoom().isAutoRenewal() || this.nodeConfig.waitClose()) {
            log.info("房间时长到期 roomId = {},roomCfgId = {}", roomId, slotsRoomController.getRoom().getRoomCfgId());
            return false;
        }

        if (slotsRoomController.getRoom().getRoomPlayers().isEmpty()) {
            List<LanguageParamData> params = new ArrayList<>();
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(slotsRoomController.getRoom().getRoomCfgId());
            params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
            params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
            mailService.addCfgMail(slotsRoomController.getRoom().getCreator(), 2, null, params);
            log.info("房间时长到期,没有玩家暂停续费 roomId = {},roomCfgId = {}", roomId, slotsRoomController.getRoom().getRoomCfgId());
            return false;
        }

        // 自动续费，检查玩家金币是否足够
        RoomExpendCfg roomExpendCfg = getAutoRenewalCfg();
        if (roomExpendCfg == null) {
            log.warn("未检查到自动续费配置 roomId = {},roomCfgId = {}", roomId, slotsRoomController.getRoom().getRoomCfgId());
            return false;
        }

        List<Integer> requiredMoney = roomExpendCfg.getRequiredMoney();
        int itemNum = requiredMoney.get(1);
        // 时长，毫秒
        long durationTime = (long) roomExpendCfg.getDurationTime() * TimeHelper.ONE_MINUTE_OF_MILLIS;
        // 从房间底庄中扣除金币，如果不足直接暂停游戏
        if (itemNum > slotsRoomController.getRoom().getPredictCostGoldNum()) {
            // 自动续费失败，房间准备金不足
            log.info("自动续费失败，房间准备金不足: need: {} rest: {}", itemNum, slotsRoomController.getRoom().getPredictCostGoldNum());
            List<LanguageParamData> params = new ArrayList<>();
            WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(slotsRoomController.getRoom().getRoomCfgId());
            params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
            params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));
            mailService.addCfgMail(slotsRoomController.getRoom().getCreator(), 3, null, params);
            return false;
        }
        long overdueTime = slotsRoomController.getRoom().getOverdueTime();
        long totalTake = 0;
        while (itemNum < slotsRoomController.getRoom().getPredictCostGoldNum()) {
            slotsRoomController.getRoom().setPredictCostGoldNum(slotsRoomController.getRoom().getPredictCostGoldNum() - itemNum);
            totalTake += itemNum;
            overdueTime += durationTime;
            if (overdueTime > now) {
                break;
            }
        }

        if (totalTake > 0) {
            //如果房间续费
            //因为续费金额从保证金里面扣除
            //所以这里要从池子里面扣除
            Long remain = roomSlotsPoolDao.addToBigPool(roomId, -totalTake);
            updatePoolValue(roomId, remain);
        }

        // 续费时长
        slotsRoomController.getRoom().setOverdueTime(overdueTime);
        slotsRoomController.getRoom().setPredictCostGoldNum(slotsRoomController.getRoom().getPredictCostGoldNum());

        Map<Integer, Long> itemMap = Map.of(requiredMoney.getFirst(), (long) itemNum);
        ItemOperationResult itemOperationResult = new ItemOperationResult();
        itemOperationResult.setDiamond(slotsRoomController.getRoom().getPredictCostGoldNum());
        slotsLogger.roomOperate(slotsRoomController.getRoom(), 2, roomExpendCfg.getDurationTime(), itemMap, itemOperationResult);
        log.error("房间自动续费成功, roomId = {},roomCfgId = {},overdueTime={} totalTake={}", roomId, slotsRoomController.getRoom().getRoomCfgId(), overdueTime, totalTake);
        return true;
    }

    /**
     * 解散房间
     * 只是将房间标记未解散的状态
     *
     * @param slotsRoomController
     */
    private void dissbandRoom(SlotsRoomController slotsRoomController) {
        // 解散房间
        slotsRoomController.destroyOnNextRoundStart();

        List<LanguageParamData> params = new ArrayList<>(2);
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(slotsRoomController.getRoom().getRoomCfgId());
        params.add(new LanguageParamData(1, warehouseCfg.getNameid() + ""));
        params.add(new LanguageParamData(TimeHelper.getDate(System.currentTimeMillis())));

        Number gainNumber = roomSlotsPoolDao.getBigPoolByRoomId(slotsRoomController.getRoom().getId());
        if (gainNumber == null || gainNumber.longValue() < 1) {
            log.warn("解散房间时无保证金可退还 playerId = {},roomId = {},gainNumber = {}", slotsRoomController.getRoom().getCreator(), slotsRoomController.getRoom().getId(), gainNumber == null ? "null" : gainNumber.longValue());
            return;
        }
        List<Item> returnItems = List.of(new Item(warehouseCfg.getTransactionItemId(), gainNumber.longValue()));
        Mail mail = mailService.addCfgMail(slotsRoomController.getRoom().getCreator(), 35, returnItems, params);
        slotsLogger.roomDisband(slotsRoomController.getRoom(), mail.getId(), returnItems);
    }

    /**
     * 销毁房间
     * 清除房间相关的数据
     *
     * @param room
     */
    private void destoryRoom(SlotsFriendRoom room) {
        //检查房间玩家是否全部离开
        if (!room.empty()) {
            log.warn("房间状态标记已解散，但是玩家未离开，解散失败 roomId = {}", room.getId());
            return;
        }
        //删除room对象
        slotsFriendRoomDao.removeRoom(room.getGameType(), room.getId(), room.getRoomCfgId());
        //删除缓存中的slotsRoomController
        this.roomControllers.remove(room.getId());
        //删除奖池
        roomSlotsPoolDao.removePoolByRoomId(room.getId());
    }

    @Override
    public FriendRoom getFriendRoomInfo(long roomId) {
        return null;
    }

    @Override
    public CommonResult<FriendRoom> updateFriendRoom(long playerId, int roomCfgId, long roomId, int addTime, boolean autoRenewal, long predictCostGoldNum, String roomAliasName) {
        SlotsRoomController roomController = roomControllers.get(roomId);
        CommonResult<FriendRoom> result = new CommonResult<>(Code.SUCCESS);
        if (roomController == null) {
            SlotsFriendRoom room = slotsFriendRoomDao.getRoomByCfgId(roomCfgId, roomId, false);
            if (room == null) {
                log.warn("修改好友房失败,未找到房间信息 playerId = {},roomCfgId = {},roomId = {}", playerId, roomCfgId, roomId);
                result.code = Code.NOT_FOUND;
                return result;
            }

            if (!this.marsCurator.nodePath.equals(room.getPath())) {
                result.code = Code.NOT_FOUND;
                return result;
            }

            roomController = roomControllers.computeIfAbsent(roomId, k -> new SlotsRoomController(room));
        }

        SlotsFriendRoom friendRoom = roomController.getRoom();
        if (playerId != friendRoom.getCreator()) {
            log.warn("修改好友房失败,房主不匹配 playerId = {},roomCfgId = {},roomId = {},roomCreator = {}", playerId, roomCfgId, roomId, friendRoom.getCreator());
            result.code = Code.FORBID;
            return result;
        }

        if (!StringUtils.isEmpty(roomAliasName)) {
            friendRoom.setAliasName(roomAliasName);
        }

        friendRoom.setAutoRenewal(autoRenewal);

        if (predictCostGoldNum > 0) {
            Long remain = roomSlotsPoolDao.addToBigPool(roomId, predictCostGoldNum);
            friendRoom.setPool(friendRoom.getPool() + predictCostGoldNum);
            friendRoom.setPredictCostGoldNum(remain);
            updatePoolValue(roomId, remain);

            //通知房间中的玩家，该游戏状态可以继续玩
            SlotsFactoryManager slotsFactoryManager = CommonUtil.getContext().getBean(SlotsFactoryManager.class);
            AbstractSlotsGameManager gameManager = slotsFactoryManager.getGameManager(friendRoom.getGameType(), friendRoom.getRoomCfgId());
            CommonResult<Long> commonResult = gameManager.checkAndGetPredictCostGoldNum(roomController);
            if (commonResult.success()) {
                roomController.notifyAllPlayers(new ResSlotsStatus(200));
            }
        }

        if (addTime > 0) {
            long curTime = System.currentTimeMillis();
            // 不管时间是否暂停，都只需要给原有的过期时间加上增量时间
            if (friendRoom.getOverdueTime() < curTime) {
                // 房间已经过期，续时间
                friendRoom.setOverdueTime(curTime + addTime);
            } else {
                // 房间未过期，续时间
                friendRoom.setOverdueTime(friendRoom.getOverdueTime() + addTime);
            }
        }

        //保存一次房间信息
        slotsFriendRoomDao.save(friendRoom);
        log.info("修改好友房信息成功 playerId = {},roomCfgId = {},roomId = {},roomExpendCfgId = {},autoRenewal = {},predictCostGoldNum = {},roomAliasName = {}",
                playerId, roomCfgId, roomId, addTime, autoRenewal, predictCostGoldNum, roomAliasName);
        result.data = friendRoom;
        return result;
    }

    /**
     * 记录slots房间下注统计信息
     *
     * @param roomId
     * @param betValue
     */
    public void playerBet(long roomId, long playerId, long betValue, long roomInCome) {
        SlotsRoomController slotsRoomController = roomControllers.get(roomId);
        if (slotsRoomController == null) {
            log.warn("记录slots房间下注统计信息失败，未获取到 slotsRoomController，roomId = {},playerId = {} ", roomId, playerId);
            return;
        }
        slotsRoomController.playerBet(playerId, betValue, roomInCome);
    }

    /**
     * 关机
     */
    public void shutDown() {
        slotsRoomCollect();
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
     * slots好友房定时统计
     */
    public synchronized void slotsRoomCollect() {
        Map<Long, SlotsRoomController> tmpRoomControllers = new HashMap<>(this.roomControllers);
        if (tmpRoomControllers.isEmpty()) {
            return;
        }

        try {
            //先将所有房间切换
            Set<SlotsFriendRoom> rooms = new HashSet<>();
            for (Map.Entry<Long, SlotsRoomController> en : tmpRoomControllers.entrySet()) {
                rooms.add(en.getValue().getRoom());
            }

            //等待房间切换完毕
            Thread.sleep(500);

            int month = TimeHelper.getMonthNumerical();
            for (SlotsFriendRoom friendRoom : rooms) {
                WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(friendRoom.getRoomCfgId());
//                log.debug("开始统计 roomId = {},room = {}", friendRoom.getId(), JSON.toJSONString(friendRoom));

                //查询FriendRoomBillHistoryBean的id
                long id = friendRoomSlotsBillHistoryDao.queryId(friendRoom.getGameType(), month, friendRoom.getCreator());

                SlotsBillInfo slotsBillInfo = friendRoom.toggleFlag();

                FriendRoomBillHistoryBean historyBean = new FriendRoomBillHistoryBean();
                historyBean.setId(id);
                historyBean.setRoomCreator(friendRoom.getCreator());
                historyBean.setTotalFlowing(slotsBillInfo.getTotalFlowing());
                historyBean.setTotalIncome(slotsBillInfo.getTotalIncome());

                if (slotsBillInfo.getPartInPlayerIncome() != null && !slotsBillInfo.getPartInPlayerIncome().isEmpty()) {
                    historyBean.setPartInPlayerIncome(slotsBillInfo.getPartInPlayerIncome());
                } else {
                    historyBean.setPartInPlayerIncome(new HashMap<>());
                }
                if (slotsBillInfo.getPartInPlayerBet() != null && !slotsBillInfo.getPartInPlayerBet().isEmpty()) {
                    historyBean.setPartInPlayerBetScore(slotsBillInfo.getPartInPlayerBet());
                } else {
                    historyBean.setPartInPlayerBetScore(new HashMap<>());
                }
                historyBean.setCreatedAt(System.currentTimeMillis());
                historyBean.setGameType(friendRoom.getGameType());
                historyBean.setRoomCreator(friendRoom.getCreator());
                historyBean.setMonth(month);
                historyBean.setItemId(warehouseCfg.getTransactionItemId());
                historyBean.setGameMajorType(CommonUtil.getMajorTypeByGameType(friendRoom.getGameType()));

                //保存到数据库
                friendRoomBillHistoryDao.saveSlotsBillHistory(historyBean);

                if (friendRoom.getStatus() == 3) {
                    destoryRoom(friendRoom);
                } else {
                    slotsFriendRoomDao.save(friendRoom);
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 获取缓存中的奖池值
     *
     * @param roomId
     * @return
     */
    public long getPoolValue(long roomId) {
        if (roomId < 1) {
            return 0;
        }
        SlotsRoomController slotsRoomController = roomControllers.get(roomId);
        if (slotsRoomController == null) {
            return 0;
        }
        return this.poolMap.getOrDefault(roomId, 0L);
    }

    /**
     * 修改缓存中的奖池值
     *
     * @param roomId
     * @param newValue
     */
    public void updatePoolValue(long roomId, long newValue) {
        if (roomId < 1) {
            return;
        }
        SlotsRoomController slotsRoomController = roomControllers.get(roomId);
        if (slotsRoomController == null) {
            return;
        }
        slotsRoomController.getRoom().setPredictCostGoldNum(newValue);
        this.poolMap.put(roomId, newValue);
    }
}
