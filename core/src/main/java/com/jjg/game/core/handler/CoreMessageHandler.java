package com.jjg.game.core.handler;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.pb.ReqGm;
import com.jjg.game.core.pb.ResGm;
import com.jjg.game.core.service.CorePlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * @author 11
 * @date 2025/6/11 16:09
 */
@Component
@MessageType(MessageConst.MessageTypeDef.CORE_MESSAGE_TYPE)
public class CoreMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private CoreSendMessageManager coreSendMessageManager;
    @Autowired
    private CoreLogger coreLogger;

    /**
     * @param req
     */
    @Command(MessageConst.CoreMessage.REQ_GM)
    public void reqGm(ReqGm req) {
        log.info("收到gm消息 playerId:{} order:{}", req.playerId, req.order);
        try {
            if (!nodeConfig.isGm()) {
                coreLogger.gmOrder(req.order, req.playerId, "gm功能已经关闭");
                log.info("gm功能已经关闭 playerId:{} order:{}", req.playerId, req.order);
                return;
            }

            if (req.order.isEmpty()) {
                coreLogger.gmOrder(req.order, req.playerId, "参数错误，使用gm失败");
                log.info("参数错误，使用gm失败 playerId:{} order:{}", req.playerId, req.order);
                return;
            }

            String[] arr = req.order.split(" ");
            if (arr.length < 1) {
                coreLogger.gmOrder(req.order, req.playerId, "参数错误2，使用gm失败");
                log.info("参数错误2，使用gm失败 playerId = {},order = {}", req.playerId, req.order);
                return;
            }

            String cmd = arr[0];
            String params = arr.length > 1 ? arr[1] : null;

            Map<String, GmListener> map = CommonUtil.getContext().getBeansOfType(GmListener.class);
            //执行结果
            Pair<Boolean, String> result = null;
            for (GmListener listener : map.values()) {
                result = listener.gm(req.playerId, cmd, params);
                if (Objects.nonNull(result) && result.getFirst()) {
                    break;
                }
            }
            if (Objects.isNull(result)) {
                coreLogger.gmOrder(req.order, req.playerId, "执行gm命令失败,未找到处理逻辑");
                log.info("执行gm命令失败 playerId = {},order = {}", req.playerId, req.order);
            } else {
                coreLogger.gmOrder(req.order, req.playerId, result.getSecond());
                log.info("执行gm命令成功 playerId = {},order = {} result = {}", req.playerId, req.order, result.getSecond());
            }
        } catch (Exception e) {
            log.error("", e);
        }

    }

    /**
     * gm修改金币
     *
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    public void addGold(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        Long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addGold(playerController.playerId(), num, "gmAdd", null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }

        coreSendMessageManager.packMoneyChangeMessage(playerController, result.data.getGold(), result.data.getDiamond());
    }

    /**
     * gm修改钻石
     *
     * @param res
     * @param playerController
     * @param order
     * @param params
     * @throws Exception
     */
    public void addDiamond(ResGm res, PlayerController playerController, String order, String params) throws Exception {
        if (params == null || params.isEmpty()) {
            res.code = Code.PARAM_ERROR;
            log.debug("params为空，使用gm失败 playerId = {},order = {}", playerController.playerId(), order);
            return;
        }

        Long num = Long.parseLong(params);
        CommonResult<Player> result = playerService.addDiamond(playerController.playerId(), num, "gmAdd", null);
        if (!result.success()) {
            res.code = result.code;
            log.debug("使用gm失败 playerId = {},order = {},code = {}", playerController.playerId(), order, result.code);
            return;
        }
        coreSendMessageManager.packMoneyChangeMessage(playerController, result.data.getGold(), result.data.getDiamond());
    }
}
