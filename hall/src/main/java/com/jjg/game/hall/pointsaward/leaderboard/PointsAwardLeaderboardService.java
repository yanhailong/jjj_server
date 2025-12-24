package com.jjg.game.hall.pointsaward.leaderboard;

import com.jjg.game.common.redis.RedisLock;
import com.jjg.game.common.utils.PageUtils;
import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.core.data.RankEntry;
import com.jjg.game.core.manager.AwardCodeManager;
import com.jjg.game.core.service.RankService;
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
import java.util.*;

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
    private final HallPlayerService hallPlayerService;
    private final AwardCodeManager awardCodeManager;

    private final Map<Integer, List<PointsAwardLeaderboardData>> rankMap = new HashMap<>();
    private final RankService rankService;

    /**
     * 排行榜管理器
     */
    private PointsAwardLeaderboardManager manager;

    public PointsAwardLeaderboardService(RedissonClient redissonClient, HallPlayerService hallPlayerService, AwardCodeManager awardCodeManager, RankService rankService) {
        this.redissonClient = redissonClient;
        this.hallPlayerService = hallPlayerService;
        this.awardCodeManager = awardCodeManager;
        this.rankService = rankService;
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

    private String getRankKey(int type) {
        return PointsAwardConstant.RedisKey.POINTS_AWARD_RANKING + type;
    }

    /**
     * 更新（或写入）玩家积分到排行榜；传入的 points 为最终总积分。
     * 不满足最低分时移除。
     * 并发控制：对同一排行榜使用分布式锁，保证读取-计算-写入与裁剪的原子性。
     */
    public void upsert(int type, long playerId, int points) {
        rankService.addPoints(getRankKey(type), playerId, points);
    }

    public long getEndTime(int type){
        return manager.getEndTime(type);
    }

    /**
     * 根据玩家ID查询当前排名
     * 未上榜返回 -1
     */
    public int getRank(int type, long playerId) {
        RankEntry rank = rankService.getRank(getRankKey(type), playerId);
        if (rank == null || resolveMinPoints(type) > rank.getPoints()) {
            return -1;
        }
        return (int) rank.getRank();
    }



    /**
     * 获取排行榜前 N 名玩家信息
     */
    public List<PointsAwardLeaderboardInfo> topN(int type, int n) {
        if (n <= 0) {
            return List.of();
        }
        List<RankEntry> rankEntries = rankService.topN(getRankKey(type), n);
        if (rankEntries.isEmpty()) {
            return List.of();
        }
        //筛选
        int minPoints = resolveMinPoints(type);
        List<RankEntry> finalRankEntries = new ArrayList<>(rankEntries.size());
        for (RankEntry entry : rankEntries) {
            if (entry.getPoints() < minPoints) {
                break;
            }
            finalRankEntries.add(entry);
        }
        List<PointsAwardLeaderboardInfo> ret = new ArrayList<>(finalRankEntries.size());
        Map<Long, Player> playerMap = hallPlayerService.multiGetPlayerMap(finalRankEntries.stream().map(RankEntry::getPlayerId).toList());
        int rank = 1;
        for (RankEntry rankEntry : finalRankEntries) {
            PointsAwardLeaderboardInfo info = new PointsAwardLeaderboardInfo();
            info.setPlayerId(rankEntry.getPlayerId());
            info.setConfigId(rank);
            info.setRank(rank++);
            info.setRankPoints((int) rankEntry.getPoints());
            Player player = playerMap.get(info.getPlayerId());
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
        // 上周榜最低分
        int dayMin = GameDataManager.getGlobalConfigCfg(41).getIntValue();
        // 月榜最低分
        int monthMin = GameDataManager.getGlobalConfigCfg(42).getIntValue();
        return type == PointsAwardConstant.Leaderboard.TYPE_MONTH ? monthMin : dayMin;
    }

    /**
     * 清空指定类型排行榜
     */
    public void reset(int type) {
        rankService.reset(getRankKey(type));
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
        List<PointsAwardLeaderboardData> tmpList = this.rankMap.get(type);
        List<PointsAwardLeaderboardData> list;
        if (tmpList == null || tmpList.isEmpty()) {
            list = new ArrayList<>();
        } else {
            list = new ArrayList<>(tmpList);
        }
        // 只有在排行榜活跃时才添加当前数据
        PointsAwardLeaderboardData data;
        data = new PointsAwardLeaderboardData();
        data.setRankType(type);
        data.setRankingInfoList(topN(type, manager.getMaxRankSize(type)));
        data.setEndTime(getEndTime(type));
        list.addFirst(data);
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


    public void loadRank(int type) {
        List<PointsAwardLeaderboardData> list = manager.getRankingHistory(type);
        rankMap.put(type, list);

        log.debug("加载排行榜数据 type = {},size = {}", type, list.size());
    }
}