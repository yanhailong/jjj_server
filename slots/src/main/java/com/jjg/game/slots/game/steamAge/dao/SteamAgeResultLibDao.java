package com.jjg.game.slots.game.steamAge.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.steamAge.data.SteamAgeResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lihaocao
 * @date 2025/12/2 17:29
 */
@Repository
public class SteamAgeResultLibDao extends AbstractResultLibDao<SteamAgeResultLib> {
    public SteamAgeResultLibDao() {
        super(SteamAgeResultLib.class);
    }
}
