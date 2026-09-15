package com.jjg.game.sim.service;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.service.SpecialGuestDailyCountService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.VisitorGenPaidCfg;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 处理现金购买特殊游客的充值到账。
 *
 * <p>订单的 productId 保存 {@link VisitorGenPaidCfg} 配置 ID，desc 保存下单场景 ID 和展示轮次；
 * 额度只在创建订单时检查，到账成功后累计购买次数，不因额度变化拒绝发放。</p>
 */
@Service
public class SpecialGuestOrderService implements OrderGenerate {
    private static final Logger log = LoggerFactory.getLogger(SpecialGuestOrderService.class);
    private static final int COST_CASH = 2;
    private static final String SIM_NODE_TABLE = "simnode";

    private final SpecialGuestDailyCountService dailyCountService;
    @Autowired
    private SimGuestService simGuestService;
    @Autowired
    private SimPlayerContextRegistry simPlayerContextRegistry;

    public SpecialGuestOrderService(SpecialGuestDailyCountService dailyCountService) {
        this.dailyCountService = dailyCountService;
    }

    @Override
    public CommonResult<BigDecimal> generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        // 特殊游客必须先经过模拟经营购买接口校验当前展示商品，禁止通用下单接口绕过游客列表。
        log.warn("通用下单接口禁止创建特殊游客订单 playerId={},productId={}",
                player.getId(), req.productId);
        return new CommonResult<>(Code.FAIL);
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.BUY_GUEST;
    }

    /**
     * 充值到账后将游客写入下单场景。配置无效或场景写入失败时返回 false，让订单到账流程按既有机制重试。
     */
    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType()) {
            return true;
        }
        SimPlayerContext context = simPlayerContextRegistry.getContext(player.getId());
        if (context == null) {
            return false;
        }
        VisitorGenPaidCfg cfg = getCashCfg(order.getProductId());
        if (cfg == null) {
            log.error("购买特殊游客到账失败，配置错误 playerId={},orderId={},productId={}",
                    player.getId(), order.getId(), order.getProductId());
            return false;
        }

        int code = simGuestService.invitePurchasedSpecialGuest(context, cfg.getVisitorID(), cfg.getVisitorCount());
        if (code != Code.SUCCESS) {
            log.info("现金购买特殊游客失败 playerId={},orderId={},cfgId={},visitorItemId={},visitorCount={},code={}",
                    player.getId(), order.getId(), cfg.getId(), cfg.getVisitorID(), cfg.getVisitorCount(), code);
            return false;
        }
        simGuestService.recordCashSpecialGuestPurchase(context, order, cfg.getId());
        dailyCountService.addPaidCount(player.getId(), cfg.getId());
        log.info("现金购买特殊游客到账成功 playerId={},orderId={},cfgId={},visitorItemId={},visitorCount={}",
                player.getId(), order.getId(), cfg.getId(), cfg.getVisitorID(), cfg.getVisitorCount());
        return true;
    }


    private VisitorGenPaidCfg getCashCfg(String productId) {
        try {
            VisitorGenPaidCfg cfg = GameDataManager.getVisitorGenPaidCfg(Integer.parseInt(productId));
            if (cfg == null || cfg.getCostType() != COST_CASH || cfg.getPriceValue1() <= 0
                    || cfg.getVisitorID() <= 0 || cfg.getVisitorCount() <= 0) {
                return null;
            }
            return cfg;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
