package com.jjg.game.core.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.TaskConstant;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.task.pb.Task;
import com.jjg.game.core.task.pb.req.ReqReceiveTaskAward;
import com.jjg.game.core.task.pb.req.ReqTaskList;
import com.jjg.game.core.task.pb.res.ResReceiveTaskAward;
import com.jjg.game.core.task.pb.res.ResTaskList;
import com.jjg.game.core.task.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务相关消息处理类
 */
@Component
@MessageType(MessageConst.MessageTypeDef.TASK_TYPE)
public class TaskMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(TaskMessageHandler.class);

    private final TaskService taskService;

    public TaskMessageHandler(TaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * 获取玩家任务列表
     *
     * @param message 消息对象
     */
    @Command(TaskConstant.MsgBean.REQ_TASK)
    public void getTaskList(PlayerController playerController, ReqTaskList message) {
        Player player = playerController.getPlayer();
        if (player == null) {
            return;
        }
        ResTaskList resTaskList = new ResTaskList(Code.SUCCESS);
        resTaskList.setType(message.getType());
        try {
            List<Task> taskList = taskService.getPlayerTaskList(player.getId(), message.getType());
            resTaskList.setTaskList(taskList);
        } catch (Exception e) {
            log.error("获取玩家任务类型[{}]的列表出错!", message.getType(), e);
            resTaskList.code = Code.EXCEPTION;
        }
        playerController.send(resTaskList);
        log.debug("玩家获取任务列表 playerId = {},res = {}", player.getId(), JSON.toJSONString(resTaskList));
    }

    /**
     * 领取任务奖励
     */
    @Command(TaskConstant.MsgBean.REQ_TASK_REWARD)
    public void receiveAward(PlayerController playerController, ReqReceiveTaskAward message) {
        Player player = playerController.getPlayer();
        if (player == null) {
            return;
        }
        int taskId = message.getTaskId();
        boolean isSuccess = taskService.receiveTask(player.getId(), taskId);
        int code = isSuccess ? Code.SUCCESS : Code.FAIL;
        ResReceiveTaskAward resReceiveTaskAward = new ResReceiveTaskAward(code);
        resReceiveTaskAward.setTaskId(taskId);
        playerController.send(resReceiveTaskAward);
    }


}