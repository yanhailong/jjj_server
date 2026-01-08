package com.jjg.game.slots.game.elephantgod.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.elephantgod.data.ElephantGodResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class ElephantGodResultLibDao extends AbstractResultLibDao<ElephantGodResultLib> {
    public ElephantGodResultLibDao() {
        super(ElephantGodResultLib.class);
    }
}
