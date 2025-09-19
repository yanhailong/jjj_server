package com.jjg.game.hall.handler;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.protostuff.Command;
import com.jjg.game.common.protostuff.MessageType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.core.data.ItemOperationResult;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.ShopProduct;
import com.jjg.game.core.listener.GmListener;
import com.jjg.game.hall.constant.ShopConstant;
import com.jjg.game.hall.pb.req.ReqBuyProduct;
import com.jjg.game.hall.pb.req.ReqShop;
import com.jjg.game.hall.pb.res.ResBuyProduct;
import com.jjg.game.hall.pb.res.ResShop;
import com.jjg.game.hall.pb.struct.ShopProductInfo;
import com.jjg.game.hall.service.ShopService;
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
public class ShopMessageHandler implements GmListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ShopService shopService;

    /**
     * 获取商城
     */
    @Command(ShopConstant.MsgBean.REQ_SHOP)
    public void reqShop(PlayerController playerController, ReqShop req) {
        ResShop res = new ResShop(Code.SUCCESS);
        List<ShopProduct> shopProductList = shopService.getShop(playerController.getPlayer());
        if(shopProductList != null && !shopProductList.isEmpty()) {
            res.shopProductInfoList = new ArrayList<>(shopProductList.size());
            shopProductList.forEach((shopProduct) -> {
                ShopProductInfo info = new ShopProductInfo();
                info.id = shopProduct.getId();
                info.type = shopProduct.getType();
                info.endTime = shopProduct.getEndTime();
                info.originalCount = shopProduct.getOriginalCount();
                info.payType = shopProduct.getPayType();
                info.money = shopProduct.getMoney();
                info.label1 = shopProduct.getLabel1();
                info.label2 = shopProduct.getLabel2();
                info.pic = shopProduct.getPic();
                res.shopProductInfoList.add(info);
            });
        }
        playerController.send(res);
    }

    /**
     * 下单
     */
    @Command(ShopConstant.MsgBean.REQ_BUY_PRODUCT)
    public void reqBuyProduct(PlayerController playerController, ReqBuyProduct req) {
        ResBuyProduct res = new ResBuyProduct(Code.SUCCESS);
        try{
            ShopProduct shopProduct = shopService.getShopProduct(req.productId);
            if(shopProduct == null) {
                res.code = Code.NOT_FOUND;
                playerController.send(res);
                log.debug("获取商品信息失败 playerId = {},productId = {}", playerController.playerId(), req.productId);
                return;
            }

            //检查是否开启
            if(!shopService.checkProductOpen(playerController.getPlayer(), shopProduct)){
                log.debug("商品未开启，或者玩家未达到开启条件 playerId = {},productId = {}", playerController.playerId(), req.productId);
                res.code = Code.FORBID;
                playerController.send(res);
                return;
            }

            if(shopProduct.getPayType() < 1){  //充值
                CommonResult<String> orderResult = shopService.generateOrder(playerController.playerId(), shopProduct);
                if(!orderResult.success()){
                    res.code = orderResult.code;
                    playerController.send(res);
                    return;
                }
                res.orderId = orderResult.data;
                playerController.send(res);
                log.debug("玩家充值下单成功 playerId = {},productId = {},orderId = {}", playerController.playerId(), req.productId, res.orderId);
            }else {  //道具兑换
                CommonResult<ItemOperationResult> exchangeResult = shopService.exchange(playerController, shopProduct);
                if(!exchangeResult.success()){
                    res.code = exchangeResult.code;
                    playerController.send(res);
                    return;
                }

                if(shopProduct.getRewardItems() != null && !shopProduct.getRewardItems().isEmpty()){
                    res.items = new ArrayList<>(shopProduct.getRewardItems().size());
                    shopProduct.getRewardItems().forEach((k,v) -> {
                        ItemInfo itemInfo = new ItemInfo();
                        itemInfo.itemId = k;
                        itemInfo.count = v;
                        res.items.add(itemInfo);
                    });
                }
                playerController.send(res);
                log.debug("玩家道具购买成功 playerId = {},productId = {},res = {}", playerController.playerId(), req.productId, JSON.toJSONString(res));
            }
        }catch (Exception e) {
            log.error("",e);
        }
    }

    @Override
    public CommonResult<String> gm(PlayerController playerController, String[] gmOrders) {
        CommonResult<String> res = new CommonResult<>(Code.SUCCESS);
        try {
            if ("payCallback".equalsIgnoreCase(gmOrders[0])) {
                log.debug("收到充值回调的gm命令 playerId = {},gmOrders = {}", playerController.playerId(), gmOrders);
                res.code = shopService.payCallback(gmOrders[1]);
            } else {
                res.code = Code.NOT_FOUND;
            }
        } catch (Exception e) {
            log.error("", e);
            res.code = Code.EXCEPTION;
        }
        return res;
    }
}
