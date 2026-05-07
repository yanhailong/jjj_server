package com.jjg.game.slots.game.garaGemstone2.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.garaGemstone2.data.GaraGemstone2ResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class GaraGemstone2ResultLibDao extends AbstractResultLibDao<GaraGemstone2ResultLib> {
    public GaraGemstone2ResultLibDao() {
        super(GaraGemstone2ResultLib.class);
    }
}
