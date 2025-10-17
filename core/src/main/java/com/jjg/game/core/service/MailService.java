package com.jjg.game.core.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.reddot.IRedDotService;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.MailDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.RedDotManager;
import com.jjg.game.core.pb.reddot.RedDotDetails;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import com.jjg.game.sampledata.bean.MailCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author 11
 * @date 2025/8/11 17:41
 */
@Service
public class MailService implements IRedDotService {
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
        return mailDao.readMail(playerId, mailId);
    }

    /**
     * 领取邮件内的道具
     *
     * @param playerId
     * @param mailId
     * @return
     */
    public CommonResult<Integer> getMailItems(long playerId, long mailId, String desc) {
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        try {
            Mail mail = getMail(playerId, mailId);
            if (mail == null) {
                result.code = Code.NOT_FOUND;
                log.debug("未找到玩家有邮件，获取道具失败 playerId = {},mailId = {},desc = {}", playerId, mailId, desc);
                return result;
            }

            if (mail.getStatus() == GameConstant.Mail.STATUS_GET_ITEMS) {
                result.code = Code.NOT_FOUND;
                log.debug("该道具已被领取，获取道具失败 playerId = {},mailId = {},desc = {}", playerId, mailId, desc);
                return result;
            }

            if (mail.getItems() == null || mail.getItems().isEmpty()) {
                result.code = Code.NOT_FOUND;
                log.debug("该邮件内没有道具，获取道具失败 playerId = {},mailId = {},desc = {}", playerId, mailId, desc);
                return result;
            }

            Map<Integer, Long> map = new HashMap<>();
            mail.getItems().forEach(mailItem -> {
                int id = mailItem.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if (itemCfg == null) {
                    log.debug("未找到该道具，领取邮件内道具失败， playerId = {},itemId = {},desc = {}", playerId, id, desc);
                } else {
                    map.merge(id, mailItem.getItemCount(), Long::sum);
                }
            });

            if (!map.isEmpty()) {
                mailDao.getMailItems(playerId, mailId);
                playerPackService.addItems(playerId, map, "getMailItems");
            }
            //邮件变化时通知客户端刷新小红点
            redDotManager.updateRedDot(this, 0, playerId);
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
        getMailItems(playerId, mailId, "removeGetMailItems");
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
        CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(playerId, map,
            "getAllMailsItems");
        if (!addItemsResult.success()) {
            log.debug("一键领取失败 playerId = {},code = {}", playerId, addItemsResult.code);
            result.code = addItemsResult.code;
            return result;
        }
        result.data = map;
        log.info("一键领取结果 playerId = {}, batchUpdateCount = {}, addItemsResultCode = {}", playerId, count,
            addItemsResult.code);
        //邮件变化时通知客户端刷新小红点
        redDotManager.updateRedDot(this, 0, playerId);
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
        redDotManager.updateRedDot(this, 0, playerId);
    }


    /**
     * 添加系统配置邮件
     */
    public void addCfgMail(
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
        redDotManager.updateRedDot(this, 0, playerId);
    }

    /**
     * 添加系统配置邮件
     */
    public void addCfgMail(long playerId, int mailCfgId) {
        MailCfg mailCfg = GameDataManager.getMailCfg(mailCfgId);
        addCfgMail(playerId, mailCfg.getTitle(), mailCfg.getText(), new ArrayList<>(), new ArrayList<>());
    }

    /**
     * 添加系统配置邮件
     */
    public void addCfgMail(long playerId, int mailCfgId, List<Item> items) {
        MailCfg mailCfg = GameDataManager.getMailCfg(mailCfgId);
        addCfgMail(playerId, mailCfg.getTitle(), mailCfg.getText(), items, new ArrayList<>());
    }


    /**
     * 添加系统配置邮件
     */
    public void addCfgMail(long playerId, int mailCfgId, List<Item> items, List<LanguageParamData> params) {
        MailCfg mailCfg = GameDataManager.getMailCfg(mailCfgId);
        addCfgMail(playerId, mailCfg.getTitle(), mailCfg.getText(), items, params);
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
        redDotManager.updateRedDot(this, 0, playerId);
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
            //邮件变化时通知客户端刷新小红点
            redDotManager.updateRedDot(this, 0, playerId);
        }
        long saveCount = mailDao.batchSaveMails(mails);
        log.debug("批量保存邮件数量 mails.size = {}", saveCount);

        log.warn("这里要通知玩家收到邮件....");
        //通知收到邮件，
//        for(long playerId : playerIds){
//            PFSession session = playerSessionService.getSession(playerId);
//            if(session == null){
//                continue;
//            }
//        }
    }

    /**
     * 添加全服邮件
     *
     * @param title
     * @param content
     * @param items
     */
    public void addAllServerMail(String title, String content, List<Item> items) {
        LanguageData titleData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, title);
        LanguageData contentData = new LanguageData(GameConstant.Language.TYPE_ORIGINAL, content);

        Mail mail = createMail(titleData, contentData, items, true);
        mailDao.saveServerMail(mail);

        //给在线的玩家添加邮件
        Map<Long, PlayerSessionInfo> all = playerSessionService.getAll();
        Set<Long> playerIds = new HashSet<>();
        for (Map.Entry<Long, PlayerSessionInfo> en : all.entrySet()) {
            PFSession session = playerSessionService.getSession(en.getValue());
            if (session == null) {
                continue;
            }
            addMail(en.getValue().getPlayerId(), title, content, items);
            playerIds.add(en.getValue().getPlayerId());
        }

        if (!playerIds.isEmpty()) {
            mailDao.addPlayersServerMail(playerIds, mail.getId());
        }
    }

    /**
     * 玩家获取全服邮件
     *
     * @param playerId
     */
    public void playerGetServerMails(long playerId) {
        List<Mail> serverMail = mailDao.getServerMails();
        if (serverMail == null || serverMail.isEmpty()) {
            return;
        }

        //本次接收的全服邮件列表
        List<Mail> getMails = new ArrayList<>();

        int now = TimeHelper.nowInt();
        for (Mail mail : serverMail) {
            try {
                //是否接收邮件
                boolean reve = mailDao.playerHasServerMail(playerId, mail.getId());
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
                mailDao.addPlayerServerMail(playerId, mail.getId());

                Mail getMail = mail.clone();
                getMail.setId(IdUtil.getSnowflakeNextId());
                getMail.setServerMail(false);
                getMail.setPlayerId(playerId);
                getMails.add(getMail);
            } catch (Exception e) {
                log.error("领取全服邮件异常 playerId = {},mailId = {}", playerId, mail.getId(), e);
            }
        }

        if (!getMails.isEmpty()) {
            long count = mailDao.batchSaveMails(getMails);
            log.info("玩家接收全服邮件成功 playerId = {},count = {}", playerId, count);
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
        mailDao.cleanMails();
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
        long itemMailsCount = mailDao.getItemsMailsCount(playerId, GameConstant.Mail.STATUS_NOT_READ);
        redDotDetails.setCount(itemMailsCount);
        return List.of(redDotDetails);
    }

}
