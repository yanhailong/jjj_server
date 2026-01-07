package com.jjg.game.hall.handler;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.EnumUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.constant.EFunctionType;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.MarsNode;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.CountDao;
import com.jjg.game.core.dao.PlayerSkinDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.ChooseWareListener;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.pb.ReqChooseWare;
import com.jjg.game.core.pb.ResChooseWare;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.GameFunctionService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.casino.manager.CasinoManager;
import com.jjg.game.hall.casino.pb.req.*;
import com.jjg.game.hall.constant.HallCode;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.pb.req.*;
import com.jjg.game.hall.pb.res.*;
import com.jjg.game.hall.pb.struct.MailInfo;
import com.jjg.game.hall.pb.struct.NoticeInfo;
import com.jjg.game.hall.pb.struct.PackItemInfo;
import com.jjg.game.hall.room.HallRoomService;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.hall.service.HallService;
import com.jjg.game.hall.service.NoticeService;
import com.jjg.game.hall.utils.HallTool;
import com.jjg.game.hall.vip.VipManager;
import com.jjg.game.hall.vip.data.VipCfgCache;
import com.jjg.game.hall.vip.pb.req.ReqVipClaimGiftReward;
import com.jjg.game.hall.vip.pb.req.ReqVipInfo;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.AvatarCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.WarehouseCfg;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/6/10 17:13
 */
@Component
@MessageType(MessageConst.MessageTypeDef.HALL_TYPE)
public class HallMessageHandler implements GmListener, ChooseWareListener {
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private HallService hallService;
    @Autowired
    private HallRoomService hallRoomService;
    @Autowired
    private CasinoManager casinoManager;
    @Autowired
    private HallPlayerService hallPlayerService;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private MailService mailService;
    @Autowired
    private VipManager vipManager;
    @Autowired
    private GameFunctionService gameFunctionService;
    @Autowired
    private CorePlayerService corePlayerService;
    @Autowired
    private PlayerSkinDao playerSkinDao;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private CountDao countDao;
    @Autowired
    private PlayerPackService playerPackService;

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
                playerController.send(res);
                log.debug("游戏类型错误，选择游戏失败 playerId = {},gameType = {}", playerController.playerId(), req.gameType);
                return;
            }
            //如果游戏状态下架或者已经关闭禁止进入
            if (!hallService.canJoinGame(req.gameType)) {
                res.code = Code.FORBID;
                playerController.send(res);
                log.debug("游戏已关闭，选择游戏失败 playerId = {},gameType = {}", playerController.playerId(), req.gameType);
                return;
            }
            List<WareHouseConfigInfo> wareHouseConfigList = hallService.getWareHouseConfigByGameType(req.gameType);
            if (wareHouseConfigList == null || wareHouseConfigList.isEmpty()) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.debug("未找到对应的游戏场次配置，选择游戏失败 playerId = {},gameType = {}", playerController.playerId(), req.gameType);
                return;
            }

            res.wareHouseList = wareHouseConfigList;
            log.info("玩家选择游戏，playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 获取奖池信息
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_POOL)
    public void reqPool(PlayerController playerController, ReqPool req) {
        ResPool res = new ResPool(HallCode.SUCCESS);
        try {
            res.warePoolInfoList = hallService.getPoolListByGameType(req.gameType);
            log.info("玩家获取奖池信息，playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 获取玩家信息
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_QUERY_PLAYER_INFO)
    public void reqQueryPlayerInfo(PlayerController playerController, ReqQueryPlayerInfo req) {
        ResQueryPlayerInfo res = new ResQueryPlayerInfo(HallCode.SUCCESS);
        try {
            Player player = hallPlayerService.get(playerController.playerId());
            if (player == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.debug("玩家信息不存在，获取信息失败 playerId = {}", playerController.playerId());
                return;
            }

            res.playerId = player.getId();
            res.nick = player.getNickName();
            res.createTime = player.getCreateTime();
            res.gender = player.getGender();
            res.vipLevel = player.getVipLevel();
            res.gold = player.getGold();
            res.diamond = player.getDiamond();
            res.safeBoxGold = player.getSafeBoxGold();
            res.safeBoxDiamond = player.getSafeBoxDiamond();
            res.headImgId = player.getHeadImgId();
            res.headFrameId = player.getHeadFrameId();
            res.nationalId = player.getNationalId();
            res.titleId = player.getTitleId();
            res.level = player.getLevel();
            res.exp = player.getExp();

            Account account = accountDao.queryAccountByPlayerId(playerController.playerId());
            if (account == null) {
                log.warn("没有找到玩家的账号信息 playerId = {}", playerController.playerId());
            } else {
                res.phoneNumber = account.getThirdAccount(LoginType.PHONE);
                res.email = account.getEmail();

                Map<LoginType, String> thirdAccountsMap = account.getThirdAccounts();
                if (thirdAccountsMap != null && !thirdAccountsMap.isEmpty()) {
                    res.bindThirdAccountList = new ArrayList<>();
                    for (Map.Entry<LoginType, String> en : thirdAccountsMap.entrySet()) {
                        res.bindThirdAccountList.add(en.getKey().getValue());
                    }
                }
            }
            playerController.setPlayer(player);
            log.info("获取玩家信息，playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 修改玩家信息
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_CHANGE_PLAYER_INFO)
    public void reqChangePlayerInfo(PlayerController playerController, ReqChangePlayerInfo req) {
        ResChangePlayerInfo res = new ResChangePlayerInfo(HallCode.SUCCESS);
        try {
            byte gender = (byte) req.gender;
            if (!HallTool.checkGender(gender)) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.debug("性别参数错误，修改信息失败 playerId = {},gender = {}", playerController.playerId(), req.gender);
                return;
            }

            CommonResult<Player> result = hallService.changePlayerInfo(playerController, req.nick, (byte) req.gender);
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }
            log.info("修改玩家信息成功，playerId = {}", playerController.playerId());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 请求验证码
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_VER_CODE)
    public void reqVerCode(PlayerController playerController, ReqVerCode req) {
        Thread.ofVirtual().start(() -> {
            ResVerCode res = new ResVerCode(HallCode.SUCCESS);
            try {
                CommonResult<Integer> result = null;
                if (req.type == VerCodeType.SMS_BIND_PHONE.getValue()) {
                    result = hallService.bindPhone(playerController.playerId(), req.data);
                } else if (req.type == VerCodeType.MAIL_BIND_MAIL.getValue()) {
                    result = hallService.bindEmail(playerController.playerId(), req.data);
                } else {
                    log.debug("没有该类型的验证码 playerId = {},verCodeType = {},data = {}", playerController.playerId(), req.type, req.data);
                    result = new CommonResult<>(Code.PARAM_ERROR);
                }

                if (!result.success()) {
                    res.code = result.code;
                    playerController.send(res);
                    return;
                }
                log.info("获取验证码成功，playerId = {},verCodeType = {},data = {},verCode = {}", playerController.playerId(), req.type, req.data, result.data);
            } catch (Exception e) {
                log.error("", e);
                res.code = Code.EXCEPTION;
            }
            playerController.send(res);
        });
    }

    /**
     * 确认验证码
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_CONFIRM_VER_CODE)
    public void reqConfirmVerCode(PlayerController playerController, ReqConfirmVerCode req) {
        ResConfirmVerCode res = new ResConfirmVerCode(HallCode.SUCCESS);
        try {
            log.debug("确认验证码 req = {}", JSON.toJSONString(req));
            CommonResult<String> result = hallService.comfirmVerCode(playerController.getPlayer(), req.verCodeType, req.verCode);
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }
            log.info("确认验证码成功 playerId = {},verCodeType = {},verCode = {},data = {}", playerController.playerId(), req.verCodeType, req.verCode, result.data);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 获取所有的头像信息
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_ALL_AVATAR)
    public void reqAllAvatar(PlayerController playerController, ReqAllAvatar req) {
        Player player = playerController.getPlayer();
        ResAllAvatar res = new ResAllAvatar(HallCode.SUCCESS);
        try {
            PlayerSkin playerSkin = hallService.allAvatar(playerController.playerId());
            if (playerSkin == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.debug("未找到该玩家的头像信息 playerId = {}", playerController.playerId());
                return;
            }

            unlockSkin(playerSkin, playerController.getPlayer().getVipLevel(), playerController.getPlayer().getLevel());

            if (CollectionUtil.isNotEmpty(playerSkin.getUnlockAvatarSet())) {
                res.avatars = new ArrayList<>(playerSkin.getUnlockAvatarSet());
            }
            if (CollectionUtil.isNotEmpty(playerSkin.getUnlockFrameSet())) {
                res.frames = new ArrayList<>(playerSkin.getUnlockFrameSet());
            }
            if (CollectionUtil.isNotEmpty(playerSkin.getUnlockTitleSet())) {
                res.titles = new ArrayList<>(playerSkin.getUnlockTitleSet());
            }
            if (CollectionUtil.isNotEmpty(playerSkin.getUnlockChipsSet())) {
                res.unlockChipsId = new ArrayList<>(playerSkin.getUnlockChipsSet());
            }
            if (CollectionUtil.isNotEmpty(playerSkin.getUnlockBackgroundSet())) {
                res.unlockBackgroundId = new ArrayList<>(playerSkin.getUnlockBackgroundSet());
            }
            if (CollectionUtil.isNotEmpty(playerSkin.getUnlockCardBackgroundSet())) {
                res.unlockCardBackgroundId = new ArrayList<>(playerSkin.getUnlockCardBackgroundSet());
            }

            log.debug("玩家获取所有头像信息 playerId = {}", playerController.playerId());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 解锁皮肤
     *
     * @param playerSkin
     * @param vipLevel
     */
    private void unlockSkin(PlayerSkin playerSkin, int vipLevel, int playerLevel) {
        Map<AvatarType, Set<Integer>> addIdsMap = new HashMap<>();

        // 先收集所有需要解锁的皮肤ID，最后统一设置
        Map<AvatarType, Set<Integer>> finalUnlockMap = new HashMap<>();

        // 初始化最终解锁集合（复制当前已解锁的）
        for (AvatarType type : AvatarType.values()) {
            if (type == AvatarType.NATIONAL) continue;

            Set<Integer> current = type.getSkinGetter().apply(playerSkin);
            Set<Integer> finalSet = (current != null) ? new HashSet<>(current) : new HashSet<>();
            finalUnlockMap.put(type, finalSet);
        }

        // 等级解锁
        for (Map.Entry<Integer, AvatarCfg> en : GameDataManager.getAvatarCfgMap().entrySet()) {
            AvatarCfg cfg = en.getValue();
            if (cfg.getPlayerLv() < 1 || cfg.getPlayerLv() > playerLevel) {
                continue;
            }

            AvatarType type = EnumUtil.getBy(AvatarType.class, t -> t.getType() == cfg.getResourceType());
            if (type == null || type == AvatarType.NATIONAL) {
                continue;
            }

            Set<Integer> finalSet = finalUnlockMap.get(type);
            if (!finalSet.contains(cfg.getId())) {
                finalSet.add(cfg.getId());
                addIdsMap.computeIfAbsent(type, t -> new HashSet<>()).add(cfg.getId());
            }
        }

        // VIP解锁
        for (AvatarType type : AvatarType.values()) {
            if (type == AvatarType.NATIONAL) {
                continue;
            }

            List<Integer> skinIds = VipCfgCache.getSkinsByType(vipLevel, type.getType());
            if (CollectionUtil.isEmpty(skinIds)) {
                continue;
            }

            Set<Integer> finalSet = finalUnlockMap.get(type);
            for (Integer skinId : skinIds) {
                if (!finalSet.contains(skinId)) {
                    finalSet.add(skinId);
                    addIdsMap.computeIfAbsent(type, t -> new HashSet<>()).add(skinId);
                }
            }
        }

        // 统一设置最终结果
        for (Map.Entry<AvatarType, Set<Integer>> entry : finalUnlockMap.entrySet()) {
            AvatarType type = entry.getKey();
            type.getSkinConsumer().accept(playerSkin, entry.getValue());
        }

        if (!addIdsMap.isEmpty()) {
            playerSkinDao.addByType(playerSkin.getPlayerId(), addIdsMap);
            log.debug("玩家新解锁头像 playerId = {},addIdsMap = {}", playerSkin.getPlayerId(), addIdsMap);
        }
    }

    /**
     * 选择头像框
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_SELECT_AVATAR)
    public void reqSelectAvatar(PlayerController playerController, ReqSelectAvatar req) {
        ResSelectAvatar res = new ResSelectAvatar(HallCode.SUCCESS);
        try {
            CommonResult<Player> result = hallService.selectAvatar(playerController.getPlayer(), req.id);
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }
            playerController.setPlayer(result.data);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 获取背包
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_GET_PACK)
    public void reqGetPack(PlayerController playerController, ReqGetPack req) {
        ResGetPack res = new ResGetPack(HallCode.SUCCESS);
        try {
            res.packItemInfos = getPlayerPack(playerController.playerId());
            log.debug("返回玩家背包数据 playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 使用道具
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_USE_ITEM)
    public void reqUseItem(PlayerController playerController, ReqUseItem req) {
        ResUseItem res = new ResUseItem(HallCode.SUCCESS);
        if (req.useItemCount <= 0 || req.itemId <= 0) {
            res.code = Code.ERROR_REQ;
            playerController.send(res);
            return;
        }
        try {
            CommonResult<Map<Integer, Long>> useResult = hallService.useItem(playerController.getPlayer(), req.girdId, req.itemId, req.useItemCount);
            if (!useResult.success()) {
                res.code = useResult.code;
                playerController.send(res);
                return;
            }
            res.packItemInfos = getPlayerPack(playerController.playerId());
            if (useResult.data != null) {
                res.addItemInfos = ItemUtils.buildItemInfo(useResult.data);
            }
            log.debug("使用道具 playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }


    /**
     * 获取邮件
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_GET_MAILS)
    public void reqGetMails(PlayerController playerController, ReqGetMails req) {
        ResGetMails res = new ResGetMails(HallCode.SUCCESS);
        try {
            List<Mail> mailList = mailService.getMailByPlayerId(playerController.playerId(), req.page);
            if (mailList != null && !mailList.isEmpty()) {
                res.mails = new ArrayList<>(mailList.size());

                mailList.forEach(mail -> {
                    MailInfo info = new MailInfo();
                    info.id = mail.getId();

                    if (mail.getTitle() != null) {
                        info.title = mail.getTitle().toPbInfo();
                    }

                    if (mail.getContent() != null) {
                        info.content = mail.getContent().toPbInfo();
                    }
                    info.sendTime = mail.getSendTime();
                    info.timeout = mail.getTimeout();
                    info.status = mail.getStatus();
                    if (mail.getItems() != null && !mail.getItems().isEmpty()) {
                        info.items = new ArrayList<>(mail.getItems().size());
                        mail.getItems().forEach(mailItem -> {
                            ItemInfo infoItem = new ItemInfo();
                            infoItem.itemId = mailItem.getId();
                            infoItem.count = mailItem.getItemCount();
                            info.items.add(infoItem);
                        });
                    }
                    res.mails.add(info);
                });
            }
            if (req.page == 0) {
                mailService.updateRedDot(playerController.playerId());
            }
            log.debug("玩家获取邮件列表 playerId = {},page = {},size = {}", playerController.playerId(), req.page, res.mails == null ? 0 : res.mails.size());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 阅读邮件
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_READ_MAIL)
    public void reqReadMail(PlayerController playerController, ReqReadMail req) {
        ResReadMail res = new ResReadMail(HallCode.SUCCESS);
        try {
            if (req.id < 1) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误，阅读邮件失败 playerId = {},id = {}", playerController.playerId(), req.id);
                return;
            }
            mailService.readMail(playerController.playerId(), req.id);
            log.debug("玩家阅读邮件 playerId = {},id = {}", playerController.playerId(), req.id);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);

    }

    /**
     * 领取邮件内的道具
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_GET_MAIL_ITEMS)
    public void reqGetMailItems(PlayerController playerController, ReqGetMailItems req) {
        ResGetMailItems res = new ResGetMailItems(HallCode.SUCCESS);
        try {
            if (req.id < 1) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误，领取邮件内的道具失败 playerId = {},id = {}", playerController.playerId(), req.id);
                return;
            }
            CommonResult<Integer> result = mailService.getMailItems(playerController.playerId(), req.id, AddType.GET_MAIL_ITEMS);
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }
            log.info("领取邮件附件 playerId = {},mailId = {}", playerController.playerId(), req.id);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 删除邮件
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_REMOVE_MAIL)
    public void reqRemoveMail(PlayerController playerController, ReqRemoveMail req) {
        ResRemoveMail res = new ResRemoveMail(HallCode.SUCCESS);
        try {
            if (req.id < 1) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("参数错误，删除邮件失败 playerId = {},id = {}", playerController.playerId(), req.id);
                return;
            }
            mailService.removeMail(playerController.playerId(), req.id);
            log.debug("玩家删除邮件成功 playerId = {},id = {}", playerController.playerId(), req.id);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 删除已读邮件
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_REMOVE_READ_MAILS)
    public void reqRemoveReadMails(PlayerController playerController, ReqRemoveReadMails req) {
        ResRemoveReadMails res = new ResRemoveReadMails(HallCode.SUCCESS);
        try {
            long count = mailService.removeReadMails(playerController.playerId());
            log.debug("玩家删除已读邮件 playerId = {},count = {}", playerController.playerId(), count);
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 一键领取
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_GET_ALL_MAILS_ITEMS)
    public void reqGetAllMailsItems(PlayerController playerController, ReqGetAllMailsItems req) {
        ResGetAllMailsItems res = new ResGetAllMailsItems(HallCode.SUCCESS);
        try {
            CommonResult<Map<Integer, Long>> result = mailService.getAllMailsItems(playerController.playerId());
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }

            List<ItemInfo> items = new ArrayList<>();

            result.data.forEach((k, v) -> {
                ItemInfo item = new ItemInfo();
                item.itemId = k;
                item.count = v;
                items.add(item);
            });

            res.items = items;
            log.debug("玩家一键领取 playerId = {}", playerController.playerId());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 保险箱转移金币
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_TRANS_SAFE_BOX_GOLD)
    public void reqTransSafeBoxGold(PlayerController playerController, ReqTransSafeBoxGold req) {
        ResTransSafeBoxGold res = new ResTransSafeBoxGold(HallCode.SUCCESS);
        try {
            CommonResult<Player> result;
            if (req.deposit) {
                result = hallPlayerService.goldInSafeBox(playerController.playerId(), req.value, AddType.DEPOSIT_IN_SAFE_BOX);
            } else {
                result = hallPlayerService.goldOutFromSafeBox(playerController.playerId(), req.value, AddType.WITHDRAW_FROM_SAFE_BOX);
            }
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }

            res.gold = result.data.getGold();
            res.safeBoxGold = result.data.getSafeBoxGold();
            log.debug("玩家转移保险箱金币成功 playerId = {},deposit = {},gold = {},changeGold = {},safeBoxGold = {}", playerController.playerId(), req.deposit, result.data.getGold(), req.value, result.data.getSafeBoxGold());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 保险箱转移钻石
     *
     * @param playerController
     * @param req
     */
    @Command(HallConstant.MsgBean.REQ_TRANS_SAFE_BOX_DIAMOND)
    public void reqTransSafeBoxDiamond(PlayerController playerController, ReqTransSafeBoxDiamond req) {
        ResTransSafeBoxDiamond res = new ResTransSafeBoxDiamond(HallCode.SUCCESS);
        try {
            CommonResult<Player> result;
            if (req.deposit) {
                result = hallPlayerService.diamondInSafeBox(playerController.playerId(), req.value, AddType.DEPOSIT_IN_SAFE_BOX);
            } else {
                result = hallPlayerService.diamondOutFromSafeBox(playerController.playerId(), req.value, AddType.WITHDRAW_FROM_SAFE_BOX);
            }
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }

            res.diamond = result.data.getDiamond();
            res.safeBoxDiamond = result.data.getSafeBoxDiamond();
            log.debug("玩家转移保险箱钻石成功 playerId = {},deposit = {},diamond = {},changeDiamond = {},safeBoxDiamond = {}", playerController.playerId(), req.deposit, result.data.getGold(), req.value, result.data.getSafeBoxGold());
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**********************************************************************************************************/

    /**
     * 加入房间之前检查前置条件
     */
    private CommonResult<WareHouseConfigInfo> checkBeforeJoinRoom(PlayerController playerController, int gameType, int roomCfgId) {

        if (gameType < 1) {
            log.debug("游戏类型错误，选择场次失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return new CommonResult<>(Code.PARAM_ERROR);
        }

        //如果游戏状态下架或者已经关闭禁止进入
        if (!hallService.canJoinGame(gameType)) {
            log.debug("游戏已关闭，选择游戏失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return new CommonResult<>(Code.FORBID);
        }

        List<WareHouseConfigInfo> wareHouseConfigList = hallService.getWareHouseConfigByGameType(gameType);
        if (wareHouseConfigList == null || wareHouseConfigList.isEmpty()) {
            log.debug("未找到对应的游戏场次配置，选择场次失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        WareHouseConfigInfo info = wareHouseConfigList.stream().filter(c -> c.wareId == roomCfgId).findFirst().orElse(null);
        if (info == null) {
            log.debug("未找到对应的游戏场次配置2，选择场次失败 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), gameType, roomCfgId);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        //判断是否检查钻石余额
        boolean checkDiamond = false;
        WarehouseCfg warehouseCfg = GameDataManager.getWarehouseCfg(roomCfgId);
        if (warehouseCfg.getTransactionItemId() > 0) {
            ItemCfg itemCfg = GameDataManager.getItemCfg(warehouseCfg.getTransactionItemId());
            if (itemCfg != null && itemCfg.getType() == GameConstant.Item.TYPE_DIAMOND) {
                checkDiamond = true;
            }
        }

        Player player = hallPlayerService.get(playerController.getPlayer().getId());
        if (warehouseCfg.getEnterLimit() != -1 && (checkDiamond ? warehouseCfg.getEnterLimit() > player.getDiamond() : warehouseCfg.getEnterLimit() > player.getGold())) {
            log.debug("玩家携带货币不足 playerId = {},gameType = {},roomCfgId = {},transactionId = {},gold = {},diamond = {},enterLimit = {},checkDiamond = {}", playerController.playerId(), gameType, roomCfgId, warehouseCfg.getTransactionItemId(), player.getGold(), player.getDiamond(), warehouseCfg.getEnterLimit(), checkDiamond);
            return new CommonResult<>(Code.NOT_ENOUGH);
        }

        if (info.limitPlayerLevelMin > playerController.getPlayer().getLevel()) {
            log.debug("玩家等级不足 playerId = {},gameType = {},roomCfgId = {}", playerController.playerId(), gameType, roomCfgId);
            return new CommonResult<>(Code.LEVEL_NOT_ENOUGH);
        }

        MarsNode node = nodeManager.getGameNodeByWeight(gameType, playerController.playerId(), playerController.getPlayer().getIp());
        if (node == null) {
            log.debug("获取游戏节点为空，进入游戏失败 playerId = {},gameType = {}", playerController.playerId(), gameType);
            return new CommonResult<>(Code.NOT_FOUND);
        }

        if (warehouseCfg.getEnterMax() != -1 && (checkDiamond ? warehouseCfg.getEnterMax() < player.getDiamond() : warehouseCfg.getEnterMax() < player.getGold())) {
            log.debug("玩家携带货币超过房间限制 playerId = {},gameType = {},roomCfgId = {},transactionId = {},gold = {},diamond = {},enterMax = {},checkDiamond = {}", playerController.playerId(), gameType, roomCfgId, warehouseCfg.getTransactionItemId(), player.getGold(), player.getDiamond(), warehouseCfg.getEnterMax(), checkDiamond);
            return new CommonResult<>(Code.GOLD_TOO_MUCH);
        }
        return new CommonResult<>(Code.SUCCESS, info);
    }


    /**
     * 我的赌场 请求购买一键领取
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_BUY_CLAIM_ALL_REWARDS)
    public void reqCasinoBuyClaimAllRewards(PlayerController playerController, ReqCasinoBuyClaimAllRewards req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoBuyClaimAllRewards(playerController, req));
        }
    }

    /**
     * 我的赌场 请求玩家赌场信息
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_INFO)
    public void reqCasinoInfo(PlayerController playerController, ReqCasinoInfo req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoInfo(playerController, req));
        }
    }

    /**
     * 我的赌场 一键领取收益
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_CLAIM_ALL_REWARDS)
    public void reqCasinoClaimAllRewards(PlayerController playerController, ReqCasinoClaimAllRewards req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoClaimAllRewards(playerController, req));
        }
    }

    /**
     * 我的赌场 领取机台收益
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_CLAIM_REWARDS)
    public void reqCasinoClaimRewards(PlayerController playerController, ReqCasinoClaimRewards req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoClaimRewards(playerController, req));
        }
    }

    /**
     * 我的赌场 领取机台收益
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_EMPLOY_STAFF)
    public void reqCasinoEmployStaff(PlayerController playerController, ReqCasinoEmployStaff req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoEmployStaff(playerController, req));
        }
    }

    /**
     * 我的赌场 请求楼层操作
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_FLOOR_OPERATION)
    public void reqCasinoFloorOperation(PlayerController playerController, ReqCasinoFloorOperation req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoFloorOperation(playerController, req));
        }
    }

    /**
     * 我的赌场 请求楼层操作
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_UPGRADE_MACHINE)
    public void reqCasinoUpgradeMachine(PlayerController playerController, ReqCasinoUpgradeMachine req) {
        if (gameFunctionService.checkGameFunctionOpen(playerController.getPlayer(), EFunctionType.MY_CASINO)) {
            playerController.send(casinoManager.reqCasinoUpgradeMachine(playerController, req));
        }
    }

    /**
     * 我的赌场 请求退出赌场
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CASINO_EXIT)
    public void reqCasinoExit(PlayerController playerController, ReqCasinoExit req) {
        playerController.send(casinoManager.reqCasinoExit(playerController.getPlayer(), req));
    }

    /**
     * 添加收藏游戏
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_LIKE_GAME)
    public void reqLikeGame(PlayerController playerController, ReqLikeGame req) {
        ResLikeGame res = new ResLikeGame(Code.SUCCESS);
        try {
            res.gameTypeList = hallService.addLikeGame(playerController.playerId(), req.gameTypes);
            log.debug("添加收藏游戏后，返回列表 res = {}", JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 添加收藏游戏
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_CANCEL_LIKE_GAME)
    public void reqCancelLikeGame(PlayerController playerController, ReqCancelLikeGame req) {
        ResCancelLikeGame res = new ResCancelLikeGame(Code.SUCCESS);
        try {
            res.gameTypeList = hallService.cancelLikeGames(playerController.playerId(), req.gameTypes);
            log.debug("取消收藏游戏后，返回列表 res = {}", JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * VIP 请求Vip数据
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_VIP_INFO)
    public void reqVipInfo(PlayerController playerController, ReqVipInfo req) {
        playerController.send(vipManager.reqVipInfo(playerController, req));
    }


    /**
     * VIP 请求领取VIP礼包
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_VIP_CLAIM_GIFT_REWARD)
    public void reqVipClaimGiftReward(PlayerController playerController, ReqVipClaimGiftReward req) {
        playerController.send(vipManager.reqVipClaimGiftReward(playerController, req));
    }

    /**
     * 请求绑定第三方账号
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_BIND_THIRD_ACCOUNT)
    public void reqBindThirdAccount(PlayerController playerController, ReqBindThirdAccount req) {
        ResBindThirdAccount res = new ResBindThirdAccount(Code.SUCCESS);
        try {
            CommonResult<List<Item>> result = hallService.bindThirdAccount(playerController.getPlayer(), req.type, req.token);
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }
            log.debug("账号绑定成功 type = {},res = {}", req.type, JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }

    /**
     * 请求购买头像
     *
     * @param playerController 玩家信息
     */
    @Command(HallConstant.MsgBean.REQ_BUY_AVATAR)
    public void reqBuyAvatar(PlayerController playerController, ReqBuyAvatar req) {
        ResBuyAvatar res = new ResBuyAvatar(Code.SUCCESS);
        try {
            CommonResult<Integer> result = hallService.buyAvatar(playerController.playerId(), req.id);
            if (!result.success()) {
                res.code = result.code;
                playerController.send(res);
                return;
            }

            res.giveId = req.id;
            log.debug("购买头像成功 id = {},avatarId = {}", req.id, result.data);

//            playerController.send(getAllAvatar(playerController));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }


    /**
     * 游戏功能开放列表
     */
    @Command(HallConstant.MsgBean.REQ_FUNCTION_OPEN_LIST)
    public void reqGameFunctionOpenList(PlayerController playerController) {
        Player player = corePlayerService.get(playerController.playerId());
        List<Integer> openedFuncIdList = gameFunctionService.getOpenedFuncIdList(player);
        ResFunctionOpenList res = new ResFunctionOpenList(Code.SUCCESS);
        res.openedFunctionIdList = openedFuncIdList;
        playerController.send(res);
    }

    /**
     * 获取所有的公告
     */
    @Command(HallConstant.MsgBean.REQ_ALL_NOTICE)
    public void reqAllNotice(PlayerController playerController, ReqAllNotice req) {
        ResAllNotice res = new ResAllNotice(Code.SUCCESS);
        try {
            List<Notice> notices = noticeService.getNotices();
            if (notices != null && !notices.isEmpty()) {
                //获取玩家已经阅读的公告
                Set<Long> readSet = noticeService.getPlayerReadNotice(playerController.playerId());

                res.noticeList = new ArrayList<>(notices.size());
                for (Notice notice : notices) {
                    NoticeInfo noticeInfo = new NoticeInfo();
                    BeanUtils.copyProperties(notice, noticeInfo);

                    if (readSet.contains(notice.getId())) {
                        noticeInfo.setRead(true);
                    }
                    res.noticeList.add(noticeInfo);
                }
            }
            log.debug("返回公告列表 playerId = {}", playerController.playerId());
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("", e);
        }
        playerController.send(res);
    }

    /**
     * 读取公告
     */
    @Command(HallConstant.MsgBean.REQ_READ_NOTICE)
    public void reqReadNotice(PlayerController playerController, ReqReadNotice req) {
        ResReadNotice res = new ResReadNotice(Code.SUCCESS);
        try {
            noticeService.readNotice(playerController.playerId(), req.id);
            log.debug("阅读公告 playerId = {},id = {}", playerController.playerId(), req.id);
        } catch (Exception e) {
            res.code = Code.EXCEPTION;
            log.error("", e);
        }
        playerController.send(res);
    }

    /**
     * 请求领取注册奖励
     */
    @Command(HallConstant.MsgBean.REQ_GET_REGISTER_REWARDS)
    public void ReqGetRegisterRewards(PlayerController playerController) {
        ResGetRegisterRewards res = new ResGetRegisterRewards(Code.SUCCESS);
        boolean register = countDao.setIfAbsent(CountDao.CountType.PLAYER_COUNT.getParam().formatted("register"), String.valueOf(playerController.playerId()));
        if (register) {
            //发送奖励
            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(50);
            if (globalConfigCfg != null && StringUtils.isNotEmpty(globalConfigCfg.getValue())) {
                String[] split = StringUtils.split(globalConfigCfg.getValue(), ";");
                Map<Integer, Long> rewards = new HashMap<>();
                for (String string : split) {
                    String[] itemInfo = StringUtils.split(string, "_");
                    if (itemInfo.length == 2) {
                        rewards.put(Integer.valueOf(itemInfo[0]), Long.valueOf(itemInfo[1]));
                    }
                }
                CommonResult<ItemOperationResult> addItems = playerPackService.addItems(playerController.playerId(), rewards, AddType.PLAYER_REGISTER);
                if (!addItems.success()) {
                    log.error("玩家领取注册奖励失败 playerId:{}", playerController.playerId());
                }
                playerController.send(res);
                return;
            }
        }
        res.code = Code.REPEAT_OP;
        playerController.send(res);
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("enterGame".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                ReqChooseGame req = new ReqChooseGame();
                req.gameType = Integer.parseInt(gmOrders[1]);
                reqChooseGame(playerController, req);
            } else if ("addAvatar".equalsIgnoreCase(gmOrders[0])) {
                int id = Integer.parseInt(gmOrders[1]);
                hallService.addPlayerAvatar(playerController.playerId(), id);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }

    /*****************************************************************************************************/


    /**
     * 获取玩家的背包数据
     *
     * @param playerId
     * @return
     */
    private List<PackItemInfo> getPlayerPack(long playerId) {
        PlayerPack playerPack = hallService.getPlayerPack(playerId);
        if (playerPack == null || playerPack.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        List<PackItemInfo> packItemInfos = new ArrayList<>();
        playerPack.getItems().forEach((key, value) -> {
            PackItemInfo info = new PackItemInfo();
            info.girdId = key;
            info.item = new ItemInfo();
            info.item.itemId = value.getId();
            info.item.count = value.getItemCount();
            packItemInfos.add(info);
        });
        return packItemInfos;
    }

    @Override
    public void onChooseWare(PlayerController playerController, ReqChooseWare req) {
        ResChooseWare res = new ResChooseWare(HallCode.SUCCESS);
        try {
            log.info("收到玩家选择游戏场次 playerId={},req={}", playerController.playerId(), JSONObject.toJSONString(req));
            CommonResult<WareHouseConfigInfo> checkRes = checkBeforeJoinRoom(playerController, req.gameType, req.wareId);
            if (checkRes.code != Code.SUCCESS) {
                res.code = checkRes.code;
                playerController.send(res);
                return;
            }
            //slots类游戏没有房间
            //是不是slots游戏
            if (CommonUtil.getMajorTypeByGameType(req.gameType) == CoreConst.GameMajorType.SLOTS) {
                res.code = hallRoomService.enterSlotsNode(playerController, req.wareId);
            } else {
                // 进入大厅加入房间的逻辑
                res.code = hallRoomService.hallJoinRoom(playerController, req.wareId);
            }
            log.info("玩家选择场次，playerId = {},res = {}", playerController.playerId(), JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        playerController.send(res);
    }
}
