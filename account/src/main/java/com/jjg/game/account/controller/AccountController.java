package com.jjg.game.account.controller;

import com.jjg.game.account.config.AccountConfig;
import com.jjg.game.account.data.*;
import com.jjg.game.account.dto.OAuthLoginDto;
import com.jjg.game.account.service.AccountService;
import com.jjg.game.account.service.HttpService;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.account.dto.GuestLoginDto;
import com.jjg.game.core.dao.BlackListDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.account.vo.LoginVo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.WebResult;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


/**
 * @author 11
 * @date 2025/5/24 15:01
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "account")
public class AccountController extends AbstractController {

    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    @Autowired
    private AccountConfig accountConfig;
    @Autowired
    private BlackListDao blackListDao;
    @Autowired
    private HttpService httpService;
    @Autowired
    private AccountService accountService;


    /**
     * 游客登录
     *
     * @param dto
     * @return
     */
    @RequestMapping("guestlogin")
    public WebResult<LoginVo> guestLogin(@RequestBody GuestLoginDto dto, HttpServletRequest request) {
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
        if (StringUtils.isNotEmpty(clientIp)) {
            boolean blackIp = blackListDao.blackIp(clientIp);
            if (blackIp) {
                log.debug("该ip已被封禁，无法登录 guest = {},ip = {}", dto.getGuest(), clientIp);
                return fail(Code.BAN_CAUSE_BLACK_LIST);
            }
        }

        //登录逻辑
        CommonResult<Account> accountResult = accountService.login(LoginType.GUEST, new GuestUserInfo(dto.getGuest()), dto.getMac(), dto.getChannel());
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.GUEST, accountResult.data);

        log.info("游客获取token成功 guest = {},playerId = {},token = {}", dto.getGuest(), accountResult.data.getPlayerId(),loginVoWebResult.getData().getToken());
        return loginVoWebResult;
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

//            if (dto.getToken().length() < 5 || dto.getToken().length() > 50) {
//                log.debug("token 长度不在范围内，谷歌登录失败, token = {}", dto.getToken());
//                return fail(Code.PARAM_ERROR);
//            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if (StringUtils.isNotEmpty(clientIp)) {
                boolean blackIp = blackListDao.blackIp(clientIp);
                if (blackIp) {
                    log.debug("该ip已被封禁，无法登录 token = {},ip = {}", dto.getToken(), clientIp);
                    return fail(Code.BAN_CAUSE_BLACK_LIST);
                }
            }

            //高并发情况下，采用本地校验
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

            CommonResult<GoogleUserInfo> userInfoResult = httpService.verifyGoogleToken(dto.getToken());
            if (!userInfoResult.success()) {
                return fail(userInfoResult.code);
            }

            //登录逻辑
            CommonResult<Account> accountResult = accountService.login(LoginType.GOOGLE, userInfoResult.data, dto.getMac(), dto.getChannel());
            if (!accountResult.success()) {
                return fail(accountResult.code);
            }

            //组装返回结果
            WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.GOOGLE, accountResult.data);

            log.info("谷歌登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(),loginVoWebResult.getData().getToken());
            return loginVoWebResult;
        } catch (Exception e) {
            log.error("谷歌登录异常", e);
            return exception();
        }
    }

    /**
     * 苹果登录
     */
    @RequestMapping("applelogin")
    public WebResult<LoginVo> appleLogin(@RequestBody OAuthLoginDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getToken())) {
                log.debug("参数为空，apple登录失败");
                return fail(Code.PARAM_ERROR);
            }

            if (dto.getToken().length() < 5 || dto.getToken().length() > 50) {
                log.debug("token 长度不在范围内，apple登录失败, token = {}", dto.getToken());
                return fail(Code.PARAM_ERROR);
            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if (StringUtils.isNotEmpty(clientIp)) {
                boolean blackIp = blackListDao.blackIp(clientIp);
                if (blackIp) {
                    log.debug("该ip已被封禁，无法登录 token = {},ip = {}", dto.getToken(), clientIp);
                    return fail(Code.BAN_CAUSE_BLACK_LIST);
                }
            }

            CommonResult<AppleUserInfo> userInfoResult = httpService.verifyAppleToken(dto.getToken());
            if (!userInfoResult.success()) {
                log.debug("token校验失败, 登录失败 = {},ip = {},code = {}", dto.getToken(), clientIp,userInfoResult.code);
                return fail(Code.BAN_CAUSE_BLACK_LIST);
            }

            //登录逻辑
            CommonResult<Account> accountResult = accountService.login(LoginType.APPLE, userInfoResult.data, dto.getMac(), dto.getChannel());
            if (!accountResult.success()) {
                return fail(accountResult.code);
            }

            //组装返回结果
            WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.APPLE, accountResult.data);

            log.info("apple登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(),loginVoWebResult.getData().getToken());
            return loginVoWebResult;
        } catch (Exception e) {
            log.error("第三方登录异常", e);
            return exception();
        }
    }

    /**
     * 苹果登录
     */
    @RequestMapping("facebooklogin")
    public WebResult<LoginVo> facebookLogin(@RequestBody OAuthLoginDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getToken())) {
                log.debug("参数为空，facebook登录失败");
                return fail(Code.PARAM_ERROR);
            }

            if (dto.getToken().length() < 5 || dto.getToken().length() > 50) {
                log.debug("token 长度不在范围内，facebook登录失败, token = {}", dto.getToken());
                return fail(Code.PARAM_ERROR);
            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if (StringUtils.isNotEmpty(clientIp)) {
                boolean blackIp = blackListDao.blackIp(clientIp);
                if (blackIp) {
                    log.debug("该ip已被封禁，无法登录 token = {},ip = {}", dto.getToken(), clientIp);
                    return fail(Code.BAN_CAUSE_BLACK_LIST);
                }
            }

            CommonResult<FacebookUserInfo> userInfoResult = httpService.verifyFacebookToken(dto.getToken());
            if (userInfoResult == null) {
                log.debug("token校验失败, 登录失败 = {},ip = {}", dto.getToken(), clientIp);
                return fail(Code.BAN_CAUSE_BLACK_LIST);
            }

            //登录逻辑
            CommonResult<Account> accountResult = accountService.login(LoginType.FACEBOOK, userInfoResult.data, dto.getMac(), dto.getChannel());
            if (!accountResult.success()) {
                return fail(accountResult.code);
            }

            //组装返回结果
            WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.FACEBOOK, accountResult.data);

            log.info("apple登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(),loginVoWebResult.getData().getToken());
            return loginVoWebResult;
        } catch (Exception e) {
            log.error("facebook登录异常", e);
            return exception();
        }
    }

    /**
     * 组装登录返回结果
     * @param loginType
     * @param account
     * @return
     */
    private WebResult<LoginVo> loginResult(LoginType loginType,Account account) {
        //生成token
        String token = RandomUtils.getUUid();
        //保存token，方便weboskcet连接时进行校验
        playerSessionTokenDao.save(token, loginType.getValue(), account.getPlayerId());

        LoginVo vo = new LoginVo();
        vo.setToken(token);
        vo.setGameserver(accountConfig.getGameserver());
        vo.setPlayerId(account.getPlayerId());
        return success(vo);
    }
}
