package com.jjg.game.core.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * @author 11
 * @date 2025/9/25 20:01
 */
@Repository
public class BlackListDao {
    private final String ipTableName = "blackIp";
    private final String idTableName = "blackId";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 检查ip是否在黑名单中
     *
     * @param ip
     * @return
     */
    public boolean blackIp(String ip) {
        Boolean member = redisTemplate.opsForSet().isMember(ipTableName, ip);
        if (member == null) {
            return false;
        }
        return member;
    }

    /**
     * 添加黑名单
     *
     * @param ip
     */
    public void addBlackIp(String ip) {
        redisTemplate.opsForSet().add(ipTableName, ip);
    }

    /**
     * 添加黑名单
     *
     * @param ip
     */
    public void addBlackIps(List<String> ip) {
        redisTemplate.opsForSet().add(ipTableName, ip.toArray());
    }

    /**
     * 添加黑名单
     *
     * @param ip
     */
    public void removeBlackIps(List<String> ip) {
        redisTemplate.opsForSet().remove(ipTableName, ip.toArray());
    }

    /**
     * 移除黑名单
     *
     * @param ip
     */
    public void removeBlackIp(String ip) {
        redisTemplate.opsForSet().remove(ipTableName, ip);
    }

    /**
     * 获取所有黑名单ip
     *
     * @return
     */
    public Set<Object> getAllBlackIp() {
        return redisTemplate.opsForSet().members(ipTableName);
    }

    /**
     * 检查id是否在黑名单中
     *
     * @param id
     * @return
     */
    public boolean blackId(long id) {
        Boolean member = redisTemplate.opsForSet().isMember(idTableName, id);
        if (member == null) {
            return false;
        }
        return member;
    }

    /**
     * 添加黑名单
     *
     * @param id
     */
    public void addBlackId(long id) {
        redisTemplate.opsForSet().add(idTableName, id);
    }

    /**
     * 添加黑名单
     */
    public void addBlackIds(List<Long> ids) {
        redisTemplate.opsForSet().add(idTableName, ids.toArray());
    }

    /**
     * 移除黑名单
     */
    public void removeBlackIds(List<Long> ids) {
        redisTemplate.opsForSet().remove(idTableName, ids.toArray());
    }

    /**
     * 移除黑名单
     *
     * @param id
     */
    public void removeBlackId(long id) {
        redisTemplate.opsForSet().remove(idTableName, id);
    }

    /**
     * 获取所有黑名单id
     *
     * @return
     */
    public Set<Object> getAllBlackId() {
        return redisTemplate.opsForSet().members(idTableName);
    }
}
