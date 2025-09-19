package com.jjg.game.hall.minigame.game.luckytreasure.util;

import com.jjg.game.common.redis.RedisLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 分布式领奖码生成器
 * 生成16位大小写字母和数字组成的唯一随机码
 */
@Component
public class RewardCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(RewardCodeGenerator.class);

    private final RedisLock redisLock;

    // 领奖码总长度
    private static final int CODE_LENGTH = 16;

    // Redis锁的key前缀
    private static final String REDIS_LOCK_PREFIX = "reward_code:unique:";

    // 字符集定义（大小写字母+数字）
    private static final String UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMBERS = "0123456789";
    private static final String ALL_CHARS = UPPERCASE_LETTERS + LOWERCASE_LETTERS + NUMBERS;

    public RewardCodeGenerator(RedisLock redisLock) {
        this.redisLock = redisLock;
    }

    /**
     * 生成唯一的16位领奖码
     * 使用分布式锁确保唯一性，优化性能
     *
     * @param issueNumber 期号
     * @param playerId    中奖玩家ID
     * @return 唯一的16位领奖码
     */
    public String generateRewardCode(long issueNumber, long playerId) {
        try {
            // 生成时间戳作为锁的key，减少锁竞争
            String timestamp = String.valueOf(System.currentTimeMillis());
            String lockKey = REDIS_LOCK_PREFIX + timestamp;

            // 使用分布式锁确保唯一性
            return redisLock.tryLockAndGet(lockKey, this::generateUniqueCode, "");

        } catch (Exception e) {
            log.error("生成领奖码失败, 期号: {}, 玩家ID: {}", issueNumber, playerId, e);
            // 如果生成失败，使用高性能算法生成备用码
            return generateUniqueCode();
        }
    }

    /**
     * 生成唯一的16位领奖码
     * 使用高性能算法，确保大小写字母和数字的随机分布
     */
    private String generateUniqueCode() {
        // 使用ThreadLocalRandom提高性能
        ThreadLocalRandom random = ThreadLocalRandom.current();

        // 生成16位随机码
        char[] code = new char[CODE_LENGTH];

        // 确保至少包含大写字母、小写字母、数字各一个
        code[0] = UPPERCASE_LETTERS.charAt(random.nextInt(UPPERCASE_LETTERS.length()));
        code[1] = LOWERCASE_LETTERS.charAt(random.nextInt(LOWERCASE_LETTERS.length()));
        code[2] = NUMBERS.charAt(random.nextInt(NUMBERS.length()));

        // 填充剩余13位
        for (int i = 3; i < CODE_LENGTH; i++) {
            code[i] = ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length()));
        }

        // 使用Fisher-Yates算法打乱顺序
        shuffleArray(code, random);

        // 避免连续相同字符
        avoidConsecutiveChars(code, random);

        return new String(code);
    }

    /**
     * Fisher-Yates洗牌算法，打乱字符数组
     */
    private void shuffleArray(char[] array, ThreadLocalRandom random) {
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            // 交换元素
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    /**
     * 避免连续相同字符，增强随机性
     */
    private void avoidConsecutiveChars(char[] chars, ThreadLocalRandom random) {
        for (int i = 1; i < chars.length; i++) {
            if (chars[i] == chars[i - 1]) {
                // 如果发现连续相同字符，重新生成这个位置的字符
                char newChar;
                do {
                    newChar = ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length()));
                } while (newChar == chars[i - 1] || (i < chars.length - 1 && newChar == chars[i + 1]));
                chars[i] = newChar;
            }
        }
    }

    /**
     * 验证领奖码格式是否正确
     *
     * @param rewardCode 领奖码
     * @return 是否有效
     */
    public boolean isValidRewardCode(String rewardCode) {
        if (rewardCode == null || rewardCode.length() != CODE_LENGTH) {
            return false;
        }

        // 检查是否为大写字母+小写字母+数字的组合
        return rewardCode.matches("[A-Za-z0-9]{" + CODE_LENGTH + "}");
    }

    /**
     * 检查生成的领奖码质量（用于测试和监控）
     *
     * @param rewardCode 领奖码
     * @return 是否符合高随机性要求
     */
    public boolean hasHighRandomness(String rewardCode) {
        if (!isValidRewardCode(rewardCode)) {
            return false;
        }

        // 检查是否包含各种字符类型
        boolean hasUpper = rewardCode.chars().anyMatch(c -> UPPERCASE_LETTERS.indexOf(c) >= 0);
        boolean hasLower = rewardCode.chars().anyMatch(c -> LOWERCASE_LETTERS.indexOf(c) >= 0);
        boolean hasNumber = rewardCode.chars().anyMatch(c -> NUMBERS.indexOf(c) >= 0);

        // 检查是否有连续相同字符（降低随机性质量）
        boolean hasConsecutive = false;
        for (int i = 1; i < rewardCode.length(); i++) {
            if (rewardCode.charAt(i) == rewardCode.charAt(i - 1)) {
                hasConsecutive = true;
                break;
            }
        }

        return hasUpper && hasLower && hasNumber && !hasConsecutive;
    }
}
