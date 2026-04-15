package com.jjg.game.poker.game.common.cardlib;

import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.core.utils.LZ4CompressionUtil;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Poker 牌库通用 Redis DAO 基类
 * 借鉴 Slots 的 {@code AbstractResultLibDao} 双库切换模式
 * <p>
 * 职责：
 * <ul>
 *     <li>双库（lib1/lib2）切换读写</li>
 *     <li>Protostuff + LZ4 批量序列化/反序列化</li>
 *     <li>生成锁与生成时间戳管理</li>
 *     <li>水池余额 HASH（与 slots 共用 {@code pool:{gameType}} 前缀）</li>
 * </ul>
 * <p>
 * Redis key 命名规则（与 Slots 对齐）：
 * <ul>
 *     <li>当前库标识：PokerResultLibCurrent:{gameType}</li>
 *     <li>库1 前缀：  PokerResultLib1:{gameType}:{sectionKey}</li>
 *     <li>库2 前缀：  PokerResultLib2:{gameType}:{sectionKey}</li>
 *     <li>生成锁：    PokerResultLibGenLock:{gameType}</li>
 *     <li>生成时间：  PokerResultLibLastGenTime:{gameType}</li>
 * </ul>
 *
 * @param <T> 牌库条目类型，必须实现 {@link CardLibEntry}
 */
public abstract class AbstractCardLibDao<T extends CardLibEntry> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ==================== 统一 Redis key 前缀 ====================
    private static final String KEY_CURRENT   = "PokerResultLibCurrent:";
    private static final String KEY_LIB1      = "PokerResultLib1:";
    private static final String KEY_LIB2      = "PokerResultLib2:";
    private static final String KEY_GEN_LOCK  = "PokerResultLibGenLock:";
    private static final String KEY_GEN_TIME  = "PokerResultLibLastGenTime:";
    private static final String KEY_STREAK    = "PokerResultLibStreak:";
    private static final String KEY_PROFIT    = "PokerResultLibProfit:";

    /** 水池余额 Redis key前缀（与 slots 一致: pool:{gameType}，HASH: roomCfgId → balance） */
    protected static final String POOL_PREFIX = "pool:";

    /** 反序列化目标类 */
    protected final Class<T> clazz;
    /** 游戏类型 ID */
    protected final int gameType;

    /** 记录当前使用的是哪个库的 Redis key */
    protected final String currentLibKey;
    /** 双库名称：库1 前缀 */
    protected final String lib1Prefix;
    /** 双库名称：库2 前缀 */
    protected final String lib2Prefix;
    /** 生成锁 Redis key */
    protected final String genLockKey;
    /** 最后一次生成时间 Redis key */
    protected final String lastGenTimeKey;

    /** 当前正在使用的结果库名（内存缓存） */
    protected volatile String currentLibName;

    @Autowired
    protected RedisTemplate redisTemplate;

    @Autowired
    protected RedissonClient redisson;

    /**
     * 子类只需传入牌库条目 Class 和 gameType，所有 Redis key 由基类统一生成
     *
     * @param clazz    牌库条目 Class，用于 Protostuff 反序列化
     * @param gameType 游戏类型 ID（如 {@code CoreConst.GameType.TO_SOUTH}）
     */
    protected AbstractCardLibDao(Class<T> clazz, int gameType) {
        this.clazz = clazz;
        this.gameType = gameType;
        this.currentLibKey = KEY_CURRENT + gameType;
        this.lib1Prefix    = KEY_LIB1 + gameType + ":";
        this.lib2Prefix    = KEY_LIB2 + gameType + ":";
        this.genLockKey    = KEY_GEN_LOCK + gameType;
        this.lastGenTimeKey = KEY_GEN_TIME + gameType;
    }

    // ==================== 子类可复用的 key 构建方法 ====================

    /** 获取玩家连胜记录 Redis key：PokerResultLibStreak:{gameType} */
    protected String getStreakKey() {
        return KEY_STREAK + gameType;
    }

    /** 获取玩家盈亏记录 Redis key：PokerResultLibProfit:{gameType} */
    protected String getProfitKey() {
        return KEY_PROFIT + gameType;
    }

    /**
     * 初始化：从 Redis 加载当前库名
     */
    public void init() {
        this.currentLibName = getCurrentLibNameFromRedis();
    }

    /**
     * 从 Redis 获取当前正在使用的库名
     */
    public String getCurrentLibNameFromRedis() {
        return (String) redisTemplate.opsForValue().get(currentLibKey);
    }

    public String getCurrentLibName() {
        return currentLibName;
    }

    /**
     * 获取新库名（与当前库相反的那个）
     */
    public String getNewLibName() {
        String current = getCurrentLibNameFromRedis();
        if (current == null || current.isEmpty() || lib1Prefix.equals(current)) {
            return lib2Prefix;
        }
        return lib1Prefix;
    }

    /**
     * 构建 Redis key: {libName}{sectionKey}
     */
    private String buildKey(String libName, int sectionKey) {
        return libName + sectionKey;
    }

    /**
     * 根据倍数找到所属的分区 Key
     * 算法: 在已排序的 sectionKeys 中找 floor(multiplier) = 最大的 key <= multiplier
     *
     * @param multiplier        有符号倍数
     * @param sortedSectionKeys 从小到大排序的分区边界
     * @return 匹配的分区 key
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
     * 批量保存牌库到 Redis
     *
     * @param libName           库名
     * @param libList           牌库列表
     * @param sortedSectionKeys 从小到大排序的分区边界（来自配置 typeProp 的 key）
     */
    public void batchSaveToRedis(String libName, List<T> libList,
                                 List<Integer> sortedSectionKeys) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (T lib : libList) {
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
        redisTemplate.opsForValue().set(currentLibKey, newLibName);
        this.currentLibName = newLibName;
    }

    /**
     * 从指定分区的牌库中随机获取一条记录
     *
     * @param sectionKey 分区 key（来自配置 typeProp）
     * @return 牌库条目，可能为 null
     */
    public T getCardLib(int sectionKey) {
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
    protected T deserialize(byte[] compressedData) {
        if (compressedData == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(compressedData);
        int originalLength = buffer.getInt();
        byte[] data = new byte[compressedData.length - 4];
        buffer.get(data);
        data = LZ4CompressionUtil.decompressFast(data, originalLength);
        return ProtostuffUtil.deserialize(data, clazz);
    }

    /**
     * 清理旧库（删除与当前库相反的那个）
     */
    public void clearOldLib() {
        if (currentLibName == null || currentLibName.isEmpty()) {
            log.debug("从 redis 删除牌库失败，currentLibName 为空");
            return;
        }
        String removeName = lib1Prefix.equals(currentLibName) ? lib2Prefix : lib1Prefix;
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
        log.info("从 redis 清理牌库 libName={}, 删除 Key 数量={}, 耗时={}ms",
                pattern, deleted, System.currentTimeMillis() - start);
    }

    /**
     * 添加生成锁
     */
    public boolean addGenerateLock() {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(genLockKey, true, 30, TimeUnit.MINUTES)
        );
    }

    /**
     * 检查是否有生成锁
     */
    public boolean hasGenerateLock() {
        Object o = redisTemplate.opsForValue().get(genLockKey);
        return o != null && Boolean.parseBoolean(o.toString());
    }

    /**
     * 移除生成锁
     */
    public void removeGenerateLock() {
        redisTemplate.delete(genLockKey);
    }

    /**
     * 记录最后一次生成时间
     */
    public void addGenerateTime() {
        redisTemplate.opsForValue().set(lastGenTimeKey, System.currentTimeMillis());
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
     * @param roomCfgId 房间配置 ID
     * @param initValue 初始值（来自 Room_Chess.xlsx 的 initBasePool）
     */
    public void initPoolBalance(int gameType, int roomCfgId, long initValue) {
        redisTemplate.opsForHash().putIfAbsent(POOL_PREFIX + gameType, roomCfgId, initValue);
        log.info("初始化水池余额 gameType={}, roomCfgId={}, initValue={}", gameType, roomCfgId, initValue);
    }

    /**
     * 获取当前水池余额
     *
     * @return 当前水池余额，不存在返回 0
     */
    public long getPoolBalance(int gameType, int roomCfgId) {
        Object val = redisTemplate.opsForHash().get(POOL_PREFIX + gameType, roomCfgId);
        if (val == null) return 0;
        return Long.parseLong(val.toString());
    }

    /**
     * 水池余额增减（原子操作）
     * 正数 = 系统收钱（玩家输），负数 = 系统赔钱（玩家赢）
     *
     * @return 操作后的余额
     */
    public long addPoolBalance(int gameType, int roomCfgId, long value) {
        return redisTemplate.opsForHash().increment(POOL_PREFIX + gameType, roomCfgId, value);
    }
}
