package com.jjg.game.account.controller;

import com.alibaba.fastjson.JSONObject;
import com.jjg.game.account.config.AccountConfig;
import com.jjg.game.account.data.LoginResult;
import com.jjg.game.account.dto.LoginConfigDto;
import com.jjg.game.account.dto.LoginDto;
import com.jjg.game.account.dto.LoginSmsDto;
import com.jjg.game.account.dto.ServerUrlDto;
import com.jjg.game.account.service.AccountService;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.service.ThirdAccountHttpService;
import com.jjg.game.account.vo.LoginConfigVo;
import com.jjg.game.account.vo.ServerUrlVo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.core.data.*;
import com.jjg.game.account.vo.LoginVo;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.service.BlackListService;
import com.jjg.game.core.service.LoginConfigService;
import com.jjg.game.core.service.SmsService;
import com.jjg.game.core.utils.CoreUtil;
import com.jjg.game.sampledata.GameDataManager;
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
    private BlackListService blackListService;
    @Autowired
    private ThirdAccountHttpService thirdAccountHttpService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private SmsService smsService;
    @Autowired
    private LoginConfigService loginConfigService;
    @Autowired
    private CountDao countDao;

    /**
     * 获取开启的登录方式
     *
     * @return
     */
    @RequestMapping("loginConfig")
    public WebResult<List<LoginConfigVo>> loginConfig(@RequestBody LoginConfigDto dto) {
        List<LoginConfigVo> resultList = new ArrayList<>(GameDataManager.getLoginConfigCfgList().size());
        GameDataManager.getLoginConfigCfgList().forEach(config -> {
            LoginConfigVo vo = new LoginConfigVo();

            vo.setType(config.getType());
            if (config.getType() == LoginType.GOOGLE.getValue()) {
                if (dto.getDevice() == DeviceType.ANDROID.getValue()) {
                    vo.setOpen(loginConfigService.isOpen(config.getType()));
                } else {
                    vo.setOpen(false);
                }
            } else if (config.getType() == LoginType.APPLE.getValue()) {
                if (dto.getDevice() == DeviceType.IOS.getValue()) {
                    vo.setOpen(loginConfigService.isOpen(config.getType()));
                } else {
                    vo.setOpen(false);
                }
            } else {
                vo.setOpen(loginConfigService.isOpen(config.getType()));
            }

            resultList.add(vo);
        });
        return success(resultList);
    }

    /**
     * 游客登录
     *
     * @param dto
     * @return
     */
    @RequestMapping("login")
    public WebResult<LoginVo> login(@RequestBody LoginDto dto, HttpServletRequest request) {
        try {
            log.debug("收到登录消息 dto = {}", JSONObject.toJSONString(dto));
            if (StringUtils.isEmpty(dto.getData())) {
                log.debug("参数为空，登录失败 dto = {}", JSONObject.toJSONString(dto));
                return fail(Code.REQ_LOGIN_PARAMS_EMPTY);
            }

            LoginType loginType = LoginType.valueOf(dto.getLoginType());
            if (loginType == null) {
                log.debug("登录类型错误，登录失败 dto = {}", JSONObject.toJSONString(dto));
                return fail(Code.LOGIN_TYPE_EMPTY_OR_NOT_EXIST);
            }

            if (!loginConfigService.isOpen(dto.getLoginType())) {
                log.debug("该登录类型被后台关闭，登录失败 dto = {}", JSONObject.toJSONString(dto));
                return fail(Code.LOGIN_TYPE_NOT_ENABLED);
            }

            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if (StringUtils.isNotEmpty(clientIp)) {
                boolean blackIp = blackListService.isBlackIp(clientIp);
                if (blackIp) {
                    log.debug("该ip已被封禁，无法登录 ip = {},dto = {}", clientIp, JSONObject.toJSONString(dto));
                    return fail(Code.IP_BLOCKED_LOGIN_DISABLED);
                }
            }

            if(StringUtils.isBlank(dto.getSubChannel())){
                dto.setSubChannel("1");
            }

            switch (loginType) {
                case GUEST -> {
                    return guestLogin(dto, clientIp);
                }
                case GOOGLE -> {
                    return googleLogin(dto, clientIp);
                }
                case APPLE -> {
                    return appleLogin(dto, clientIp);
                }
                case FACEBOOK -> {
                    return facebookLogin(dto, clientIp);
                }
                case PHONE -> {
                    return phoneLogin(dto, clientIp);
                }
                default -> {
                    return fail(Code.LOGIN_TYPE_EMPTY_OR_NOT_EXIST);
                }
            }
        } catch (Exception e) {
            log.error("", e);
            return exception();
        }
    }

    /**
     * 获取服务器地址
     */
    @RequestMapping("serverurl")
    private WebResult<ServerUrlVo> serverUrl(@RequestBody ServerUrlDto dto, @RequestHeader("token") String token, HttpServletRequest request) {
        try {
            long playerId = dto.getPlayerId();
            if (StringUtils.isEmpty(token)) {
                log.debug("参数不能为空，获取服务器地址失败 token = {}", token);
                return fail(Code.SERVER_URL_TOKEN_EMPTY);
            }
            if (playerId < 0) {
                log.debug("参数不能为空，获取服务器地址失败 playerId = {}", playerId);
                return fail(Code.PLAYER_ID_NOT_EXIST);
            }
            //检查是否在黑名单中
            String clientIp = getClientIp(request);
            if (StringUtils.isNotEmpty(clientIp)) {
                boolean blackIp = blackListService.isBlackIp(clientIp);
                if (blackIp) {
                    log.debug("该ip已被封禁，获取服务器地址失败 ip = {},dto = {}", clientIp, JSONObject.toJSONString(dto));
                    return fail(Code.IP_BLOCKED_SERVER_URL_UNAVAILABLE);
                }
            }

            //检查玩家id是否被封禁
            boolean blackId = blackListService.isBlackId(playerId);
            if (blackId) {
                log.debug("该playerId已被封禁，获取服务器地址失败 ip = {},dto = {}", clientIp, JSONObject.toJSONString(dto));
                return fail(Code.PLAYER_ID_BLOCKED_SERVER_URL_UNAVAILABLE);
            }

            //从数据库查询PlayerSessionToken对象信息
            PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(playerId);
            if (playerSessionToken == null) {
                log.debug("没有从redis中找到playerSessionToken对象,获取服务器地址失败, playerId = {}", playerId);
                return fail(Code.ERROR_REQ);
            }

            //校验token
            if (!playerSessionToken.getToken().equals(token)) {
                log.debug("token校验失败,获取服务器地址失败, playerId = {},dbToken = {},reqToken = {}", playerId, playerSessionToken.getToken(), token);
                return fail(Code.TOKEN_EXPIRED_SERVER_URL_UNAVAILABLE);
            }

            //检查该登录类型是否被关闭
            if (!loginConfigService.isOpen(playerSessionToken.getLoginType())) {
                log.debug("该登录类型被后台关闭，获取服务器地址失败 dto = {}", JSONObject.toJSONString(dto));
                return fail(Code.LOGIN_TYPE_NOT_ENABLED);
            }

            //如果与缓存数据不一致，就更新缓存
            checkDiffAndSave(clientIp, dto, playerSessionToken);

            //组装返回信息
            ServerUrlVo serverUrlVo = new ServerUrlVo();
            serverUrlVo.setGameServersUrls(accountConfig.getGameservers());

            //是否配置未充值的服务器地址
            if (accountConfig.getFlags() != null && !accountConfig.getFlags().isEmpty()) {
                List<String> poorList = accountConfig.getFlags().get("poor");
                if (poorList != null && !poorList.isEmpty()) {
                    //查询玩家充值金额
                    Long rechargeValue = countDao.getCountLong(CountDao.CountType.RECHARGE.getParam(), String.valueOf(playerId));
                    if (rechargeValue == null || rechargeValue < 1) {
                        serverUrlVo.setGameServersUrls(poorList);
                    }
                }
            }

            serverUrlVo.setResourceUrls(accountConfig.getResourceurls());
            log.info("获取服务器地址 playerId = {},token = {},gameServersUrls = {},resourceUrls = {}", playerId, token, serverUrlVo.getGameServersUrls(), accountConfig.getResourceurls());
            return success(serverUrlVo);
        } catch (Exception e) {
            log.error("", e);
            return exception();
        }
    }


    /**
     * 登录验证码
     *
     * @param dto
     * @return
     */
    @RequestMapping("loginsms")
    public WebResult loginsms(@RequestBody LoginSmsDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getPhone())) {
                log.debug("获取登录验证码失败,手机号不能为空 phone = {}", dto.getPhone());
                return fail(Code.PHONE_NUMBER_EMPTY);
            }

            String phone = dto.getPhone().trim();
            String realPhone = CoreUtil.validPhoneNumber(phone);
            if (StringUtils.isEmpty(realPhone)) {
                log.debug("获取登录验证码失败,手机号格式错误 phone = {}", dto.getPhone());
                return fail(Code.PHONE_NUMBER_FORMAT_INVALID);
            }
            int code = smsService.sendCode(phone, VerCodeType.SMS_LOGIN);
            if (code != Code.SUCCESS) {
                return fail(code);
            }
            return success();
        } catch (Exception e) {
            log.error("", e);
            return exception();
        }
    }


    /***********************************************************************************************************/
    /**
     * 游客登录
     *
     * @param dto
     * @return
     */
    private WebResult<LoginVo> guestLogin(LoginDto dto, String ip) {
        //登录逻辑
        LoginResult<Account> accountResult = accountService.login(LoginType.GUEST, new GuestUserInfo(dto.getData()), dto, ip);
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.GUEST, accountResult.data, accountResult.isRegister(), dto, ip);

        log.info("游客获取token成功 guest = {},playerId = {},token = {}", dto.getData(), accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * 谷歌登录
     *
     * @param dto
     * @return
     */
    private WebResult<LoginVo> googleLogin(LoginDto dto, String ip) {
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

        CommonResult<GoogleUserInfo> userInfoResult = thirdAccountHttpService.verifyGoogleToken(dto.getData());
        if (!userInfoResult.success()) {
            return fail(userInfoResult.code);
        }

        //登录逻辑
        LoginResult<Account> accountResult = accountService.login(LoginType.GOOGLE, userInfoResult.data, dto, ip);
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.GOOGLE, accountResult.data, accountResult.isRegister(), dto, ip);

        log.info("谷歌登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * apple登录
     *
     * @param dto
     * @return
     */
    private WebResult<LoginVo> appleLogin(LoginDto dto, String ip) {
        CommonResult<AppleUserInfo> userInfoResult = thirdAccountHttpService.verifyAppleToken(dto.getData());
        if (!userInfoResult.success()) {
            return fail(Code.BAN_CAUSE_BLACK_LIST);
        }

        //登录逻辑
        LoginResult<Account> accountResult = accountService.login(LoginType.APPLE, userInfoResult.data, dto, ip);
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.APPLE, accountResult.data, accountResult.isRegister(), dto, ip);

        log.info("apple登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * apple登录
     *
     * @param dto
     * @return
     */
    private WebResult<LoginVo> phoneLogin(LoginDto dto, String ip) {
        String[] arr = dto.getData().split(",");
        if (arr.length != 2) {
            log.debug("手机登录参数错误,应该包含手机号和验证码 dto = {}", JSONObject.toJSONString(dto));
            return fail(Code.VERIFICATION_CODE_ERROR);
        }

        String phone = arr[0].trim();
        if (StringUtils.isEmpty(phone)) {
            log.debug("手机登录失败,手机号不能为空 phone = {}", phone);
            return fail(Code.PHONE_NUMBER_EMPTY);
        }

        String realPhone = CoreUtil.validPhoneNumber(phone);
        if (StringUtils.isEmpty(realPhone)) {
            log.debug("手机登录失败,手机号格式错误 phone = {}", phone);
            return fail(Code.PHONE_NUMBER_FORMAT_INVALID);
        }

        int code = Integer.parseInt(arr[1].trim());
        CommonResult<PhoneUserInfo> userInfoResult = thirdAccountHttpService.verifyPhoneLoginCode(phone, code);
        if (!userInfoResult.success()) {
            return fail(Code.BAN_CAUSE_BLACK_LIST);
        }

        //登录逻辑
        LoginResult<Account> accountResult = accountService.login(LoginType.PHONE, userInfoResult.data, dto, ip);
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.PHONE, accountResult.data, accountResult.isRegister(), dto, ip);

        log.info("phone登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * facebook登录
     *
     * @param dto
     * @return
     */
    public WebResult<LoginVo> facebookLogin(LoginDto dto, String ip) {
        CommonResult<FacebookUserInfo> userInfoResult = thirdAccountHttpService.verifyFacebookToken(dto.getData());
        if (!userInfoResult.success()) {
            log.debug("token校验失败, 登录失败 dto= {},code = {}", JSONObject.toJSONString(dto), userInfoResult.code);
            return fail(Code.BAN_CAUSE_BLACK_LIST);
        }

        //登录逻辑
        LoginResult<Account> accountResult = accountService.login(LoginType.FACEBOOK, userInfoResult.data, dto, ip);
        if (!accountResult.success()) {
            return fail(accountResult.code);
        }

        //组装返回结果
        WebResult<LoginVo> loginVoWebResult = loginResult(LoginType.FACEBOOK, accountResult.data, accountResult.isRegister(), dto, ip);

        log.info("facebook 登录获取 token成功 playerId = {},token = {}", accountResult.data.getPlayerId(), loginVoWebResult.getData().getToken());
        return loginVoWebResult;
    }

    /**
     * 组装登录返回结果
     *
     * @param loginType
     * @param account
     * @return
     */
    private WebResult<LoginVo> loginResult(LoginType loginType, Account account, boolean register, LoginDto dto, String ip) {
        //生成token
        String token = RandomUtils.getUUid();

        //默认安卓设备
        DeviceType deviceType = DeviceType.valueOf(dto.getDevice());
        if (deviceType == null) {
            deviceType = DeviceType.ANDROID;
        }

        //默认google渠道
        ChannelType channelType = ChannelType.valueOf(dto.getChannel(), ChannelType.GOOGLE);

        //保存token，方便weboskcet连接时进行校验
        playerSessionTokenDao.save(token, loginType.getValue(), account.getPlayerId(), channelType.getValue(), ip, deviceType.getValue(),
                dto.getMac(), account.getChannel().getValue(), dto.getShareId(), dto.getSubChannel());

        LoginVo vo = new LoginVo();
        vo.setToken(token);
        vo.setPlayerId(account.getPlayerId());
        return success(vo);
    }

    /**
     * 如果与缓存数据不一致，就更新缓存
     */
    private void checkDiffAndSave(String clientIp, ServerUrlDto dto, PlayerSessionToken playerSessionToken) {
        boolean change = false;
        //对比ip
        if (StringUtils.isNotEmpty(clientIp) && !clientIp.equals(playerSessionToken.getIp())) {
            playerSessionToken.setIp(clientIp);
            change = true;
        }

        //对比mac
        if (StringUtils.isNotEmpty(dto.getMac()) && !dto.getMac().equals(playerSessionToken.getMac())) {
            playerSessionToken.setMac(dto.getMac());
            change = true;
        }

        //对比设备类型
        DeviceType deviceType = DeviceType.valueOf(dto.getDevice());
        if (deviceType == null) {
            deviceType = DeviceType.ANDROID;
        }
        if (deviceType.getValue() != playerSessionToken.getDevice()) {
            playerSessionToken.setDevice(deviceType.getValue());
            change = true;
        }

        //对比渠道类型
        ChannelType channelType = ChannelType.valueOf(dto.getChannel(), ChannelType.GOOGLE);
        if (channelType.getValue() != playerSessionToken.getChannel()) {
            playerSessionToken.setChannel(channelType.getValue());
            change = true;
        }

        if (change) {
            playerSessionTokenDao.save(playerSessionToken);
        }
    }
}
