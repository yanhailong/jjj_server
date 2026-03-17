package com.jjg.game.core.logger;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.utils.RandomUtils;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.data.*;
import com.jjg.game.core.pb.RechargeType;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RobotUtil;
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

    protected final ObjectMapper objectMapper = new ObjectMapper();

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
    public void useGold(Player player, long beforeGold, long gold, AddType addType, String desc) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();

            addGoldChange(json, beforeGold, gold, player.getGold());
            addSafeBoxGoldChange(json, player.getSafeBoxGold(), 0, player.getSafeBoxGold());

            json.put("addType", addType.getValue());
            json.put("deviceType", player.getDeviceType());
            json.put("desc", desc);
            json.put("subChannel", player.getSubChannel());
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
    public void transSafeBoxGold(Player player, long beforeGold, long beforeSafeBoxGold, long gold, AddType addType,
                                 String desc) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();

            json = addGoldChange(json, beforeGold, gold, player.getGold());
            json = addSafeBoxGoldChange(json, beforeSafeBoxGold, gold, player.getSafeBoxGold());

            json.put("addType", addType.getValue());
            json.put("deviceType", player.getDeviceType());
            json.put("desc", desc);
            json.put("subChannel", player.getSubChannel());
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
    public void useDiamond(Player player, long beforeDiamond, long diamond, AddType addType, String desc) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();

            json = addDiamondChange(json, beforeDiamond, diamond, player.getDiamond());
            json = addSafeBoxDiamondChange(json, player.getSafeBoxDiamond(), 0, player.getSafeBoxDiamond());

            json.put("addType", addType.getValue());
            json.put("desc", desc);
            json.put("deviceType", player.getDeviceType());
            json.put("subChannel", player.getSubChannel());
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
                                    AddType addType, String desc) {

        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();

            json = addDiamondChange(json, beforeDiamond, diamond, player.getDiamond());
            json = addSafeBoxDiamondChange(json, beforeSafeBoxDiamond, diamond, player.getSafeBoxDiamond());

            json.put("addType", addType.getValue());
            json.put("desc", desc);
            json.put("deviceType", player.getDeviceType());
            json.put("subChannel", player.getSubChannel());
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
    public void useSafeBoxGold(Player player, long beforeGold, long gold, AddType addType, String desc) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();

            json = addGoldChange(json, player.getGold(), 0, player.getGold());
            json = addSafeBoxGoldChange(json, beforeGold, gold, player.getSafeBoxGold());

            json.put("addType", addType.getValue());
            json.put("desc", desc);
            json.put("deviceType", player.getDeviceType());
            json.put("subChannel", player.getSubChannel());
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
    public void useSafeBoxDiamond(Player player, long beforeDiamond, long diamond, AddType addType, String desc) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();

            json = addDiamondChange(json, beforeDiamond, diamond, player.getDiamond());
            json = addSafeBoxDiamondChange(json, beforeDiamond, diamond, player.getSafeBoxDiamond());

            json.put("addType", addType.getValue());
            json.put("desc", desc);
            json.put("deviceType", player.getDeviceType());
            json.put("subChannel", player.getSubChannel());
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
    public void vip(Player player, int beforeLevel, int vipLevel, AddType addType, String desc) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("beforeLevel", beforeLevel);
            json.put("currentLevel", vipLevel);
            json.put("addType", addType.getValue());
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
     * @param result
     */
    public void level(Player player, int beforeLevel, int level, List<ItemInfo> items, CommonResult<ItemOperationResult> result) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("beforeLevel", beforeLevel);
            json.put("currentLevel", level);
            if (result != null) {
                json.put("result", objectMapper.writeValueAsString(result.data));
            }
            if (items != null && !items.isEmpty()) {
                JSONArray jsonArray = new JSONArray();
                items.forEach(item -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("itemId", item.itemId);
                    jsonObject.put("count", item.count);
                    jsonArray.add(jsonObject);
                });

                json.put("items", jsonArray);
            } else {
                json.put("items", null);
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
     * @param device   设备
     * @return
     */
    public void enterGame(Player player, int gameType, int roomCfgId, int device) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("gameType", gameType);
            json.put("roomCfgId", roomCfgId);
            json.put("channel", player.getChannel().getValue());
            json.put("device", device);
            json.put("subChannel", player.getSubChannel());
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
    public void exitGame(Player player, int onlineTimeLen, int device) {
        try {
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("gameType", player.getGameType());
            json.put("channel", player.getChannel().getValue());
            json.put("online", onlineTimeLen);
            json.put("device", device);
            json.put("subChannel", player.getSubChannel());
            sendLog("exitGame", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 消耗道具
     *
     * @param playerId
     * @param addType
     */
    public void consumeItem(long playerId, Map<Integer, Long> beforeMap, Map<Integer, Long> costMap, Map<Integer, Long> afterMap, AddType addType) {
        try {
            if (RobotUtil.isRobot(playerId)) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            //道具表  logType  1.获得   2.消耗
            json.put("logType", 2);
            //前
            JSONArray beforeJsonArray = ItemUtils.itemMapToJsonArray(beforeMap);
            json.put("before", beforeJsonArray);

            //变化值
            JSONArray jsonArray = ItemUtils.itemMapToJsonArray(costMap);
            json.put("items", jsonArray);

            //后
            JSONArray afterJsonArray = ItemUtils.itemMapToJsonArray(afterMap);
            json.put("after", afterJsonArray);

            json.put("addType", addType.getValue());
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
    public void addItems(long playerId, Map<Integer, Long> beforeMap, Map<Integer, Long> map, Map<Integer, Long> afterMap, AddType addType, String desc) {
        try {
            if (RobotUtil.isRobot(playerId)) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("playerId", playerId);
            //道具表  logType  1.获得   2.消耗
            json.put("logType", 1);

            JSONArray beforeJsonArray = ItemUtils.itemMapToJsonArray(beforeMap);
            json.put("before", beforeJsonArray);

            JSONArray jsonArray = ItemUtils.itemMapToJsonArray(map);
            json.put("items", jsonArray);

            JSONArray afterJsonArray = ItemUtils.itemMapToJsonArray(afterMap);
            json.put("after", afterJsonArray);

            json.put("addType", addType.getValue());
            json.put("desc", desc);
            sendLog("addItems", null, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }


    public void order(Player player, Order order, String channelProductId, String regionCode) {
        if (RobotUtil.isRobot(player.getId())) {
            return;
        }
        order(player, order, null, channelProductId, regionCode);
    }

    /**
     * 订单
     *
     * @param player
     * @param order
     */
    public void order(Player player, Order order, String money, String channelProductId, String regionCode) {
        if (RobotUtil.isRobot(player.getId())) {
            return;
        }
        order(player, order, money, channelProductId, regionCode, order.getDesc());
    }

    public void order(Player player, Order order, String money, String channelProductId, String regionCode, String desc) {
        if (RobotUtil.isRobot(player.getId())) {
            return;
        }

        String price = money;
        if (StringUtils.isEmpty(money) && order.getPrice() != null) {
            price = order.getPrice().toPlainString();
        }

        order(player, order.getId(), order.getChannelOrderId(), order.getPlayerChannel(), order.getPayChannel(), order.getRechargeType(), price, channelProductId, order.getCreateTime(), order.getUpdateTime(),
                order.getOrderStatus(), 0, regionCode, desc);
    }

    /**
     * 商城法币购买
     *
     * @param player
     * @param order
     */
    public void shop(Player player, Order order, ShopProduct shopProduct, String money, String channelProductId, String region) {
        if (RobotUtil.isRobot(player.getId())) {
            return;
        }

        String price = money;
        if (StringUtils.isEmpty(money) && order.getPrice() != null) {
            price = order.getPrice().toPlainString();
        }

        shop(player, order.getId(), order.getChannelOrderId(), shopProduct.getType(), order.getPlayerChannel(), order.getPayChannel(), order.getRechargeType(), price, channelProductId, order.getCreateTime(), order.getUpdateTime(),
                order.getOrderStatus(), shopProduct.getPayType(), region);
    }

    /**
     * 商城
     *
     * @param player
     * @param shopProduct
     */
    public void shop(Player player, ShopProduct shopProduct, int registerChannel, long money) {
        if (RobotUtil.isRobot(player.getId())) {
            return;
        }
        int now = TimeHelper.nowInt();
        shop(player, null, null, shopProduct.getType(), registerChannel, player.getChannel().getValue(), RechargeType.SHOP, String.valueOf(money), null, now, now, OrderStatus.SUCCESS, shopProduct.getPayType(), null);
    }

    /***********************************************************************************************/

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

        topic = StringUtils.isEmpty(topic) ? GAME_LOGS_TOPIC : topic.toLowerCase();
        kafkaTemplate.send(topic, RandomUtils.getUUid(), msg);

//        log.debug("打印日志数据 topic = {},msg = {}", topic, msg);
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
                      String price, String chanelProductId, long createTime, long updateTime, OrderStatus orderStatus, int payType, String regionCode, String desc) {
        try {
            JSONObject json = new JSONObject();
            json.put("orderId", orderId);
            json.put("merchantOrderId", merchantOrderId);
            json.put("playerChannel", playerChannel);
            json.put("payChannel", payChannel);
            json.put("rechargeType", rechargeType.getType());
            json.put("money", price);
            json.put("createTime", createTime);
            json.put("updateTime", updateTime);
            json.put("status", orderStatus.code);
            json.put("payType", payType);
            json.put("regionCode", regionCode);
            json.put("chanelProductId", chanelProductId);
            json.put("subChannel", player.getSubChannel());
            json.put("desc", desc);

            sendLog("order", player, json);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    public void shop(Player player, String orderId, String merchantOrderId, int shopProductType, int playerChannel, int payChannel, RechargeType rechargeType,
                     String price, String chanelProductId, int createTime, int updateTime, OrderStatus orderStatus, int payType, String regionCode) {
        try {
            JSONObject json = new JSONObject();
            json.put("orderId", orderId);
            json.put("merchantOrderId", merchantOrderId);
            json.put("shopProductType", shopProductType);
            json.put("playerChannel", playerChannel);
            json.put("payChannel", payChannel);
            json.put("rechargeType", rechargeType.getType());
            json.put("money", price);
            json.put("createTime", createTime);
            json.put("updateTime", updateTime);
            json.put("status", orderStatus.code);
            json.put("payType", payType);
            json.put("chanelProductId", chanelProductId);
            json.put("subChannel", player.getSubChannel());
            json.put("regionCode", regionCode);

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
            if (RobotUtil.isRobot(player.getId())) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("rewardsType", rewardsType);
            json.put("functionType", 1);
            if (rewards != null) {
                json.put("rewards", objectMapper.writeValueAsString(rewards));
            }
            if (result != null) {
                json.put("result", objectMapper.writeValueAsString(result));
            }
            json.put("addExp", addExp);
            json.put("vipLevel", player.getVipLevel());
            sendLog("function", player, json);
        } catch (Exception e) {
            log.error("sendVipLog", e);
        }
    }

    /**
     * 房间操作日志
     */
    public void roomOperate(FriendRoom friendRoom, int operateType, int operateTimeLen, Map<Integer, Long> spendItemMap, ItemOperationResult result) {
        try {
            JSONObject json = new JSONObject();
            //1.创建房间  2.自动续费  3.手动续费
            json.put("operateType", operateType);
            json.put("functionType", 5);
            json.put("roomId", friendRoom.getId());
            json.put("gameType", friendRoom.getGameType());
            json.put("roomCfgId", friendRoom.getRoomCfgId());
            //时长(分钟)
            json.put("timeLen", operateTimeLen);
            //自动续费
            json.put("autoRenewal", friendRoom.isAutoRenewal());
            //消耗
            json.put("spend", ItemUtils.itemMapToJsonArray(spendItemMap));
            //消耗
            json.put("remain", objectMapper.writeValueAsString(result));
            json.put("playerId", friendRoom.getCreator());
            sendLog("function", null, json);
        } catch (Exception e) {
            log.error("sendVipLog", e);
        }
    }

    /**
     * 房间解散日志
     */
    public void roomDisband(FriendRoom friendRoom, long mailId, List<Item> returnItems) {
        try {
            JSONObject json = new JSONObject();
            json.put("functionType", 6);
            json.put("roomId", friendRoom.getId());
            json.put("gameType", friendRoom.getGameType());
            json.put("roomCfgId", friendRoom.getRoomCfgId());
            json.put("mailId", mailId);
            //返还道具
            json.put("returnItems", ItemUtils.itemListToJson(returnItems));
            json.put("playerId", friendRoom.getCreator());
            sendLog("function", null, json);
        } catch (Exception e) {
            log.error("sendVipLog", e);
        }
    }

    /**
     * 房间解散日志
     */
    public void addMail(Mail mail) {
        try {
            //根据需求，附件为空的邮件，不发送日志
            if (mail.getItems() == null || mail.getItems().isEmpty()) {
                return;
            }

            JSONObject json = new JSONObject();
            json.put("playerId", mail.getPlayerId());
            //邮件id
            json.put("mailId", mail.getId());

            //标题，多语言结构
            //{"type":0,"content":"aaa","langId":"1122","params":[{"type":"1","param":"123"}]}
            json.put("title", objectMapper.writeValueAsString(mail.getTitle()));
            json.put("content", objectMapper.writeValueAsString(mail.getContent()));

            //发送时间
            json.put("sendTime", mail.getSendTime());
            //过期时间
            json.put("timeout", mail.getTimeout());
            //是否为全服邮件
            json.put("serverMail", mail.isServerMail());
            //附件道具
            json.put("items", ItemUtils.itemListToJson(mail.getItems()));
            //来源，添加类型
            json.put("addType", mail.getAddType().getValue());
            //备注
            json.put("desc", mail.getDesc());

            sendLog("mail", null, json);
        } catch (Exception e) {
            log.error("sendVipLog", e);
        }
    }
}
