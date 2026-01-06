package com.jjg.game.core.recharge.dao;

import org.redisson.api.RDeque;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 离线充值 DAO
 * key: offlineRecharge:{playerId}
 * value: RDeque<String>（充值 JSON）
 *
 * @author lm
 * @date 2026/1/5
 */
@Repository
public class OfflineRechargeDao {

    private static final String KEY_PATTERN = "offlineRecharge:%s";

    private final RedissonClient redissonClient;

    public OfflineRechargeDao(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    private String getKey(long playerId) {
        return String.format(KEY_PATTERN, playerId);
    }

    private RDeque<String> getDeque(long playerId) {
        return redissonClient.getDeque(getKey(playerId));
    }

    /* ================== 写入 ================== */

    /**
     * 添加一条离线充值记录
     */
    public void addRecharge(long playerId, String rechargeJson) {
        getDeque(playerId).addLast(rechargeJson);
    }

    /**
     * 批量添加离线充值记录
     */
    public void addRechargeBatch(long playerId, Collection<String> rechargeJsonList) {
        if (rechargeJsonList == null || rechargeJsonList.isEmpty()) {
            return;
        }
        getDeque(playerId).addAll(rechargeJsonList);
    }

    /* ================== 读取 ================== */

    /**
     * 获取所有离线充值（不删除）
     */
    public List<String> getAll(long playerId) {
        return new ArrayList<>(getDeque(playerId));
    }

    /**
     * 弹出一条离线充值（消费）
     */
    public String pollOne(long playerId) {
        return getDeque(playerId).pollFirst();
    }

    /**
     * 一次性获取并清空所有离线充值（推荐：玩家上线时使用）
     */
    public List<String> pollAll(long playerId) {
        RDeque<String> deque = getDeque(playerId);
        List<String> result = new ArrayList<>(deque.size());
        String json;
        while ((json = deque.pollFirst()) != null) {
            result.add(json);
        }
        return result;
    }

    /* ================== 管理 ================== */

    /**
     * 当前离线充值数量
     */
    public int size(long playerId) {
        return getDeque(playerId).size();
    }

    /**
     * 是否存在离线充值
     */
    public boolean hasRecharge(long playerId) {
        return !getDeque(playerId).isEmpty();
    }

    /**
     * 清空离线充值
     */
    public void clear(long playerId) {
        getDeque(playerId).clear();
    }

    /**
     * 删除 Key（彻底移除）
     */
    public void delete(long playerId) {
        redissonClient.getKeys().delete(getKey(playerId));
    }
}
