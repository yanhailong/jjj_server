package com.jjg.game.slots.game.garaGemstone3.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.garaGemstone3.data.GaraGemstone3ResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class GaraGemstone3ResultLibDao extends AbstractResultLibDao<GaraGemstone3ResultLib> {
    public GaraGemstone3ResultLibDao() {
        super(GaraGemstone3ResultLib.class);
    }
}
