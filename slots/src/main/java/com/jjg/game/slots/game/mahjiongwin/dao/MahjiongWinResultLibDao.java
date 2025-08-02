package com.jjg.game.slots.game.mahjiongwin.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.mahjiongwin.data.MahjiongWinResultLib;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class MahjiongWinResultLibDao extends AbstractResultLibDao<MahjiongWinResultLib> {
    public MahjiongWinResultLibDao(@Autowired MongoTemplate mongoTemplate) {
        super(MahjiongWinResultLib.class, mongoTemplate);
    }
}
