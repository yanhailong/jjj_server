package com.jjg.game.hall.pointsaward.leaderboard;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.hall.pointsaward.constant.PointsAwardConstant;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardData;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardHistory;
import com.jjg.game.hall.pointsaward.pb.PointsAwardLeaderboardInfo;
import com.jjg.game.hall.pointsaward.pb.res.ResLoadLeaderboardHistory;
import com.jjg.game.hall.service.HallPlayerService;
import com.jjg.game.sampledata.GameDataManager;
import com.jjg.game.sampledata.bean.PointsAwardRankingCfg;
import org.redisson.api.RDeque;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    /**
     * 排行榜管理器
     */
    private PointsAwardLeaderboardManager manager;

    public PointsAwardLeaderboardService(RedissonClient redissonClient, RedisLock redisLock, HallPlayerService hallPlayerService) {
        this.redissonClient = redissonClient;
        this.redisLock = redisLock;
        this.hallPlayerService = hallPlayerService;
    }

    public void init(PointsAwardLeaderboardManager manager) {
        this.manager = manager;

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
     * 更新（或写入）玩家积分到排行榜；传入的 points 为“增量值”（本次增加的积分），不是最终总积分。
     * 不满足最低分时移除。
     * 并发控制：对同一排行榜使用分布式锁，保证读取-计算-写入与裁剪的原子性。
     */
    public void upsert(int type, long playerId, long points, long tsMillis) {
        String lockKey = PointsAwardConstant.RedisLockKey.POINTS_AWARD_RANKING_LOCK + type;
        redisLock.lockAndRun(lockKey, PointsAwardConstant.Leaderboard.LOCK_LEASE_MILLIS, () -> {
            RScoredSortedSet<Long> s = set(type);
            // 读取当前玩家积分（取整去掉时间偏移的小数部分）
            Double currentScore = s.getScore(playerId);
            long currentPoints = currentScore == null ? 0L : (long) Math.floor(currentScore);
            // 累加本次增量，得到新的总积分
            long newPoints = currentPoints + points;
            int minPoints = resolveMinPoints(type);
            log.info("upsert playerId = {},type = {},points = {},tsMillis = {},newPoints = {},minPoints = {}", playerId, type, points, tsMillis, newPoints, minPoints);
            if (newPoints < minPoints) {
                return;
            }
            // 写入新的分数（总积分 + 时间偏移），并按照需要裁剪榜单大小
            s.add(toScore(newPoints, tsMillis), playerId);
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
        Map<Integer, PointsAwardRankingCfg> rankingCfgMap = manager.getRankingCfgMap(type);
        int rank = 1;
        for (ScoredEntry<Long> e : entries) {
            PointsAwardLeaderboardInfo info = new PointsAwardLeaderboardInfo();
            info.setPlayerId(e.getValue());
            info.setRank(rank++);
            info.setRankPoints((int) Math.floor(e.getScore()));
            Player player = hallPlayerService.get(info.getPlayerId());
            info.setGender(player.getGender());
            info.setHeadFrameId(player.getHeadFrameId());
            info.setHeadImgId(player.getHeadImgId());
            info.setNickName(player.getNickName());
            info.setNationalId(player.getNationalId());
            info.setTitleId(player.getTitleId());
            PointsAwardRankingCfg rankingCfg = rankingCfgMap.get(rank);
            info.setConfigId(rankingCfg.getId());
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
        });
    }

    public String historyKey(long playerId) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_PLAYER_RANKING_HISTORY + playerId;
    }

    /**
     * 添加历史记录
     */
    public void addHistory(PointsAwardLeaderboardInfo info, PointsAwardRankingCfg cfg, String code, String rankName) {
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
        history.setCode(code);
        history.setRankName(rankName);
        dequeHistory.addFirst(history);
        //只保留一定条数
        if (dequeHistory.size() > PointsAwardConstant.Leaderboard.MAX_HISTORY_SIZE) {
            dequeHistory.removeLast();
        }
    }

    /**
     * 获取排行数据
     *
     * @param type  排行榜类型
     * @param count 数据条数
     */
    public PointsAwardLeaderboardData getData(int type, int count) {
        List<PointsAwardLeaderboardInfo> rankingInfos = topN(type, count);
        PointsAwardLeaderboardData data = new PointsAwardLeaderboardData();
        data.setRankType(type);
        data.setRankingInfoList(rankingInfos);
        data.setName(manager.buildName(type, System.currentTimeMillis()));
        return data;
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
        // 分页处理
        int totalCount = historyList.size();
        int totalPage = (totalCount + pageSize - 1) / pageSize;
        int startIndex = (pageIndex - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalCount);

        List<PointsAwardLeaderboardHistory> histories = historyList.subList(startIndex, endIndex);

        response.setPageIndex(pageIndex);
        response.setPageSize(pageSize);
        response.setTotalCount(totalCount);
        response.setTotalPage(totalPage);
        response.setHistoryList(histories);
        return response;

    }

}