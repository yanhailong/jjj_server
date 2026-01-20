package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.rpc.GmToHallBridge;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2026/1/19
 */
@Component
public class HallRPCController extends CoreRPCController implements GmToHallBridge {

    @Autowired
    private AccountDao accountDao;
    @Autowired
    private HallPlayerService playerService;
    @Autowired
    private HallService hallService;

    @Override
    public int playerBindPhone(long playerId, String phone, int type) {
        log.info("收到绑定或解绑手机请求 playerId = {},phone = {},type = {}", playerId, phone, type);
        int code = Code.SUCCESS;
        try {
            Player player = playerService.get(playerId);
            if (player == null) {
                log.warn("后台绑定或解绑手机时，未找到该玩家信息 playerId = {},phone = {},type = {}", playerId, phone, type);
                return Code.NOT_FOUND;
            }

            if (type == 1) {  //绑定
                return hallService.playerBindPhone(player, phone).code;
            } else if (type == 2) {  //解绑
                CommonResult<Account> accountCommonResult = accountDao.removeThirdAccount(player, LoginType.PHONE);
                if (!accountCommonResult.success()) {
                    log.warn("绑定或解绑手机失败1 playerId = {},failCode = {}", player.getId(), accountCommonResult.code);
                    return accountCommonResult.code;
                }
                log.info("玩家解绑手机成功 playerId = {}", playerId, phone, type);
            } else {
                code = Code.FAIL;
                log.warn("不支持的绑定类型 playerId = {},phone = {},type = {}", playerId, phone, type);
            }
        } catch (Exception e) {
            log.error("", e);
            code = Code.EXCEPTION;
        }
        return code;
    }

    @Override
    public int afterVerifySmsSuccess(long playerId, String phone, int type) {
        try {
            log.info("大厅收到后台在短信验证成功后的消息 playerId = {},phone = {},type = {}", playerId, phone, type);
            VerCodeType verCodeType = VerCodeType.getType(type);
            if (verCodeType == null) {
                return Code.SUCCESS;
            }

            Player player = null;
            switch (verCodeType) {
                case SMS_BIND_PHONE:
                    player = playerService.get(playerId);
                    return hallService.playerBindPhone(player, phone).code;
                default:
                    return Code.SUCCESS;
            }
        } catch (Exception e) {
            log.error("", e);
            return Code.EXCEPTION;
        }
    }
}
