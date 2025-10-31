package com.jjg.game.hall.pointsaward.leaderboard;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.PageUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.manager.AwardCodeManager;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardHistory;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardInfo;
import com.jjg.game.hall.pointsaward.pb.res.ResLoadLeaderboardHistory;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.GlobalConfigCfg;
import com.jjg.game.sampledata.bean.PointsAwardRankingCfg;
import org.redisson.api.RDeque;
import org.redisson.api.RMap;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 使用 Redisson 实现的分布式排行榜服务
 * 排序规则：
 * - 主要按玩家积分降序排列
 * - 积分相同时按时间升序排列（时间越早排名越靠前）
 * <p>
 * 上榜条件：玩家积分必须大于 50
 * 排行榜限制：最多只保留 50 名玩家，并支持实时获取最后一名的分数
 */
@Service
public class PointsAwardLeaderboardService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedissonClient redissonClient;
    private final RedisLock redisLock;
    private final HallPlayerService hallPlayerService;
    private final AwardCodeManager awardCodeManager;

    /**
     * 排行榜管理器
     */
    private PointsAwardLeaderboardManager manager;

    public PointsAwardLeaderboardService(RedissonClient redissonClient, RedisLock redisLock, HallPlayerService hallPlayerService, AwardCodeManager awardCodeManager) {
        this.redissonClient = redissonClient;
        this.redisLock = redisLock;
        this.hallPlayerService = hallPlayerService;
        this.awardCodeManager = awardCodeManager;
    }

    public void init(PointsAwardLeaderboardManager manager) {
        this.manager = manager;

    }

    /**
     * 玩家登录成功
     *
     * @param playerId 玩家id
     */
    public void login(long playerId) {
        RDeque<PointsAwardLeaderboardHistory> dequeHistory = redissonClient.getDeque(historyKey(playerId));
        List<PointsAwardLeaderboardHistory> historyList = dequeHistory.readAll();
        long now = System.currentTimeMillis();
        //计算领奖码过期
        historyList.forEach(history -> {
            long settlementTime = history.getExpiredTime();
            //过期
            if (settlementTime > 0 && settlementTime < now) {
                String code = history.getCode();
                awardCodeManager.deleteCode(code);
                log.info("玩家[{}]排行榜[{}]领奖码[{}]过期!清除领奖码!", playerId, history.getEndTime(), code);
            }
        });
    }

    private RScoredSortedSet<Long> set(int type) {
        String key = PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING + type;
        return redissonClient.getScoredSortedSet(key);
    }

    /**
     * 将积分与时间编码为 ZSET 分数
     * 积分为整数部分，时间偏移为小数部分（保证 < 1）
     */
    private double toScore(long points, long tsMillis) {
        double epsilon = (PointsAwardConstant.Leaderboard.TIME_BASE_MS - tsMillis) / PointsAwardConstant.Leaderboard.EPSILON_DIVISOR;
        return points + epsilon;
    }

    /**
     * 更新（或写入）玩家积分到排行榜；传入的 points 为最终总积分。
     * 不满足最低分时移除。
     * 并发控制：对同一排行榜使用分布式锁，保证读取-计算-写入与裁剪的原子性。
     */
    public void upsert(int type, long playerId, long points, long tsMillis) {
        String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + type;
        redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
            RMap<Long, Long> playerPointsMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_POINTS + type);
            long totalPoints = playerPointsMap.merge(playerId, points, Long::sum);
            RScoredSortedSet<Long> s = set(type);
            int minPoints = resolveMinPoints(type);
            log.info("upsert playerId = {},type = {}, points = {}, totalPoints = {},tsMillis = {},minPoints = {}", playerId, type, points, totalPoints, tsMillis, minPoints);
            if (totalPoints < minPoints) {
                return;
            }
            // 写入新的分数（总积分 + 时间偏移），并按照需要裁剪榜单大小
            s.add(toScore(totalPoints, tsMillis), playerId);
            int size = s.size();
            int maxSize = manager.getMaxSize(type);
            if (size > maxSize) {
                int excess = size - maxSize;
                s.removeRangeByRank(0, excess - 1);
            }
        });
    }

    // 根据时间戳判定上午或下午榜类型
    private int resolveHalfDayType(long tsMillis) {
        LocalDate date = LocalDate.ofInstant(Instant.ofEpochMilli(tsMillis), ZoneId.systemDefault());
        long startOfDayMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long noonMillis = startOfDayMillis + 12L * 60 * 60 * 1000;
        return tsMillis < noonMillis ? PointsAwardConstant.Leaderboard.AM : PointsAwardConstant.Leaderboard.PM;
    }

    /**
     * 判断指定类型的排行榜在当前时间是否处于活跃状态
     *
     * @param type 排行榜类型
     * @return true 如果排行榜当前活跃，false 如果排行榜当前不活跃
     */
    private boolean isLeaderboardActive(int type) {
        LocalDateTime nowDateTime = LocalDateTime.now();
        int currentHour = nowDateTime.getHour();
        return switch (type) {
            case PointsAwardConstant.Leaderboard.AM ->
                // 上午榜活跃时间：00:00-12:00
                    currentHour < 12;
            case PointsAwardConstant.Leaderboard.PM ->
                // 下午榜活跃时间：12:00-24:00
                    currentHour >= 12;
            case PointsAwardConstant.Leaderboard.TYPE_MONTH ->
                // 月榜始终活跃
                    true;
            default -> false;
        };
    }

    /**
     * 默认根据传入时间戳选择上午或下午榜类型
     */
    public void upsert(long playerId, long points, long tsMillis) {
        int type = resolveHalfDayType(tsMillis);
        upsert(type, playerId, points, tsMillis);
    }

    /**
     * 根据玩家ID查询当前排名
     * 未上榜返回 -1
     */
    public int getRank(int type, long playerId) {
        Integer r = set(type).revRank(playerId);
        return r == null ? -1 : (r + 1);
    }

    /**
     * 获取排行榜前 N 名玩家信息
     */
    public List<PointsAwardLeaderboardInfo> topN(int type, int n) {
        if (n <= 0) {
            return List.of();
        }
        RScoredSortedSet<Long> s = set(type);
        int size = Math.min(n, s.size());
        if (size == 0) {
            return List.of();
        }
        Collection<ScoredEntry<Long>> entries = s.entryRangeReversed(0, size - 1);
        List<PointsAwardLeaderboardInfo> ret = new ArrayList<>(entries.size());
//        Map<Integer, PointsAwardRankingCfg> rankingCfgMap = manager.getRankingCfgMap(type);
        int rank = 1;
        for (ScoredEntry<Long> e : entries) {
            PointsAwardLeaderboardInfo info = new PointsAwardLeaderboardInfo();
            info.setPlayerId(e.getValue());
            info.setConfigId(rank);
            info.setRank(rank++);
            info.setRankPoints((int) Math.floor(e.getScore()));
            Player player = hallPlayerService.get(info.getPlayerId());
            info.setGender(player.getGender());
            info.setHeadFrameId(player.getHeadFrameId());
            info.setHeadImgId(player.getHeadImgId());
            info.setNickName(player.getNickName());
            info.setNationalId(player.getNationalId());
            info.setTitleId(player.getTitleId());
            info.setLevel(player.getLevel());
            ret.add(info);
        }
        return ret;
    }

    /**
     * 上榜最低分
     *
     * @param type
     * @return
     */
    private int resolveMinPoints(int type) {
        // 上下午榜最低分
        int dayMin = GameDataManager.getGlobalConfigCfg(41).getIntValue();
        // 月榜最低分
        int monthMin = GameDataManager.getGlobalConfigCfg(42).getIntValue();
        return type == PointsAwardConstant.Leaderboard.TYPE_MONTH ? monthMin : dayMin;
    }

    /**
     * 清空指定类型排行榜
     */
    public void reset(int type) {
        String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + type;
        redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
            RScoredSortedSet<Long> s = set(type);
            s.delete();
            RMap<Long, Long> playerPointsMap = redissonClient.getMap(PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING_POINTS + type);
            playerPointsMap.clear();
        });
    }

    public String historyKey(long playerId) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_PLAYER_RANKING_HISTORY + playerId;
    }

    /**
     * 添加历史记录
     */
    public void addHistory(PointsAwardLeaderboardInfo info, PointsAwardRankingCfg cfg, String code, long endTime) {
        RDeque<PointsAwardLeaderboardHistory> dequeHistory = redissonClient.getDeque(historyKey(info.getPlayerId()));
        PointsAwardLeaderboardHistory history = new PointsAwardLeaderboardHistory();
        history.setPlayerId(info.getPlayerId());
        history.setRank(info.getRank());
        history.setRankPoints(info.getRankPoints());
        history.setGender(info.getGender());
        history.setHeadFrameId(info.getHeadFrameId());
        history.setHeadImgId(info.getHeadImgId());
        history.setNickName(info.getNickName());
        history.setNationalId(info.getNationalId());
        history.setTitleId(info.getTitleId());
        history.setReward(cfg.getGetItem());
        history.setPrice(cfg.getPrice());
        history.setPicRes(cfg.getPicRes());
        history.setAwardType(cfg.getAwardType());
        history.setRankType(cfg.getType());
        history.setEndTime(endTime);
        if (code != null) {
            long now = System.currentTimeMillis();
            history.setCode(code);
            history.setExpiredTime(now);
            GlobalConfigCfg globalConfigCfg = GameDataManager.getGlobalConfigCfg(47);
            if (globalConfigCfg != null) {
                int day = globalConfigCfg.getIntValue();
                long time = day * 24 * 60 * 60 * 1000L;
                history.setExpiredTime(now + time);
            }
        }
        dequeHistory.addFirst(history);
        //只保留一定条数
        if (dequeHistory.size() > PointsAwardConstant.Leaderboard.PLAYER_MAX_HISTORY_SIZE) {
            PointsAwardLeaderboardHistory lastHistoryEntry = dequeHistory.removeLast();
            if (lastHistoryEntry != null) {
                String awardCode = lastHistoryEntry.getCode();
                if (awardCode != null) {
                    awardCodeManager.deleteCode(awardCode);
                }
            }
        }
    }

    /**
     * 获取积分奖励排行榜数据。
     *
     * @param type      排行榜类型
     * @param pageIndex 页码，从1开始
     * @param pageSize  每页显示的数据数量
     * @return 分页后的积分奖励排行榜数据
     */

    public PageUtils.PageResult<PointsAwardLeaderboardData> getData(int type, int pageIndex, int pageSize) {
        List<PointsAwardLeaderboardData> list = manager.getRankingHistory(type);
        // 只有在排行榜活跃时才添加当前数据
        if (isLeaderboardActive(type)) {
            PointsAwardLeaderboardData data = new PointsAwardLeaderboardData();
            data.setRankType(type);
            data.setRankingInfoList(topN(type, PointsAwardConstant.Leaderboard.MAX_RANK_SIZE));
            data.setEndTime(System.currentTimeMillis());
            list.addFirst(data);
        }
        return PageUtils.page(list, pageIndex, pageSize);
    }

    public ResLoadLeaderboardHistory getHistory(long playerId, int pageIndex, int pageSize) {
        // 限制每页最大条数
        if (pageSize > 20) {
            pageSize = 20;
        }
        if (pageSize <= 0) {
            pageSize = 10;
        }
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        ResLoadLeaderboardHistory response = new ResLoadLeaderboardHistory(Code.SUCCESS);
        RDeque<PointsAwardLeaderboardHistory> dequeHistory = redissonClient.getDeque(historyKey(playerId));
        List<PointsAwardLeaderboardHistory> historyList = dequeHistory.readAll();

        PageUtils.PageResult<PointsAwardLeaderboardHistory> pageResult = PageUtils.page(historyList, pageIndex, pageSize);

        response.setPageIndex(pageIndex);
        response.setPageSize(pageSize);
        response.setTotalCount(pageResult.getTotalCount());
        response.setTotalPage(pageResult.getMaxPageIndex());
        response.setHistoryList(pageResult.getData());
        return response;
    }

    /**
     * 领奖码已经使用
     *
     * @param code 被使用的领奖码
     */
    public void receiveCode(long playerId, String code) {
        redisLock.lockAndRun(PointsAwardConstant.RedisLockKey.PLAYER_RANKING_AWARD_LOCK + playerId, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
            RDeque<PointsAwardLeaderboardHistory> dequeHistory = redissonClient.getDeque(historyKey(playerId));
            List<PointsAwardLeaderboardHistory> historyList = dequeHistory.readAll();

            // 查找匹配的历史记录并修改
            boolean found = false;
            for (PointsAwardLeaderboardHistory history : historyList) {
                if (history.getCode() != null && history.getCode().equals(code)) {
                    history.setExpiredTime(-1L);
                    found = true;
                    break;
                }
            }

            // 如果找到了匹配的记录，需要重新写入整个队列 目前调用不频繁暂不考虑性能
            if (found) {
                // 清空原队列
                dequeHistory.clear();
                // 重新添加修改后的数据
                for (PointsAwardLeaderboardHistory history : historyList) {
                    dequeHistory.addLast(history);
                }
            }
        });
    }

}