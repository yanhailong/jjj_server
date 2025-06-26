package com.jjg.game.dollarexpress.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.GameSampleFileChangeListener;
import com.jjg.game.core.sample.SampleConfig;
import com.jjg.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.dollarexpress.sample.GameDataManager;
import com.jjg.game.dollarexpress.sample.bean.BaseCfgBean;
import com.jjg.game.room.listener.IRoomStartListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/6/10 16:37
 */
@Component
public class DollarExpressStartManager implements IRoomStartListener, GameSampleFileChangeListener {

    private static final Logger log = LoggerFactory.getLogger(DollarExpressStartManager.class);
    @Autowired
    private SampleConfig sampleConfig;

    @Autowired
    private DollarExpressManager dollarExpressManager;

    @Autowired
    private DollarRoomManager roomManager;

    @Override
    public int[] getGameTypes() {
        return DollarExpressConst.GameType.SUPPORT_GAME_TYPES;
    }

    @Override
    public void start() {
        loadGameDataConfig();
        this.dollarExpressManager.init();

        roomManager.test();
    }

    private void loadGameDataConfig() {
        try {
            String samplePath = sampleConfig.getSamplePath();
            if (samplePath == null) {
                return;
            }
            GameDataManager.loadAllData(samplePath);
        } catch (Exception e) {
            log.error("加载配置表失败");
            throw new RuntimeException("加载配置表失败", e);
        }
    }

    @Override
    public void shutdown() {

    }


    @Override
    public void change(File changeFile) {
        String samplePath = sampleConfig.getSamplePath();
        if (samplePath == null) {
            throw new RuntimeException("samplePath is null");
        }
        try {
            Set<Class<? extends BaseCfgBean>> changeCfgBean = GameDataManager.getInstance().loadDataByChangeFileList(samplePath, Collections.singletonList(changeFile));
            Map<String, ConfigExcelChangeListener> configExcelChangeListeners = CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
            configExcelChangeListeners.values().forEach(listener -> {
                listener.change(changeCfgBean.iterator().next().getSimpleName());
            });
        } catch (Exception e) {
            log.error("加载配置表单表: {} 时发生异常", changeFile.getName(), e);
            throw new IllegalArgumentException("加载配置表单表: " + changeFile.getName() + " 时发生异常", e);
        }
    }
}
