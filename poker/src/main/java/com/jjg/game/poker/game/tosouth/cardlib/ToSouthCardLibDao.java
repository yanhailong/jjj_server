package com.jjg.game.poker.game.tosouth.cardlib;

import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.utils.LZ4CompressionUtil;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 南方前进牌库 Redis DAO
 * 借鉴 Slots 的 AbstractResultLibDao 双库切换模式
 * Redis key格式: {libName}{sectionKey}
 * 每个key对应一个SET，存储该区间的牌库条目
 */
@Component
public class ToSouthCardLibDao {
    private static final Logger log = LoggerFactory.getLogger(ToSouthCardLibDao.class);

    /** 记录当前使用的是哪个库 */
    private static final String CURRENT_LIB_KEY = "toSouthCurrentLib";

    /** 双库名称 */
    private static final String LIB_1 = "toSouthLib1:";
    private static final String LIB_2 = "toSouthLib2:";

    /** 生成锁 */
    private static final String GEN_LOCK_KEY = "toSouthGenLock";

    /** 最后一次生成时间 */
    private static final String LAST_GEN_TIME_KEY = "toSouthLastGenTime";

    /** 玩家连赢/连输记录 HASH: playerId → streak */
    private static final String PLAYER_STREAK_KEY = "toSouthStreak";

    /** 玩家总盈亏记录 HASH: playerId → totalProfit */
    private static final String PLAYER_PROFIT_KEY = "toSouthProfit";

    /** 标准池 Redis key前缀（与slots一致: pool:{gameType}，HASH: roomCfgId → balance） */
    private static final String POOL_PREFIX = "pool:";

    /** 当前正在使用的结果库名（内存缓存） */
    private volatile String currentLibName;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redisson;

    /**
     * 初始化：从Redis加载当前库名
     */
    public void init() {
        this.currentLibName = getCurrentLibNameFromRedis();
    }

    /**
     * 从Redis获取当前正在使用的库名
     */
    public String getCurrentLibNameFromRedis() {
        return (String) redisTemplate.opsForValue().get(CURRENT_LIB_KEY);
    }

    public String getCurrentLibName() {
        return currentLibName;
    }

    /**
     * 获取新库名（与当前库相反的那个）
     */
    public String getNewLibName() {
        String current = getCurrentLibNameFromRedis();
        if (current == null || current.isEmpty() || LIB_1.equals(current)) {
            return LIB_2;
        }
        return LIB_1;
    }

    /**
     * 构建Redis key: {libName}{sectionKey}
     */
    private String buildKey(String libName, int sectionKey) {
        return libName + sectionKey;
    }

    /**
     * 根据倍数找到所属的分区Key
     * 算法: 在已排序的sectionKeys中找 floor(multiplier) = 最大的 key <= multiplier
     *
     * @param multiplier       有符号倍数
     * @param sortedSectionKeys 从小到大排序的分区边界
     * @return 匹配的分区key
     */
    public static int findSectionKey(long multiplier, List<Integer> sortedSectionKeys) {
        int result = sortedSectionKeys.getFirst();
        for (int key : sortedSectionKeys) {
            if (key <= multiplier) {
                result = key;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * 批量保存牌库到Redis
     *
     * @param libName           库名
     * @param libList           牌库列表
     * @param sortedSectionKeys 从小到大排序的分区边界(来自配置typeProp的key)
     */
    public void batchSaveToRedis(String libName, List<ToSouthCardLib> libList,
                                  List<Integer> sortedSectionKeys) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (ToSouthCardLib lib : libList) {
                byte[] data = ProtostuffUtil.serialize(lib);
                byte[] compressData = LZ4CompressionUtil.compressFast(data);

                ByteBuffer buffer = ByteBuffer.allocate(4 + compressData.length);
                buffer.putInt(data.length);
                buffer.put(compressData);

                int sectionKey = findSectionKey(lib.getMultiplier(), sortedSectionKeys);
                connection.sAdd(
                        buildKey(libName, sectionKey).getBytes(),
                        buffer.array()
                );
            }
            return null;
        });
    }

    /**
     * 保存完成后切换到新库
     */
    public void afterSave(String newLibName) {
        redisTemplate.opsForValue().set(CURRENT_LIB_KEY, newLibName);
        this.currentLibName = newLibName;
    }

    /**
     * 从指定分区的牌库中随机获取一条记录
     *
     * @param sectionKey 分区key (来自配置typeProp, 例如 -39, 0, 13 等)
     * @return 牌库条目，可能为null
     */
    public ToSouthCardLib getCardLib(int sectionKey) {
        if (currentLibName == null || currentLibName.isEmpty()) {
            init();
        }
        if (currentLibName == null || currentLibName.isEmpty()) {
            return null;
        }
        String key = buildKey(currentLibName, sectionKey);
        byte[] compressedData = (byte[]) redisTemplate.execute(
                (RedisCallback<byte[]>) connection -> connection.sRandMember(key.getBytes())
        );
        return deserialize(compressedData);
    }

    /**
     * 反序列化
     */
    private ToSouthCardLib deserialize(byte[] compressedData) {
        if (compressedData == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(compressedData);
        int originalLength = buffer.getInt();
        byte[] data = new byte[compressedData.length - 4];
        buffer.get(data);
        data = LZ4CompressionUtil.decompressFast(data, originalLength);
        return ProtostuffUtil.deserialize(data, ToSouthCardLib.class);
    }

    /**
     * 清理旧库（删除与当前库相反的那个）
     */
    public void clearOldLib() {
        if (currentLibName == null || currentLibName.isEmpty()) {
            log.debug("从redis删除牌库失败，currentLibName 为空");
            return;
        }
        String removeName = LIB_1.equals(currentLibName) ? LIB_2 : LIB_1;
        clearLib(removeName);
    }

    /**
     * 直接删除指定库名的数据
     */
    public void clearLib(String libName) {
        if (libName == null || libName.isEmpty()) {
            return;
        }
        String pattern = libName + "*";
        RKeys keys = redisson.getKeys();
        long start = System.currentTimeMillis();
        long deleted = keys.deleteByPattern(pattern);
        log.info("从redis清理牌库 libName={}, 删除Key数量={}, 耗时={}ms",
                pattern, deleted, System.currentTimeMillis() - start);
    }

    /**
     * 添加生成锁
     */
    public boolean addGenerateLock() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(GEN_LOCK_KEY, true, 30, TimeUnit.MINUTES)
        );
    }

    /**
     * 检查是否有生成锁
     */
    public boolean hasGenerateLock() {
        Object o = redisTemplate.opsForValue().get(GEN_LOCK_KEY);
        return o != null && Boolean.parseBoolean(o.toString());
    }

    /**
     * 移除生成锁
     */
    public void removeGenerateLock() {
        redisTemplate.delete(GEN_LOCK_KEY);
    }

    /**
     * 记录最后一次生成时间
     */
    public void addGenerateTime() {
        redisTemplate.opsForValue().set(LAST_GEN_TIME_KEY, System.currentTimeMillis());
    }

    /**
     * 检查牌库是否存在
     */
    public boolean hasCardLib() {
        String libName = getCurrentLibNameFromRedis();
        return libName != null && !libName.isEmpty();
    }

    // ==================== 水池余额（参考 slots 的 AbstractPoolDao / SlotsPoolDao） ====================

    /**
     * 初始化水池余额（putIfAbsent，不覆盖已有值）
     *
     * @param gameType  游戏类型
     * @param roomCfgId 房间配置ID
     * @param initValue 初始值（来自 Room_Chess.xlsx 的 initBasePool）
     */
    public void initPoolBalance(int gameType, int roomCfgId, long initValue) {
        redisTemplate.opsForHash().putIfAbsent(POOL_PREFIX + gameType, roomCfgId, initValue);
        log.info("初始化水池余额 gameType={}, roomCfgId={}, initValue={}", gameType, roomCfgId, initValue);
    }

    /**
     * 获取当前水池余额
     *
     * @return 当前水池余额，不存在返回0
     */
    public long getPoolBalance(int gameType, int roomCfgId) {
        Object val = redisTemplate.opsForHash().get(POOL_PREFIX + gameType, roomCfgId);
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }

    /**
     * 水池余额增减（原子操作）
     * 正数=系统收钱（玩家输）, 负数=系统赔钱（玩家赢）
     *
     * @return 操作后的余额
     */
    public long addPoolBalance(int gameType, int roomCfgId, long value) {
        return redisTemplate.opsForHash().increment(POOL_PREFIX + gameType, roomCfgId, value);
    }

    // ==================== 玩家统计数据（持久化到Redis，跨房间保留） ====================

    /**
     * 获取玩家连赢/连输值
     *
     * @param playerId 玩家ID
     * @return 正=连赢次数, 负=连输次数, 0=无记录
     */
    public int getPlayerWinStreak(long playerId) {
        Object val = redisTemplate.opsForHash().get(PLAYER_STREAK_KEY, String.valueOf(playerId));
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    /**
     * 设置玩家连赢/连输值
     */
    public void setPlayerWinStreak(long playerId, int streak) {
        redisTemplate.opsForHash().put(PLAYER_STREAK_KEY, String.valueOf(playerId), String.valueOf(streak));
    }

    /**
     * 获取玩家总盈亏
     *
     * @param playerId 玩家ID
     * @return 总盈亏（正=盈利, 负=亏损）
     */
    public long getPlayerTotalProfit(long playerId) {
        Object val = redisTemplate.opsForHash().get(PLAYER_PROFIT_KEY, String.valueOf(playerId));
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }

    /**
     * 累加玩家总盈亏
     *
     * @param playerId 玩家ID
     * @param delta    本局盈亏变化
     */
    public void addPlayerTotalProfit(long playerId, long delta) {
        redisTemplate.opsForHash().increment(PLAYER_PROFIT_KEY, String.valueOf(playerId), delta);
    }
}
