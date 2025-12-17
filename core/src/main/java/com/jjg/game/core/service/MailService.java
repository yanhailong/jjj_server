package com.jjg.game.core.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.player.IPlayerLoginSuccess;
import com.jjg.game.core.base.player.IPlayerRegister;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.MailDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.NotifyRedDot;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.LoginConfigCfg;
import com.jjg.game.sampledata.bean.MailCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/8/11 17:41
 */
@Service
public class MailService implements IRedDotService, IPlayerLoginSuccess, IPlayerRegister {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailDao mailDao;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private PlayerSessionService playerSessionService;

    @Autowired
    private RedDotManager redDotManager;

    //每页数量
    private int mailPageSize = 50;

    /**
     * 获取玩家的邮件
     *
     * @param playerId
     * @return
     */
    public List<Mail> getMailByPlayerId(long playerId, int page) {
        return mailDao.getMailsByPlayerId(playerId, page, mailPageSize);
    }

    /**
     * 阅读邮件
     *
     * @param mailId
     */
    public boolean readMail(long playerId, long mailId) {
        Mail mail = mailDao.getMailByPlayerId(playerId, mailId);
        boolean read = mailDao.readMail(playerId, mailId);
        if (CollectionUtil.isEmpty(mail.getItems())) {
            if (read) {
                redDotManager.incrementRedDotDataAndUpdate(getModule(), playerId, -1);
            }
        }
        return read;
    }

    public void updateRedDot(long playerId) {
        redDotManager.setRedDotData(getModule(), getSubmodule(), playerId, (int) mailDao.getItemsMailsCount(playerId), true);
    }

    /**
     * 领取邮件内的道具
     *
     * @param playerId
     * @param mailId
     * @return
     */
    public CommonResult<Integer> getMailItems(long playerId, long mailId, AddType addType) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        try {
            Mail mail = getMail(playerId, mailId);
            if (mail == null) {
                result.code = Code.NOT_FOUND;
                log.debug("未找到玩家有邮件，获取道具失败 playerId = {},mailId = {},addType = {}", playerId, mailId, addType);
                return result;
            }

            if (mail.getStatus() == GameConstant.Mail.STATUS_GET_ITEMS) {
                result.code = Code.NOT_FOUND;
                log.debug("该道具已被领取，获取道具失败 playerId = {},mailId = {},addType = {}", playerId, mailId, addType);
                return result;
            }

            if (mail.getItems() == null || mail.getItems().isEmpty()) {
                result.code = Code.NOT_FOUND;
                log.debug("该邮件内没有道具，获取道具失败 playerId = {},mailId = {},addType = {}", playerId, mailId, addType);
                return result;
            }

            Map<Integer, Long> map = new HashMap<>();
            mail.getItems().forEach(mailItem -> {
                int id = mailItem.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if (itemCfg == null) {
                    log.debug("未找到该道具，领取邮件内道具失败， playerId = {},itemId = {},desc = {}", playerId, id, addType);
                } else {
                    map.merge(id, mailItem.getItemCount(), Long::sum);
                }
            });

            if (!map.isEmpty()) {
                mailDao.getMailItems(playerId, mailId);
                playerPackService.addItems(playerId, map, addType, String.valueOf(mailId));
            }
            //邮件变化时通知客户端刷新小红点
            redDotManager.incrementRedDotDataAndUpdate(getModule(), playerId, -1);
        } catch (Exception e) {
            log.error("", e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 删除邮件
     *
     * @param playerId
     * @param mailId
     */
    public void removeMail(long playerId, long mailId) {
        getMailItems(playerId, mailId, AddType.REMOVE_MAIL);
        mailDao.removeMail(playerId, mailId);
    }

    /**
     * 获取玩家的一封邮件
     *
     * @param playerId
     * @return
     */
    public Mail getMail(long playerId, long mailId) {
        return mailDao.getMailByPlayerId(playerId, mailId);
    }

    /**
     * 删除已读邮件
     *
     * @param playerId
     * @return
     */
    public long removeReadMails(long playerId) {
        return mailDao.removeReadMails(playerId);
    }

    /**
     * 一键领取
     *
     * @param playerId
     */
    public CommonResult<Map<Integer, Long>> getAllMailsItems(long playerId) {
        CommonResult<Map<Integer, Long>> result = new CommonResult<>(Code.SUCCESS);
        List<Mail> itemMails = mailDao.getItemMails(playerId, GameConstant.Mail.STATUS_GET_ITEMS);
        if (itemMails == null || itemMails.isEmpty()) {
            result.code = Code.NOT_FOUND;
            return result;
        }

        List<Long> mailIds = new ArrayList<>();
        Map<Integer, Long> map = new HashMap<>();
        for (Mail mail : itemMails) {
            if (mail.getStatus() == GameConstant.Mail.STATUS_GET_ITEMS) {
                continue;
            }
            if (mail.getItems() == null || mail.getItems().isEmpty()) {
                continue;
            }
            mail.getItems().forEach(mailItem -> {
                int id = mailItem.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if (itemCfg == null) {
                    log.debug("未找到该道具，领取邮件内道具失败， playerId = {},itemId = {}", playerId, id);
                } else {
                    map.merge(id, mailItem.getItemCount(), Long::sum);
                }
            });

            mailIds.add(mail.getId());
        }

        if (map.isEmpty()) {
            result.data = map;
            return result;
        }

        long count = mailDao.batchUpdateMailStatus(mailIds, GameConstant.Mail.STATUS_GET_ITEMS);
        CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(playerId, map, AddType.GET_ALL_MAILS_ITEMS);
        if (!addItemsResult.success()) {
            log.debug("一键领取失败 playerId = {},code = {}", playerId, addItemsResult.code);
            result.code = addItemsResult.code;
            return result;
        }
        result.data = map;
        log.info("一键领取结果 playerId = {}, batchUpdateCount = {}, addItemsResultCode = {}", playerId, count,
                addItemsResult.code);
        //邮件变化时通知客户端刷新小红点
        redDotManager.incrementRedDotDataAndUpdate(getModule(), playerId, -mailIds.size());
        return result;
    }

    /**
     * 保存邮件
     *
     * @param playerId
     * @param title
     * @param content
     * @param items
     */
    public void addMail(long playerId, String title, String content, List<Item> items) {
        LanguageData titleData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, title);
        LanguageData contentData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, content);

        Mail mail = createMail(titleData, contentData, items, false);
        mail.setId(IdUtil.getSnowflakeNextId());
        mail.setPlayerId(playerId);
        mailDao.save(mail);
        log.warn("这里应该通知玩家收到邮件 playerId = {},mailId = {}", playerId, mail.getId());
        //邮件变化时通知客户端刷新小红点
        redDotManager.incrementRedDotDataAndUpdate(getModule(), playerId, 1);
    }


    /**
     * 添加系统配置邮件
     */
    public Mail addCfgMail(
            long playerId, int titleLanId, int contentId, List<Item> items, List<LanguageParamData> params) {
        LanguageData titleData = new LanguageData(GameConstant.Language.TYPE_LANGUAGE_MATCH, "");
        LanguageData contentData = new LanguageData(GameConstant.Language.TYPE_LANGUAGE_MATCH, "");
        titleData.setLangId(titleLanId);
        contentData.setLangId(contentId);
        contentData.setParams(params);
        Mail mail = createMail(titleData, contentData, items, false);
        mail.setId(IdUtil.getSnowflakeNextId());
        mail.setPlayerId(playerId);
        log.debug("玩家：{} 添加配置邮件：{}", playerId, JSON.toJSONString(mail));
        mailDao.save(mail);
        //邮件变化时通知客户端刷新小红点
        redDotManager.incrementRedDotDataAndUpdate(getModule(), playerId, 1);
        return mail;
    }


    /**
     * 添加系统配置邮件
     */
    public Mail addCfgMail(long playerId, int mailCfgId) {
        MailCfg mailCfg = GameDataManager.getMailCfg(mailCfgId);
        return addCfgMail(playerId, mailCfg.getTitle(), mailCfg.getText(), Collections.emptyList(), Collections.emptyList());
    }

    /**
     * 添加系统配置邮件
     */
    public Mail addCfgMail(long playerId, int mailCfgId, List<Item> items) {
        MailCfg mailCfg = GameDataManager.getMailCfg(mailCfgId);
        return addCfgMail(playerId, mailCfg.getTitle(), mailCfg.getText(), items, Collections.emptyList());
    }


    /**
     * 添加系统配置邮件
     */
    public Mail addCfgMail(long playerId, int mailCfgId, List<Item> items, List<LanguageParamData> params) {
        MailCfg mailCfg = GameDataManager.getMailCfg(mailCfgId);
        return addCfgMail(playerId, mailCfg.getTitle(), mailCfg.getText(), items, params);
    }


    /**
     * 保存多语言邮件
     *
     * @param playerId
     * @param title
     * @param content
     * @param items
     */
    public void addLangMail(long playerId, LanguageData title, LanguageData content, List<Item> items) {
        Mail mail = createMail(title, content, items, false);
        mail.setId(IdUtil.getSnowflakeNextId());
        mail.setPlayerId(playerId);
        mailDao.save(mail);
        log.warn("这里应该通知玩家收到邮件 playerId = {},mailId = {}", playerId, mail.getId());
        //邮件变化时通知客户端刷新小红点
        redDotManager.incrementRedDotDataAndUpdate(getModule(), playerId, 1);
    }

    /**
     * 批量保存邮件
     *
     * @param playerIds
     * @param title
     * @param content
     * @param items
     */
    public void addMails(List<Long> playerIds, String title, String content, List<Item> items) {
        List<Mail> mails = new ArrayList<>();

        LanguageData titleData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, title);
        LanguageData contentData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, content);

        for (long playerId : playerIds) {
            Mail mail = createMail(titleData, contentData, items, false);
            mail.setId(IdUtil.getSnowflakeNextId());
            mail.setPlayerId(playerId);
            mails.add(mail);
        }
        long saveCount = mailDao.batchSaveMails(mails);
        log.debug("批量保存邮件数量 mails.size = {}", saveCount);
        List<PlayerSessionInfo> infos = playerSessionService.getInfos(playerIds);
        if (CollectionUtil.isEmpty(infos)) {
            return;
        }
        List<Mail> itemsMailsCount = mailDao.getItemsMailsCount(playerIds);
        Map<Long, Long> collect = itemsMailsCount.stream().collect(Collectors.groupingBy(Mail::getPlayerId, Collectors.counting()));
        for (PlayerSessionInfo info : infos) {
            PFSession session = playerSessionService.getSession(info);
            if (session == null || info.getPlayerId() == 0) {
                continue;
            }
            Long count = collect.get(info.getPlayerId());
            if (count == null) {
                continue;
            }
            redDotManager.incrementRedDotDataAndUpdate(getModule(), session.playerId, 1);
            updateRedDot(count, session);
        }
    }

    /**
     * 添加全服邮件
     *
     * @param title
     * @param content
     * @param items
     */
    /**
     * 给所有在线玩家发送全服邮件（分批执行，防止内存或查询压力过大）
     */
    public void addAllServerMail(String title, String content, List<Item> items) {
        LanguageData titleData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, title);
        LanguageData contentData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, content);

        // 创建并保存全服邮件模板
        Mail mail = createMail(titleData, contentData, items, true);
        mailDao.saveServerMail(mail);

        // 获取所有在线玩家
        Map<Long, PlayerSessionInfo> all = playerSessionService.getAll();
        if (all.isEmpty()) {
            return;
        }

        // 在线玩家session映射
        Map<Long, PFSession> playerSessions = new HashMap<>();
        for (Map.Entry<Long, PlayerSessionInfo> en : all.entrySet()) {
            PFSession session = playerSessionService.getSession(en.getValue());
            if (session != null) {
                addMail(en.getValue().getPlayerId(), title, content, items);
                playerSessions.put(en.getValue().getPlayerId(), session);
            }
        }

        if (playerSessions.isEmpty()) {
            return;
        }

        // 批处理参数
        final int BATCH_SIZE = 1000;
        List<Long> allPlayerIds = new ArrayList<>(playerSessions.keySet());

        // 按批次处理
        for (int i = 0; i < allPlayerIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, allPlayerIds.size());
            List<Long> batchIds = allPlayerIds.subList(i, end);

            // 添加邮件引用（标记这些玩家收到这封全服邮件）
            mailDao.addPlayersServerMail(new HashSet<>(batchIds), mail.getId());

            // 查询这批玩家的邮件（带items的未读或已读）
            List<Mail> batchMails = mailDao.getItemsMailsCount(batchIds);

            // 按玩家分组统计数量
            Map<Long, Long> countMap = batchMails.stream()
                    .collect(Collectors.groupingBy(Mail::getPlayerId, Collectors.counting()));

            // 给这批玩家逐个发红点通知
            for (Long playerId : batchIds) {
                Long count = countMap.get(playerId);
                if (count == null) {
                    continue;
                }
                PFSession session = playerSessions.get(playerId);
                if (session == null) {
                    continue;
                }
                updateRedDot(count, session);
            }
        }
    }

    private void updateRedDot(Long count, PFSession session) {
        List<RedDotDetails> redDotDetailsList = new ArrayList<>();
        RedDotDetails redDotDetails = new RedDotDetails();
        redDotDetails.setRedDotModule(getModule());
        redDotDetails.setRedDotType(RedDotDetails.RedDotType.COUNT);
        redDotDetails.setCount(count);
        redDotDetailsList.add(redDotDetails);
        NotifyRedDot notifyRedDot = new NotifyRedDot();
        notifyRedDot.setRedDotList(redDotDetailsList);
        session.send(notifyRedDot);
    }


    /**
     * 玩家获取全服邮件
     *
     * @param player
     */
    public void playerGetServerMails(Player player) {
        List<Mail> serverMail = mailDao.getServerMails();
        if (serverMail == null || serverMail.isEmpty()) {
            return;
        }

        //本次接收的全服邮件列表
        List<Mail> getMails = new ArrayList<>();

        int now = TimeHelper.nowInt();
        for (Mail mail : serverMail) {
            try {
                if(player.getCreateTime() > mail.getSendTime()){
                    continue;
                }

                //是否接收邮件
                boolean reve = mailDao.playerHasServerMail(player.getId(), mail.getId());
                //检查邮件是否过期
                if (mail.getTimeout() < now) {
                    log.info("检测到系统邮件到期 mailId = {},title = {},timeout = {}", mail.getId(), mail.getTitle(),
                            mail.getTimeout());
                    if (reve) {
                        mailDao.removeServerMail(mail.getId());
                    }
                    continue;
                }

                if (reve) {
                    continue;
                }
                mailDao.addPlayerServerMail(player.getId(), mail.getId());

                Mail getMail = mail.clone();
                getMail.setId(IdUtil.getSnowflakeNextId());
                getMail.setServerMail(false);
                getMail.setPlayerId(player.getId());
                getMails.add(getMail);
            } catch (Exception e) {
                log.error("领取全服邮件异常 playerId = {},mailId = {}", player.getId(), mail.getId(), e);
            }
        }

        if (!getMails.isEmpty()) {
            long count = mailDao.batchSaveMails(getMails);
            log.info("玩家接收全服邮件成功 playerId = {},count = {}", player.getId(), count);
            redDotManager.incrementRedDotDataAndUpdate(getModule(), player.getId(), getMails.size());
        }
    }

    /**
     * 创建mail对象
     *
     * @param title
     * @param content
     * @param items
     * @param serverMail
     * @return
     */
    private Mail createMail(LanguageData title, LanguageData content, List<Item> items, boolean serverMail) {
        Mail mail = new Mail();
        mail.setId(IdUtil.getSnowflakeNextId());
        mail.setTitle(title);
        mail.setContent(content);

        mail.setServerMail(serverMail);

        int sendTime = TimeHelper.nowInt();
        mail.setSendTime(sendTime);

        mail.setItems(items);

        int expireTime =
                GameDataManager.getGlobalConfigCfg(GameConstant.GlobalConfig.DEFAULT_MAIL_VALID_TIME).getIntValue();
        mail.setTimeout(mail.getSendTime() + expireTime);
        return mail;
    }

    public void cleanMails() {
        MailDao.CleanMailsResult result = mailDao.cleanMails();

        // 处理需要自动领取附件的邮件
        if (!result.getMailsToAutoClaim().isEmpty()) {
            processAutoClaimMails(result.getMailsToAutoClaim());
        }

        log.info("邮件清理完成: 删除无附件邮件{}封, 删除已领取附件邮件{}封, 自动领取附件邮件{}封, 删除全服邮件{}封",
                result.getNoItemsDeletedCount(), result.getClaimedItemsDeletedCount(),
                result.getMailsToAutoClaim().size(), result.getServerMailsDeletedCount());
    }

    /**
     * 自动领取邮件附件并删除邮件
     */
    private void processAutoClaimMails(List<Mail> mails) {
        // 按玩家ID分组，便于批量处理
        Map<Long, List<Mail>> playerMailsMap = mails.stream()
                .collect(Collectors.groupingBy(Mail::getPlayerId));

        Set<Long> mailIdsToDelete = new HashSet<>();
        for (Map.Entry<Long, List<Mail>> entry : playerMailsMap.entrySet()) {
            long playerId = entry.getKey();
            List<Mail> playerMails = entry.getValue();

            try {
                // 批量领取附件
                Map<Integer, Long> itemsToAdd = new HashMap<>();


                for (Mail mail : playerMails) {
                    if (mail.getItems() != null && !mail.getItems().isEmpty()) {
                        // 收集附件
                        for (Item item : mail.getItems()) {
                            itemsToAdd.merge(item.getId(), item.getItemCount(), Long::sum);
                        }
                        mailIdsToDelete.add(mail.getId());
                    }
                }

                if (!itemsToAdd.isEmpty()) {
                    // 添加道具到玩家背包
                    playerPackService.addItems(playerId, itemsToAdd, AddType.GET_MAIL_ITEMS);
                    log.debug("玩家{}自动领取{}封邮件的附件，获得道具: {}", playerId, mailIdsToDelete.size(), itemsToAdd);
                }
            } catch (Exception e) {
                log.error("处理玩家{}自动领取邮件附件时发生错误", playerId, e);
            }
        }

        if (!mailIdsToDelete.isEmpty()) {
            // 批量删除邮件
            long deletedCount = mailDao.batchRemoveMails(mailIdsToDelete);

            log.info("批量删除过期自动领取邮件 {} 封", deletedCount);
        }
    }

    /**
     * 获取所属模块{@link RedDotDetails.RedDotModule}
     */
    @Override
    public RedDotDetails.RedDotModule getModule() {
        return RedDotDetails.RedDotModule.MAIL;
    }

    /**
     * 初始化红点信息
     *
     * @param playerId  玩家id
     * @param submodule 子模块
     *                  </p>
     *                  (如果指定了子模块则加载子模块数据,没有则加载所有子模块)
     */
    @Override
    public List<RedDotDetails> initialize(long playerId, int submodule) {
        RedDotDetails redDotDetails = new RedDotDetails();
        redDotDetails.setRedDotModule(getModule());
        redDotDetails.setRedDotType(RedDotDetails.RedDotType.COUNT);
        long itemMailsCount = mailDao.getItemsMailsCount(playerId);
        redDotDetails.setCount(itemMailsCount);
        return List.of(redDotDetails);
    }

    @Override
    public void onPlayerLoginSuccess(PlayerController playerController, Player player, boolean firstLogin) {
        playerGetServerMails(player);
    }

    @Override
    public void playerRegister(Player player) {
        LoginType loginType = player.getLoginType();
        if (loginType == null) {
            return;
        }

        //查找奖励
        LoginConfigCfg loginConfigCfg = GameDataManager.getLoginConfigCfgList().stream().filter(cfg -> cfg.getType() == loginType.getValue()).findFirst().orElse(null);
        if (loginConfigCfg == null || loginConfigCfg.getAwardItem() == null || loginConfigCfg.getAwardItem().isEmpty()) {
            return;
        }

        int mailId = 0;
        if (loginType == LoginType.GOOGLE) {
            mailId = GameConstant.Mail.ID_BIND_GOOGLE;
        } else if (loginType == LoginType.FACEBOOK) {
            mailId = GameConstant.Mail.ID_BIND_FACEBOOK;
        } else if (loginType == LoginType.APPLE) {
            mailId = GameConstant.Mail.ID_BIND_APPLE;
        } else if (loginType == LoginType.PHONE) {
            mailId = GameConstant.Mail.ID_BIND_PHONE;
        } else {
            log.debug("注册时未找到该登录方式奖励的邮件id playerId = {},loginType = {}", player.getId(), loginType);
            return;
//            mailId = GameConstant.Mail.ID_BIND_GOOGLE;
        }
        addCfgMail(player.getId(), mailId, ItemUtils.buildItems(loginConfigCfg.getAwardItem()));
    }
}
