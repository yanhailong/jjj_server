package com.jjg.game.slots.game.captainjack.dao;

import com.alibaba.fastjson.JSON;
import com.jjg.game.slots.data.SlotsPlayerGameData;
import com.jjg.game.slots.data.SlotsPlayerGameDataDTO;
import com.jjg.game.slots.game.captainjack.data.CaptainJackPlayerGameData;
import com.jjg.game.slots.game.captainjack.data.CaptainJackResultLib;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author 11
 * @date 2025/8/5 14:11
 */
@Document
public class CaptainJackPlayerGameDataDTO extends SlotsPlayerGameDataDTO {
    //寻宝次数
    private int alreadyDigCount;
    //当前依赖的寻宝libId
    private String digLibJson;

    public int getAlreadyDigCount() {
        return alreadyDigCount;
    }

    public void setAlreadyDigCount(int alreadyDigCount) {
        this.alreadyDigCount = alreadyDigCount;
    }

    public String getDigLibJson() {
        return digLibJson;
    }

    public void setDigLibJson(String digLibJson) {
        this.digLibJson = digLibJson;
    }

    @Override
    public <T extends SlotsPlayerGameData> T converToGameData(Class<T> cla) throws Exception {
        T t = super.converToGameData(cla);
        if (t instanceof CaptainJackPlayerGameData gameData) {
            if (getAlreadyDigCount() > 0) {
                gameData.addAlreadyDigCount(getAlreadyDigCount());
            }
            if (StringUtils.isNotEmpty(getDigLibJson())) {
                gameData.setResultLib(JSON.parseObject(getDigLibJson(), CaptainJackResultLib.class));
            }
        }
        return t;
    }
}
