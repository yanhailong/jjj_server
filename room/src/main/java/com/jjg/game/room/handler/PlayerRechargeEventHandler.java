package com.jjg.game.room.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.concurrent.IProcessorHandler;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.NoticeBaseInfoChange;
import com.jjg.game.core.utils.MessageBuildUtil;
import com.jjg.game.core.manager.VipCheckManager;
import com.jjg.game.room.controller.AbstractGameController;
import com.jjg.game.room.data.room.GamePlayer;
import com.jjg.game.room.message.RoomMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * 玩家充值handler
 *
 * @author lm
 * @date 2025/9/29 10:37
 */
public record PlayerRechargeEventHandler(Player player, AbstractGameController<?, ?> gameController,
                                         Order order) implements IProcessorHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerRechargeEventHandler.class);

    @Override
    public void action() throws Exception {
        try {
            GamePlayer gamePlayer = gameController.getGamePlayer(player.getId());
            if (gamePlayer == null) {
                log.error("玩家充值时修改玩家vip等级失败 gamePlayer为空 playerId:{} order:{} ", player.getId(), JSON.toJSONString(order));
                return;
            }
            VipCheckManager.rechargeCheckVipLevel(gamePlayer, BigDecimal.valueOf(order.getPrice()));
            //修改vip等级和经验
            player.setVipLevel(gamePlayer.getVipLevel());
            player.setVipExp(gamePlayer.getVipExp());
            NoticeBaseInfoChange notice = MessageBuildUtil.buildNoticeBaseInfoChange(gamePlayer);
            gameController.broadcastToPlayers(RoomMessageBuilder.newBuilder().sendPlayer(gamePlayer.getId(), notice));
        } catch (Exception e) {
            log.error("玩家充值时修改玩家vip等级失败 playerId:{} order:{} ", player.getId(), JSON.toJSONString(order), e);
        }
    }
}
