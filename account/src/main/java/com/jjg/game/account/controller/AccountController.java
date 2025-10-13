package com.jjg.game.account.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.jjg.game.account.config.AccountConfig;
import com.jjg.game.account.constant.AccountConstant;
import com.jjg.game.account.dto.OAuthLoginDto;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.account.dao.PlayerIdDao;
import com.jjg.game.account.logger.AccountLogger;
import com.jjg.game.account.dto.GuestLoginDto;
import com.jjg.game.core.dao.BlackListDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.account.vo.LoginVo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.ChannelType;
import com.jjg.game.core.data.WebResult;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
    @Autowired
    private BlackListDao blackListDao;

    private static final String CLIENT_ID = "your-google-client-id.apps.googleusercontent.com";


    /**
     * 游客登录
     *
     * @param dto
     * @return
     */
    @RequestMapping("guestlogin")
    public WebResult<LoginVo> guestLogin(@RequestBody GuestLoginDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getGuest())) {
                log.debug("参数为空，游客登录失败");
                return fail(Code.PARAM_ERROR);
            }

            if (dto.getGuest().length() < 5 || dto.getGuest().length() > 50) {
                log.debug("guest长度不在范围内，游客登录失败, guest = {}", dto.getGuest());
                return fail(Code.PARAM_ERROR);
            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if(StringUtils.isNotEmpty(clientIp)){
                boolean blackIp = blackListDao.blackIp(clientIp);
                if(blackIp){
                    log.debug("该ip已被封禁，无法登录 guest = {},ip = {}", dto.getGuest(), clientIp);
                    return fail(Code.BAN_CAUSE_BLACK_LIST);
                }
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
                account.setChannel(ChannelType.valueOf(dto.getChannel()));
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

                //检测黑名单
                if(blackListDao.blackId(account.getPlayerId())){
                    log.debug("该用户在黑名单，无法登录 guest = {},playerId = {}", dto.getGuest(), account.getPlayerId());
                    return fail(Code.BAN_ACCOUNT);
                }

                if (!Objects.equals(dto.getMac(), account.getLastLoginMac())) {
                    accountDao.save(account);
                }
            }

            //生成token
            String token = RandomUtils.getUUid();
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

    /**
     * 谷歌登录
     */
    @RequestMapping("googlelogin")
    public WebResult<LoginVo> googleLogin(@RequestBody OAuthLoginDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getToken())) {
                log.debug("参数为空，谷歌登录失败");
                return fail(Code.PARAM_ERROR);
            }

            if (dto.getToken().length() < 5 || dto.getToken().length() > 50) {
                log.debug("guest长度不在范围内，谷歌登录失败, guest = {}", dto.getToken());
                return fail(Code.PARAM_ERROR);
            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if(StringUtils.isNotEmpty(clientIp)){
                boolean blackIp = blackListDao.blackIp(clientIp);
                if(blackIp){
                    log.debug("该ip已被封禁，无法登录 token = {},ip = {}", dto.getToken(), clientIp);
                    return fail(Code.BAN_CAUSE_BLACK_LIST);
                }
            }

//            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
//                    new NetHttpTransport(),
//                    new GsonFactory())
//                    .setAudience(Collections.singletonList(CLIENT_ID))
//                    .build();
//
//            // 验证ID Token
//            GoogleIdToken idToken = verifier.verify(dto.getToken());
//            if (idToken == null) {
//                log.debug("token无效，谷歌登录失败, guest = {}", dto.getToken());
//                return fail(Code.PARAM_ERROR);
//            }
//
//            // 提取用户信息
//            GoogleIdToken.Payload payload = idToken.getPayload();
//            String userId = payload.getSubject();
//            String email = payload.getEmail();
//            String name = (String) payload.get("name");



            LoginVo vo = new LoginVo();
            return success(vo);
        } catch (Exception e) {
            log.error("第三方登录异常", e);
            return exception();
        }
    }
}
