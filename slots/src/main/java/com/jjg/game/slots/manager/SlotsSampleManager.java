package com.jjg.game.slots.manager;

import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.BaseCfgBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/7/1 16:36
 */
@Component
public class SlotsSampleManager extends AbstractSampleManager {
    public void init() {
        log.info("开始加载slots游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return SlotsConst.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() throws Exception {
        GameDataManager.loadAllData(getSamplePath());
    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        Set<Class<? extends BaseCfgBean>> changeCfgBean =
            GameDataManager.getInstance().loadDataByChangeFileList(
                getSamplePath(), Collections.singletonList(file));
        return changeCfgBean.stream().map(aClass -> (Class<?>) aClass).collect(Collectors.toSet());
    }
}
