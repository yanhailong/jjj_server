package com.jjg.game.slots.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 游戏级共享展示数据。中奖时只更新时间戳，机器人资料只在名单变化时写入。 */
@Repository
public class TogetherPlayRobotDao {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<DisplayRobot>> ROBOT_LIST_TYPE = new TypeReference<>() { };
    private static final String SAVE_SCRIPT = """
            if redis.call('ZCARD', KEYS[2]) == 0 then
                return 0
            end
            if (redis.call('HGET', KEYS[1], 'schedule') or '') ~= ARGV[1] then
                return 0
            end
            redis.call('HSET', KEYS[1], 'schedule', ARGV[2])
            if ARGV[3] ~= '' then
                redis.call('HSET', KEYS[1], 'robots', ARGV[3])
            end
            return 1
            """;

    private final RedissonClient redissonClient;

    public TogetherPlayRobotDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public DisplayData get(int gameType) {
        Map<String, String> values = redissonClient.<String, String>getMap(
                key(gameType), StringCodec.INSTANCE).readAllMap();
        String schedule = values.get("schedule");
        if (schedule == null) {
            return null;
        }
        try {
            return new DisplayData(MAPPER.readValue(schedule, Schedule.class),
                    MAPPER.readValue(values.get("robots"), ROBOT_LIST_TYPE));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("好友同玩机器人数据解析失败 gameType=" + gameType, e);
        }
    }

    /** 校验旧时间戳并原子推进；只有成功的节点可以发送该次中奖通知。 */
    public boolean save(int gameType, Schedule before, Schedule after, List<DisplayRobot> robots) {
        try {
            Long saved = redissonClient.getScript(StringCodec.INSTANCE).eval(
                    RScript.Mode.READ_WRITE, SAVE_SCRIPT, RScript.ReturnType.INTEGER,
                    List.of(key(gameType), TogetherPlayDao.key(gameType)),
                    before == null ? "" : MAPPER.writeValueAsString(before),
                    MAPPER.writeValueAsString(after), robots == null ? "" : MAPPER.writeValueAsString(robots));
            return saved != null && saved == 1;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("好友同玩机器人数据编码失败 gameType=" + gameType, e);
        }
    }

    static String key(int gameType) {
        return "togetherPlay:robot:" + gameType;
    }

    // 未命中离开概率时也推进检查进度，其他节点不能在同一周期重复尝试。
    public record Schedule(long nextJoinTime, long nextRewardTime, long lastLeaveCheckTime) { }

    public record DisplayRobot(int cfgId, long playerId, String nick, int headImg, int headFrame, long joinTime) { }

    public record DisplayData(Schedule schedule, List<DisplayRobot> robots) { }
}
