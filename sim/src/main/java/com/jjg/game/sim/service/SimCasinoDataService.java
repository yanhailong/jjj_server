package com.jjg.game.sim.service;

import com.jjg.game.sim.dao.SimCasinoDao;
import com.jjg.game.sim.data.CasinoData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * CasinoData 数据访问服务
 *
 * @author 11
 * @date 2026/5/26
 */
@Service
public class SimCasinoDataService {
    private static final Logger log = LoggerFactory.getLogger(SimCasinoDataService.class);

    @Autowired
    private SimCasinoDao simCasinoDao;

    /**
     * 按 playerId 加载所有赌场
     */
    public List<CasinoData> findByPlayerId(long playerId) {
        List<CasinoData> list = simCasinoDao.findByPlayerId(playerId);
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 落库单个赌场
     */
    public void save(CasinoData data) {
        if (data == null) {
            return;
        }
        try {
            data.buildKey();
            simCasinoDao.save(data);
        } catch (Exception e) {
            log.error("保存 CasinoData 失败 playerId={},casinoId={}", data.getPlayerId(), data.getCasinoId(), e);
        }
    }

    /**
     * 批量落库
     */
    public void saveAll(Collection<CasinoData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<CasinoData> toSave = new ArrayList<>(list.size());
        for (CasinoData data : list) {
            if (data == null) {
                continue;
            }
            data.buildKey();
            toSave.add(data);
        }
        if (toSave.isEmpty()) {
            return;
        }
        try {
            simCasinoDao.saveAll(toSave);
        } catch (Exception e) {
            log.error("批量保存 CasinoData 失败 size={}", toSave.size(), e);
        }
    }
}
