package com.jjg.game.core.service;

import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.ShopProductDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.CoreSendMessageManager;
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
public class ShopService implements OrderGenerate {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ShopProductDao shopProductDao;
    @Autowired
    private OrderService orderService;
    @Autowired
    private PlayerPackService playerPackService;
    @Autowired
    private CorePlayerService playerService;
    @Autowired
    private CoreSendMessageManager sendMessageManager;
    @Autowired
    private PlayerSessionService playerSessionService;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private AccountDao accountDao;

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

        CommonResult<ItemOperationResult> result = playerPackService.useItem(playerController.playerId(), shopProduct.getPayType(), shopProduct.getMoney().intValue() * count, addItemMap, AddType.ITEM_EXCHANGE);
        if (!result.success()) {
            return result;
        }

        //如果钻石或金币有变化，则要
        if (result.data.getDiamond() > 0 || result.data.getGoldNum() > 0) {
            sendMessageManager.buildMoneyChangeMessage(playerController.playerId(), playerService);
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
        if (shopProduct == null || !checkProductOpen(player, shopProduct)) {
            log.debug("获取商品为空或则商品未开启 playerId = {}, shopProductId = {}", player, shopProductId);
            return null;
        }
        String channelProductId = shopProduct.channelProductId(player.getChannel().getValue());
        if (channelProductId == null) {
            log.debug("获取商品的渠道商品id为空 playerId = {}, shopProductId = {}", player, shopProductId);
            return null;
        }
        return shopProduct.getMoney();
    }

    @Override
    public RechargeType getRechargeType() {
        return RechargeType.SHOP;
    }
}
