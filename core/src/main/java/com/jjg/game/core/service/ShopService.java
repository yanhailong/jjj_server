package com.jjg.game.core.service;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEventCategory;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.ShopProductDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.pb.NotifyPayCallBack;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.utils.ConditionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/9/18 14:15
 */
@Component
public class ShopService implements OrderGenerate, GameEventListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ShopProductDao shopProductDao;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private CoreSendMessageManager sendMessageManager;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private AccountDao accountDao;
    @Autowired
    private ClusterSystem clusterSystem;
    //商城商品
    private Map<Long, ShopProduct> shopProductMap;

    public void init() {
        //加载商城商品
        loadShopProducts();
    }

    /**
     * 获取商城
     *
     * @param player
     * @return
     */
    public List<ShopProduct> getShop(Player player, int channel) {
        if (this.shopProductMap == null || this.shopProductMap.isEmpty()) {
            return null;
        }

        List<ShopProduct> list = new ArrayList<>();

        int now = TimeHelper.nowInt();
        for (Map.Entry<Long, ShopProduct> en : this.shopProductMap.entrySet()) {
            ShopProduct shopProduct = en.getValue();
            //检查是否开启
            if (!checkProductOpen(player, shopProduct, now, channel)) {
                continue;
            }
            list.add(shopProduct);
        }
        return list;
    }


    /**
     * 道具兑换
     *
     * @param playerController
     * @return
     */
    public CommonResult<ItemOperationResult> exchange(PlayerController playerController, ShopProduct shopProduct, int count) {
        Map<Integer, Long> addItemMap;
        //添加商品
        if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
            addItemMap = shopProduct.getRewardItems();
        } else {
            addItemMap = Collections.emptyMap();
        }

        CommonResult<ItemOperationResult> result = playerPackService.useItem(playerController.playerId(), shopProduct.getPayType(), shopProduct.getMoney().longValue(), addItemMap, AddType.ITEM_EXCHANGE);
        if (!result.success()) {
            return result;
        }

        Account account = accountDao.queryAccountByPlayerId(playerController.playerId());
        coreLogger.shop(playerController.getPlayer(), shopProduct, account.getChannel().getValue());
        return result;
    }

    /**
     * 加载商城商品
     */
    public void loadShopProducts() {
        List<ShopProduct> all = shopProductDao.getAll();
        if (all != null && !all.isEmpty()) {
            this.shopProductMap = all.stream().collect(Collectors.toUnmodifiableMap(ShopProduct::getId, Function.identity()));
            log.info("加载商城商品数量 size = {}", this.shopProductMap.size());
        }
    }

    /**
     * 通过商品id获取商品信息
     *
     * @param productId
     * @return
     */
    public ShopProduct getShopProduct(long productId) {
        if (this.shopProductMap == null || this.shopProductMap.isEmpty()) {
            return null;
        }
        return this.shopProductMap.get(productId);
    }

    /**
     * 检查商品条件，是否开启
     *
     * @param player
     * @param shopProduct
     * @return
     */
    public boolean checkProductOpen(Player player, ShopProduct shopProduct) {
        return checkProductOpen(player, shopProduct, TimeHelper.nowInt());
    }

    /**
     * 检查商品条件，是否开启
     *
     * @param player
     * @param shopProduct
     * @return
     */
    public boolean checkProductOpen(Player player, ShopProduct shopProduct, int now) {
        int channel = ChannelType.GOOGLE.getValue();
        if (player.getChannel() != null) {
            channel = player.getChannel().getValue();
        }

        return checkProductOpen(player, shopProduct, now, channel);
    }

    /**
     * 检查商品条件，是否开启
     *
     * @param player
     * @param shopProduct
     * @return
     */
    public boolean checkProductOpen(Player player, ShopProduct shopProduct, int now, int channel) {
        //是否开启
        if (!shopProduct.isOpen()) {
            return false;
        }

        //检查解锁条件
        if (ConditionUtil.checkCondition(player, shopProduct.getConditionsMap()) != Code.SUCCESS) {
            return false;
        }

        //检查开始时间
        if (shopProduct.getStartTime() > 0 && now < shopProduct.getStartTime()) {
            return false;
        }

        //检查开始时间
        if (shopProduct.getEndTime() > 0 && now > shopProduct.getEndTime()) {
            return false;
        }

        if (shopProduct.getChannelProductIdMap() == null) {
            return false;
        }
        return shopProduct.getChannelProductIdMap().containsKey(channel);
    }

    @Override
    public BigDecimal generateOrderDetailInfo(Player player, ReqGenerateOrder req) {
        long shopProductId = Long.parseLong(req.productId);
        ShopProduct shopProduct = shopProductMap.get(shopProductId);
        if (shopProduct == null) {
            log.debug("获取商品为空 playerId = {}, shopProductId = {}", player, shopProductId);
            return null;
        }

        if (!checkProductOpen(player, shopProduct)) {
            log.debug("商品未开启 playerId = {}, shopProductId = {}", player, shopProductId);
            return null;
        }

        String channelProductId = shopProduct.channelProductId(player.getChannel().getValue());
        if (channelProductId == null) {
            log.debug("获取商品的渠道商品id为空 playerId = {}, shopProductId = {}", player, shopProductId);
            return null;
        }
        return shopProduct.getMoney();
    }

    /**
     * 处理商城订单
     * @param player 玩家数据
     * @param order 订单数据
     * @param money 实际支付金额
     * @param regionCode 地区代码
     */
    private void handleShopOrder(Player player, Order order, String money, String regionCode) {
        ShopProduct shopProduct = getShopProduct(Long.parseLong(order.getProductId()));
        if (shopProduct == null) {
            log.error("未找到该商品 orderId = {},productId = {}", order.getId(), order.getProductId());
            return;
        }
        List<ItemInfo> itemInfoList = null;
        if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
            CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(order.getPlayerId(), shopProduct.getRewardItems(), AddType.RECHARGE, order.getId());
            if (!addItemsResult.success()) {
                log.warn("支付成功，但是添加道具失败 playerId = {},orderId = {},productId = {},code = {}", order.getPlayerId(), order.getId(), shopProduct.getId(), addItemsResult.code);
            } else {
                itemInfoList = new ArrayList<>();
                for (Map.Entry<Integer, Long> en : shopProduct.getRewardItems().entrySet()) {
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.itemId = en.getKey();
                    itemInfo.count = en.getValue();
                    itemInfoList.add(itemInfo);
                }
                log.debug("商城充值后添加道具成功 playerId = {},orderId = {}", order.getPlayerId(), order.getId());
            }
        }
        coreLogger.shop(player, order, shopProduct, money, regionCode);
        //通知玩家充值成功
        notifyPlayerRechargeCallBack(player, order, itemInfoList);
    }

    /**
     * 通知玩家充值成功
     * @param player 玩家数据
     * @param order 订单
     * @param itemInfoList 奖励信息
     */
    private void notifyPlayerRechargeCallBack(Player player, Order order, List<ItemInfo> itemInfoList) {
        NotifyPayCallBack notify = new NotifyPayCallBack();
        notify.orderId = order.getId();
        notify.items = itemInfoList;
        clusterSystem.sendToPlayer(notify, player.getId());
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.SHOP;
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEventCategory.PlayerRechargeEvent event) {
            Order order = event.getOrder();
            Player player = event.getPlayer();
            if (order.getRechargeType() == RechargeType.SHOP) {
                //获取商品
                handleShopOrder(player, order, event.getMoney(), event.getRegionCode());
            }
        }
    }

    @Override
    public Map<EGameEventType, Object> getSubTypeMap() {
        return Map.of(EGameEventType.RECHARGE, getRechargeType());
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.RECHARGE);
    }
}
