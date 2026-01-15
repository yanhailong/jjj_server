package com.jjg.game.hall.dao;

import com.jjg.game.core.dao.AbstractPoolDao;
import com.jjg.game.hall.logger.HallLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author 11
 * @date 2025/6/18 16:18
 */
@Component
public class HallPoolDao extends AbstractPoolDao {
    @Autowired
    private HallLogger hallLogger;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String luaScript = """
            local cursor = "0"
                             local pattern = ARGV[1]
                             local result = {}
            
                             repeat
                                 local scanResult = redis.call('SCAN', cursor, 'MATCH', pattern, 'COUNT', 100)
                                 cursor = scanResult[1]
                                 local keys = scanResult[2]
            
                                 for _, key in ipairs(keys) do
                                     local hashData = redis.call('HGETALL', key)
                                     for i = 1, #hashData, 2 do
                                         table.insert(result, hashData[i])
                                         table.insert(result, hashData[i + 1])
                                     end
                                 end
                             until cursor == "0"
            
                             return result
            """;

    public void snapshot() {
        try {
            RedisScript<List> script = new DefaultRedisScript<>(luaScript, List.class);

            List<String> result = (List<String>) stringRedisTemplate.execute(script, Collections.emptyList(), pool_prefix + "*");

            if (!result.isEmpty()) {
                Map<Integer, Long> poolMap = new HashMap<>();

                for (int i = 0; i < result.size(); i += 2) {
                    if (i + 1 < result.size()) {
                        try {
                            String field = result.get(i);
                            String value = result.get(i + 1);
                            poolMap.put(Integer.parseInt(field), Long.parseLong(value));
                        } catch (NumberFormatException e) {
                            log.warn("数据格式错误: {}={}", result.get(i), result.get(i + 1));
                        }
                    }
                }

                if (!poolMap.isEmpty()) {
                    hallLogger.pool(poolMap);
                    log.info("奖池快照完成，记录 {} 条数据", poolMap.size());
                } else {
                    log.warn("处理后的奖池数据为空");
                }
            } else {
                log.warn("获取奖池数据为空");
            }

        } catch (Exception e) {
            log.error("执行Lua脚本失败", e);
        }
    }
}
