package com.jjg.game.core.logger;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.core.constant.RechargeType;
import com.jjg.game.core.data.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/19 9:58
 */
public class BaseLogger {
    @Autowired
    protected NodeConfig nodeConfig;
    @Autowired
    protected KafkaTemplate<String, String> kafkaTemplate;

    private final String GAME_LOGS_TOPIC = "game-logs";

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * 在线统计
     */
    public void online(int num, String serverIp) {
        try {
            JSONObject json = new JSONObject();
            json.put("num", num);
            json.put("serverIp", serverIp);
            sendLog("online", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void gmOrder(String order, Long playerId, String result) {
        try {
            JSONObject json = new JSONObject();
            json.put("order", order);
            if (playerId != null) {
                json.put("playerId", playerId);
            }
            json.put("result", result);
            sendLog("gm", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 金币变化
     *
     * @param player
     * @param gold
     * @param addType
     */
    public void useGold(Player player, long beforeGold, long gold, String addType, String desc) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();

            addGoldChange(json, beforeGold, gold, player.getGold());
            addSafeBoxGoldChange(json, player.getSafeBoxGold(), 0, player.getSafeBoxGold());

            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("goldChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 保险箱金币和携带金币互转
     *
     * @param player
     * @param addType
     */
    public void transSafeBoxGold(Player player, long beforeGold, long beforeSafeBoxGold, long gold, String addType,
                                 String desc) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();

            json = addGoldChange(json, beforeGold, gold, player.getGold());
            json = addSafeBoxGoldChange(json, beforeSafeBoxGold, gold, player.getSafeBoxGold());

            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("goldChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 钻石变化
     *
     * @param player
     * @param diamond
     * @param addType
     */
    public void useDiamond(Player player, long beforeDiamond, long diamond, String addType, String desc) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();

            json = addDiamondChange(json, beforeDiamond, diamond, player.getDiamond());
            json = addSafeBoxDiamondChange(json, player.getSafeBoxDiamond(), 0, player.getSafeBoxDiamond());

            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("diamondChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 保险箱钻石和携带钻石互转
     *
     * @param player
     * @param addType
     */
    public void transSafeBoxDiamond(Player player, long beforeDiamond, long beforeSafeBoxDiamond, long diamond,
                                    String addType, String desc) {

        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();

            json = addDiamondChange(json, beforeDiamond, diamond, player.getDiamond());
            json = addSafeBoxDiamondChange(json, beforeSafeBoxDiamond, diamond, player.getSafeBoxDiamond());

            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("diamondChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 保险箱金币变化
     *
     * @param player
     * @param gold
     * @param addType
     */
    public void useSafeBoxGold(Player player, long beforeGold, long gold, String addType, String desc) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();

            json = addGoldChange(json, player.getGold(), 0, player.getGold());
            json = addSafeBoxGoldChange(json, beforeGold, gold, player.getSafeBoxGold());

            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("goldChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 保险箱钻石变化
     *
     * @param player
     * @param diamond
     * @param addType
     */
    public void useSafeBoxDiamond(Player player, long beforeDiamond, long diamond, String addType, String desc) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();

            json = addDiamondChange(json, beforeDiamond, diamond, player.getDiamond());
            json = addSafeBoxDiamondChange(json, beforeDiamond, diamond, player.getSafeBoxDiamond());

            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("diamondChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * vip等级变化
     *
     * @param player
     * @param beforeLevel
     * @param vipLevel
     * @param addType
     */
    public void vip(Player player, int beforeLevel, int vipLevel, String addType, String desc) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("beforeLevel", beforeLevel);
            json.put("currentLevel", vipLevel);
            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("vipLevelChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * vip等级变化
     *
     * @param player
     * @param beforeLevel
     * @param level
     */
    public void level(Player player, int beforeLevel, int level, List<ItemInfo> items) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("beforeLevel", beforeLevel);
            json.put("currentLevel", level);

            if (items != null && !items.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                items.forEach(item -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("itemId", item.itemId);
                    jsonObject.put("count", item.count);
                    jsonArray.add(jsonObject);
                });
                json.put("items", jsonArray);
            }
            sendLog("levelChange", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 进入游戏
     *
     * @param player
     * @param gameType
     * @return
     */
    public void enterGame(Player player, int gameType, int roomCfgId) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("gameType", gameType);
            json.put("roomCfgId", roomCfgId);
            sendLog("enterGame", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 退出游戏
     *
     * @param player
     * @return
     */
    public void exitGame(Player player) {
        if (player instanceof RobotPlayer) {
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("gameType", player.getGameType());
            sendLog("exitGame", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 使用道具
     *
     * @param playerId
     * @param itemId
     * @param count
     * @param addType
     */
    public void useItem(long playerId, int itemId, int count, String addType) {
        try {
            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            json.put("itemId", itemId);
            json.put("count", count);
            json.put("addType", addType);
            sendLog("useitem", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 获取道具
     *
     * @param playerId
     * @param addType
     */
    public void addItems(long playerId, Map<Integer, Long> map, String addType, String desc) {
        try {
            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            JSONArray jsonArray = new JSONArray();
            map.forEach((k, v) -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("itemId", k);
                jsonObject.put("count", v);
                jsonArray.add(jsonObject);
            });

            json.put("items", jsonArray);
            json.put("addType", addType);
            json.put("desc", desc);
            sendLog("addItems", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 获取道具
     *
     * @param playerId
     * @param addType
     */
    public long addItem(long playerId, int itemId, long count, String addType) {
        long snowflakeNextId = IdUtil.getSnowflakeNextId();
        try {
            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            json.put("operationId", snowflakeNextId);

            JSONArray jsonArray = new JSONArray();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("itemId", itemId);
            jsonObject.put("count", count);
            jsonArray.add(jsonObject);

            json.put("items", jsonArray);
            json.put("addType", addType);
            sendLog("addItems", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
        return snowflakeNextId;
    }

    /**
     * 订单
     *
     * @param player
     * @param order
     */
    public void order(Player player, Order order) {
        order(player, order.getId(), order.getId(), order.getPlayerChannel(), order.getPayChannel(), order.getRechargeType(), order.getPrice(), order.getCreateTime(), order.getUpdateTime(),
                order.getOrderStatus(), order.getPayType());
    }

    /**
     * 商城法币购买
     *
     * @param player
     * @param order
     */
    public void shop(Player player, Order order, ShopProduct shopProduct) {
        shop(player, order.getId(), order.getId(), shopProduct.getType(), order.getPlayerChannel(), order.getPayChannel(), order.getRechargeType(), order.getPrice(), order.getCreateTime(), order.getUpdateTime(),
                order.getOrderStatus(), order.getPayType());
    }

    /**
     * 商城
     *
     * @param player
     * @param shopProduct
     */
    public void shop(Player player, ShopProduct shopProduct, int registerChannel) {
        long now = System.currentTimeMillis();
        shop(player, null, null, shopProduct.getType(), registerChannel, player.getChannel().getValue(), RechargeType.SHOP, shopProduct.getMoney(), now, now, OrderStatus.SUCCESS, shopProduct.getPayType());
    }

    /***********************************************************************************************/

    protected void sendLog(Player player, JSONObject json) {
        sendLog(null, player, json);
    }

    protected void sendLog(String topic, Player player, JSONObject json) {
        if (player instanceof RobotPlayer) {
            return;
        }
        if (player != null) {
            json.put("playerId", player.getId());
            json.put("gameType", player.getGameType());
            json.put("playerName", player.getNickName());
            json.put("roomCfgId", player.getRoomCfgId());
        }

        json.put("time", System.currentTimeMillis());
        json.put("nodeName", nodeConfig.getName());
        json.put("nodeType", nodeConfig.getType());

        String msg = JSONObject.toJSONString(json, SerializerFeature.WriteNonStringKeyAsString);

        kafkaTemplate.send(StringUtils.isEmpty(topic) ? GAME_LOGS_TOPIC : topic.toLowerCase(), msg);

        log.debug("打印日志数据 msg = {}", msg);
    }

    protected void sendLog(JSONObject json) {
        sendLog(null, json);
    }

    /**
     * 金币变化字段
     *
     * @return
     */
    private JSONObject addGoldChange(JSONObject json, long beforeGold, long goldChange, long afterGold) {
        json.put("beforeGold", beforeGold);
        json.put("goldChange", goldChange);
        json.put("afterGold", afterGold);
        return json;
    }

    /**
     * 钻石变化字段
     *
     * @return
     */
    private JSONObject addDiamondChange(JSONObject json, long beforeDiamond, long diamondChange, long afterDiamond) {
        json.put("beforeDiamond", beforeDiamond);
        json.put("diamondChange", diamondChange);
        json.put("afterDiamond", afterDiamond);
        return json;
    }

    /**
     * 保险箱金币变化字段
     *
     * @return
     */
    private JSONObject addSafeBoxGoldChange(JSONObject json, long beforeSafeBoxGold, long safeBoxGoldChange,
                                            long afterSafeBoxGold) {
        json.put("beforeSafeBoxGold", beforeSafeBoxGold);
        json.put("safeBoxGoldChange", safeBoxGoldChange);
        json.put("afterSafeBoxGold", afterSafeBoxGold);
        return json;
    }

    /**
     * 保险箱钻石变化字段
     *
     * @return
     */
    private JSONObject addSafeBoxDiamondChange(JSONObject json, long beforeSafeBoxDiamond, long SafeBoxDiamondChange,
                                               long afterSafeBoxDiamond) {
        json.put("beforeSafeBoxDiamond", beforeSafeBoxDiamond);
        json.put("safeBoxDiamondChange", SafeBoxDiamondChange);
        json.put("afterSafeBoxDiamond", afterSafeBoxDiamond);
        return json;
    }

    public void order(Player player, String orderId, String merchantOrderId, int playerChannel, int payChannel, RechargeType rechargeType,
                      long price, long createTime, long updateTime, OrderStatus orderStatus, int payType) {
        try {
            JSONObject json = new JSONObject();
            json.put("orderId", orderId);
            json.put("merchantOrderId", merchantOrderId);
            json.put("nick", player.getNickName());
            json.put("playerChannel", playerChannel);
            json.put("payChannel", payChannel);
            json.put("rechargeType", rechargeType.getType());
            json.put("money", price);
            json.put("createTime", createTime);
            json.put("updateTime", updateTime);
            json.put("status", orderStatus.code);
            json.put("payType", payType);

            sendLog("order", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void shop(Player player, String orderId, String merchantOrderId, int shopProductType, int playerChannel, int payChannel, RechargeType rechargeType,
                     long price, long createTime, long updateTime, OrderStatus orderStatus, int payType) {
        try {
            JSONObject json = new JSONObject();
            json.put("orderId", orderId);
            json.put("merchantOrderId", merchantOrderId);
            json.put("shopProductType", shopProductType);
            json.put("nick", player.getNickName());
            json.put("playerChannel", playerChannel);
            json.put("payChannel", payChannel);
            json.put("rechargeType", rechargeType.getType());
            json.put("money", price);
            json.put("createTime", createTime);
            json.put("updateTime", updateTime);
            json.put("status", orderStatus.code);
            json.put("payType", payType);

            sendLog("shop", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    /**
     * 发送vip日志
     *
     * @param player      玩家数据
     * @param rewardsType 奖励类型
     * @param rewards     奖励
     * @param result      奖励完成后结果
     * @param addExp      增加经验值
     */
    public void sendVipLog(Player player, int rewardsType, Map<Integer, Long> rewards, ItemOperationResult result, long addExp) {
        try {
            JSONObject json = new JSONObject();
            json.put("rewardsType", rewardsType);
            json.put("functionType", 1);
            if (rewards != null) {
                json.put("rewards", JSON.toJSONString(rewards, SerializerFeature.WriteNonStringKeyAsString));
            }
            if (result != null) {
                json.put("result", JSON.toJSONString(result, SerializerFeature.WriteNonStringKeyAsString));
            }
            json.put("addExp", addExp);
            sendLog("function", player, json);
        } catch (Exception e) {
            log.error("sendVipLog", e);
        }
    }
}
