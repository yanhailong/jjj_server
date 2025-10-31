package com.jjg.game.core.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.ShopConstant;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.*;
import com.jjg.game.core.service.ShopService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 11
 * @date 2025/9/18 14:12
 */
@Component
@MessageType(MessageConst.MessageTypeDef.SHOP_TYPE)
public class ShopMessageHandler {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ShopService shopService;

    /**
     * 获取商城
     */
    @Command(ShopConstant.MsgBean.REQ_SHOP)
    public void reqShop(PlayerController playerController, ReqShop req) {
        ResShop res = new ResShop(Code.SUCCESS);
        List<ShopProduct> shopProductList = shopService.getShop(playerController.getPlayer(), req.channel);
        if (shopProductList != null && !shopProductList.isEmpty()) {
            res.shopProductInfoList = new ArrayList<>(shopProductList.size());
            shopProductList.forEach((shopProduct) -> {
                ShopProductInfo info = new ShopProductInfo();
                info.id = shopProduct.getId();
                info.type = shopProduct.getType();
                info.endTime = shopProduct.getEndTime();
                info.valueType = shopProduct.getValueType();
                info.showValue = shopProduct.getValue();

                if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
                    Long show = shopProduct.getRewardItems().get(shopProduct.getValueType());
                    if (show != null) {
                        info.value = show;
                    }

                    List<ItemInfo> items = new ArrayList<>();
                    shopProduct.getRewardItems().forEach((k,v) -> {
                        ItemInfo item = new ItemInfo();
                        item.itemId = k;
                        item.count = v;
                        items.add(item);
                    });
                    info.items = items;
                }

                if (info.value < 1) {
                    info.value = shopProduct.getValue();
                }

                info.payType = shopProduct.getPayType();
                info.money = shopProduct.getMoney().toString();
                info.label1 = shopProduct.getLabel1();
                info.label2 = shopProduct.getLabel2();
                info.pic = shopProduct.getPic();
                info.channelProductId = shopProduct.channelProductId(req.channel);
                res.shopProductInfoList.add(info);
            });
        }
        playerController.send(res);
        log.debug("返回商品 req = {},resp = {}", JSON.toJSONString(req),JSON.toJSONString(res));
    }

    /**
     * 下单
     */
    @Command(ShopConstant.MsgBean.REQ_BUY_PRODUCT)
    public void reqBuyProduct(PlayerController playerController, ReqBuyProduct req) {
        ResBuyProduct res = new ResBuyProduct(Code.SUCCESS);
        try {
            ShopProduct shopProduct = shopService.getShopProduct(req.productId);
            if (shopProduct == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.debug("获取商品信息失败，兑换失败 playerId = {},productId = {}", playerController.playerId(), req.productId);
                return;
            }

            if (req.count < 1) {
                res.code = Code.PARAM_ERROR;
                playerController.send(res);
                log.debug("兑换个数不能小于1，兑换失败 playerId = {},productId = {},count = {}", playerController.playerId(), req.productId, req.count);
                return;
            }

            //检查是否开启
            if (!shopService.checkProductOpen(playerController.getPlayer(), shopProduct)) {
                log.debug("商品未开启，或者玩家未达到开启条件，兑换失败 playerId = {},productId = {}", playerController.playerId(), req.productId);
                res.code = Code.FORBID;
                playerController.send(res);
                return;
            }

            if (shopProduct.getPayType() < 1) {  //充值
                res.code = Code.FORBID;
                log.debug("该商品不支持兑换，兑换失败 playerId = {},productId = {}", playerController.playerId(), req.productId);
                playerController.send(res);
                return;
            }

            CommonResult<ItemOperationResult> exchangeResult = shopService.exchange(playerController, shopProduct, req.count);
            if (!exchangeResult.success()) {
                res.code = exchangeResult.code;
                playerController.send(res);
                return;
            }

            if (shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()) {
                res.items = new ArrayList<>(shopProduct.getRewardItems().size());
                shopProduct.getRewardItems().forEach((k, v) -> {
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.itemId = k;
                    itemInfo.count = v;
                    res.items.add(itemInfo);
                });
            }
            res.productId = shopProduct.getId();
            playerController.send(res);
            log.debug("玩家道具购买成功 playerId = {},productId = {},res = {}", playerController.playerId(), req.productId, JSON.toJSONString(res));
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
