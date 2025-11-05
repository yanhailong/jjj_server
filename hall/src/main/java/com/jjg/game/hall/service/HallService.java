package com.jjg.game.hall.service;

import cn.hutool.core.util.EnumUtil;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.PlayerSkinDao;
import com.jjg.game.core.dao.VerCodeDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.DropItemManager;
import com.jjg.game.core.service.*;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.dao.HallPoolDao;
import com.jjg.game.hall.dao.LikeGameDao;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.pb.res.NotifyGameList;
import com.jjg.game.hall.pb.struct.GameListConfig;
import com.jjg.game.hall.pb.struct.WarePoolInfo;
import com.jjg.game.hall.utils.HallTool;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/18 14:56
 */
@Component
public class HallService implements ConfigExcelChangeListener, TimerListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private GameStatusService gameStatusService;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private VerCodeDao verCodeDao;
    @Autowired
    private PlayerSkinDao playerSkinDao;
    @Autowired
    private HallPlayerService hallPlayerService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private LikeGameDao likeGameDao;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private TimerCenter timerCenter;
    @Autowired
    private HallPoolDao poolDao;
    @Autowired
    private SmsService smsService;
    @Autowired
    private ThirdAccountHttpService thirdAccountHttpService;
    @Autowired
    private DropItemManager dropItemManager;

    private Map<Integer, List<WareHouseConfigInfo>> wareHouseConfigMap = new HashMap<>();
    //游戏类型->游戏状态
    private Map<Integer, GameStatus> gameStatusesMap;
    //排序后的gameList
    private List<GameListConfig> sortGameList;
    //游戏倍场界面的奖池
    private Map<Integer, List<WarePoolInfo>> poolMap;
    @Autowired
    private MailService mailService;

    public Map<Integer, GameStatus> getGameStatusesMap() {
        return gameStatusesMap;
    }

    //默认头像id
    private int defaultHeadImgId = 0;
    //默认头像框id
    private int defaultHeadFrameId = 0;
    //默认国旗id
    private int defaultNationalId = 0;
    //默认称号id
    private int defaultTitleId = 0;
    //默认筹码id
    private int defaultChipsId = 0;
    //默认背景id
    private int defaultBackgroundId = 0;
    //默认牌背ID
    private int defaultCardBackgroundId = 0;


    private TimerEvent<String> updatePoolEvent;

    public void init() {
        //缓存倍场的配置信息
        initWareHouseConfigData();
        //缓存每个游戏的状态
        loadGameStatuses(gameStatusService.getAllGameStatus());

        //添加更新奖池的定时任务
        addUpdatePoolEvent();
    }

    /**
     * 添加更新奖池的定时任务
     */
    public void addUpdatePoolEvent() {
        this.updatePoolEvent = new TimerEvent<>(this, "updatePoolEvent", 10).withTimeUnit(TimeUnit.SECONDS);
        timerCenter.add(this.updatePoolEvent);
    }

    /**
     * 缓存每个游戏的状态
     *
     * @param gameStatuses
     */
    public void loadGameStatuses(List<GameStatus> gameStatuses) {
        if (gameStatuses == null || gameStatuses.isEmpty()) {
            this.gameStatusesMap = new HashMap<>();
        } else {
            this.gameStatusesMap = gameStatuses.stream().collect(Collectors.toMap(GameStatus::gameId, gs -> gs));
        }
        this.sortGameList = sortGameList();
    }

    /**
     * 刷新游戏状态
     */
    public void refreshGameStatuses() {
        loadGameStatuses(gameStatusService.getAllGameStatus());

        NotifyGameList notify = new NotifyGameList();
        notify.gameList = this.sortGameList;
        log.debug("推送游戏列表");
//        clusterSystem.sessionMap().entrySet().forEach(en -> en.getValue().send(notify));
        clusterSystem.broadcastToOnlinePlayer(notify);
    }

    public List<WareHouseConfigInfo> getWareHouseConfigByGameType(int gameType) {
        return wareHouseConfigMap.get(gameType);
    }

    /**
     * 根据游戏获取奖池
     *
     * @param gameType
     * @return
     */
    public List<WarePoolInfo> getPoolListByGameType(int gameType) {
        if (this.poolMap == null || this.poolMap.isEmpty()) {
            return null;
        }
        return this.poolMap.get(gameType);
    }

    /**
     * 判断是否能进入游戏
     *
     * @param gameType
     * @return
     */
    public boolean canJoinGame(int gameType) {
        GameStatus gameStatus = gameStatusesMap.get(gameType);
        if (Objects.nonNull(gameStatus)) {
            return gameStatus.open() == 1 && gameStatus.status() == 1;
        }
        return false;
    }

    /**
     * 绑定手机号
     *
     * @param playerId
     * @param data
     * @return
     */
    public CommonResult<Integer> bindPhone(long playerId, String data) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        if (StringUtils.isEmpty(data)) {
            result.code = Code.PARAM_ERROR;
            log.debug("参数为空,获取绑定手机验证码失败 playerId = {},phone = {}", playerId, data);
            return result;
        }

        if (!HallTool.validPhoneNumber(data)) {
            result.code = Code.PARAM_ERROR;
            log.debug("手机号格式错误,获取绑定手机验证码失败 playerId = {},phone = {}", playerId, data);
            return result;
        }

        int now = TimeHelper.nowInt();
        result = verCodeDao.verCodeIdleTime(playerId, VerCodeType.SMS_BIND_PHONE);
        if (now <= result.data) {
            log.debug("操作频繁，请稍后再试获取绑定手机验证码 playerId = {},data = {}", playerId, data);
            result.code = Code.REPEAT_OP;
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            result.code = Code.NOT_FOUND;
            log.debug("没有找到玩家账号信息，绑定手机号失败 playerId = {},phone = {}", playerId, data);
            return result;
        }

        if (data.equals(account.getThirdAccount(LoginType.PHONE))) {
            result.code = Code.REPEAT_OP;
            log.debug("玩家当前绑定手机号与新手机号一致，绑定手机号失败 playerId = {},oldPhone = {},newPhone = {}", playerId, account.getThirdAccount(LoginType.PHONE), data);
            return result;
        }

        CommonResult<Integer> sendCodeResult = smsService.sendCode(playerId, data, VerCodeType.SMS_BIND_PHONE);
        if (!sendCodeResult.success()) {
            log.debug("发送短信失败 playerId = {},phone = {},code = {}", playerId, data, sendCodeResult.code);
            result.code = sendCodeResult.code;
            return result;
        }
        result.data = sendCodeResult.data;
        return result;
    }

    /**
     * 绑定邮箱
     *
     * @param playerId
     * @param data
     * @return
     */
    public CommonResult<Integer> bindEmail(long playerId, String data) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        if (StringUtils.isEmpty(data)) {
            result.code = Code.PARAM_ERROR;
            log.debug("参数为空,获取绑定邮箱验证码失败 playerId = {},email = {}", playerId, data);
            return result;
        }

        if (!HallTool.checkEmail(data)) {
            result.code = Code.PARAM_ERROR;
            log.debug("邮箱格式不正确,获取绑定邮箱验证码失败 playerId = {},email = {}", playerId, data);
            return result;
        }

        int now = TimeHelper.nowInt();
        result = verCodeDao.verCodeIdleTime(playerId, VerCodeType.MAIL_BIND_MAIL);
        if (now <= result.data) {
            log.debug("操作频繁，请稍后再试获取绑定邮箱验证码 playerId = {},data = {}", playerId, data);
            result.code = Code.REPEAT_OP;
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            result.code = Code.NOT_FOUND;
            log.debug("没有找到玩家账号信息，获取绑定邮箱验证码失败 playerId = {},email = {}", playerId, data);
            return result;
        }

        if (data.equals(account.getEmail())) {
            result.code = Code.REPEAT_OP;
            log.debug("玩家当前绑定邮箱与新邮箱一致，获取绑定邮箱验证码失败 playerId = {},oldEmail = {},newEmail = {}", playerId, account.getEmail(), data);
            return result;
        }

        int verCode = RandomUtils.randomNum(HallConstant.VerCode.CODE_MIN, HallConstant.VerCode.CODE_MAX);
        verCodeDao.addVerCode(playerId, VerCodeType.MAIL_BIND_MAIL, data, verCode);
        result.data = verCode;
        return result;
    }

    /**
     * 确认验证码
     *
     * @param playerId
     * @param verCodeType
     * @param verCode
     * @return
     */
    public CommonResult<String> comfirmVerCode(long playerId, int verCodeType, int verCode) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);

        VerCodeType smsType = VerCodeType.getType(verCodeType);
        if (smsType == null) {
            result.code = Code.PARAM_ERROR;
            log.debug("验证码类型错误，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId, verCodeType, verCode);
            return result;
        }

        if (smsType != VerCodeType.MAIL_BIND_MAIL && smsType != VerCodeType.SMS_BIND_PHONE) {
            result.code = Code.PARAM_ERROR;
            log.debug("验证码类型错误，确认验证码失败2 playerId = {},verCodeType = {},verCode = {}", playerId, verCodeType, verCode);
            return result;
        }

        if (verCode < HallConstant.VerCode.CODE_MIN || verCode > HallConstant.VerCode.CODE_MAX) {
            result.code = Code.PARAM_ERROR;
            log.debug("验证码不在范围内，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId, verCodeType, verCode);
            return result;
        }

        CommonResult<String> verResult = verCodeDao.verifyVerCode(playerId, smsType, verCode);
        if (!verResult.success()) {
            result.code = verResult.code;
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerId);
        if (account == null) {
            result.code = Code.NOT_FOUND;
            log.debug("没有找到玩家账号信息，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId, verCodeType, verCode);
            return result;
        }

        boolean update = false;
        if (smsType == VerCodeType.SMS_BIND_PHONE) {
            CommonResult<Account> accountCommonResult = accountDao.addThirdAccount(playerId, LoginType.PHONE, verResult.data);
            if (!accountCommonResult.success()) {
                result.code = accountCommonResult.code;
                log.debug("更新到数据库失败，确认验证码失败1 playerId = {},verCodeType = {},verCode = {},failCode = {}", playerId, verCodeType, verCode, accountCommonResult.code);
                return result;
            }
            update = true;
        } else if (smsType == VerCodeType.MAIL_BIND_MAIL) {
            update = accountDao.updateEmail(playerId, verResult.data);
        }

        if (!update) {
            result.code = Code.FAIL;
            log.debug("更新到数据库失败，确认验证码失败 playerId = {},verCodeType = {},verCode = {}", playerId, verCodeType, verCode);
            return result;
        }

        //删除验证码
        verCodeDao.delVerCode(playerId, smsType);
        result.data = verResult.data;

        //绑定手机号要发送奖励邮件
        if (smsType == VerCodeType.SMS_BIND_PHONE) {
            LoginConfigCfg loginConfigCfg = GameDataManager.getLoginConfigCfgList().stream().filter(cfg -> cfg.getType() == LoginType.PHONE.getValue()).findFirst().orElse(null);
            if (loginConfigCfg == null || loginConfigCfg.getAwardItem() == null || loginConfigCfg.getAwardItem().isEmpty()) {
                log.debug("未找到绑定手机号的奖励 playerId = {}, type = {}", playerId, LoginType.PHONE.getValue());
                return result;
            }

            List<Item> list = HallTool.mapToItemList(loginConfigCfg.getAwardItem());
            mailService.addCfgMail(playerId, HallConstant.Mail.ID_BIND_PHONE, list);
            log.debug("已发送绑定手机奖励邮件 playerId = {},rewaredList = {}", playerId, list);
        }
        return result;
    }

    /**
     * 用户修改信息
     *
     * @param nick
     * @param gender
     * @return
     */
    public CommonResult<Player> changePlayerInfo(PlayerController playerController, String nick, byte gender) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        try {
            boolean[] change = new boolean[2];
            //TODO 后面要敏感词检测，还要判断是否消费道具
            if (StringUtils.isNotEmpty(nick) && !nick.equals(playerController.getPlayer().getNickName())) {  //修改昵称
                //检查新的昵称是否存在
                boolean exist = hallPlayerService.nickExist(nick);
                if (exist) {
                    result.code = Code.EXIST;
                    log.debug("该昵称已经存在，修改昵称失败 playerId = {},newNick = {}", playerController.getPlayer().getId(), nick);
                    return result;
                }
                change[0] = true;
            }

            if (gender != playerController.getPlayer().getGender()) {  //修改性别
                if (!HallTool.checkGender(gender)) {
                    result.code = Code.PARAM_ERROR;
                    log.debug("性别参数错误，修改性别失败 playerId = {},newGender = {}", playerController.getPlayer().getId(), gender);
                    return result;
                }
                change[1] = true;
            }

            if (!change[0] && !change[1]) {
                result.code = Code.SUCCESS;
                log.debug("玩家请求修改的昵称和性别与原本一致，无需修改 playerId = {},nick = {},gender = {}", playerController.getPlayer().getId(), nick, gender);
                return result;
            }

            Player player = hallPlayerService.doSave(playerController.playerId(), p -> {
                if (change[0]) {
                    p.setNickName(nick);
                }
                if (change[1]) {
                    p.setGender(gender);
                }
            });

            if (player == null) {
                result.code = Code.NOT_FOUND;
                log.debug("修改信息失败 playerId = {}", playerController.playerId());
                return result;
            }

            playerController.setPlayer(player);
            result.data = player;

            if (change[0]) {
                hallPlayerService.savePlayerNick(playerController.playerId(), nick);
            }
        } catch (Exception e) {
            log.error("", e);
        }
        return result;
    }

    /**
     * 获取所有头像信息
     *
     * @param playerId
     * @return
     */
    public PlayerSkin allAvatar(long playerId) {
        return playerSkinDao.getPlayerSkin(playerId);
    }

    /**
     * 切换头像
     *
     * @param player 玩家信息
     * @param id     皮肤id
     * @return 选择结果
     */
    public CommonResult<Player> selectAvatar(Player player, int id) {
        CommonResult<Player> result = new CommonResult<>(Code.SUCCESS);
        long playerId = player.getId();
        AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
        if (avatarCfg == null) {
            log.debug("未在头像配置表中找到该配置 id = {}", id);
            result.code = Code.NOT_FOUND;
            return result;
        }
        AvatarType type = EnumUtil.getBy(AvatarType.class, (avatarType -> avatarType.getType() == avatarCfg.getResourceType()));
        if (Objects.isNull(type)) {
            result.code = Code.PARAM_ERROR;
            return result;
        }
        Integer nowId = type.getGetter().apply(player);
        if (nowId == id) {
            result.code = Code.ALREADY_WORN;
            return result;
        }
        boolean has;
        if (StringUtils.isEmpty(type.getField())) {
            has = true;
        } else {
            has = playerSkinDao.hasByType(playerId, type, id);
        }
        //检查玩家是否拥有该id
        if (!has) {
            log.debug("玩家没有该头像id = {},type = {}", id, avatarCfg.getResourceType());
            result.code = Code.NOT_UNLOCKED;
            return result;
        }
        // 或者根据需求抛出异常
        result.data = hallPlayerService.doSave(playerId, id, type.getConsumer());
        log.info("选择头像资源成功 playerId = {},id = {},type = {}", playerId, id, avatarCfg.getResourceType());
        return result;
    }

    /**
     * 添加头像等信息
     *
     * @param playerId
     * @param id
     */
    public void addPlayerAvatar(long playerId, int id) {
        AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
        if (avatarCfg == null) {
            log.debug("未在头像配置表中找到该配置 id = {}", id);
            return;
        }
        AvatarType type = EnumUtil.getBy(AvatarType.class, (avatarType -> avatarType.getType() == avatarCfg.getResourceType()));
        if (Objects.isNull(type)) {
            log.debug("添加头像信息失败该类型 playerId = {},cfgId = {}", playerId, avatarCfg.getId());
            return;
        }
        boolean add = playerSkinDao.addByType(playerId, type, id);
        if (!add) {
            log.debug("添加头像信息失败 playerId = {},cfgId = {}", playerId, avatarCfg.getId());
            return;
        }
        log.info("添加头像信息成功 playerId = {},cfgId = {}", playerId, avatarCfg.getId());
    }

    /**
     * 保存默认的头像信息
     *
     * @param playerId
     */
    public void saveDefaultAvatar(long playerId) {
        try {
            PlayerSkin playerSkin = new PlayerSkin();
            playerSkin.setPlayerId(playerId);
            playerSkin.addAvatar(this.defaultHeadImgId);
            playerSkin.addFrame(this.defaultHeadFrameId);
            playerSkin.addTitle(this.defaultTitleId);
            playerSkin.addChip(this.defaultChipsId);
            playerSkin.addBackground(this.defaultBackgroundId);
            playerSkin.addCardBackground(this.defaultCardBackgroundId);
            this.playerSkinDao.save(playerSkin);
            log.info("保存默认的头像信息成功  playerId = {}", playerId);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 获取玩家背包
     *
     * @param playerId
     * @return
     */
    public PlayerPack getPlayerPack(long playerId) {
        return this.playerPackService.getFromAllDB(playerId);
    }

    /**
     * 使用道具
     *
     * @param player
     * @param itemId
     */
    public CommonResult<Map<Integer, Long>> useItem(Player player, int girdId, int itemId, long useItemCount) {
        CommonResult<Map<Integer, Long>> result = new CommonResult<>(Code.SUCCESS);
        try {
            log.debug("玩家使用道具 playerId = {},girdId = {},itemId = {}", player.getId(), girdId, itemId);
            ItemCfg itemCfg = GameDataManager.getItemCfg(itemId);
            if (itemCfg == null) {
                result.code = Code.NOT_FOUND;
                log.debug("未找到该道具配置，使用道具失败 playerId = {},itemId = {}", player.getId(), itemId);
                return result;
            }

            //检查道具类型
            if (itemCfg.getType() != GameConstant.Item.TYPE_CAN_USE) {
                result.code = Code.FORBID;
                log.debug("改道具不可被使用，使用道具失败 playerId = {},itemId = {}", player.getId(), itemId);
                return result;
            }

            Map<Integer, Long> addItemsMap = new HashMap<>();
            //是否有获取的道具
            if (itemCfg.getGetItem() != null && !itemCfg.getGetItem().isEmpty()) {
                for (Map.Entry<Integer, Long> en : itemCfg.getGetItem().entrySet()) {
                    int addItemId = en.getKey();
                    ItemCfg addItemCfg = GameDataManager.getItemCfg(addItemId);
                    if (addItemCfg == null) {
                        log.debug("未找到获得新道具的配置 playerId = {},itemId = {}", player.getId(), addItemId);
                        continue;
                    }
                    addItemsMap.merge(addItemId, en.getValue(), Long::sum);
                }

                CommonResult<ItemOperationResult> useResult = playerPackService.useItem(player.getId(), girdId, itemId, useItemCount, addItemsMap, AddType.USE_ITEM);
                if (!useResult.success()) {
                    log.debug("使用道具后获得新道具失败 playerId = {},itemId = {}", player.getId(), itemId);
                    result.code = useResult.code;
                    return result;
                }
            }

            //是否有掉落的道具
            if (itemCfg.getDropId() > 0) {
                Map<Integer, Long> useItem = dropItemManager.triggerDropItem(player, AddType.USE_ITEM, player.getId() + "", itemCfg.getDropId());
                addItemsMap.putAll(useItem);
            }

            if (addItemsMap.isEmpty()) {
                log.debug("使用道具失败 playerId = {},itemId = {}", player.getId(), itemId);
                result.code = Code.FORBID;
                return result;
            }

            result.data = addItemsMap;
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 添加收藏游戏
     *
     * @param playerId
     * @param gameTypes
     * @return
     */
    public List<Integer> addLikeGame(long playerId, List<Integer> gameTypes) {
        TreeSet<Integer> set = likeGameDao.addLikeGame(playerId, gameTypes);
        if (set == null) {
            return null;
        }
        return new ArrayList<>(set);
    }

    /**
     * 添加收藏游戏
     *
     * @param playerId
     * @param gameTypes
     * @return
     */
    public List<Integer> cancelLikeGames(long playerId, List<Integer> gameTypes) {
        TreeSet<Integer> set = likeGameDao.calcelLikeGame(playerId, gameTypes);
        if (set == null) {
            return null;
        }
        return new ArrayList<>(set);
    }

    /**
     * 绑定第三方账号
     *
     * @param type
     * @param token
     */
    public CommonResult<List<Item>> bindThirdAccount(long playerId, int type, String token) {
        CommonResult<List<Item>> result = new CommonResult<>(Code.SUCCESS);
        try {
            LoginType loginType = LoginType.valueOf(type);
            if (loginType == null) {
                log.debug("类型错误，绑定第三方账号失败 type = {}", type);
                result.code = Code.FAIL;
                return result;
            }

            if (StringUtils.isEmpty(token)) {
                log.debug("token不能为空，绑定第三方账号失败 type = {}", type);
                result.code = Code.FAIL;
                return result;
            }

            CommonResult<Account> addResult;
            int mailId = 0;
            if (loginType == LoginType.GOOGLE) {
                CommonResult<GoogleUserInfo> verifyResult = thirdAccountHttpService.verifyGoogleToken(token);
                if (!verifyResult.success()) {
                    result.code = verifyResult.code;
                    return result;
                }
                addResult = accountDao.addThirdAccount(playerId, loginType, verifyResult.data);

                mailId = HallConstant.Mail.ID_BIND_GOOGLE;
            } else if (loginType == LoginType.FACEBOOK) {
                CommonResult<FacebookUserInfo> verifyResult = thirdAccountHttpService.verifyFacebookToken(token);
                if (!verifyResult.success()) {
                    result.code = verifyResult.code;
                    return result;
                }

                addResult = accountDao.addThirdAccount(playerId, loginType, verifyResult.data);
                mailId = HallConstant.Mail.ID_BIND_FACEBOOK;
            } else if (loginType == LoginType.APPLE) {
                CommonResult<AppleUserInfo> verifyResult = thirdAccountHttpService.verifyAppleToken(token);
                if (!verifyResult.success()) {
                    result.code = verifyResult.code;
                    return result;
                }
                addResult = accountDao.addThirdAccount(playerId, loginType, verifyResult.data);
                mailId = HallConstant.Mail.ID_BIND_APPLE;
            } else {
                log.debug("该接口不支持该类型绑定，绑定第三方账号失败 type = {}", type);
                result.code = Code.FAIL;
                return result;
            }

            if (!addResult.success()) {
                result.code = addResult.code;
                return result;
            }

            LoginConfigCfg loginConfigCfg = GameDataManager.getLoginConfigCfgList().stream().filter(cfg -> cfg.getType() == type).findFirst().orElse(null);
            if (loginConfigCfg == null || loginConfigCfg.getAwardItem() == null || loginConfigCfg.getAwardItem().isEmpty()) {
                log.debug("未找到绑定的奖励 type = {}", type);
                return result;
            }

//            CommonResult<ItemOperationResult> bindRewardResult = playerPackService.addItems(playerId, loginConfigCfg.getAwardItem(), AddType.BIND_REWARD);
//            if (!bindRewardResult.success()) {
//                log.debug("添加绑定奖励失败 playerId = {},type = {}", playerId, type);
//                return result;
//            }

            result.data = HallTool.mapToItemList(loginConfigCfg.getAwardItem());

            mailService.addCfgMail(playerId, mailId, result.data);
            log.debug("已发送绑定账号奖励邮件 playerId = {},type = {},rewaredList = {}", playerId, type, result.data);
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 购买头像
     */
    public CommonResult<Integer> buyAvatar(long playerId, int id) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        try {
            AvatarCfg avatarCfg = GameDataManager.getAvatarCfg(id);
            if (avatarCfg == null) {
                log.debug("未找到该配置 id = {}", id);
                result.code = Code.NOT_FOUND;
                return result;
            }

            if (avatarCfg.getBuyItem() == null || avatarCfg.getBuyItem().size() < 2) {
                log.debug("该配置的buyItem配置错误 id = {}", id);
                result.code = Code.SAMPLE_ERROR;
                return result;
            }

            AvatarType avatarType = EnumUtil.getBy(AvatarType.class, (at -> at.getType() == avatarCfg.getResourceType()));
            if (Objects.isNull(avatarType)) {
                log.debug("该配置的buyItem配置错误1 id = {},type = {}", id, avatarCfg.getResourceType());
                result.code = Code.PARAM_ERROR;
                return result;
            }

            int moneyId = avatarCfg.getBuyItem().get(0);
            ItemCfg itemCfg = GameDataManager.getItemCfg(moneyId);
            if (itemCfg == null) {
                log.debug("该配置的buyItem配置错误，未找到对应的item配置 id = {},itemCfgId = {}", id, moneyId);
                result.code = Code.SAMPLE_ERROR;
                return result;
            }

            CommonResult<Player> deductResult;
            if (itemCfg.getType() == GameConstant.Item.TYPE_GOLD) {
                deductResult = hallPlayerService.deductGold(playerId, avatarCfg.getBuyItem().get(1), AddType.BUY_AVATAR, id + "");
            } else if (itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                deductResult = hallPlayerService.deductDiamond(playerId, avatarCfg.getBuyItem().get(1), AddType.BUY_AVATAR, id + "");
            } else {
                log.debug("该配置的buyItem配置错误，配置的itemIdc错误，id = {},itemCfgId = {}", id, moneyId);
                result.code = Code.SAMPLE_ERROR;
                return result;
            }
            if (!deductResult.success()) {
                result.code = deductResult.code;
                return result;
            }

            Map<AvatarType, List<Integer>> addIdsMap = new HashMap<>();
            addIdsMap.computeIfAbsent(avatarType, k -> new ArrayList<>()).add(id);

            //获取赠送的id
            if(avatarCfg.getBuyItem().size() >= 3){
                int giveId = avatarCfg.getBuyItem().get(2);
                AvatarCfg giveAvatarCfg = GameDataManager.getAvatarCfg(giveId);
                if (giveAvatarCfg != null) {
                    AvatarType giveAvatarType = EnumUtil.getBy(AvatarType.class, (at -> at.getType() == giveAvatarCfg.getResourceType()));
                    if (!Objects.isNull(giveAvatarType)) {
                        addIdsMap.computeIfAbsent(giveAvatarType, k -> new ArrayList<>()).add(giveAvatarCfg.getId());
                    } else {
                        giveId = 0;
                    }
                }

                //解锁头像
                boolean success = playerSkinDao.addByType(playerId, addIdsMap);
                if (!success) {
                    log.debug("添加头像信息失败 playerId = {},addIdsMap = {}", playerId, addIdsMap);
                    result.code = Code.FAIL;
                    return result;
                }
                result.data = giveId;
            }
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /***********************************************************************************************************/

    @Override
    public void initSampleCallbackCollector() {
        addInitSampleFileObserveWithCallBack(WarehouseCfg.EXCEL_NAME, this::initWareHouseConfigData).addChangeSampleFileObserveWithCallBack(WarehouseCfg.EXCEL_NAME, this::initWareHouseConfigData);
        addInitSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initGlobalConfig).addChangeSampleFileObserveWithCallBack(GlobalConfigCfg.EXCEL_NAME, this::initGlobalConfig);
    }

    /**
     * 缓存倍场配置
     */
    private void initWareHouseConfigData() {
        Map<Integer, List<WareHouseConfigInfo>> tempwareHouseConfigMap = new HashMap<>();

        for (WarehouseCfg c : GameDataManager.getWarehouseCfgList()) {
            List<WareHouseConfigInfo> tempList = tempwareHouseConfigMap.computeIfAbsent(c.getGameID(), k -> new ArrayList<>());
            if (c.getRoomType() < GameConstant.RoomTypeCons.FRIEND_ROOM_TYPE_START) {
                WareHouseConfigInfo info = new WareHouseConfigInfo();
                info.wareId = c.getId();
                info.limitGoldMin = c.getEnterLimit();
                info.limitPlayerLevelMin = c.getPlayerLvLimit();
                info.betShow = c.getBetShow();
                tempList.add(info);
            }
        }

        //根据场次id，从小到大排序
        tempwareHouseConfigMap.replaceAll((key, list) -> list.stream().sorted(Comparator.comparingInt(wh -> wh.wareId)).collect(Collectors.toList()));

        this.wareHouseConfigMap = tempwareHouseConfigMap;
    }

    /**
     * 缓存global表的配置
     */
    private void initGlobalConfig() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(GameConstant.GlobalConfig.DEFAULT_AVATAR_CFG_ID);
        String[] arr = globalConfigCfg.getValue().split(":");
        this.defaultHeadImgId = Integer.parseInt(arr[0]);
        this.defaultHeadFrameId = Integer.parseInt(arr[1]);
        this.defaultNationalId = Integer.parseInt(arr[2]);
        this.defaultTitleId = Integer.parseInt(arr[3]);
        this.defaultChipsId = Integer.parseInt(arr[4]);
        this.defaultCardBackgroundId = Integer.parseInt(arr[5]);
        this.defaultBackgroundId = Integer.parseInt(arr[6]);
    }

    public int getDefaultHeadImgId() {
        return defaultHeadImgId;
    }

    public int getDefaultHeadFrameId() {
        return defaultHeadFrameId;
    }

    public int getDefaultNationalId() {
        return defaultNationalId;
    }

    public int getDefaultTitleId() {
        return defaultTitleId;
    }

    public int getDefaultChipsId() {
        return defaultChipsId;
    }

    public int getDefaultBackgroundId() {
        return defaultBackgroundId;
    }

    public int getDefaultCardBackgroundId() {
        return defaultCardBackgroundId;
    }

    /**
     * 游戏列表配置
     */
    public List<GameListConfig> sortGameList() {
        try {
            if (this.gameStatusesMap == null || this.gameStatusesMap.isEmpty()) {
                return null;
            }

            return this.gameStatusesMap.values().stream()
                    //1.先过滤选出上架的游戏
                    .filter(gs -> gs.status() == 1)

                    // 2.排序：按 sort 升序，再按 gameId 升序
                    .sorted(Comparator.comparingInt(GameStatus::sort).thenComparingInt(GameStatus::gameId))

                    // 3.转换 GameStatus → GameListConfig
                    .map(gameStatus -> {
                        GameListConfig config = new GameListConfig();
                        config.sid = gameStatus.gameId();
                        config.status = gameStatus.open();
                        config.iconType = gameStatus.icon_category();
                        config.rightTopIcon = gameStatus.right_top_icon();
                        config.name = gameStatus.name();
                        return config;
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("", e);
        }
        return Collections.emptyList();
    }

    public List<GameListConfig> getSortGameList() {
        return sortGameList;
    }

    @Override
    public void onTimer(TimerEvent e) {
        if (this.updatePoolEvent == e) {
            updatePoolEvent();
        }
    }

    /**
     * 更新奖池
     */
    private void updatePoolEvent() {
        if (this.sortGameList == null || this.sortGameList.isEmpty()) {
            return;
        }
        Map<Integer, List<WarePoolInfo>> tmpPoolMap = new HashMap<>();

        this.sortGameList.forEach(cfg -> {
            if (CommonUtil.getMajorTypeByGameType(cfg.sid) == CoreConst.GameMajorType.SLOTS) {
                Map<Object, Object> smallPool = poolDao.getSmallPoolByRoomCfgId(cfg.sid);
                Map<Object, Object> fakeSmallPool = poolDao.getFakeSmallPoolByRoomCfgId(cfg.sid);

                List<WarePoolInfo> warePoolInfoList = new ArrayList<>();
                for (Map.Entry<Object, Object> en : smallPool.entrySet()) {
                    WarePoolInfo warePoolInfo = new WarePoolInfo();
                    warePoolInfo.wareId = Integer.parseInt(en.getKey().toString());
                    long smallPoolValue = Long.parseLong(en.getValue().toString());

                    Object o = fakeSmallPool.get(warePoolInfo.wareId);
                    if (o != null) {
                        long fakeSmallPoolValue = Long.parseLong(o.toString());
                        if (fakeSmallPoolValue > smallPoolValue) {
                            smallPoolValue = fakeSmallPoolValue;
                        }
                    }

                    warePoolInfo.pool = smallPoolValue;
                    warePoolInfoList.add(warePoolInfo);
                }

                tmpPoolMap.put(cfg.sid, warePoolInfoList);
            }
        });

        this.poolMap = tmpPoolMap;
    }
}
