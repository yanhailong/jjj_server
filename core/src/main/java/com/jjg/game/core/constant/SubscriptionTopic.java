package com.jjg.game.core.constant;

import java.util.function.Predicate;

/**
 * 消息订阅主题
 */
public enum SubscriptionTopic {
    /**
     * 夺宝奇兵库存更新
     */
    TOPIC_LUCKY_TREASURE_UPDATE("subscription:luckyTreasureUpdate"),

    ;

    private final String topic;

    SubscriptionTopic(String topic) {
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    /**
     * 根据指定的条件筛选并返回对应的订阅主题。
     *
     * @param topic 用于筛选订阅主题的条件，测试条件为主题的名称
     * @return 返回符合条件的订阅主题，如果没有找到则返回 null
     */
    public static SubscriptionTopic getTopic(Predicate<String> topic) {
        for (SubscriptionTopic t : SubscriptionTopic.values()) {
            if (topic.test(t.getTopic())) {
                return t;
            }
        }
        return null;
    }

}
