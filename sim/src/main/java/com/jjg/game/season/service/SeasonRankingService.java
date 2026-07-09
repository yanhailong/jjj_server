package com.jjg.game.season.service;

import com.jjg.game.season.dao.SeasonPlayerDao;
import com.jjg.game.season.data.SeasonPlayerData;
import com.jjg.game.season.data.SeasonRankEntry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 按赛季隔离键查询总榜。
 */
@Service
public class SeasonRankingService {
    private final SeasonPlayerDao seasonPlayerDao;

    public SeasonRankingService(SeasonPlayerDao seasonPlayerDao) {
        this.seasonPlayerDao = seasonPlayerDao;
    }

    public List<SeasonRankEntry> ranking(String seasonKey, int limit) {
        List<SeasonPlayerData> players = seasonPlayerDao.findRanking(seasonKey, limit);
        List<SeasonRankEntry> result = new ArrayList<>(players.size());
        for (int index = 0; index < players.size(); index++) {
            SeasonPlayerData player = players.get(index);
            SeasonRankEntry entry = new SeasonRankEntry();
            entry.setRank(index + 1);
            entry.setPlayerId(player.getPlayerId());
            entry.setSeasonCoin(player.getSeasonCoin());
            entry.setTotalEarnedCoin(player.getTotalEarnedCoin());
            entry.setTierId(player.getTierId());
            result.add(entry);
        }
        return result;
    }

    public int rankOf(SeasonPlayerData data) {
        return Math.toIntExact(seasonPlayerDao.findRank(data.getSeasonKey(), data.getPlayerId(),
                data.getSeasonCoin(), data.getTotalEarnedCoin()));
    }
}
