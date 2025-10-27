package com.jjg.game.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 房间排序分值工具类
 * <p>
 * 用于在 Redis ZSet 中为房间生成可比较的分值(score)，
 * 实现以下排序规则（从小到大）：
 * maxPlayers 越小越靠前；
 * 2️⃣ readyPlayers 越小越靠前；
 * 3️⃣ seconds 越小（创建时间早）越靠前。
 * <p>
 * 结构设计：
 * ┌────────────┬────────────┬─────────────────────────────
 * │ maxPlayers │ readyPlayers │ seconds │
 * │ (10 bits)  │ (10 bits)    │ (32 bits) │
 * └────────────┴────────────┴─────────────────────────────┘
 *
 * @author GPT
 */
public class RoomScoreUtil {

    private static final Logger log = LoggerFactory.getLogger(RoomScoreUtil.class);

    private static final int MAX_PLAYER_BITS = 10;
    private static final int READY_PLAYER_BITS = 10;
    private static final int SECONDS_BITS = 32;

    private static final long MAX_PLAYER_MASK = (1L << MAX_PLAYER_BITS) - 1;  // 0x3FF
    private static final long READY_PLAYER_MASK = (1L << READY_PLAYER_BITS) - 1;  // 0x3FF
    private static final long SECONDS_MASK = (1L << SECONDS_BITS) - 1;  // 0xFFFFFFFF

    /**
     * 生成用于 Redis ZSet 排序的分值（score）
     *
     * @param maxPlayers   房间最大人数 (0~1024)
     * @param readyPlayers 已准备人数 (0~1024)
     * @param seconds      当前秒数 (建议使用 System.currentTimeMillis()/1000)
     * @return 可用于 ZSet 的 double score
     */
    public static double computeScore(int maxPlayers, int readyPlayers, int seconds) {
        if (maxPlayers < 0 || maxPlayers > MAX_PLAYER_MASK) {
            log.error("maxPlayers {} is invalid", maxPlayers);
            return 0;
        }
        if (readyPlayers < 0 || readyPlayers > READY_PLAYER_MASK) {
            log.error("readyPlayers {} is invalid", readyPlayers);
        }

        long score = ((long) maxPlayers << (READY_PLAYER_BITS + SECONDS_BITS))  // 高10位
                | ((long) readyPlayers << SECONDS_BITS)                      // 中10位
                | (seconds & SECONDS_MASK);                                  // 低32位
        return (double) score;
    }

    /**
     * 反解析分值
     *
     * @param scoreValue Redis ZSet 中的分值
     * @return 房间属性对象
     */
    public static RoomScoreInfo parseScore(double scoreValue) {
        long score = (long) scoreValue;

        int seconds = (int) (score & SECONDS_MASK);
        int readyPlayers = (int) ((score >> SECONDS_BITS) & READY_PLAYER_MASK);
        int maxPlayers = (int) ((score >> (READY_PLAYER_BITS + SECONDS_BITS)) & MAX_PLAYER_MASK);

        return new RoomScoreInfo(maxPlayers, readyPlayers, seconds);
    }

    /**
     * 封装房间信息
     */
    public record RoomScoreInfo(int maxPlayers, int readyPlayers, int seconds) {
    }

    // 测试
    public static void main(String[] args) {
        int maxPlayers = 8;
        int readyPlayers = 3;
        int seconds = (int) (System.currentTimeMillis() / 1000);

        double score = computeScore(maxPlayers, readyPlayers, seconds);
        System.out.println("生成的score = " + (long) score);

        RoomScoreInfo info = parseScore(35186133385773L);
        System.out.println("反解析: " + info);
    }
}
