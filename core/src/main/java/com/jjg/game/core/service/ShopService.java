package com.jjg.game.core.service;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.RechargeType;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.dao.ShopProductDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.manager.CoreSendMessageManager;
import com.jjg.game.core.pb.NotifyPayCallBack;
import com.jjg.game.core.utils.ConditionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/9/18 14:15
 */
@Component
public class ShopService {
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
    private Map<Integer, ShopProduct> shopProductMap;

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
    public List<ShopProduct> getShop(Player player) {
        if (this.shopProductMap == null || this.shopProductMap.isEmpty()) {
            return null;
        }

        List<ShopProduct> list = new ArrayList<>();

        int now = TimeHelper.nowInt();
        for (Map.Entry<Integer, ShopProduct> en : this.shopProductMap.entrySet()) {
            ShopProduct shopProduct = en.getValue();
            //检查是否开启
            if (!checkProductOpen(player, shopProduct, now)) {
                continue;
            }
            list.add(shopProduct);
        }
        return list;
    }

    /**
     * 下单
     *
     * @param player
     * @return
     */
    public CommonResult<String> generateOrder(Player player, ShopProduct shopProduct, RechargeType rechargeType) {
        CommonResult<String> result = new CommonResult<>(Code.SUCCESS);
        Order order = orderService.generateOrder(player, shopProduct.getId(),shopProduct.getMoney(),rechargeType);
        if(order == null) {
            log.info("玩家下单失败 playerId = {},productId = {}", player.getId(), shopProduct.getId());
            result.code = Code.FAIL;
            return result;
        }
        result.data = order.getId();
        return result;
    }

    /**
     * 道具兑换
     *
     * @param playerController
     * @return
     */
    public CommonResult<ItemOperationResult> exchange(PlayerController playerController, ShopProduct shopProduct) {
        Map<Integer,Long> addItemMap;
        //添加商品
        if(shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()){
            addItemMap = shopProduct.getRewardItems();
        }else {
            addItemMap = Collections.emptyMap();
        }

        CommonResult<ItemOperationResult> result = playerPackService.useItem(playerController.playerId(),shopProduct.getPayType(), shopProduct.getMoney(),addItemMap,"exchange");
        if(!result.success()){
            return result;
        }

        //如果钻石或金币有变化，则要
        if(result.data.getDiamond() > 0 || result.data.getGoldNum() > 0) {
            sendMessageManager.buildMoneyChangeMessage(playerController.playerId(),playerService);
        }
        Account account = accountDao.queryAccountByPlayerId(playerController.playerId());
        coreLogger.shop(playerController.getPlayer(),shopProduct,account.getChannel().getValue());
        return result;
    }

    /**
     * 加载商城商品
     */
    public void loadShopProducts() {
        List<ShopProduct> all = shopProductDao.getAll();
        if (all != null && !all.isEmpty()) {
            this.shopProductMap = all.stream()
                    .collect(Collectors.toUnmodifiableMap(ShopProduct::getId, Function.identity()));
            log.info("加载商城商品数量 size = {}", this.shopProductMap.size());
        }
    }

    /**
     * 通过商品id获取商品信息
     * @param productId
     * @return
     */
    public ShopProduct getShopProduct(int productId) {
        if(this.shopProductMap == null || this.shopProductMap.isEmpty()) {
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
        return true;
    }
}
