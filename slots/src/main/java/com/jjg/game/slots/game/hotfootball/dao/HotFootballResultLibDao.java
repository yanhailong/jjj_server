package com.jjg.game.slots.game.hotfootball.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.hotfootball.data.HotFootballResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class HotFootballResultLibDao extends AbstractResultLibDao<HotFootballResultLib> {
    public HotFootballResultLibDao() {
        super(HotFootballResultLib.class);
    }
}
