package com.jjg.game.slots.game.captainjack.data;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.utils.ObjectMapperUtil;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.captainjack.dao.CaptainJackPlayerGameDataDTO;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lm
 * @date 2025/12/2 17:27
 */
public class CaptainJackPlayerGameData extends SlotsPlayerGameData {
    //寻宝次数
    private AtomicInteger alreadyDigCount;
    //当前依赖的寻宝libId
    private CaptainJackResultLib resultLib;

    public int addAlreadyDigCount(int count) {
        if (alreadyDigCount == null) {
            alreadyDigCount = new AtomicInteger(0);
        }
        return alreadyDigCount.addAndGet(count);
    }

    public int getAlreadyDigCount() {
        return alreadyDigCount == null ? 0 : alreadyDigCount.get();
    }

    public void setAlreadyDigCount(AtomicInteger alreadyDigCount) {
        this.alreadyDigCount = alreadyDigCount;
    }

    public CaptainJackResultLib getResultLib() {
        return resultLib;
    }

    public void setResultLib(CaptainJackResultLib resultLib) {
        this.resultLib = resultLib;
    }

}
