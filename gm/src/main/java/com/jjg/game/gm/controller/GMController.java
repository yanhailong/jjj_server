package com.jjg.game.gm.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.constant.BackendGMCmd;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.GameStatus;
import com.jjg.game.core.pb.gm.ReqMarqueeServer;
import com.jjg.game.core.pb.gm.ReqRefreshGameStatus;
import com.jjg.game.core.pb.gm.ReqStopMarqueeServer;
import com.jjg.game.core.service.GameStatusService;
import com.jjg.game.gm.dto.GameStatusDto;
import com.jjg.game.gm.dto.MailDto;
import com.jjg.game.gm.dto.MarqueeDto;
import com.jjg.game.gm.vo.WebResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
        //获取大厅节点
        List<ClusterClient> hallNodes = ClusterSystem.system.getNodesByType(NodeType.HALL);
        //获取游戏节点
        List<ClusterClient> gameNodes = ClusterSystem.system.getNodesByType(NodeType.GAME);

        //构建请求消息
        ReqMarqueeServer req = new ReqMarqueeServer(Code.SUCCESS);
        req.content = dto.content();
        PFMessage pfMessage = MessageUtil.getPFMessage(req);
        ClusterMessage msg = new ClusterMessage(pfMessage);

        StringBuilder res = new StringBuilder();

        //通知大厅节点
        for (ClusterClient clusterClient : hallNodes) {
            try {
                clusterClient.write(msg);
            } catch (Exception e) {
                log.error("gm推送跑马灯信息失败", e);
                res.append("""
                        gm推送跑马灯信息到节点 %s 失败""".formatted(clusterClient.nodeConfig.getName()));
            }
        }

        //通知游戏节点
        for (ClusterClient clusterClient : gameNodes) {
            try {
                clusterClient.write(msg);
            } catch (Exception e) {
                log.error("gm推送跑马灯信息失败", e);
                res.append("""
                        gm推送跑马灯信息到节点 %s 失败""".formatted(clusterClient.nodeConfig.getName()));
            }
        }
        //返回修改结果
        return !res.isEmpty() ? fail(res.toString()) : success("推送成功");
    }

    /**
     * 停止跑马灯
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.STOP_MARQUEE)
    public WebResult<String> stopMarquee(@RequestBody @Valid MarqueeDto dto) {
        log.info("收到后台的停止跑马灯信息请求 {}", dto);
        //获取大厅节点
        List<ClusterClient> hallNodes = ClusterSystem.system.getNodesByType(NodeType.HALL);
        //获取游戏节点
        List<ClusterClient> gameNodes = ClusterSystem.system.getNodesByType(NodeType.GAME);

        //构建请求消息
        ReqStopMarqueeServer req = new ReqStopMarqueeServer(Code.SUCCESS);
        PFMessage pfMessage = MessageUtil.getPFMessage(req);
        ClusterMessage msg = new ClusterMessage(pfMessage);

        StringBuilder res = new StringBuilder();

        //通知大厅节点
        for (ClusterClient clusterClient : hallNodes) {
            try {
                clusterClient.write(msg);
            } catch (Exception e) {
                log.error("gm推送停止跑马灯信息失败", e);
                res.append("""
                        gm推送停止跑马灯信息到节点 %s 失败""".formatted(clusterClient.nodeConfig.getName()));
            }
        }

        //通知游戏节点
        for (ClusterClient clusterClient : gameNodes) {
            try {
                clusterClient.write(msg);
            } catch (Exception e) {
                log.error("gm推送停止跑马灯信息失败", e);
                res.append("""
                        gm推送停止跑马灯信息到节点 %s 失败""".formatted(clusterClient.nodeConfig.getName()));
            }
        }
        //返回修改结果
        return !res.isEmpty() ? fail(res.toString()) : success("推送成功");
    }

    /**
     * 邮件
     * @param dto
     * @return
     */
    @RequestMapping(BackendGMCmd.SEND_EMAIL)
    public WebResult<String> mail(@RequestBody @Valid MailDto dto) {
        log.info("收到后台的邮件请求 {}", dto);
        StringBuilder res = new StringBuilder();
        //返回修改结果
        return !res.isEmpty() ? fail(res.toString()) : success("推送成功");
    }
}
