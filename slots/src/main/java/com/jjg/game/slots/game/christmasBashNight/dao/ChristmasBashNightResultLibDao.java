package com.jjg.game.slots.game.christmasBashNight.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.christmasBashNight.data.ChristmasBashNightResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class ChristmasBashNightResultLibDao extends AbstractResultLibDao<ChristmasBashNightResultLib> {
    public ChristmasBashNightResultLibDao() {
        super(ChristmasBashNightResultLib.class);
    }
}
