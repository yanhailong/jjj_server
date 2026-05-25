package com.jjg.game.sim.service;

import com.jjg.game.sim.dao.SimPlayerGameDao;
import com.jjg.game.sim.data.SimPlayerGameData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;

/**
 * @author 11
 * @date 2026/5/18
 */
@Service
public class SimPlayerGameDataService {
    private Logger log = LoggerFactory.getLogger(getClass());

    private final String TABLE_NAME = "simPlayerGameData";

    @Autowired
    private SimPlayerGameDao simPlayerGameDao;

    /**
     * 数据数据
     *
     * @param playerId
     * @return
     */
    public SimPlayerGameData getSimPlayerGameData(long playerId) {
        Optional<SimPlayerGameData> optional = simPlayerGameDao.findById(playerId);
        if (optional.isPresent()) {
            return optional.get();
        }
        return null;
    }

    /**
     * 落库
     *
     * @param simPlayerGameData
     */
    public void save(SimPlayerGameData simPlayerGameData) {
        simPlayerGameDao.save(simPlayerGameData);
    }

    /**
     * 批量落库
     *
     * @param list
     */
    public void saveAll(Collection<SimPlayerGameData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            simPlayerGameDao.saveAll(list);
        } catch (Exception e) {
            log.error("批量保存 SimPlayerGameData 失败 size={}", list.size(), e);
        }
    }
}
