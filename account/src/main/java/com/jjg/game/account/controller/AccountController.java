package com.jjg.game.account.controller;

import com.jjg.game.account.config.AccountConfig;
import com.jjg.game.account.constant.AccountConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.account.dao.PlayerIdDao;
import com.jjg.game.account.logger.AccountLogger;
import com.jjg.game.account.dto.GuestLoginDto;
import com.jjg.game.core.data.Account;
import com.jjg.game.account.vo.LoginVo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.WebResult;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * @author 11
 * @date 2025/5/24 15:01
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "account")
public class AccountController extends AbstractController {

    @Autowired
    private AccountDao accountDao;

    @Autowired
    private PlayerIdDao playerIdDao;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private AccountLogger accountLogger;
    @Autowired
    private AccountConfig accountConfig;


    /**
     * 游客登录
     *
     * @param dto
     * @return
     */
    @RequestMapping("guestlogin")
    public WebResult<LoginVo> guestLogin(@RequestBody GuestLoginDto dto) {
        try {
            if (StringUtils.isEmpty(dto.getGuest())) {
                log.debug("参数为空，游客登录失败");
                return fail(Code.PARAM_ERROR);
            }

            if (dto.getGuest().length() < 5 || dto.getGuest().length() > 50) {
                log.debug("guest长度不在范围内，游客登录失败, guest = {}", dto.getGuest());
                return fail(Code.PARAM_ERROR);
            }

            //查询该账号是否存在
            Account account = accountDao.queryAccountByGuest(dto.getGuest());

            if (account == null) {
                //注册新账号
                long playerId = playerIdDao.getNewId();
                // 如果player获取为0，则需要中断流程
                if (playerId <= 0) {
                    log.warn("创建新的账号，从数据库中获取玩家ID失败 gust：{}", dto.getGuest());
                    return fail(Code.FAIL);
                }
                account = new Account();
                account.setPlayerId(playerId);
                account.setGuest(dto.getGuest());
                account.setAccountType(AccountConstant.AccountType.GUEST);
                account.setRegisterMac(dto.getMac());
                account.setLastLoginMac(dto.getMac());
                account.setLastLoginTime(System.currentTimeMillis());
                account.setChannel(dto.getChannel());
                account = accountDao.insert(account);

                accountLogger.register(dto.getGuest(), GameConstant.LoginType.GUEST, playerId);
                log.debug("创建新的游客账号 guest = {},playerId = {}", dto.getGuest(), playerId);
            } else {
                //如果不为空，要检测是否已经认证
                if (account.getAccountType() != AccountConstant.AccountType.GUEST) {
                    log.debug("该用户已经认证，无法使用游客登录 guest = {},playerId = {}", dto.getGuest(), account.getPlayerId());
                    return fail(Code.PARAM_ERROR);
                }

                if (account.getStatus() == GameConstant.AccountStatus.BAN) {
                    log.debug("该用户已被封号，无法登录 guest = {},playerId = {}", dto.getGuest(), account.getPlayerId());
                    return fail(Code.BAN_ACCOUNT);
                }

                if (!Objects.equals(dto.getMac(), account.getLastLoginMac())) {
                    accountDao.save(account);
                }
            }

            //生成token
            String token = genernateToken();
            //保存token，方便weboskcet连接时进行校验
            playerSessionTokenDao.save(token, GameConstant.LoginType.GUEST, account.getPlayerId());

            LoginVo vo = new LoginVo();
            vo.setToken(token);
            vo.setGameserver(accountConfig.getGameserver());
            vo.setPlayerId(account.getPlayerId());
            log.info("游客获取token成功 guest = {},playerId = {}", dto.getGuest(), account.getPlayerId());
            return success(vo);
        } catch (Exception e) {
            log.error("", e);
            return exception();
        }
    }
}
