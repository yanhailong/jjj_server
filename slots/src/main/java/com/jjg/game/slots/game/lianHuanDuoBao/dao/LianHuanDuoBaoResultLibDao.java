package com.jjg.game.slots.game.lianHuanDuoBao.dao;

import com.jjg.game.slots.dao.AbstractResultLibDao;
import com.jjg.game.slots.game.lianHuanDuoBao.data.LianHuanDuoBaoResultLib;
import org.springframework.stereotype.Repository;

/**
 * @author lm
 * @date 2026/6/2
 */
//TODO 连环夺宝配表完成后取消注释即可启用
//@Repository
public class LianHuanDuoBaoResultLibDao extends AbstractResultLibDao<LianHuanDuoBaoResultLib> {
    public LianHuanDuoBaoResultLibDao() {
        super(LianHuanDuoBaoResultLib.class);
    }
}
