package com.jjg.game.hall.service;

import cn.hutool.core.collection.CollectionUtil;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.redeemcode.RedeemCodeDao;
import com.jjg.game.core.dao.redeemcode.RedeemCodeInfoDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.PlayerPackService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.hall.logger.HallLogger;
import com.jjg.game.hall.pb.res.ResRedeemCode;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.LongCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * @author lm
 * @date 2026/3/12 15:59
 */
@Service
public class RedeemCodeService {
    private static final Logger log = LoggerFactory.getLogger(RedeemCodeService.class);
    private static final long REDEEM_CODE_COOLDOWN_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final String CODE_LOCK_PREFIX = "redeemCode:code:%s";
    private static final String COOLDOWN_KEY_PREFIX = "redeemCode:cooldown:%s";

    private final RedeemCodeInfoDao redeemCodeInfoDao;
    private final RedeemCodeDao redeemCodeDao;
    private final PlayerPackService playerPackService;
    private final RedisLock redisLock;
    private final RedissonClient redissonClient;
    private final HallLogger hallLogger;

    public RedeemCodeService(RedeemCodeInfoDao redeemCodeInfoDao, RedeemCodeDao redeemCodeDao, PlayerPackService playerPackService,
                             RedisLock redisLock, RedissonClient redissonClient, HallLogger hallLogger) {
        this.redeemCodeInfoDao = redeemCodeInfoDao;
        this.redeemCodeDao = redeemCodeDao;
        this.playerPackService = playerPackService;
        this.redisLock = redisLock;
        this.redissonClient = redissonClient;
        this.hallLogger = hallLogger;
    }

    /**
     * 礼包码领取
     *
     * @param playerController 玩家数据
     * @param code             礼包码
     * @return 响应
     */
    public ResRedeemCode redeem(PlayerController playerController, String code) {
        ResRedeemCode res = new ResRedeemCode(Code.SUCCESS);
        if (playerController == null || playerController.playerId() < 1) {
            res.code = Code.PARAM_ERROR;
            return res;
        }
        if (StringUtils.isBlank(code)) {
            res.code = Code.REDEEM_CODE_INVALID;
            return res;
        }
        String finalCode = code.trim();
        long playerId = playerController.playerId();
        long now = System.currentTimeMillis();
        long cooldownEndTime = getCooldownEndTime(playerId);
        if (cooldownEndTime > now) {
            res.code = Code.REDEEM_CODE_ERROR_USE;
            res.cooldownEndTime = cooldownEndTime;
            return res;
        }
        boolean codeLock = false;
        String codeLockKey = CODE_LOCK_PREFIX.formatted(finalCode);
        try {
            codeLock = redisLock.tryLockWithDefaultTime(codeLockKey);
            if (!codeLock) {
                log.warn("礼包码兑换获取礼包码锁失败 playerId = {}, code = {}", playerId, finalCode);
                res.code = Code.FAIL;
                return res;
            }
            RedeemCode redeemCode = redeemCodeDao.findById(finalCode).orElse(null);
            if (redeemCode == null) {
                log.info("礼包码不存在 playerId = {}, code = {}", playerId, finalCode);
                res.cooldownEndTime = setCooldownEndTime(playerId);
                res.code = Code.REDEEM_CODE_INVALID;
                return res;
            }
            if (redeemCode.getUsePlayerId() > 0) {
                log.info("礼包码已被使用 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                res.cooldownEndTime = setCooldownEndTime(playerId);
                res.code = Code.REDEEM_CODE_REPEAT_USE;
                return res;
            }
            Optional<RedeemCodeInfo> redeemCodeInfoOptional = redeemCodeInfoDao.findById(redeemCode.getRedeemId());
            RedeemCodeInfo redeemCodeInfo = redeemCodeInfoOptional.orElse(null);
            if (redeemCodeInfo == null) {
                log.warn("礼包码配置不存在 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                res.cooldownEndTime = setCooldownEndTime(playerId);
                res.code = Code.REDEEM_CODE_INVALID;
                return res;
            }
            if (!redeemCodeInfo.isUse() || now < redeemCodeInfo.getStartTime() || now > redeemCodeInfo.getEndTime()) {
                log.info("礼包码未生效或已过期 playerId = {}, code = {}, redeemId = {}, startTime = {}, endTime = {}, isUse = {}",
                        playerId, finalCode, redeemCode.getRedeemId(), redeemCodeInfo.getStartTime(), redeemCodeInfo.getEndTime(), redeemCodeInfo.isUse());
                res.cooldownEndTime = setCooldownEndTime(playerId);
                res.code = Code.REDEEM_CODE_INVALID;
                return res;
            }
            if (redeemCodeDao.queryRedeemCodesByRedeemIdAndUsePlayerId(redeemCode.getRedeemId(), playerId)) {
                res.code = Code.REDEEM_CODE_PLAYER_REPEAT_USE;
                log.info("玩家已领取过相同礼包 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                return res;
            }

            if (!redeemCodeDao.updateUsePlayerIdAndUseTime(finalCode, playerId)) {
                res.code = Code.REDEEM_CODE_REPEAT_USE;
                log.info("玩家领取礼包时礼包码被使用 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                return res;
            }

            Map<Integer, Long> rewards = redeemCodeInfo.getRewardsItem();
            if (CollectionUtil.isNotEmpty(rewards)) {
                CommonResult<ItemOperationResult> result = playerPackService.addItems(playerId, rewards, AddType.REDEEM_CODE_REWARDS, finalCode);
                if (!result.success()) {
                    log.info("礼包码奖励发送失败进行回滚 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                    redeemCodeDao.rollBackUsePlayerIdAndUseTime(finalCode, playerId);
                    log.info("礼包码回滚完成 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                    res.code = Code.UNKNOWN_ERROR;
                    return res;
                }
                //记录日志
                hallLogger.sendRedeemLog(playerController.getPlayer(), redeemCodeInfo.getId(), rewards, result.data, finalCode);
                res.rewardItemInfos = ItemUtils.buildItemInfo(rewards);
            } else {
                log.warn("礼包码奖励配置为空 playerId = {}, code = {}, redeemId = {}", playerId, finalCode, redeemCode.getRedeemId());
                hallLogger.sendRedeemLog(playerController.getPlayer(), redeemCodeInfo.getId(), rewards, null, finalCode);
            }
            return res;
        } catch (Exception e) {
            log.error("礼包码兑换异常 playerId = {}, code = {}", playerId, finalCode, e);
            res.code = Code.EXCEPTION;
            return res;
        } finally {
            if (codeLock) {
                redisLock.tryUnlock(codeLockKey);
            }
        }
    }


    private long getCooldownEndTime(long playerId) {
        RBucket<Long> bucket = redissonClient.getBucket(COOLDOWN_KEY_PREFIX.formatted(playerId), LongCodec.INSTANCE);
        Long cooldownEndTime = bucket.get();
        return cooldownEndTime == null ? 0 : cooldownEndTime;
    }

    private long setCooldownEndTime(long playerId) {
        long cooldownEndTime = System.currentTimeMillis() + REDEEM_CODE_COOLDOWN_MILLIS;
        redissonClient.getBucket(COOLDOWN_KEY_PREFIX.formatted(playerId), LongCodec.INSTANCE)
                .set(cooldownEndTime, Duration.ofMillis(REDEEM_CODE_COOLDOWN_MILLIS));
        return cooldownEndTime;
    }
}
