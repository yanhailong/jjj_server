package com.jjg.game.gm.controller;

import cn.hutool.core.util.IdUtil;
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
import com.jjg.game.core.data.GameStatus;
import com.jjg.game.core.data.Item;
import com.jjg.game.core.data.Mail;
import com.jjg.game.core.data.Marquee;
import com.jjg.game.core.manager.CoreMarqueeManager;
import com.jjg.game.core.pb.NotifyAllNodesMarqueeServer;
import com.jjg.game.core.pb.NotifyAllNodesStopMarqueeServer;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.core.service.MailService;
import com.jjg.game.gm.dto.GameStatusDto;
import com.jjg.game.gm.dto.MailDto;
import com.jjg.game.gm.dto.MarqueeDto;
import com.jjg.game.gm.dto.StopMarqueeDto;
import com.jjg.game.gm.vo.WebResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
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

            boolean saved = gameStatusService.saveOrUpdateGameStatus(new GameStatus(dto.number(),
                    dto.open(), dto.status(), dto.right_top_icon()));

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

            if(StringUtils.isEmpty(dto.content()) || dto.showTime() < 0 || dto.interval_time() < 1 || dto.priority() < 0 ||
                    StringUtils.isEmpty(dto.start_time()) || StringUtils.isEmpty(dto.end_time())){
                log.debug("从后台收到的跑马灯参数错误");
                return fail("common.paramerror");
            }

            //存储到redis
            Marquee marquee = new Marquee();
            marquee.setId(dto.id());
            marquee.setContent(dto.content());
            marquee.setInterval(dto.interval_time());
            marquee.setNums(0);
            marquee.setShowTime(dto.showTime());
            marquee.setStartTime(TimeHelper.getSecondTime(dto.start_time()));
            marquee.setEndTime(TimeHelper.getSecondTime(dto.end_time()));
            marquee.setPriority(dto.priority());
            marquee.setType(dto.type() < 1 ? GameConstant.Marquee.SYSTEM_MSG : dto.type());
            marqueeDao.addMarquee(marquee);

            //构建请求消息
            NotifyAllNodesMarqueeServer notify = new NotifyAllNodesMarqueeServer();
            notify.id = marquee.getId();
            notify.content = marquee.getContent();
            notify.showTime = marquee.getShowTime();
            notify.interval = marquee.getInterval();
            notify.type = marquee.getType();
            notify.startTime = marquee.getStartTime();
            notify.endTime = marquee.getEndTime();
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

            if(StringUtils.isEmpty(dto.designated())){  //为空表示全服邮件
                Mail mail = createMail(dto);
                mailService.addAllServerMail(mail);
            }else {
                List<Mail> list = new ArrayList<>();
                String[] arr = dto.designated().split(",");
                for(String str : arr) {
                    Mail mail = createMail(dto);
                    mail.setPlayerId(Long.parseLong(str));
                    list.add(mail);
                }
                mailService.addMails(list);
            }

            //返回修改结果
            return success("common.success");
        }catch (Exception e){
            log.error("", e);
            return fail("common.exception");
        }
    }

    /**
     * 创建邮件对象
     * @param dto
     * @return
     */
    private Mail createMail(MailDto dto) {
        Mail mail = new Mail();
        mail.setId(IdUtil.getSnowflakeNextId());
        mail.setTitle(dto.title());
        mail.setContent(dto.content());

        int sendTime = TimeHelper.nowInt();
        mail.setSendTime(sendTime);
        mail.setTimeout(sendTime + GameConstant.Mail.DEFUALT_EXPIRE_TIME);

        if(StringUtils.isNotEmpty(dto.items())){
            List<Item> items = mailItemsPattern.matcher(dto.items())
                    .results()
                    .map(match -> new Item(
                            Integer.parseInt(match.group(1)),
                            Long.parseLong(match.group(2))
                    ))
                    .collect(Collectors.toList());

            mail.setItems(items);
        }
        return mail;
    }
}
