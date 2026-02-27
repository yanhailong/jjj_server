package com.jjg.game.slots.game.angrybirds.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.angrybirds.data.AngryBirdsResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2025/8/1 17:29
 */
@Repository
public class AngryBirdsResultLibDao extends AbstractResultLibDao<AngryBirdsResultLib> {
    public AngryBirdsResultLibDao() {
        super(AngryBirdsResultLib.class);
    }
}
