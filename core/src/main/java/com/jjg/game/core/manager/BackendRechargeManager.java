package com.jjg.game.core.manager;

import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.service.PlayerPackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BackendRechargeManager implements GameEventListener {
    @Autowired
    private PlayerPackService playerPackService;

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            Order order = event.getOrder();
            Player player = event.getPlayer();
            if (order.getRechargeType() != RechargeType.BACKEND && order.getRechargeType() != RechargeType.BACKEND_CALLBACK) {
                return;
            }
            dealBackendRecharge(player, order);
        }
    }

    /**
     * 处理后台充值任务
     * @param player
     * @param order
     */
    private void dealBackendRecharge(Player player, Order order) {
        if(order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        playerPackService.addItems(player.getId(),order.getItems(), AddType.BACKEND_OPERATOR,order.getId());
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }
}
