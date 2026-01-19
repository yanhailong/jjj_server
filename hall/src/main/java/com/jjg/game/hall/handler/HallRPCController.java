package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.handler.CoreRPCController;
import com.jjg.game.core.rpc.GmToHallBridge;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.utils.CoreUtil;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.LoginConfigCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private HallLogger hallLogger;
    @Autowired
    private MailService mailService;

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
                String realPhone = CoreUtil.validPhoneNumber(phone);
                if (StringUtils.isBlank(realPhone)) {
                    log.warn("后台绑定或解绑手机时，手机号格式验证错误 playerId = {},phone = {},type = {}", playerId, phone, type);
                    return Code.PARAM_ERROR;
                }

                PhoneUserInfo phoneUserInfo = new PhoneUserInfo();
                phoneUserInfo.setUserId(realPhone);
                CommonResult<Account> accountCommonResult = accountDao.addThirdAccount(player, LoginType.PHONE, phoneUserInfo);
                if (!accountCommonResult.success()) {
                    log.warn("绑定或解绑手机失败 playerId = {},failCode = {}", player.getId(), accountCommonResult.code);
                    return accountCommonResult.code;
                }

                List<Item> items = null;
                LoginConfigCfg loginConfigCfg = GameDataManager.getLoginConfigCfgList().stream().filter(cfg -> cfg.getType() == LoginType.PHONE.getValue()).findFirst().orElse(null);
                if (loginConfigCfg != null && loginConfigCfg.getAwardItem() != null && !loginConfigCfg.getAwardItem().isEmpty()) {
                    items = ItemUtils.buildItems(loginConfigCfg.getAwardItem());
                    mailService.addCfgMail(player.getId(), GameConstant.Mail.ID_BIND_PHONE, items);
                }
                hallLogger.bind(player, LoginType.PHONE.getValue(), phone);
                log.info("玩家绑定手机成功 playerId = {},phone = {},type = {},addMailItems = {}", playerId, phone, type, items == null ? null : JSONObject.toJSONString(items));
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
}
