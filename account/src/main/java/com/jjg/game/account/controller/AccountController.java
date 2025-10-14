package com.jjg.game.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.account.config.AccountConfig;
import com.jjg.game.account.data.*;
import com.jjg.game.account.dto.LoginDto;
import com.jjg.game.account.dto.ServerUrlDto;
import com.jjg.game.account.service.AccountService;
import com.jjg.game.account.service.HttpService;
import com.jjg.game.account.vo.ServerUrlVo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.dao.BlackListDao;
import com.jjg.game.core.data.Account;
import com.jjg.game.account.vo.LoginVo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.PlayerSessionToken;
import com.jjg.game.core.data.WebResult;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;


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
    @RequestMapping("login")
    public WebResult<LoginVo> login(@RequestBody LoginDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getData())) {
                log.debug("参数为空，登录失败 dto = {}", JSONObject.toJSONString(dto));
                return fail(Code.PARAM_ERROR);
            }

            LoginType loginType = LoginType.valueOf(dto.getLoginType());
            if(loginType == null){
                log.debug("登录类型错误，登录失败 dto = {}", JSONObject.toJSONString(dto));
                return fail(Code.PARAM_ERROR);
            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if (StringUtils.isNotEmpty(clientIp)) {
                boolean blackIp = blackListDao.blackIp(clientIp);
                if (blackIp) {
                    log.debug("该ip已被封禁，无法登录 ip = {},dto = {}", clientIp, JSONObject.toJSONString(dto));
                    return fail(Code.BAN_CAUSE_BLACK_LIST);
                }
            }

            switch (loginType) {
                case GUEST -> {
                    return guestLogin(dto);
                }
                case GOOGLE -> {
                    return googleLogin(dto);
                }
                case APPLE -> {
                    return appleLogin(dto);
                }
                case FACEBOOK -> {
                    return facebookLogin(dto);
                }
                case PHONE -> {
                    return phoneLogin(dto);
                }
                default -> {
                    return fail(Code.PARAM_ERROR);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            return exception();
        }
    }

    /**
     * 游客登录
     *
     * @param dto
     * @return
     */
    private WebResult<LoginVo> guestLogin(LoginDto dto) {
        //登录逻辑
        CommonResult<Account> accountResult = accountService.login(LoginType.GUEST, new GuestUserInfo(dto.getData()), dto.getMac(), dto.getChannel());
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.GUEST, accountResult.data);

        log.info("游客获取token成功 guest = {},playerId = {},token = {}", dto.getData(), accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * 谷歌登录
     * @param dto
     * @return
     */
    private WebResult<LoginVo> googleLogin(LoginDto dto) {
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

        CommonResult<GoogleUserInfo> userInfoResult = httpService.verifyGoogleToken(dto.getData());
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

        log.info("谷歌登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * apple登录
     * @param dto
     * @return
     */
    private WebResult<LoginVo> appleLogin(LoginDto dto) {
        CommonResult<AppleUserInfo> userInfoResult = httpService.verifyAppleToken(dto.getData());
        if (!userInfoResult.success()) {
            log.debug("token校验失败, 登录失败 = {},code = {}", dto.getData(), userInfoResult.code);
            return fail(Code.BAN_CAUSE_BLACK_LIST);
        }

        //登录逻辑
        CommonResult<Account> accountResult = accountService.login(LoginType.APPLE, userInfoResult.data, dto.getMac(), dto.getChannel());
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.APPLE, accountResult.data);

        log.info("apple登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * apple登录
     * @param dto
     * @return
     */
    private WebResult<LoginVo> phoneLogin(LoginDto dto) {
        CommonResult<AppleUserInfo> userInfoResult = httpService.verifyAppleToken(dto.getData());
        if (!userInfoResult.success()) {
            log.debug("token校验失败, 登录失败 = {},code = {}", dto.getData(), userInfoResult.code);
            return fail(Code.BAN_CAUSE_BLACK_LIST);
        }

        //登录逻辑
        CommonResult<Account> accountResult = accountService.login(LoginType.APPLE, userInfoResult.data, dto.getMac(), dto.getChannel());
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.APPLE, accountResult.data);

        log.info("phone登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * facebook登录
     * @param dto
     * @return
     */
    public WebResult<LoginVo> facebookLogin(LoginDto dto) {
        CommonResult<FacebookUserInfo> userInfoResult = httpService.verifyFacebookToken(dto.getData());
        if (userInfoResult == null) {
            log.debug("token校验失败, 登录失败 dto= {}", JSONObject.toJSONString(dto));
            return fail(Code.BAN_CAUSE_BLACK_LIST);
        }

        //登录逻辑
        CommonResult<Account> accountResult = accountService.login(LoginType.FACEBOOK, userInfoResult.data, dto.getMac(), dto.getChannel());
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.FACEBOOK, accountResult.data);

        log.info("facebook 登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * 获取服务器地址
     */
    @RequestMapping("serverurl")
    private WebResult<ServerUrlVo> serverUrl(@RequestBody ServerUrlDto dto, HttpServletRequest request) {
        try{
            String token = request.getHeader("token");
            long playerId = dto.getPlayerId();
            if(StringUtils.isEmpty(token) || playerId < 0) {
                log.debug("参数不能为空，获取服务器地址失败 token = {},playerId = {}", token, playerId);
                return fail(Code.PARAM_ERROR);
            }

            //从数据库查询PlayerSessionToken对象信息
            PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(playerId);
            if (playerSessionToken == null) {
                log.debug("没有从db中找到playerSessionToken对象,登录失败, playerId = {}", playerId);
                return fail(Code.ERROR_REQ);
            }

            //校验token
            if (!playerSessionToken.getToken().equals(token)) {
                log.debug("token校验失败,登录失败, playerId = {},dbToken = {},reqToken = {}", playerId,
                        playerSessionToken.getToken(), token);
                return fail(Code.EXPIRE);
            }

            ServerUrlVo serverUrlVo = new ServerUrlVo();

            List<String> gameServersUrls = new ArrayList<>();
            gameServersUrls.add(accountConfig.getGameserver());
            serverUrlVo.setGameServersUrls(gameServersUrls);
            log.info("获取服务器地址 playerId = {},token = {},gameServersUrls = {}", playerId, token,gameServersUrls);
            return success(serverUrlVo);
        }catch (Exception e){
            log.error("", e);
            return exception();
        }
    }

    /**
     * 组装登录返回结果
     *
     * @param loginType
     * @param account
     * @return
     */
    private WebResult<LoginVo> loginResult(LoginType loginType, Account account) {
        //生成token
        String token = RandomUtils.getUUid();
        //保存token，方便weboskcet连接时进行校验
        playerSessionTokenDao.save(token, loginType.getValue(), account.getPlayerId());

        LoginVo vo = new LoginVo();
        vo.setToken(token);
//        vo.setGameserver(accountConfig.getGameserver());
        vo.setPlayerId(account.getPlayerId());
        return success(vo);
    }
}
