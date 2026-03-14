package com.jjg.game.core.manager;

import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.pb.RechargeType;
import org.springframework.stereotype.Component;

@Component
public class BackendCallBackRechargeManager extends BackendRechargeManager {


    @Override
    public RechargeType getRechargeType() {
        return RechargeType.BACKEND_CALLBACK;
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType()) {
            return true;
        }
        return dealBackendRecharge(player, order);
    }
}
