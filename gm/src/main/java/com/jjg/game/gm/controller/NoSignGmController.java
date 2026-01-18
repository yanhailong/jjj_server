package com.jjg.game.gm.controller;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.pb.NotifyKickout;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.AccountStatus;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.dao.VerCodeDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.service.SmsService;
import com.jjg.game.core.utils.CoreUtil;
import com.jjg.game.gm.dto.DelAccountDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "ngm")
public class NoSignGmController extends AbstractController {
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private SmsService smsService;
    @Autowired
    private VerCodeDao verCodeDao;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private NodeConfig nodeConfig;

    /**
     * 封禁
     */
    @RequestMapping(BackendGMCmd.DEL_ACCOUNT)
    public WebResult<String> delAccount(@RequestBody DelAccountDto dto) {
        try {
            log.info("收到后台删除账号的请求 dto = {}", dto);
            if (!nodeConfig.gm) {
                log.warn("该接口已关闭 dto = {}", dto);
                return fail("common.fail");
            }

            if (dto.playerId() < 1 || StringUtils.isEmpty(dto.phone())) {
                log.debug("删除账号时，参数错误 dto = {}", dto);
                return fail("common.paramerror");
            }

            String realPhone = CoreUtil.validPhoneNumber(dto.phone());
            if (StringUtils.isEmpty(realPhone)) {
                log.debug("删除账号时，手机格式错误 dto = {}", dto);
                return fail("common.paramerror");
            }

            Account account = accountDao.queryAccountByPlayerId(dto.playerId());
            if (account == null) {
                log.debug("删除账号时，无法找到该账号 dto = {}", dto);
                return fail("common.paramerror");
            }

            String dbPhone = account.getThirdAccount(LoginType.PHONE);
            if (StringUtils.isEmpty(dbPhone)) {
                log.debug("删除账号时，该玩家的手机号为空 dto = {}", dto);
                return fail("common.paramerror");
            }

            if (!dbPhone.equals(dto.phone())) {
                log.debug("删除账号时，手机号不匹配 dto = {},dbPhone = {}", dto, dbPhone);
                return fail("common.paramerror");
            }

            if (dto.type() == 1) {  //请求验证码
                CommonResult<Integer> smsResult = smsService.sendCode(dto.playerId(), dbPhone, VerCodeType.DELETE_ACCOUNT);
                if (!smsResult.success()) {
                    log.debug("删除账号时，发送验证码失败 type = {}，code = {}", dto.type(), smsResult.code);
                    return fail("common.paramerror");
                }
                //返回修改结果
                return success("common.success");
            }

            if (dto.type() == 2) {  //确认验证码
                if (dto.smsCode() < GameConstant.VerCode.CODE_MIN || dto.smsCode() > GameConstant.VerCode.CODE_MAX) {
                    log.debug("删除账号时，验证码不在范围内 dto = {}", dto);
                    return fail("common.paramerror");
                }

                CommonResult<String> verifyResult = verCodeDao.verifyVerCode(dto.playerId(), VerCodeType.DELETE_ACCOUNT, dto.smsCode());
                if (!verifyResult.success()) {
                    log.debug("删除账号时，验证码错误 dto = {}", dto);
                    return fail("common.paramerror");
                }
            } else {
                log.debug("删除账号时，类型错误 type = {}", dto.type());
                return fail("common.paramerror");
            }

            accountDao.checkAndSave(dto.playerId(), a -> a.setStatus(AccountStatus.DELETE.getCode()));
            PFSession session = playerSessionService.getSession(dto.playerId());
            if (session != null) {
                //先踢人
                NotifyKickout notifyKickout = new NotifyKickout();
                session.send(notifyKickout);
            }
            playerSessionTokenDao.delToken(dto.playerId());
            //返回修改结果
            return success("common.success");
        } catch (Exception e) {
            log.error("", e);
            return fail("common.exception");
        }
    }
}
