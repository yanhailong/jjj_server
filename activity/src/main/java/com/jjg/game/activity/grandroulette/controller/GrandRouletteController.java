package com.jjg.game.activity.grandroulette.controller;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import com.jjg.game.activity.common.controller.BaseActivityController;
import com.jjg.game.activity.common.dao.RecordDao;
import com.jjg.game.activity.common.data.ActivityData;
import com.jjg.game.activity.common.data.ActivityTargetType;
import com.jjg.game.activity.common.data.ActivityType;
import com.jjg.game.activity.common.data.PlayerActivityData;
import com.jjg.game.activity.common.message.bean.BaseActivityDetailInfo;
import com.jjg.game.activity.constant.ActivityConstant;
import com.jjg.game.activity.grandroulette.dao.GrandRouletteDao;
import com.jjg.game.activity.grandroulette.data.GrandRouletteRechargeActivityData;
import com.jjg.game.activity.grandroulette.data.GrandRouletteRecord;
import com.jjg.game.activity.grandroulette.data.GrandRouletteSubordinateInfo;
import com.jjg.game.activity.grandroulette.message.bean.GrandRouletteActivityInfo;
import com.jjg.game.activity.grandroulette.message.bean.GrandRouletteRewardInfo;
import com.jjg.game.activity.grandroulette.message.bean.GrandRouletteSubordinate;
import com.jjg.game.activity.grandroulette.message.req.ReqGrandRouletteHistory;
import com.jjg.game.activity.grandroulette.message.res.*;
import com.jjg.game.activity.sharepromote.dao.SharePromoteDao;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.Pair;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.common.utils.TimeHelper;
import com.jjg.game.core.base.gameevent.EGameEventType;
import com.jjg.game.core.base.gameevent.GameEvent;
import com.jjg.game.core.base.gameevent.GameEventListener;
import com.jjg.game.core.base.gameevent.PlayerEvent;
import com.jjg.game.core.constant.AddType;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.dao.AccountDao;
import com.jjg.game.core.data.*;
import com.jjg.game.core.service.PlayerSessionService;
import com.jjg.game.core.utils.ItemUtils;
import com.jjg.game.core.utils.RedisUtils;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.BaseCfgBean;
import com.jjg.game.sampledata.bean.FreespinCfg;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lm
 * @date 2025/9/3
 */
@Component
public class GrandRouletteController extends BaseActivityController implements GameEventListener {

    public static final String PREFIX = "grandroulette";
    private final Logger log = LoggerFactory.getLogger(GrandRouletteController.class);
    private final RecordDao recordDao;
    private final GrandRouletteDao grandRouletteDao;
    private final int DEFAULT_ID = 0;
    private final AccountDao accountDao;
    private final SharePromoteDao sharePromoteDao;
    private final int DEFAULT_TIME = 72;
    private final int DEFAULT_INDEX = 1;
    private final PlayerSessionService playerSessionService;

    public GrandRouletteController(RecordDao recordDao, GrandRouletteDao grandRouletteDao, AccountDao accountDao,
                                   SharePromoteDao sharePromoteDao, PlayerSessionService playerSessionService) {
        this.recordDao = recordDao;
        this.grandRouletteDao = grandRouletteDao;
        this.accountDao = accountDao;
        this.sharePromoteDao = sharePromoteDao;
        this.playerSessionService = playerSessionService;
    }

    @Override
    public boolean addPlayerProgress(Player player, ActivityData activityData, long progress, long activityTargetKey, Object additionalParameters) {
        //这个活动不主动刷新
        long playerId = player.getId();
        long beneficiaryPlayerId = getBeneficiaryPlayerId(playerId);
        if (beneficiaryPlayerId == 0) {
            return false;
        }
        Map<Integer, GrandRouletteRechargeActivityData> playerActivityDataMap = playerActivityDao.getPlayerActivityData(beneficiaryPlayerId, activityData.getType(), activityData.getId());
        if (playerActivityDataMap == null) {
            return false;
        }
        GrandRouletteRechargeActivityData playerActivityData = playerActivityDataMap.get(DEFAULT_ID);
        if (playerActivityData == null || playerActivityData.getEndTime() < System.currentTimeMillis()) {
            return false;
        }

        boolean effectiveBet = (activityTargetKey & ActivityTargetType.EFFECTIVE_BET.getTargetKey()) != 0;
        if (effectiveBet) {
            //添加有效流水进度
            grandRouletteDao.addCumulativeGold(activityData.getId(), playerId, progress);
        } else {
            //添加充值进度
            grandRouletteDao.addCumulativeRecharge(activityData.getId(), playerId, RedisUtils.fromLong(progress));
        }
        return false;
    }

    /**
     * 获取上级id
     *
     */
    private long getBeneficiaryPlayerId(long playerId) {
        // 获取playerId绑定的玩家信息
        String bindInfo = sharePromoteDao.getBindInfo(playerId);
        if (StringUtils.isEmpty(bindInfo)) {
            return 0;
        }
        String[] bindInfoArr = StringUtils.split(bindInfo, "_");
        if (bindInfoArr.length != 2) {
            return 0;
        }
        //被绑定的玩家id
        return Long.parseLong(bindInfoArr[0]);
    }

    /**
     * 玩家参加大转盘活动
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @param times        加入次数（暂未使用）
     * @return 返回大转盘活动详情
     */
    @Override
    public AbstractResponse joinActivity(Player player, ActivityData activityData, int detailId, int times) {
        long playerId = player.getId();
        long activityId = activityData.getId();

        ResGrandRouletteJoinActivity res = new ResGrandRouletteJoinActivity(Code.SUCCESS);
        res.activityId = activityId;
        Map<Integer, FreespinCfg> baseCfgBeanMap = getDetailCfgBean(activityData);
        if (CollectionUtil.isEmpty(baseCfgBeanMap)) {
            log.error("配置错误 未找到大转盘配置 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.PARAM_ERROR;
            return res;
        }
        Pair<Long, Long> playerTimes = grandRouletteDao.getPlayerTimes(activityId, playerId);
        if (playerTimes.getSecond() <= 0) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        //获取目标金币
        GlobalConfigCfg targetCfg = GameDataManager.getGlobalConfigCfg(129);
        if (targetCfg == null) {
            log.error("配置错误 未找到大转盘全局配置129 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        ConditionParam param = getConditionParam();
        if (param == null) {
            log.error("配置错误 未找到大转盘全局配置128 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //目标金币数量
        long targetNum = targetCfg.getIntValue();
        int ratio = 0;
        Map<Long, Integer> timesConfig = Map.of();
        //获取配置
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(127);
        if (globalConfigCfg != null) {
            String configValue = globalConfigCfg.getValue();
            if (StringUtils.isNotEmpty(configValue)) {
                String[] configArr = StringUtils.split(configValue, "|");
                if (configArr.length > 0) {
                    if (configArr.length == 1) {
                        ratio = Integer.parseInt(configArr[0]);
                    } else if (configArr.length == 2) {
                        ratio = Integer.parseInt(configArr[1]);
                        String[] timesCfg = StringUtils.split(configArr[0], "_");
                        timesConfig = new HashMap<>(timesCfg.length);
                        for (int i = 0; i < timesCfg.length; i++) {
                            timesConfig.put((long) i, Integer.parseInt(timesCfg[i]));
                        }
                    }
                }
            }
        }
        //根据玩家次数计算金币数量
        Integer index = timesConfig.getOrDefault(playerTimes.getFirst(), 0);
        //修改金币
        Map<Integer, GrandRouletteRechargeActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId,
                ActivityType.GRAND_ROULETTE, activityData.getId());
        if (playerActivityData == null) {
            playerActivityData = new HashMap<>();
            grandRouletteDao.addCumulativeTimes(activityData.getId(), playerId, 1, 0, 0);
        }
        GrandRouletteRechargeActivityData data = playerActivityData.computeIfAbsent(DEFAULT_ID, key -> new GrandRouletteRechargeActivityData());
        if (data.getEndTime() == 0) {
            data.setActivityId(activityData.getId());
            data.setEndTime(getRealEndTime());
        }
        if (data.getEndTime() < System.currentTimeMillis()) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        BigDecimal getNum;
        if (index > 0) {
            //添加金币
            FreespinCfg freespinCfg = baseCfgBeanMap.get(index);
            if (freespinCfg == null) {
                res.code = Code.SAMPLE_ERROR;
                return res;
            }
            int randomNum = RandomUtil.randomInt(freespinCfg.getLowerlimit(), freespinCfg.getUpperlimit(), true, true);
            getNum = getGetNumByRatio(randomNum, targetNum);
        } else {
            //没有固定的次数 如果次数小于限制则计算插值倍率
            if (playerTimes.getFirst() < param.needNum()) {
                long need = targetNum - data.getCumulativeGold();
                getNum = BigDecimal.valueOf(need)
                        .multiply(BigDecimal.valueOf(ratio))
                        .divide(BigDecimal.valueOf(10000), RoundingMode.DOWN);
                index = DEFAULT_INDEX;
            } else {
                FreespinCfg freespinCfg = RandomUtil.randomEle(baseCfgBeanMap.values().stream().toList());
                int randomNum = RandomUtil.randomInt(freespinCfg.getLowerlimit(), freespinCfg.getUpperlimit(), true, true);
                getNum = getGetNumByRatio(randomNum, targetNum);
                index = freespinCfg.getId();
            }
        }
        long cumulativeGold = data.getCumulativeGold() + getNum.longValue();
        //大于次数则检查其他条件
        boolean canGet = checkRewardCondition(activityId, playerId, param);
        if (!canGet && cumulativeGold >= targetNum) {
            cumulativeGold = data.getCumulativeGold();
            getNum = BigDecimal.ZERO;
            index = DEFAULT_INDEX;
        }
        if (cumulativeGold >= targetNum) {
            data.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
        }
        //扣除次数
        long remainTimes = grandRouletteDao.addCumulativeTimes(activityData.getId(), playerId, 0, 1, -1);
        data.setCumulativeGold(cumulativeGold);
        //回存数据
        playerActivityDao.savePlayerActivityData(playerId, ActivityType.GRAND_ROULETTE, activityData.getId(), playerActivityData);
        res.index = index;
        res.remainTimes = (int) remainTimes;
        res.infoList = ItemUtils.buildGoldInfo(getNum.longValue());
        res.currentGold = data.getCumulativeGold();
        res.targetGold = targetNum;
        return res;
    }

    private long getRealEndTime() {
        GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(130);
        long currentTimeMillis = System.currentTimeMillis();
        if (globalConfigCfg == null) {
            return currentTimeMillis + TimeHelper.ONE_HOUR_OF_MILLIS * DEFAULT_TIME;
        }
        return currentTimeMillis + (long) globalConfigCfg.getIntValue() * TimeHelper.ONE_HOUR_OF_MILLIS;
    }

    private BigDecimal getGetNumByRatio(int randomNum, long targetNum) {
        return BigDecimal.valueOf(randomNum)
                .multiply(BigDecimal.valueOf(targetNum))
                .divide(BigDecimal.valueOf(1000000), RoundingMode.DOWN);
    }


    public ConditionParam getConditionParam() {
        GlobalConfigCfg limitCfg = GameDataManager.getGlobalConfigCfg(128);
        if (limitCfg == null || StringUtils.isEmpty(limitCfg.getValue())) {
            return null;
        }
        String[] limitCfgArr = StringUtils.split(limitCfg.getValue(), "|");
        if (limitCfgArr.length != 4) {
            return null;
        }
        //需要人数
        int needNum = Integer.parseInt(limitCfgArr[0]);
        //需要金币数
        int needGoldNum = Integer.parseInt(limitCfgArr[1]);
        //需要充值金额
        BigDecimal needRechargeNum = new BigDecimal(limitCfgArr[2]);
        //需要达成上面条件的人数
        int needConcludeNum = Integer.parseInt(limitCfgArr[3]);
        return new ConditionParam(needNum, needGoldNum, needRechargeNum, needConcludeNum);
    }

    @Override
    public <T extends GameEvent> void handleEvent(T gameEvent) {
        if (gameEvent instanceof PlayerEvent playerEvent && playerEvent.getNewlyValue() instanceof Account account) {
            Player player = playerEvent.getPlayer();
            //判断是否有活动开启
            ActivityData openActivityData = activityManager.getOpenActivityData(player, ActivityType.GRAND_ROULETTE);
            if (openActivityData == null) {
                return;
            }
            //判断是否有上级
            long playerId = player.getId();
            long beneficiaryPlayerId = getBeneficiaryPlayerId(playerId);
            if (beneficiaryPlayerId == 0) {
                return;
            }
            long activityId = openActivityData.getId();
            GrandRouletteRechargeActivityData playerActivityData = getGrandRouletteRechargeActivityData(beneficiaryPlayerId, activityId);
            if (playerActivityData == null) {
                return;
            }
            if (!grandRouletteDao.addBindIpInfo(account.getRegisterIp(), account.getRegisterMac())) {
                return;
            }
            //进行绑定和加次数
            grandRouletteDao.addCumulativeTimes(activityId, beneficiaryPlayerId, 0, 0, 1);
            //添加到下级
            grandRouletteDao.addSubordinateId(activityId, beneficiaryPlayerId, playerId, TimeHelper.nowInt());
            //通知变化
            NotifyBindSubordinatesChange notify = new NotifyBindSubordinatesChange();
            notify.bindSubordinates = buildSubordinateInfo(playerId, activityId);
            PFSession session = playerSessionService.getSession(playerId);
            if (session == null) {
                return;
            }
            session.send(notify);
            updateRodDot(beneficiaryPlayerId, openActivityData, false, true);
        }
    }

    private GrandRouletteRechargeActivityData getGrandRouletteRechargeActivityData(long playerId, long activityId) {
        Map<Integer, GrandRouletteRechargeActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId,
                ActivityType.GRAND_ROULETTE, activityId);
        if (playerActivityData == null) {
            return null;
        }
        return playerActivityData.get(DEFAULT_ID);
    }

    @Override
    public List<EGameEventType> needMonitorEvents() {
        return List.of(EGameEventType.BIND_PHONE);
    }

    /**
     * 检查达成条件
     */
    public boolean checkRewardCondition(long activityId, long playerId, ConditionParam param) {
        GrandRouletteSubordinateInfo subordinateIds = grandRouletteDao.getSubordinateIds(activityId, playerId);
        if (subordinateIds == null) {
            return false;
        }
        Map<Long, Integer> subordinateMap = subordinateIds.getSubordinateMap();
        if (CollectionUtil.isEmpty(subordinateMap)) {
            return false;
        }
        if (subordinateMap.size() < param.needNum) {
            return false;
        }
        //获取充值流水
        int reachedNum = 0;
        Map<Long, Long> multipleCumulativeRecharge = grandRouletteDao.getMultipleCumulativeRecharge(activityId, subordinateMap.keySet());
        for (Map.Entry<Long, Long> entry : multipleCumulativeRecharge.entrySet()) {
            BigDecimal bigDecimal = RedisUtils.fromLong(entry.getValue());
            if (bigDecimal.compareTo(param.needRechargeNum) >= 0) {
                reachedNum++;
            }
        }
        if (reachedNum < param.needConcludeNum) {
            return false;
        }
        //检查金币流水
        reachedNum = 0;
        Map<Long, Long> multipleCumulativeGold = grandRouletteDao.getMultipleCumulativeGold(activityId, subordinateMap.keySet());
        for (Map.Entry<Long, Long> entry : multipleCumulativeGold.entrySet()) {
            if (entry.getValue() >= param.needGoldNum) {
                reachedNum++;
            }
        }
        return reachedNum >= param.needConcludeNum;
    }

    /**
     * 玩家大转盘领取奖励
     *
     * @param player       玩家对象
     * @param activityData 活动数据
     * @param detailId     活动明细ID
     * @return 返回领取奖励结果
     */
    @Override
    public AbstractResponse claimActivityRewards(Player player, ActivityData activityData, int detailId) {
        long playerId = player.getId();
        long activityId = activityData.getId();
        ResGrandRouletteClaimRewards res = new ResGrandRouletteClaimRewards(Code.SUCCESS);
        //获取目标金币
        GlobalConfigCfg targetCfg = GameDataManager.getGlobalConfigCfg(129);
        if (targetCfg == null) {
            log.error("领奖配置错误 未找到大转盘全局配置129 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        //获取玩家金币数量
        Map<Integer, GrandRouletteRechargeActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId,
                ActivityType.GRAND_ROULETTE, activityData.getId());
        GrandRouletteRechargeActivityData data = playerActivityData.get(DEFAULT_ID);
        if (data == null) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        if (data.getEndTime() <= System.currentTimeMillis()) {
            res.code = Code.ERROR_REQ;
            return res;
        }
        if (data.getClaimStatus() == ActivityConstant.ClaimStatus.CLAIMED) {
            res.code = Code.REPEAT_OP;
            return res;
        }
        long cumulativeGold = data.getCumulativeGold();
        if (targetCfg.getIntValue() > cumulativeGold) {
            log.error("领奖配置错误 未达成金币条件领取奖励 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.ERROR_REQ;
            return res;
        }
        ConditionParam conditionParam = getConditionParam();
        if (conditionParam == null) {
            log.error("领奖配置错误 未找到大转盘全局配置128 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.SAMPLE_ERROR;
            return res;
        }
        boolean checked = checkRewardCondition(activityId, playerId, conditionParam);
        if (!checked) {
            log.error("领奖配置错误 未达成条件领取奖励 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            res.code = Code.ERROR_REQ;
            return res;
        }
        //领取奖励
        data.setClaimStatus(ActivityConstant.ClaimStatus.CLAIMED);
        data.setCumulativeGold(0);
        playerActivityDao.savePlayerActivityData(playerId, ActivityType.GRAND_ROULETTE, activityData.getId(), playerActivityData);
        CommonResult<ItemOperationResult> result = playerPackService.addItems(playerId, Map.of(ItemUtils.getGoldItemId(), cumulativeGold), AddType.GRAND_ROULETTE_REWARDS);
        if (!result.success()) {
            log.error("发送奖励失败 回滚数据 playerId:{} activityId:{} detailId:{}", playerId, activityData.getId(), detailId);
            data.setCumulativeGold(cumulativeGold);
            data.setClaimStatus(ActivityConstant.ClaimStatus.CAN_CLAIM);
            playerActivityDao.savePlayerActivityData(playerId, ActivityType.GRAND_ROULETTE, activityData.getId(), playerActivityData);
            res.code = Code.UNKNOWN_ERROR;
            return res;
        }
        try {
            GrandRouletteRecord grandRouletteRecord = new GrandRouletteRecord();
            grandRouletteRecord.setNum(cumulativeGold);
            grandRouletteRecord.setPlayerId(playerId);
            grandRouletteRecord.setReachedTimes(System.currentTimeMillis());
            recordDao.addRecord(PREFIX, activityId, playerId, grandRouletteRecord, 100, true);
        } catch (Exception e) {
            log.error("大转盘添加记录失败 playerId:{}", playerId, e);
        }
        res.activityId = activityId;
        res.infoList = ItemUtils.buildGoldInfo(cumulativeGold);
        return res;
    }

    /**
     * 请求大转盘历史记录
     *
     * @param player 玩家数据
     * @param req    请求参数
     * @return 响应结果
     */
    public AbstractResponse reqGrandRouletteHistory(Player player, ReqGrandRouletteHistory req) {
        // 查询玩家或全局中奖记录（分页）
        ResGrandRouletteHistory res = new ResGrandRouletteHistory(Code.SUCCESS);
        Pair<Boolean, List<GrandRouletteRecord>> playerRecordActivities = null;
        // type == 1：个人记录，type == 2：全局记录
        if (req.type == 1) {
            playerRecordActivities = recordDao.getPlayerRecords(PREFIX, req.activityId, player.getId(),
                    req.startIndex, req.startIndex + Math.min(req.size, ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM), GrandRouletteRecord.class);
        } else if (req.type == 2) {
            playerRecordActivities = recordDao.getRecords(PREFIX, req.activityId, req.startIndex, req.startIndex +
                    Math.min(req.size, ActivityConstant.OfficialAwards.GET_MAX_RECORD_NUM), GrandRouletteRecord.class);
        }
        if (playerRecordActivities != null && CollectionUtil.isNotEmpty(playerRecordActivities.getSecond())) {
            res.recordList = playerRecordActivities.getSecond();
            // 是否还有下一页（由 DAO 返回的布尔值）
            res.hasNext = playerRecordActivities.getFirst();
            res.startIndex = req.startIndex;
        }
        res.type = req.type;
        return res;
    }

    /**
     * 构建玩家大转盘活动详情
     *
     * @param player       玩家数据
     * @param activityData 活动ID
     * @param baseCfgBean  活动配置
     * @param data         玩家大转盘数据
     * @return 返回大转盘详情信息
     */
    @Override
    public BaseActivityDetailInfo buildPlayerActivityDetail(Player player, ActivityData activityData, BaseCfgBean baseCfgBean, PlayerActivityData data) {
        BaseActivityDetailInfo baseActivityDetailInfo = new BaseActivityDetailInfo();
        baseActivityDetailInfo.activityId = activityData.getId();
        return baseActivityDetailInfo;
    }

    /**
     * 获取玩家大转盘活动明细
     */
    @Override
    public AbstractResponse getPlayerActivityDetail(Player player, ActivityData activityData, int detailId) {
        return null;
    }

    /**
     * 构建活动类型信息
     */
    @Override
    public AbstractResponse getPlayerActivityInfoByTypeRes(Player player, Map<Long, List<BaseActivityDetailInfo>> allDetailInfo) {
        ResGrandRouletteTypeInfo cardTypeInfo = new ResGrandRouletteTypeInfo(Code.SUCCESS);
        if (CollectionUtil.isEmpty(allDetailInfo)) {
            return cardTypeInfo;
        }
        cardTypeInfo.activityData = new ArrayList<>();
        GlobalConfigCfg targetCfg = GameDataManager.getGlobalConfigCfg(129);
        if (targetCfg == null) {
            return cardTypeInfo;
        }
        for (Long activityId : allDetailInfo.keySet()) {
            ActivityData activityData = activityManager.getActivityData().get(activityId);
            if (activityData == null) {
                continue;
            }
            GrandRouletteActivityInfo grandRouletteActivityInfo = new GrandRouletteActivityInfo();
            Map<Integer, FreespinCfg> detailCfgBean = getDetailCfgBean(activityData);
            if (CollectionUtil.isEmpty(detailCfgBean)) {
                continue;
            }
            grandRouletteActivityInfo.detailInfos = new ArrayList<>();
            for (Map.Entry<Integer, FreespinCfg> entry : detailCfgBean.entrySet()) {
                FreespinCfg value = entry.getValue();
                GrandRouletteRewardInfo rewardInfo = new GrandRouletteRewardInfo();
                long upperNum = getGetNumByRatio(value.getUpperlimit(), targetCfg.getIntValue()).longValue();
                rewardInfo.rewardMax = ItemUtils.buildGoldInfo(upperNum);
                long lowerNUm = getGetNumByRatio(value.getLowerlimit(), targetCfg.getIntValue()).longValue();
                rewardInfo.rewardMin = ItemUtils.buildGoldInfo(lowerNUm);
                rewardInfo.index = entry.getKey();
                grandRouletteActivityInfo.detailInfos.add(rewardInfo);
            }
            grandRouletteActivityInfo.targetGold = targetCfg.getIntValue();
            Map<Integer, GrandRouletteRechargeActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(player.getId(), ActivityType.GRAND_ROULETTE, activityId);
            GrandRouletteRechargeActivityData data = playerActivityData.get(DEFAULT_ID);
            if (data != null) {
                grandRouletteActivityInfo.endTime = data.getEndTime();
                grandRouletteActivityInfo.playerState = data.getClaimStatus();
                grandRouletteActivityInfo.currentGold = data.getCumulativeGold();

            } else {
                grandRouletteActivityInfo.playerState = 4;
            }
            Account account = accountDao.queryAccountByPlayerId(player.getId());
            if (account.getThirdAccounts().containsKey(LoginType.PHONE)) {
                grandRouletteActivityInfo.bindPhoneState = 1;
            }
            Pair<Long, Long> playerTimes = grandRouletteDao.getPlayerTimes(activityId, player.getId());
            grandRouletteActivityInfo.remainTimes = playerTimes.getSecond().intValue();
            grandRouletteActivityInfo.bindSubordinates = buildSubordinateInfo(player.getId(), activityId);
            cardTypeInfo.activityData.add(grandRouletteActivityInfo);
        }
        return cardTypeInfo;
    }

    private List<GrandRouletteSubordinate> buildSubordinateInfo(long playerId, long activityId) {
        GrandRouletteSubordinateInfo subordinateIds = grandRouletteDao.getSubordinateIds(activityId, playerId);
        if (subordinateIds != null && CollectionUtil.isNotEmpty(subordinateIds.getSubordinateMap())) {
            List<GrandRouletteSubordinate> subordinateArrayList = new ArrayList<>(subordinateIds.getSubordinateMap().size());
            for (Map.Entry<Long, Integer> entry : subordinateIds.getSubordinateMap().entrySet()) {
                subordinateArrayList.add(new GrandRouletteSubordinate(entry.getKey(), entry.getValue()));
            }
            return subordinateArrayList;
        }
        return null;
    }

    @Override
    public Map<Integer, FreespinCfg> getDetailCfgBean(ActivityData activityData) {
        return GameDataManager.getFreespinCfgList()
                .stream()
                .filter(cfg -> activityData.getValue().contains(cfg.getId()))
                .collect(Collectors.toMap(BaseCfgBean::getId, cfg -> cfg));
    }

    @Override
    public boolean hasRedDot(long playerId, ActivityData activityData) {
        GrandRouletteRechargeActivityData data = getGrandRouletteRechargeActivityData(playerId, activityData.getId());
        if (data == null) {
            return false;
        }
        if (data.getEndTime() < System.currentTimeMillis()) {
            return false;
        }
        Pair<Long, Long> playerTimes = grandRouletteDao.getPlayerTimes(activityData.getId(), playerId);
        if (playerTimes.getSecond() > 0) {
            return true;
        }
        return super.hasRedDot(playerId, activityData);
    }

    @Override
    public Map<Integer, PlayerActivityData> checkPlayerDataAndResetOnRequest(Player player, ActivityData activityData) {
        long playerId = player.getId();
        long activityDataId = activityData.getId();
        //重置数据
        Map<Integer, PlayerActivityData> playerActivityData = playerActivityDao.getPlayerActivityData(playerId, ActivityType.GRAND_ROULETTE,
                activityDataId);
        if (playerActivityData == null) {
            return null;
        }
        GrandRouletteRechargeActivityData data = (GrandRouletteRechargeActivityData) playerActivityData.get(DEFAULT_ID);
        long currentTimeMillis = System.currentTimeMillis();
        if (data == null || data.getEndTime() == 0 || data.getEndTime() >= currentTimeMillis) {
            return null;
        }
        //需要重置数据
        data.setEndTime(0);
        data.setCumulativeGold(0);
        data.setClaimStatus(ActivityConstant.ClaimStatus.NOT_CLAIM);
        data.setRound(data.getRound() + 1);
        grandRouletteDao.resetPlayerActivityData(activityDataId, playerId);
        grandRouletteDao.addCumulativeTimes(activityDataId, playerId, 1, 0, 1);
        playerActivityDao.savePlayerActivityData(playerId, ActivityType.GRAND_ROULETTE, activityDataId, playerActivityData);
        return playerActivityData;
    }

    @Override
    public void onActivityEnd(ActivityData activityData) {
        playerActivityDao.clearActivityData(ActivityType.GRAND_ROULETTE, activityData.getId());
        grandRouletteDao.resetActivityData(activityData.getId(), PREFIX);
    }

    @Override
    public void onActivityStart(ActivityData activityData) {
        playerActivityDao.clearActivityData(ActivityType.GRAND_ROULETTE, activityData.getId());
        grandRouletteDao.resetActivityData(activityData.getId(), PREFIX);
    }

    public record ConditionParam(int needNum, int needGoldNum, BigDecimal needRechargeNum, int needConcludeNum) {
    }
}
