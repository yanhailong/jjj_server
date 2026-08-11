package com.jjg.game.poker.game.douxian.room.data;

import java.util.Map;

/** Data carried by one DouXian round settlement Kafka record. */
public record DouXianKafkaRoundLog(Map<String, Object> gameData,
                                   Map<Long, Map<String, Object>> playerData) {
}
