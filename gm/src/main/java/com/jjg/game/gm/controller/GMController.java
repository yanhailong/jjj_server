package com.jjg.game.gm.controller;

import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 修改游戏状态
     *
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.CHANGE_GAME_STATUS)
    public WebResult<String> changeGameStatus(@RequestBody @Valid GameStatusDto dto) {
        log.info("收到修改游戏状态请求 {}", dto);
        boolean saved = gameStatusService.saveOrUpdateGameStatus(new GameStatus(dto.number(),
                dto.open(), dto.status(), dto.right_top_icon()));
        if (!saved) {
            return fail("修改游戏状态失败,无法保存到Redis");
        }
        //获取大厅节点
        List<ClusterClient> nodesByType = ClusterSystem.system.getNodesByType(NodeType.HALL);
        //构建请求消息
        ReqRefreshGameStatus msg = new ReqRefreshGameStatus();

        String cmdParm;
        try {
            cmdParm = new ObjectMapper().writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            log.error("", e);
            return fail("反虚拟化失败");
        }
        msg.cmdParam = cmdParm;
        byte[] data = ProtostuffUtil.serialize(msg);
        PFMessage pfMessage = new PFMessage(MessageConst.ToServer.REQ_REFRESH_GAME_STATUS, data);
        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
        StringBuilder res = new StringBuilder();
        for (ClusterClient clusterClient : nodesByType) {
            try {
                //通知大厅节点修改游戏状态
                clusterClient.write(clusterMessage);
            } catch (Exception e) {
                log.error("请求改变游戏状态时发送失败", e);
                res.append("""
                        请求改变游戏状态时发送到节点 %s 失败""".formatted(clusterClient.nodeConfig.getName()));
            }
        }
        //返回修改结果
        return !res.isEmpty() ? fail(res.toString()) : success("修改成功");
    }

    /**
     * 添加跑马灯
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SNED_MARQUEE)
    public WebResult<String> sendMarquee(@RequestBody @Valid MarqueeDto dto) {
        log.info("收到后台的跑马灯信息请求 {}", dto);

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
        return success("推送成功");
    }

    /**
     * 停止跑马灯
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.STOP_MARQUEE)
    public WebResult<String> stopMarquee(@RequestBody StopMarqueeDto dto) {
        log.info("收到后台的停止跑马灯信息请求 id = {}", dto.id());
        boolean exist = marqueeDao.exist(dto.id());
        if (!exist) {
            return fail("该跑马灯不存在");
        }

        //构建请求消息
        NotifyAllNodesStopMarqueeServer notify = new NotifyAllNodesStopMarqueeServer();
        notify.id = dto.id();
        marqueeManager.notifyHallAndGameNodeStopMarquee(notify);

        //返回修改结果
        return success("推送成功");
    }

    /**
     * 邮件
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SEND_EMAIL)
    public WebResult<String> sendEmail(@RequestBody @Valid MailDto dto) {
        log.info("收到后台的邮件请求 {}", dto);

        if(dto.type() == 0){  //指定邮件
            if(dto.playerIds() == null || dto.playerIds().isEmpty()){
                log.debug("指定邮件中，玩家id不能为空");
                return fail("指定邮件中，玩家id不能为空");
            }
            List<Mail> list = new ArrayList<>();
            for(long playerId : dto.playerIds()) {
                Mail mail = createMail(dto);
                mail.setPlayerId(playerId);
                list.add(mail);
            }
            mailService.addMails(list);
        }else if(dto.type() == 1){  //全服邮件
            Mail mail = createMail(dto);
            mailService.addAllServerMail(mail);
        }else {
            return fail("邮件类型错误");
        }

        StringBuilder res = new StringBuilder();
        //返回修改结果
        return !res.isEmpty() ? fail(res.toString()) : success("推送成功");
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

        int sendTime = TimeHelper.getSecondTime(dto.sendTime());
        mail.setSendTime(sendTime);
        mail.setTimeout(sendTime + GameConstant.Mail.DEFUALT_EXPIRE_TIME);

        if(dto.items() != null && !dto.items().isEmpty()){
            List<Item> items = new ArrayList<>();
            for(long[] arr : dto.items()){
                Item item = new Item();
                item.setId((int)arr[0]);
                item.setCount(arr[1]);
                items.add(item);
            }
            mail.setItems(items);
        }
        return mail;
    }
}
