package com.jjg.game.hall.service;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.hall.data.WareHouseConfigInfo;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.WarehouseCfg;
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

    public void init() {
        initWareHouseConfigData();
    }

    public List<WareHouseConfigInfo> getWareHouseConfigByGameType(int gameType) {
        return wareHouseConfigMap.get(gameType);
    }

    @Override
    public void change(String className) {
        if (className.equalsIgnoreCase(WarehouseCfg.class.getSimpleName())) {
            initWareHouseConfigData();
        }
    }

    private void initWareHouseConfigData() {
        Map<Integer, List<WareHouseConfigInfo>> tempwareHouseConfigMap = new HashMap<>();

        for (WarehouseCfg c : GameDataManager.getWarehouseCfgList()) {
            List<WareHouseConfigInfo> tempList = tempwareHouseConfigMap.computeIfAbsent(c.getGameID(),
                k -> new ArrayList<>());
            WareHouseConfigInfo info = new WareHouseConfigInfo();
            info.wareId = c.getId() - (c.getGameID() * 10);
            info.pool = 99999L;
            info.limitGoldMin = c.getEnterLimit();
            info.limitVipMin = c.getVipLvLimit();
            tempList.add(info);
        }
        this.wareHouseConfigMap = tempwareHouseConfigMap;
    }
}
