package com.jjg.game.core.service;

import cn.hutool.core.util.IdUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.MailDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Mail;
import com.jjg.game.core.data.PlayerPack;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

import static io.lettuce.core.GeoArgs.Sort.desc;

/**
 * @author 11
 * @date 2025/8/11 17:41
 */
@Service
public class MailService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private MailDao mailDao;
    @Autowired
    private PlayerPackService playerPackService;

    //每页数量
    private int mailPageSize = 50;

    /**
     * 获取玩家的邮件
     * @param playerId
     * @return
     */
    public List<Mail> getMailByPlayerId(long playerId,int page) {
        return mailDao.getMailsByPlayerId(playerId,page,mailPageSize);
    }

    /**
     * 阅读邮件
     * @param mailId
     */
    public boolean readMail(long playerId,long mailId) {
        return mailDao.updateStatus(playerId,mailId, GameConstant.Mail.STAUTS_READ);
    }

    /**
     * 领取邮件内的道具
     * @param playerId
     * @param mailId
     * @return
     */
    public CommonResult<Integer> getMailItems(long playerId, long mailId, String desc){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        try{
            Mail mail = getMail(playerId, mailId);
            if(mail == null){
                result.code = Code.NOT_FOUND;
                log.debug("未找到玩家有邮件，获取道具失败 playerId = {},mailId = {},desc = {}",playerId,mailId,desc);
                return result;
            }

            if(mail.getStatus() == GameConstant.Mail.STAUTS_GET_ITEMS){
                result.code = Code.NOT_FOUND;
                log.debug("该道具已被领取，获取道具失败 playerId = {},mailId = {},desc = {}",playerId,mailId,desc);
                return result;
            }

            if(mail.getItems() == null || mail.getItems().isEmpty()){
                result.code = Code.NOT_FOUND;
                log.debug("该邮件内没有道具，获取道具失败 playerId = {},mailId = {},desc = {}",playerId,mailId,desc);
                return result;
            }

            Map<Integer,Long> map = new HashMap<>();
            mail.getItems().forEach(mailItem -> {
                int id = mailItem.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if(itemCfg == null){
                    log.debug("未找到该道具，领取邮件内道具失败， playerId = {},itemId = {},desc = {}",playerId,id,desc);
                }else {
                    map.merge(id,mailItem.getCount(),Long::sum);
                }
            });

            if(!map.isEmpty()){
                playerPackService.addItems(playerId,map,"getMailItems");
            }
        }catch (Exception e){
            log.error("",e);
            result.code = Code.EXCEPTION;
        }
        return result;
    }

    /**
     * 删除邮件
     * @param playerId
     * @param mailId
     */
    public void removeMail(long playerId,long mailId) {
        getMailItems(playerId, mailId,"removeGetMailItems");
        mailDao.removeMail(playerId,mailId);
    }

    /**
     * 获取玩家的一封邮件
     * @param playerId
     * @return
     */
    public Mail getMail(long playerId,long mailId) {
        return mailDao.getMailByPlayerId(playerId,mailId);
    }

    /**
     * 删除已读邮件
     * @param playerId
     * @return
     */
    public long removeReadMails(long playerId){
        return mailDao.removeReadMails(playerId);
    }

    /**
     * 一键领取
     * @param playerId
     */
    public CommonResult<Integer> getAllMailsItems(long playerId){
        CommonResult<Integer> result = new CommonResult<>(Code.SUCCESS);
        List<Mail> itemMails = mailDao.getItemMails(playerId);
        if(itemMails == null || itemMails.isEmpty()){
            result.code = Code.NOT_FOUND;
            return result;
        }

        List<Long> mailIds = new ArrayList<>();
        Map<Integer,Long> map = new HashMap<>();
        for(Mail mail : itemMails){
            if(mail.getStatus() == GameConstant.Mail.STAUTS_GET_ITEMS){
                continue;
            }
            if(mail.getItems() == null || mail.getItems().isEmpty()){
                continue;
            }
            mail.getItems().forEach(mailItem -> {
                int id = mailItem.getId();
                ItemCfg itemCfg = GameDataManager.getItemCfg(id);
                if(itemCfg == null){
                    log.debug("未找到该道具，领取邮件内道具失败， playerId = {},itemId = {},desc = {}",playerId,id,desc);
                }else {
                    map.merge(id,mailItem.getCount(),Long::sum);
                }
            });

            mailIds.add(mail.getId());
        }

        if(!map.isEmpty()){
            long count = mailDao.batchUpdateMailStatus(mailIds,GameConstant.Mail.STAUTS_GET_ITEMS);
            CommonResult<PlayerPack> addItemsResult = playerPackService.addItems(playerId, map, "getAllMailsItems");
            log.info("一键领取结果 playerId = {}, batchUpdateCount = {}, addItemsResultCode = {}",playerId,count,addItemsResult.code);
        }
        return result;
    }

    /**
     * 批量保存邮件
     * @param mails
     */
    public void addMails(List<Mail> mails) {
        long saveCount = mailDao.batchSaveMails(mails);
        log.debug("批量保存邮件数量 mails.size = {}",saveCount);
    }

    /**
     * 添加全服邮件
     * @param mail
     */
    public void addAllServerMail(Mail mail) {
        mailDao.saveServerMail(mail);
    }

    /**
     * 玩家获取全服邮件
     * @param playerId
     */
    public void playerGetServerMails(long playerId) {
        List<Mail> serverMail = mailDao.getServerMails();
        if(serverMail == null || serverMail.isEmpty()){
            return;
        }

        //本次领取的邮件列表
        List<Mail> getMails = new ArrayList<>();
        //获取玩家已经领取的全服邮件id
        Set<Long> playerServerMails = mailDao.getPlayerServerMails(playerId);

        int now = TimeHelper.nowInt();
        for (Mail mail : serverMail) {
            try{
                //是否领取邮件
                boolean reve = playerServerMails.contains(mail.getId());

                //检查邮件是否过期
                if(mail.getTimeout() < now){
                    log.info("检测到系统邮件到期 mailId = {},title = {},timeout = {}",mail.getId(),mail.getTitle(),mail.getTimeout());
                    if(reve){
                        mailDao.removeServerMail(mail.getId());
                    }
                    continue;
                }

                if(reve){
                    continue;
                }
                mailDao.addPlayerServerMail(playerId,mail.getId());

                Mail getMail = mail.clone();
                getMail.setId(IdUtil.getSnowflakeNextId());
                getMail.setServerMail(false);
                getMail.setPlayerId(playerId);
                getMails.add(getMail);
            }catch (Exception e){
                log.error("领取全服邮件异常 playerId = {},mailId = {}",playerId,mail.getId(),e);
            }
        }

        if(!getMails.isEmpty()){
            long count = mailDao.batchSaveMails(getMails);
            log.info("玩家接收全服邮件成功 playerId = {},count = {}",playerId,count);
        }
    }
}
