package com.jjg.game.hall.casino.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jjg.game.common.redis.RedisJsonTemplate;
import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.constant.GameConstant;
import com.jjg.game.hall.casino.dao.PlayerBuildingDao;
import com.jjg.game.core.data.CommonResult;
import com.jjg.game.hall.casino.data.MachineInfo;
import com.jjg.game.hall.casino.data.PlayerBuilding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author 11
 * @date 2025/8/7 15:16
 */
@Service
public class PlayerBuildingService {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    private final String tableName = "player:building:";
    private final String lockTableName = "lock:" + tableName;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private RedisJsonTemplate redisJsonTemplate;
    @Autowired
    private PlayerBuildingDao playerBuildingDao;
    @Autowired
    private RedisLock redisLock;

    protected String getLockKey(long playerId) {
        return lockTableName + playerId;
    }

    /**
     * 升级机台
     */
    public CommonResult<MachineInfo> levelUpgrade(long playerId, long buildMachineId, int nextCfgId) {
        CommonResult<MachineInfo> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            MachineInfo machineInfo = getBuildingInfo(playerId, buildMachineId);
            if (Objects.isNull(machineInfo)) {
                return result;
            }
            machineInfo.setConfigId(nextCfgId);
            machineInfo.setBuildLvUpStartTime(System.currentTimeMillis());
            setMachineInfo(playerId, buildMachineId, machineInfo);
            result.code = Code.SUCCESS;
            result.data = machineInfo;
        } catch (Exception e) {
            log.error("玩家升级楼层机台时，保存 playerBuilding 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }

    /**
     * 更新赌场信息
     */
    public <T> CommonResult<T> updateData(long playerId, T data, Consumer<T> callback) {
        CommonResult<T> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            callback.accept(data);
            result.code = Code.SUCCESS;
            result.data = data;
        } catch (Exception e) {
            log.error("玩家更新赌场信息，修改数据 失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }


    /**
     * 解锁楼层
     */
    public CommonResult<Map<Integer, List<Long>>> unlockFloor(long playerId, int areaId, int unlockFloorId) {
        CommonResult<Map<Integer, List<Long>>> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);
        try {
            Map<Integer, List<Long>> buildingData = getBuildingData(playerId, areaId);
            if (Objects.isNull(buildingData)) {
                return result;
            }
            buildingData.put(unlockFloorId, new ArrayList<>());
            setBuildingData(playerId, areaId, buildingData);
            result.code = Code.SUCCESS;
            result.data = buildingData;
        } catch (Exception e) {
            log.error("解锁楼层，修改数据失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);
        }
        return result;
    }


    /**
     * 购买一键领取
     */
    public CommonResult<Long> buyOneClickClaimEndTime(long playerId, int areaId, long endTime) {
        CommonResult<Long> result = new CommonResult<>(Code.FAIL);
        String key = getLockKey(playerId);
        redisLock.lock(key, GameConstant.Redis.PER_TRY_TAKE_MILE_TIME * GameConstant.Redis.LOCK_TRY_TIMES);

        try {
            String path = String.format("$.oneClickClaimEndTimeMap.%d", areaId);
            String playerKey = getKey(playerId);
            Map<Integer, Long> oneClickClaimEndTimeMap = redisJsonTemplate.getPath(playerKey, path, new TypeReference<>() {
            });
            if (Objects.isNull(oneClickClaimEndTimeMap)) {
                return result;
            }
            oneClickClaimEndTimeMap.put(areaId, endTime);
            redisJsonTemplate.setPath(playerKey, path, oneClickClaimEndTimeMap);
            result.code = Code.SUCCESS;
            result.data = endTime;
        } catch (Exception e) {
            log.error("解锁楼层，修改数据失败 playerId={}", playerId, e);
        } finally {
            redisLock.unlock(key);

        }
        return result;
    }


    /**
     * 查询 PlayerPack 对象
     * 先查询redis
     * 再查询mongodb
     *
     * @param playerId 玩家id
     * @return 玩家建筑数据
     */
    public PlayerBuilding getFromAllDB(long playerId) {
        PlayerBuilding playerBuilding = redisGet(playerId);
        if (playerBuilding != null) {
            return playerBuilding;
        }
        return playerBuildingDao.findById(playerId);
    }

    /**
     * 持久化到mongodb
     *
     * @param playerId 玩家id
     */
    public void moveToMongo(long playerId) {
        PlayerBuilding playerBuilding = redisGet(playerId);
        if (playerBuilding == null) {
            return;
        }
        playerBuildingDao.save(playerBuilding);
        redisDel(playerBuilding.getPlayerId());
    }

    private String getKey(long playerId) {
        return tableName + playerId;
    }

    /**
     * 保存整个对象
     */
    public void redisSave(PlayerBuilding playerBuilding) {
        redisJsonTemplate.set(getKey(playerBuilding.getPlayerId()), playerBuilding);
    }

    /**
     * 删除整个对象
     */
    public void redisDel(long playerId) {
        redisTemplate.delete(getKey(playerId));
    }

    /**
     * 通过玩家ID获取玩家建筑信息
     *
     * @param playerId 玩家ID
     * @return 玩家建筑信息
     */
    public PlayerBuilding redisGet(long playerId) {
        return redisJsonTemplate.get(getKey(playerId), new TypeReference<>() {
        });
    }

    /**
     * 获取BuildingInfoInfo
     */
    public Map<Integer, List<Long>> getBuildingData(long playerId, long areaId) {
        String path = String.format("$.buildingData.%d", areaId);
        return redisJsonTemplate.getPath(getKey(playerId), path, new TypeReference<>() {
        });
    }

    /**
     * 设置BuildingInfoInfo
     */
    public void setBuildingData(long playerId, long areaId, Map<Integer, List<Long>> data) {
        String path = String.format("$.buildingData.%d", areaId);
        redisJsonTemplate.setPath(getKey(playerId), path, data);
    }

    /**
     * 获取BuildingInfoInfo
     */
    public MachineInfo getBuildingInfo(long playerId, long buildMachineId) {
        String path = String.format("$.machineInfoData.%d", buildMachineId);
        return redisJsonTemplate.getPath(getKey(playerId), path, new TypeReference<>() {
        });
    }

    /**
     * 设置MachineInfo
     */
    public void setMachineInfo(long playerId, long buildMachineId, MachineInfo machineInfo) {
        String path = String.format("$.machineInfoData.%d", buildMachineId);
        redisJsonTemplate.setPath(getKey(playerId), path, machineInfo);
    }

    /**
     * 设置OneClickClaimEndTime
     */
    public void setOneClickClaimEndTime(long playerId, int casinoId, long endTime) {
        String path = String.format("$.oneClickClaimEndTimeMap.%d", casinoId);
        redisJsonTemplate.setPath(getKey(playerId), path, endTime);
    }

    /**
     * 获取OneClickClaimEndTime
     */
    public Long getOneClickClaimEndTime(long playerId, long casinoId) {
        String path = String.format("$.oneClickClaimEndTimeMap.%d", casinoId);
        return redisJsonTemplate.getPath(getKey(playerId), path, new TypeReference<>() {
        });
    }

}
