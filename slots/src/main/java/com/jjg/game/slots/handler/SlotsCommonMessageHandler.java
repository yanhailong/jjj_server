package com.jjg.game.slots.handler;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.manager.AbstractSlotsGameManager;
import com.jjg.game.slots.manager.CoopRoomManager;
import com.jjg.game.slots.manager.SlotsFactoryManager;
import com.jjg.game.slots.manager.SlotsRoomManager;
import com.jjg.game.slots.pb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;

@Component
@MessageType(MessageConst.MessageTypeDef.SLOTS_COMMON)
public class SlotsCommonMessageHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private SlotsRoomManager slotsRoomManager;
    @Autowired
    private SlotsFactoryManager slotsFactoryManager;
    @Autowired
    private CoopRoomManager coopRoomManager;

    @Command(SlotsConst.SlotsCommon.REQ_SLOTS_ROOM_POOL)
    public void reqSlotsRoomPool(PlayerController playerController, ReqSlotsRoomPool req) {
        try {
            ResSlotsRoomPool res = new ResSlotsRoomPool(Code.SUCCESS);
            res.value = slotsRoomManager.getPoolValue(playerController.roomId());
            playerController.send(res);
        } catch (Exception e) {
            log.error("玩家退出房间异常 msg: {}", e.getMessage(), e);
        }
    }

    @Command(SlotsConst.SlotsCommon.REQ_SLOTS_STATUS)
    public void reqSlotsStatus(PlayerController playerController, ReqSlotsStatus req) {
        NotifySlotsStatus res = new NotifySlotsStatus();
        try {
            AbstractSlotsGameManager<?, ?, ?> gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            if (gameManager == null) {
                log.warn("gameManager is error, playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                res.code = Code.FAIL;
                playerController.send(res);
                return;
            }
            res = gameManager.gameStatus(playerController);
        } catch (Exception e) {
            log.error("获取slots游戏状态异常 msg: {}", e.getMessage(), e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    @Command(SlotsConst.SlotsCommon.REQ_SLOTS_GET_SKILLS)
    public void reqSlotsGetSkills(PlayerController playerController, ReqSlotsGetSkills req) {
        ResSlotsGetSkills res = new ResSlotsGetSkills(Code.SUCCESS);
        try {
            AbstractSlotsGameManager<?, ?, ?> gameManager = slotsFactoryManager.getGameManager(playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
            if (gameManager == null) {
                log.warn("gameManager is error, playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), playerController.getPlayer().getGameType(), playerController.getPlayer().getRoomCfgId());
                res.code = Code.FAIL;
                playerController.send(res);
                return;
            }
            Map<Integer, Integer> skillsMap = gameManager.getSkills(playerController, req.tarPlayerId);
            if (skillsMap != null && !skillsMap.isEmpty()) {
                res.skillInfos = new ArrayList<>();
                for (Map.Entry<Integer, Integer> en : skillsMap.entrySet()) {
                    KVInfo kvInfo = new KVInfo();
                    kvInfo.key = en.getKey();
                    kvInfo.value = en.getValue();
                    res.skillInfos.add(kvInfo);
                }
            }
        } catch (Exception e) {
            log.error("获取slots游戏状态异常 msg: {}", e.getMessage(), e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 进入房间 (切节点后附着/断线重连/新成员加入)
     */
    @Command(SlotsConst.SlotsCommon.REQ_ENTER_COOP_ROOM)
    public void reqEnterCoopRoom(PlayerController playerController, ReqEnterCoopRoom req) {
        try {
            playerController.send(coopRoomManager.enterRoom(playerController, req.roomId));
        } catch (Exception e) {
            log.error("进入协作房间异常 playerId={},roomId={}", playerController.playerId(), req.roomId, e);
            ResEnterCoopRoom res = new ResEnterCoopRoom(Code.EXCEPTION);
            playerController.send(res);
        }
    }

    /**
     * 房间操作 (准备/取消准备/开始/退出|解散/踢人)
     */
    @Command(SlotsConst.SlotsCommon.REQ_COOP_ROOM_OP)
    public void reqCoopRoomOp(PlayerController playerController, ReqCoopRoomOp req) {
        try {
            playerController.send(coopRoomManager.operate(playerController, req.op, req.targetId));
        } catch (Exception e) {
            log.error("协作房间操作异常 playerId={},op={}", playerController.playerId(), req.op, e);
            ResCoopRoomOp res = new ResCoopRoomOp(Code.EXCEPTION);
            res.op = req.op;
            playerController.send(res);
        }
    }

    /**
     * 发送频道邀请
     */
    @Command(SlotsConst.SlotsCommon.REQ_COOP_INVITE)
    public void reqCoopInvite(PlayerController playerController, ReqCoopInvite req) {
        try {
            playerController.send(coopRoomManager.invite(playerController, req.channelCode, req.targetIds));
        } catch (Exception e) {
            log.error("协作房间邀请异常 playerId={},channel={}", playerController.playerId(), req.channelCode, e);
            ResCoopInvite res = new ResCoopInvite(Code.EXCEPTION);
            playerController.send(res);
        }
    }

    /**
     * 房间互动道具赠送
     */
    @Command(SlotsConst.SlotsCommon.REQ_COOP_GIFT)
    public void reqCoopGift(PlayerController playerController, ReqCoopGift req) {
        try {
            playerController.send(coopRoomManager.gift(playerController, req.targetId, req.giftId));
        } catch (Exception e) {
            log.error("协作房间赠礼异常 playerId={},targetId={},giftId={}",
                    playerController.playerId(), req.targetId, req.giftId, e);
            ResCoopGift res = new ResCoopGift(Code.EXCEPTION);
            playerController.send(res);
        }
    }
}
