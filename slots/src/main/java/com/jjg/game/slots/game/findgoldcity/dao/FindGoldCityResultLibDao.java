package com.jjg.game.slots.game.findgoldcity.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.findgoldcity.data.FindGoldCityResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author 11
 * @date 2025/8/1 17:29
 */
@Repository
public class FindGoldCityResultLibDao extends AbstractResultLibDao<FindGoldCityResultLib> {
    public FindGoldCityResultLibDao() {
        super(FindGoldCityResultLib.class);
    }
}
