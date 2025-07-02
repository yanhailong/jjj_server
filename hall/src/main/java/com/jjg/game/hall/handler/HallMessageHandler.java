package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.pb.*;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.WarehouseCfg;
import com.jjg.game.hall.service.HallRoomService;
import com.jjg.game.hall.service.HallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/10 17:13
 */
@Component
@MessageType(MessageConst.MessageTypeDef.HALL_TYPE)
public class HallMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private HallService hallService;
    @Autowired
    private HallRoomService hallRoomService;


    /**
     * 进入游戏
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_ENTER_GAME)
    public void reqChooseGame(PlayerController playerController, ReqChooseGame req) {
        ResChooseGame res = new ResChooseGame(HallCode.SUCCESS);
        try {
            if (req.gameType < 1) {
                res.code = Code.PARAM_ERROR;
                log.debug("游戏类型错误，选择游戏失败 playerId = {},gameType = {}", playerController.playerId(), req.gameType);
                return;
            }

            List<WareHouseConfigInfo> wareHouseConfigList = hallService.getWareHouseConfigByGameType(req.gameType);
            if (wareHouseConfigList == null || wareHouseConfigList.isEmpty()) {
                res.code = Code.NOT_FOUND;
                log.debug("未找到对应的游戏场次配置，选择游戏失败 playerId = {},gameType = {}", playerController.playerId(), req.gameType);
                return;
            }

            res.wareHouseList = wareHouseConfigList;
            playerController.send(res);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 选择游戏场次进入
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_CHOOSE_WARE)
    public void reqChooseWare(PlayerController playerController, ReqChooseWare req) {
        ResChooseWare res = new ResChooseWare(HallCode.SUCCESS);
        try {
            log.info("收到玩家选择游戏场次 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            CommonResult<WareHouseConfigInfo> checkRes =
                checkBeforeJoinRoom(playerController, req.gameType, req.wareId);
            if (checkRes.code != Code.SUCCESS) {
                res.code = checkRes.code;
                playerController.send(res);
                return;
            }
            int wareHouseCfgId = req.gameType * 10 + req.wareId;
            // 进入大厅加入房间的逻辑
            res.code = hallRoomService.hallJoinRoom(playerController, wareHouseCfgId);
            playerController.send(res);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 加入房间之前检查前置条件
     */
    private CommonResult<WareHouseConfigInfo> checkBeforeJoinRoom(PlayerController playerController, int gameType,
                                                                  int wareId) {

        if (gameType < 1) {
            log.debug("游戏类型错误，选择场次失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return new CommonResult<>(Code.PARAM_ERROR);
        }

        List<WareHouseConfigInfo> wareHouseConfigList = hallService.getWareHouseConfigByGameType(gameType);
        if (wareHouseConfigList == null || wareHouseConfigList.isEmpty()) {
            log.debug("未找到对应的游戏场次配置，选择场次失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        WareHouseConfigInfo info =
            wareHouseConfigList.stream().filter(c -> c.wareId == wareId).findFirst().orElse(null);
        if (info == null) {

            log.debug("未找到对应的游戏场次配置2，选择场次失败 playerId = {},gameType = {},wareId = {}", playerController.playerId()
                , gameType, wareId);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        // TODO 临时代码 info.limitGoldMin != -1
        if (info.limitGoldMin != -1 && info.limitGoldMin > playerController.getPlayer().getGold()) {
            log.debug("玩家金币不足 playerId = {},gameType = {},wareId = {}", playerController.playerId(), gameType
                , wareId);
            return new CommonResult<>(Code.NOT_ENOUGH);
        }

        if (info.limitVipMin > playerController.getPlayer().getVipLevel()) {
            log.debug("玩家vip等级不足 playerId = {},gameType = {},wareId = {}", playerController.playerId(),
                gameType, wareId);
            return new CommonResult<>(Code.VIP_NOT_ENOUGH);
        }
        int wareHouseCfgId = gameType * 10 + wareId;
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(wareHouseCfgId);
        int limitGoldMax = warehouseCfg.getEnterMax();
        if (limitGoldMax != -1 && limitGoldMax < playerController.getPlayer().getGold()) {
            log.debug("玩家金币超过房间金币限制止 playerId = {},gameType = {},wareId = {}", playerController.playerId(),
                gameType, wareId);
            return new CommonResult<>(Code.GOLD_TOO_MUCH);
        }
        return new CommonResult<>(Code.SUCCESS, info);
    }

    @Override
    public String gm(PlayerController playerController, String cmd, String params) {
        try {
            log.debug("收到gm命令 playerId = {},cmd = {},params = {}", playerController.playerId(), cmd, params);
            if ("enterGame".equals(cmd)) {
                ReqChooseGame req = new ReqChooseGame();
                req.gameType = Integer.parseInt(params);
                reqChooseGame(playerController, req);
            }

        } catch (Exception e) {
            log.error("", e);
        }
        return null;
    }
}
