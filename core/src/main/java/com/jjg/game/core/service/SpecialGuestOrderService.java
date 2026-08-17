package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.rpc.ClusterRpcReference;
import com.jjg.game.common.rpc.GameRpcContext;
import com.jjg.game.common.rpc.RpcReqParameterBuilder;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.Order;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.rpc.SpecialGuestBridge;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.VisitorGenPaidCfg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 处理现金购买特殊游客的充值到账。
 *
 * <p>订单的 productId 保存 {@link VisitorGenPaidCfg} 配置 ID，desc 保存下单场景 ID；
 * 只有游客写入该场景成功后才增加玩家当天的全局购买次数。</p>
 */
@Service
public class SpecialGuestOrderService implements OrderGenerate {
    private static final Logger log = LoggerFactory.getLogger(SpecialGuestOrderService.class);
    private static final int COST_CASH = 2;
    private static final String SIM_NODE_TABLE = "simnode";

    private final SpecialGuestDailyCountService dailyCountService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ClusterSystem clusterSystem;
    @ClusterRpcReference
    private SpecialGuestBridge specialGuestBridge;

    public SpecialGuestOrderService(SpecialGuestDailyCountService dailyCountService) {
        this.dailyCountService = dailyCountService;
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        // 特殊游客必须先经过模拟经营购买接口校验当前展示商品，禁止通用下单接口绕过游客列表。
        log.warn("通用下单接口禁止创建特殊游客订单 playerId={},productId={}",
                player.getId(), req.productId);
        return null;
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
        VisitorGenPaidCfg cfg = getCashCfg(order.getProductId());
        if (cfg == null) {
            log.error("购买特殊游客到账失败，配置错误 playerId={},orderId={},productId={}",
                    player.getId(), order.getId(), order.getProductId());
            return false;
        }
        int casinoId = getCasinoId(order.getDesc());
        if (casinoId <= 0) {
            log.error("购买特殊游客到账失败，下单场景错误 playerId={},orderId={},desc={}",
                    player.getId(), order.getId(), order.getDesc());
            return false;
        }
        if (!deliverToCasino(player, order, casinoId, cfg)) {
            return false;
        }
        int dailyBuyCount = dailyCountService.addPaidCount(player.getId(), cfg.getId());
        log.info("现金购买特殊游客到账成功 playerId={},orderId={},cfgId={},visitorItemId={},visitorCount={},dailyBuyCount={}",
                player.getId(), order.getId(), cfg.getId(), cfg.getVisitorID(), cfg.getVisitorCount(), dailyBuyCount);
        return true;
    }

    /** 将到账请求路由到玩家 SIM 会话所在的 Hall，由 Hall 更新对应场景内存并立即落库。 */
    private boolean deliverToCasino(Player player, Order order, int casinoId, VisitorGenPaidCfg cfg) {
        ClusterClient client = null;
        String ownerPath = (String) redisTemplate.opsForHash().get(SIM_NODE_TABLE, player.getId());
        if (ownerPath != null && !ownerPath.isEmpty()) {
            client = clusterSystem.getClusterByPath(ownerPath);
        }
        if (client == null) {
            client = clusterSystem.getByNodeType(NodeType.HALL, player.getIp(), player.getId());
        }
        if (client == null) {
            log.error("购买特殊游客到账失败，未找到Hall节点 playerId={},orderId={},casinoId={}",
                    player.getId(), order.getId(), casinoId);
            return false;
        }

        GameRpcContext rpcContext = GameRpcContext.getContext();
        RpcReqParameterBuilder previous = rpcContext.getReqParameterBuilder();
        try {
            rpcContext.withReqParameterBuilder(RpcReqParameterBuilder.create()
                    .addClusterClient(client).setTryMillisPerClient(2000));
            CommonResult<Boolean> result = specialGuestBridge.receiveSpecialGuest(
                    player.getId(), casinoId, cfg.getVisitorID(), cfg.getVisitorCount(), order.getId());
            if (result == null || !result.success() || !Boolean.TRUE.equals(result.data)) {
                log.error("购买特殊游客到账写入场景失败 playerId={},orderId={},casinoId={},cfgId={},code={}",
                        player.getId(), order.getId(), casinoId, cfg.getId(), result == null ? null : result.code);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("购买特殊游客到账调用Hall异常 playerId={},orderId={},casinoId={},cfgId={}",
                    player.getId(), order.getId(), casinoId, cfg.getId(), e);
            return false;
        } finally {
            rpcContext.setReqParameterBuilder(previous);
        }
    }

    private int getCasinoId(String desc) {
        try {
            return Integer.parseInt(desc);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private VisitorGenPaidCfg getCashCfg(String productId) {
        try {
            VisitorGenPaidCfg cfg = GameDataManager.getVisitorGenPaidCfg(Integer.parseInt(productId));
            if (cfg == null || cfg.getCostType() != COST_CASH || cfg.getPriceValue1() == null
                    || cfg.getPriceValue1().signum() <= 0 || cfg.getVisitorID() <= 0 || cfg.getVisitorCount() <= 0) {
                return null;
            }
            return cfg;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
