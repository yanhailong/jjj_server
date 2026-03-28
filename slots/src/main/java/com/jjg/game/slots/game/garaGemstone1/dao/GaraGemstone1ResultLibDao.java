package com.jjg.game.slots.game.garaGemstone1.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.garaGemstone1.data.GaraGemstone1ResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class GaraGemstone1ResultLibDao extends AbstractResultLibDao<GaraGemstone1ResultLib> {
    public GaraGemstone1ResultLibDao() {
        super(GaraGemstone1ResultLib.class);
    }
}
