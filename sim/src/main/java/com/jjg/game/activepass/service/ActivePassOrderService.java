package com.jjg.game.activepass.service;

import com.jjg.game.activepass.data.ActivePassPurchase;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.service.OrderService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.jjg.game.activepass.ActivePassConstant.*;

/** 复用通用预下单与充值回调；预下单productId为ActivePass.id_轨道(2初级/4高级)。 */
@Service
public class ActivePassOrderService implements OrderGenerate {
    private final ActivePassService service;
    private final ActivePassConfigService config;
    private final ActivePassRouter router;
    private final SimPlayerContextRegistry contexts;
    private final OrderService orders;

    public ActivePassOrderService(ActivePassService service, ActivePassConfigService config, ActivePassRouter router,
                                   SimPlayerContextRegistry contexts, OrderService orders) {
        this.service = service; this.config = config; this.router = router;
        this.contexts = contexts; this.orders = orders;
    }

    @Override public RechargeType getRechargeType() { return RechargeType.ACTIVE_PASS; }

    @Override
    public CommonResult<BigDecimal> generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        int[] target = parse(req.productId);
        if (target == null) { return new CommonResult<>(Code.PARAM_ERROR); }
        ActivePassPurchase result = router.execute(player.getId(), null,
                () -> prepare(contexts.getContext(player.getId()), target[0], target[1]),
                rpc -> rpc.prepareActivePassOrder(player.getId(), target[0], target[1]));
        if (result == null || result.code != Code.SUCCESS) { return new CommonResult<>(result == null ? Code.EXCEPTION : result.code); }
        req.productId = result.productId;
        return new CommonResult<>(Code.SUCCESS, result.price);
    }

    public ActivePassPurchase prepare(SimPlayerContext ctx, int passId, int track) {
        if (ctx == null || (track != BASIC && track != PREMIUM)) { return new ActivePassPurchase(Code.PARAM_ERROR); }
        var data = service.ensureCurrent(ctx, System.currentTimeMillis());
        if (data == null || data.getPassId() != passId || (data.getPurchasedTracks() & track) != 0) {
            return new ActivePassPurchase(Code.FAIL);
        }
        var period = config.index().period(passId);
        int shopId = track == BASIC ? period.config().getShopRechargeListID() : period.config().getShopRechargeList1ID();
        var shop = GameDataManager.getShopRechargeListCfg(shopId);
        if (shop == null || shop.getPrice() == null || shop.getPrice().signum() <= 0) { return new ActivePassPurchase(Code.FAIL); }
        // 创建期记录先于付款；即使跨期到账，也能在原期补发奖励。
        service.saveReceipt(data);
        return new ActivePassPurchase(Code.SUCCESS, passId + "_" + track, shop.getPrice());
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType() || order.getPlayerId() != player.getId()) { return false; }
        return router.execute(player.getId(), null,
                () -> receive(contexts.getContext(player.getId()), order.getId()),
                rpc -> rpc.receiveActivePassOrder(player.getId(), order.getId()));
    }

    /** 所属节点重新读取可信订单，不接受客户端直接解锁或指定付款金额。 */
    public boolean receive(SimPlayerContext ctx, String orderId) {
        Order order = orders.getOrder(orderId);
        if (ctx == null || order == null || order.getPlayerId() != ctx.playerId()
                || order.getRechargeType() != getRechargeType() || order.getOrderStatus() != OrderStatus.PROCESSING) { return false; }
        int[] target = parse(order.getProductId());
        return target != null && service.unlock(ctx, target[0], target[1]);
    }

    private int[] parse(String value) {
        if (value == null) { return null; }
        String[] parts = value.split("_", -1);
        if (parts.length != 2) { return null; }
        try {
            int id = Integer.parseInt(parts[0]);
            int track = Integer.parseInt(parts[1]);
            return id > 0 && (track == BASIC || track == PREMIUM) ? new int[]{id, track} : null;
        } catch (NumberFormatException e) { return null; }
    }
}
