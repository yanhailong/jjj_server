package com.jjg.game.slots.game.superGolf.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.superGolf.data.SuperGolfResultLib;
import org.springframework.stereotype.Repository;

@Repository
public class SuperGolfResultLibDao extends AbstractResultLibDao<SuperGolfResultLib> {
    public SuperGolfResultLibDao() {
        super(SuperGolfResultLib.class);
    }
}
