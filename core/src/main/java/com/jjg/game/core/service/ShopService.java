package com.jjg.game.core.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.PlayerSessionTokenDao;
import com.jjg.game.core.dao.ShopProductDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.listener.OrderGenerate;
import com.jjg.game.core.logger.CoreLogger;
import com.jjg.game.core.pb.NotifyPayCallBack;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.pb.ReqGenerateOrder;
import com.jjg.game.core.utils.ConditionUtil;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.ViplevelCfg;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
    private PlayerPackService playerPackService;
    @Autowired
    private CoreLogger coreLogger;
    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private MailService mailService;
    @Autowired
    private PlayerSessionTokenDao playerSessionTokenDao;
    //商城商品
    private Map<Long, ShopProduct> shopProductMap = Map.of();

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
            if (count > 1) {
                addItemMap = new HashMap<>(shopProduct.getRewardItems().size());
                shopProduct.getRewardItems().forEach((k, v) -> {
                    addItemMap.put(k, v * count);
                });
            } else {
                addItemMap = shopProduct.getRewardItems();
            }
        } else {
            addItemMap = Collections.emptyMap();
        }

        long useItemCount = shopProduct.getMoney().longValue() * count;
        CommonResult<ItemOperationResult> result = playerPackService.useItem(playerController.playerId(), shopProduct.getPayType(), useItemCount, addItemMap, AddType.ITEM_EXCHANGE);
        if (!result.success()) {
            return result;
        }

        PlayerSessionToken playerSessionToken = playerSessionTokenDao.getByPlayerId(playerController.playerId());
        int registerChannel = ChannelType.GOOGLE.getValue();
        if (playerSessionToken != null) {
            registerChannel = playerSessionToken.getRegisterChannel();
        }
        coreLogger.shop(playerController.getPlayer(), shopProduct, registerChannel, useItemCount);
        return result;
    }

    /**
     * 加载商城商品
     */
    public void loadShopProducts() {
        List<ShopProduct> all = shopProductDao.getAll();
        if (CollectionUtil.isNotEmpty(all)) {
            this.shopProductMap = all.stream().collect(Collectors.toUnmodifiableMap(ShopProduct::getId, Function.identity()));
            log.info("加载商城商品数量 size = {}", this.shopProductMap.size());
        } else {
            this.shopProductMap = Map.of();
            log.info("清空商城商品");
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
            log.debug("获取商品为空 playerId = {}, shopProductId = {}", player.getId(), shopProductId);
            return null;
        }

        if (!checkProductOpen(player, shopProduct)) {
            log.debug("商品未开启 playerId = {}, shopProductId = {}", player.getId(), shopProductId);
            return null;
        }
        ChannelType channel = player.getChannel();
        if (channel == null) {
            log.error("玩家channel为空 playerId = {}, shopProductId = {}", player.getId(), shopProductId);
            return null;
        }
        String channelProductId = shopProduct.channelProductId(player.getChannel().getValue());
        if (channelProductId == null) {
            log.debug("获取商品的渠道商品id为空 playerId = {}, shopProductId = {}", player.getId(), shopProductId);
            return null;
        }
        return shopProduct.getMoney();
    }

    /**
     * 处理商城订单
     *
     * @param player     玩家数据
     * @param order      订单数据
     * @param money      实际支付金额
     * @param regionCode 地区代码
     */
    public boolean handleShopOrder(Player player, Order order, String money, String regionCode, String channelProductId) {
        ShopProduct shopProduct = getShopProduct(Long.parseLong(order.getProductId()));
        if (shopProduct == null) {
            log.error("未找到商品配置 orderId = {},productId = {}", order.getId(), order.getProductId());
            return false;
        }
        List<ItemInfo> itemInfoList = null;
        if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
            CommonResult<ItemOperationResult> addItemsResult = playerPackService.addItems(order.getPlayerId(), shopProduct.getRewardItems(), AddType.SHOP_RECHARGE, order.getId());
            if (!addItemsResult.success()) {
                log.error("商城发奖失败 playerId = {},orderId = {},productId = {},code = {}",
                        order.getPlayerId(), order.getId(), shopProduct.getId(), addItemsResult.code);
                return false;
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
        try {
            coreLogger.shop(player, order, shopProduct, money, channelProductId, regionCode);
        } catch (Exception e) {
            log.error("记录商城充值日志失败 playerId = {},orderId = {}", order.getPlayerId(), order.getId(), e);
        }
        try {
            // 通知和扩展奖励不影响订单主成功链路
            notifyPlayerRechargeCallBack(player, order, itemInfoList);
        } catch (Exception e) {
            log.error("通知玩家商城充值成功失败 playerId = {},orderId = {}", order.getPlayerId(), order.getId(), e);
        }
        try {
            dealVipPrivileged(player, shopProduct);
        } catch (Exception e) {
            log.error("处理商城充值vip特权失败 playerId = {},orderId = {}", order.getPlayerId(), order.getId(), e);
        }
        return true;
    }

    /**
     * 处理vip特权
     *
     * @param player      玩家数据
     * @param shopProduct 商品数据
     */
    private void dealVipPrivileged(Player player, ShopProduct shopProduct) {
        if (shopProduct.getPayType() == -1) {
            int currencyItemId = ItemUtils.isAllCurrencyItems(shopProduct.getRewardItems());
            if (currencyItemId > 0) {
                Optional<ViplevelCfg> viplevelCfgOptional = GameDataManager.getViplevelCfgList().stream()
                        .filter(cfg -> cfg.getViplevel() == player.getVipLevel())
                        .findFirst();
                if (viplevelCfgOptional.isPresent()) {
                    Map<Integer, Integer> privilegedFunctions = viplevelCfgOptional.get().getPrivilegedFunctions();
                    if (privilegedFunctions != null && !privilegedFunctions.isEmpty()) {
                        Integer add = privilegedFunctions.get(2);
                        if (add != null && add > 0) {
                            int magnification = 0;
                            int mailId = 0;
                            AddType addType = AddType.VIP_REWARDS;
                            if (currencyItemId == ItemUtils.getDiamondItemId()) {
                                GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(54);
                                if (globalConfigCfg != null) {
                                    magnification = globalConfigCfg.getIntValue();
                                    mailId = 37;
                                    addType = AddType.VIP_REWARDS_DIAMOND;
                                }
                            } else if (currencyItemId == ItemUtils.getGoldItemId()) {
                                GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(52);
                                if (globalConfigCfg != null) {
                                    magnification = globalConfigCfg.getIntValue();
                                    mailId = 36;
                                    addType = AddType.VIP_REWARDS_GOLD;
                                }
                            }
                            if (magnification > 0) {
                                BigDecimal addNum = shopProduct.getMoney().multiply(BigDecimal.valueOf(add))
                                        .multiply(BigDecimal.valueOf(magnification)
                                                .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN));
                                if (addNum.longValue() > 0) {
                                    List<LanguageParamData> languageParamData = new ArrayList<>();
                                    languageParamData.add(new LanguageParamData(0, shopProduct.getMoney().toPlainString()));
                                    languageParamData.add(new LanguageParamData(0, String.valueOf(player.getVipLevel())));
                                    languageParamData.add(new LanguageParamData(0, NumberUtil.decimalFormat("#.##%", BigDecimal.valueOf(add).divide(BigDecimal.valueOf(10000), 4, RoundingMode.DOWN))));
                                    languageParamData.add(new LanguageParamData(0, String.valueOf(NumberUtil.decimalFormat(",##0", addNum))));
                                    mailService.addCfgMail(player.getId(), mailId, List.of(new Item(currencyItemId, addNum.longValue())), languageParamData, addType);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 通知玩家充值成功
     *
     * @param player       玩家数据
     * @param order        订单
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
    public boolean onReceivedRecharge(Player player, Order order) {
        if (order.getRechargeType() != RechargeType.SHOP) {
            return true;
        }
        //获取商品
        return handleShopOrder(player, order, order.getMoney(), order.getRegionCode(), order.getChannelProductId());
    }

    @Override
    public boolean isContinue(Order order) {
        return StringUtils.isBlank(order.getDesc());
    }
}
