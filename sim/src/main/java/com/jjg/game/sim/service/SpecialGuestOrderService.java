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
import com.jjg.game.sampledata.bean.VisitorQualityAcquisitionCfg;
import com.jjg.game.sampledata.bean.VisitorTargetListCfg;
import com.jjg.game.sim.constant.SimConstant;
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
 * <p>订单的 productId 保存商品配置 ID，desc 保存下单场景 ID、展示轮次和卡池配置 ID；
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
        int cfgId;
        VisitorTargetListCfg poolCfg;
        try {
            cfgId = Integer.parseInt(order.getProductId());
            poolCfg = GameDataManager.getVisitorTargetListCfg(Integer.parseInt(order.getDesc().split(":")[2]));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            log.error("购买特殊游客订单参数错误 playerId={},orderId={}", player.getId(), order.getId(), e);
            return false;
        }
        if (poolCfg == null) {
            log.error("购买特殊游客到账失败，配置错误 playerId={},orderId={},productId={}",
                    player.getId(), order.getId(), order.getProductId());
            return false;
        }

        int code;
        if (poolCfg.getPoolType() == SimConstant.SpecialGuest.POOL_PAID) {
            VisitorGenPaidCfg cfg = getCashCfg(cfgId);
            if (cfg == null) {
                return false;
            }
            code = simGuestService.invitePurchasedSpecialGuest(context, cfg.getVisitorID(), cfg.getVisitorCount());
        } else if (poolCfg.getPoolType() == SimConstant.SpecialGuest.POOL_QUALITY) {
            VisitorQualityAcquisitionCfg cfg = GameDataManager.getVisitorQualityAcquisitionCfg(cfgId);
            if (cfg == null || cfg.getCostType() != COST_CASH || cfg.getPriceValue1() <= 0) {
                return false;
            }
            code = simGuestService.inviteQualitySpecialGuests(context, cfg);
        } else {
            return false;
        }
        if (code != Code.SUCCESS) {
            log.info("现金购买特殊游客失败 playerId={},orderId={},poolId={},cfgId={},code={}",
                    player.getId(), order.getId(), poolCfg.getId(), cfgId, code);
            return false;
        }
        simGuestService.recordCashSpecialGuestPurchase(context, order, cfgId);
        dailyCountService.addPaidCount(player.getId(), poolCfg.getPoolType(), cfgId);
        log.info("现金购买特殊游客到账成功 playerId={},orderId={},poolId={},cfgId={}",
                player.getId(), order.getId(), poolCfg.getId(), cfgId);
        return true;
    }


    private VisitorGenPaidCfg getCashCfg(int cfgId) {
        VisitorGenPaidCfg cfg = GameDataManager.getVisitorGenPaidCfg(cfgId);
        if (cfg == null || cfg.getCostType() != COST_CASH || cfg.getPriceValue1() <= 0
                || cfg.getVisitorID() <= 0 || cfg.getVisitorCount() <= 0) {
            return null;
        }
        return cfg;
    }
}
