package com.jjg.game.hall.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.sample.AllWareHouseConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/18 14:56
 */
@Component
public class HallService implements ConfigExcelChangeListener {
    private Map<Integer, List<WareHouseConfigInfo>> wareHouseConfigMap = new HashMap<>();

    public void init(){
        initWareHouseConfigData();
    }

    public List<WareHouseConfigInfo> getWareHouseConfigByGameType(int gameType){
        return wareHouseConfigMap.get(gameType);
    }

    @Override
    public void change(String className) {
        if(className.equalsIgnoreCase(AllWareHouseConfig.class.getSimpleName())){
            initWareHouseConfigData();
        }
    }

    private void initWareHouseConfigData(){
        Map<Integer, List<WareHouseConfigInfo>> tempwareHouseConfigMap = new HashMap<>();

        for(AllWareHouseConfig c : AllWareHouseConfig.factory.getAllSamples()){
            List<WareHouseConfigInfo> tempList = tempwareHouseConfigMap.computeIfAbsent(c.getGameType(), k -> new ArrayList<>());
            WareHouseConfigInfo info = new WareHouseConfigInfo();
            info.wareId = c.getWareId();
            info.name = c.getName();
            info.pool = 99999l;
            info.limitGoldMin = c.getRequire_amount();
            info.limitVipMin = c.getRequire_viplevel();
            tempList.add(info);
        }
        this.wareHouseConfigMap = tempwareHouseConfigMap;
    }
}
