package com.jjg.game.social.channel;

import com.alibaba.fastjson.JSON;
import com.jjg.game.social.data.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 频道常驻缓存 (Redis 有界列表)。
 * <p>
 * 世界/系统/联盟频道共享: 全服一致、跨节点可读、重启可控且不增加 Mongo 压力。
 * 以 JSON 字符串存储, 最新消息在表头(LPUSH), 超过上限自动淘汰最旧(LTRIM)。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class ChannelMessageCache {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 追加一条消息并裁剪到上限。
     *
     * @param key 频道 redis key
     * @param msg 消息
     * @param cap 上限条数
     */
    public void push(String key, ChatMessage msg, int cap) {
        String json = JSON.toJSONString(msg);
        if (cap > 0) {
            stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection conn = (StringRedisConnection) connection;
                conn.lPush(key, json);
                conn.lTrim(key, 0, cap - 1);
                return null;
            });
        } else {
            stringRedisTemplate.opsForList().leftPush(key, json);
        }
    }

    /**
     * 取最新 count 条 (返回时间正序, 老在前新在后)。
     */
    public List<ChatMessage> latest(String key, int count) {
        List<ChatMessage> result = new ArrayList<>();
        if (count <= 0) {
            return result;
        }
        List<String> raw = stringRedisTemplate.opsForList().range(key, 0, count - 1);
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        //range 返回最新在前, 反转为时间正序
        for (int i = raw.size() - 1; i >= 0; i--) {
            try {
                result.add(JSON.parseObject(raw.get(i), ChatMessage.class));
            } catch (Exception ignore) {
                //单条解析失败跳过, 不影响整体
            }
        }
        return result;
    }

    /**
     * 清空频道缓存 (联盟解散时调用)。
     */
    public void clear(String key) {
        stringRedisTemplate.delete(key);
    }
}
