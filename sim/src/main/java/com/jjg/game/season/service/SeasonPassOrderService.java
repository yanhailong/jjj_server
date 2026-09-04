package com.jjg.game.season.service;

import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.sampledata.bean.ShopRechargeListCfg;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.sim.data.SimPlayerContext;
import com.jjg.game.sim.manager.SimPlayerContextRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 通行证订单适配器。预下单 productId 使用 passId_track，创建订单时转换为带赛季范围的业务标识。
 */
@Service
public class SeasonPassOrderService implements OrderGenerate {
    private static final Logger log = LoggerFactory.getLogger(SeasonPassOrderService.class);

    private final SimPlayerContextRegistry contextRegistry;
    private final SeasonLifecycleService lifecycleService;
    private final SeasonPassConfigService passConfig;
    private final SeasonPassService passService;

    public SeasonPassOrderService(SimPlayerContextRegistry contextRegistry,
                                  SeasonLifecycleService lifecycleService,
                                  SeasonPassConfigService passConfig,
                                  SeasonPassService passService) {
        this.contextRegistry = contextRegistry;
        this.lifecycleService = lifecycleService;
        this.passConfig = passConfig;
        this.passService = passService;
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        SimPlayerContext ctx = contextRegistry.getContext(player.getId());
        if (ctx == null) {
            return null;
        }
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPlayerData data = ctx.getSeasonPlayerData();
        PurchaseTarget target = parsePurchaseTarget(req.productId);
        SeasonPassConfigService.PassDefinition pass = target == null ? null
                : passConfig.activePass(data.getSeasonId(), target.passId());
        if (pass == null || !passService.hasTrackRewards(pass, target.track())) {
            return null;
        }
        int purchased = pass.followsSeason()
                ? data.getPassPurchasedTracks().getOrDefault(pass.id(), 0)
                : ctx.getSimBaseData().getNonSeasonPassPurchasedTracks().getOrDefault(pass.id(), 0);
        if ((purchased & SeasonPassService.trackBit(target.track())) != 0) {
            return null;
        }
        ShopRechargeListCfg shop = passConfig.shop(pass, target.track());
        String channelProductId = shop == null ? null : SeasonPassService.channelProductId(player, shop);
        if (shop == null || shop.getPrice() == null || shop.getPrice().signum() <= 0
                || channelProductId == null || channelProductId.isBlank()) {
            return null;
        }
        req.productId = passService.orderToken(data, pass, target.track());
        return shop.getPrice();
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.SEASON_PASS;
    }

    @Override
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != getRechargeType()) {
            return true;
        }
        if (order.getPlayerId() != player.getId()) {
            log.error("通行证充值订单玩家不匹配 orderId={},orderPlayerId={},playerId={}",
                    order.getId(), order.getPlayerId(), player.getId());
            return false;
        }
        SimPlayerContext ctx = contextRegistry.getContext(player.getId());
        if (ctx == null) {
            return false;
        }
        lifecycleService.ensureCurrent(ctx, System.currentTimeMillis());
        SeasonPassService.PurchaseToken token = passService.parseOrderToken(order.getProductId());
        boolean success = passService.unlock(ctx, token);
        if (!success) {
            log.error("通行证充值到账失败 playerId={},orderId={},token={}",
                    player.getId(), order.getId(), order.getProductId());
        }
        return success;
    }

    private PurchaseTarget parsePurchaseTarget(String productId) {
        if (productId == null || productId.isBlank()) {
            return null;
        }
        int separator = productId.indexOf('_');
        if (separator <= 0 || separator != productId.lastIndexOf('_')
                || separator >= productId.length() - 1) {
            return null;
        }
        try {
            int passId = Integer.parseInt(productId.substring(0, separator));
            int track = Integer.parseInt(productId.substring(separator + 1));
            return passId > 0 && SeasonPassService.validTrack(track)
                    ? new PurchaseTarget(passId, track) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private record PurchaseTarget(int passId, int track) {
    }
}
