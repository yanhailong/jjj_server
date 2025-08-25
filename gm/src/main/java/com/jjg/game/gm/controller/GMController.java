package com.jjg.game.gm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.MarqueeDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.core.service.CorePlayerService;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.gm.dto.*;
import com.jjg.game.gm.vo.WebResult;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.ItemCfg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/7/10 09:15
 */
@RestController
@RequestMapping(method = {RequestMethod.POST}, value = "gm")
public class GMController extends AbstractController {

    @Autowired
    private GameStatusService gameStatusService;
    @Autowired
    private MarqueeDao marqueeDao;
    @Autowired
    private CoreMarqueeManager marqueeManager;
    @Autowired
    private MailService mailService;
    @Autowired
    private CorePlayerService playerService;

    //邮件中的道具string，需要用正则匹配
    private Pattern mailItemsPattern = Pattern.compile("\\[(\\d+),(\\d+)\\]");

    /**
     * 修改游戏状态
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANGE_GAME_STATUS)
    public WebResult<String> changeGameStatus(@RequestBody GameStatusDto dto) {
        try{
            log.info("收到修改游戏状态请求 dto = {}", dto);
            if(dto.number() < 1){
                log.debug("游戏id不能为负，修改游戏状态失败 dto = {}", dto);
                return fail("common.paramerror");
            }

            if(dto.open() != 1 && dto.open() != 2){
                log.debug("开放状态错误，只能为1(开放)或2(不开放)  dto = {}", dto);
                return fail("common.paramerror");
            }

            if(dto.status() != 1 && dto.status() != 2){
                log.debug("上下架状态错误，只能为1(上架)或2(下架)  dto = {}", dto);
                return fail("common.paramerror");
            }

            if(dto.icon_category() != 0 && dto.icon_category() != 1){
                log.debug("图标大小错误，只能为0(大)或1(小)  dto = {}", dto);
                return fail("common.paramerror");
            }

            boolean saved = gameStatusService.saveOrUpdateGameStatus(new GameStatus(dto.number(),
                    dto.open(), dto.status(), dto.right_top_icon(),dto.icon_category(),dto.sort()));

            if (!saved) {
                log.info("修改游戏状态失败,无法保存到Redis , dto = {}", dto);
                return fail("common.fail");
            }
            //获取大厅节点
            List<ClusterClient> nodesByType = ClusterSystem.system.getNodesByType(NodeType.HALL);
            //构建请求消息
            ReqRefreshGameStatus msg = new ReqRefreshGameStatus();

            msg.cmdParam = new ObjectMapper().writeValueAsString(dto);

            byte[] data = ProtostuffUtil.serialize(msg);
            PFMessage pfMessage = new PFMessage(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS, data);
            ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
            for (ClusterClient clusterClient : nodesByType) {
                try {
                    //通知大厅节点修改游戏状态
                    clusterClient.write(clusterMessage);
                } catch (Exception e) {
                    log.error("请求改变游戏状态时发送失败", e);
                }
            }
            //返回修改结果
            return success("common.success");
        }catch (Exception e) {
            log.error("",e);
            return fail("common.exception");
        }
    }

    /**
     * 添加跑马灯
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SNED_MARQUEE)
    public WebResult<String> sendMarquee(@RequestBody MarqueeDto dto) {
        try{
            log.info("收到后台的跑马灯信息请求 {}", dto);
            if(dto.id() < 1){
                log.debug("开启跑马灯时，从后台收到的跑马灯id不能小于1 id = {}", dto.id());
                return fail("common.paramerror");
            }

            if(StringUtils.isEmpty(dto.content()) || dto.showTime() < 0 || dto.interval_time() < 0 || dto.priority() < 0 ||
                    StringUtils.isEmpty(dto.start_time()) || StringUtils.isEmpty(dto.end_time())){
                log.debug("从后台收到的跑马灯参数错误");
                return fail("common.paramerror");
            }

            //存储到redis
            Marquee marquee = new Marquee();
            marquee.setId(dto.id());

            LanguageData contentData = new LanguageData();
            contentData.setType(GameConstant.Language.TYPE_ORIGINAL);
            contentData.setContent(dto.content());
            marquee.setContent(contentData);

            marquee.setInterval(dto.interval_time());
            marquee.setNums(0);
            marquee.setShowTime(dto.showTime());
            marquee.setStartTime(TimeHelper.getSecondTime(dto.start_time()));
            marquee.setEndTime(TimeHelper.getSecondTime(dto.end_time()));
            marquee.setPriority(dto.priority());
            marquee.setType(dto.type() < 1 ? GameConstant.Marquee.SYSTEM_MSG : dto.type());
            marqueeDao.addMarquee(marquee);

            //构建请求消息,发送到其他节点
            NotifyAllNodesMarqueeServer notify = new NotifyAllNodesMarqueeServer();
            notify.marqueeInfo = marqueeManager.transMarqueeInfo(marquee);
            notify.type = marquee.getType();
            marqueeManager.notifyHallAndGameNodeStartMarquee(notify);
            //返回修改结果
            return success("common.success");
        }catch (Exception e){
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 停止跑马灯
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.STOP_MARQUEE)
    public WebResult<String> stopMarquee(@RequestBody StopMarqueeDto dto) {
        try{
            log.info("收到后台的停止跑马灯信息请求 id = {}", dto.id());
            if(dto.id() < 1){
                log.debug("停止跑马灯时，从后台收到的跑马灯id不能小于1 id = {}", dto.id());
                return fail("common.paramerror");
            }

            boolean exist = marqueeDao.exist(dto.id());
            if (!exist) {
                return fail("marquee.notfound");
            }

            //构建请求消息
            NotifyAllNodesStopMarqueeServer notify = new NotifyAllNodesStopMarqueeServer();
            notify.id = dto.id();
            marqueeManager.notifyHallAndGameNodeStopMarquee(notify);

            //返回修改结果
            return success("common.success");
        }catch (Exception e){
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 查询玩家信息
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.QUERY_ACCOUNT)
    public WebResult<Player> queryAccount(@RequestBody QueryAccountDto dto) {
        try{
            log.info("收到后台查询玩家信息请求 playerId = {}", dto.playerId());
            if(dto.playerId() < 1){
                log.debug("玩家id不能小于1，查询玩家信息失败 playerId = {}", dto.playerId());
                return fail("common.paramerror");
            }

            Player p = playerService.getFromAllDB(dto.playerId());
            if(p == null){
                log.debug("未找到该玩家信息 playerId = {}", dto.playerId());
                return fail("common.paramerror");
            }

            //返回修改结果
            return success(p);
        }catch (Exception e){
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 邮件
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SEND_EMAIL)
    public WebResult<String> sendEmail(@RequestBody MailDto dto) {
        try{
            log.debug("收到后台的邮件请求 dto = {}", dto);
            if(StringUtils.isEmpty(dto.title())){
                log.debug("发送邮件时，标题不能为空");
                return fail("mail.titlenull");
            }

            if(StringUtils.isEmpty(dto.content())){
                log.debug("发送邮件时，内容不能为空 title = {}",dto.content());
                return fail("mail.contentnull");
            }

            List<Item> mailItems = null;
            //解析邮件中的道具
            if(StringUtils.isNotEmpty(dto.items())){
                mailItems =  mailItemsPattern.matcher(dto.items())
                        .results()
                        .map(match -> new Item(
                                Integer.parseInt(match.group(1)),
                                Long.parseLong(match.group(2))
                        ))
                        .collect(Collectors.toList());

                for(Item item : mailItems){
                    ItemCfg itemCfg = GameDataManager.getItemCfg(item.getId());
                    if(itemCfg == null){
                        log.warn("邮件中的道具，未在配置表中找到 id = {},count = {}", item.getId(),item.getCount());
                        return fail("mail.itemerror");
                    }
                }
            }

            if(StringUtils.isEmpty(dto.designated())){  //为空表示全服邮件
                mailService.addAllServerMail(dto.title(), dto.content(), mailItems);
            }else {
                List<Long> playerIds = new ArrayList<>();
                String[] arr = dto.designated().split(",");
                for(String str : arr) {
                    playerIds.add(Long.parseLong(str));
                }
                mailService.addMails(playerIds, dto.title(), dto.content(), mailItems);
            }

            //返回修改结果
            return success("common.success");
        }catch (Exception e){
            log.error("", e);
            return fail("common.exception");
        }
    }
}
