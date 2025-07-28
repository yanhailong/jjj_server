package com.jjg.game.hall.manager;

import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.hall.constant.HallConstant;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.BaseCfgBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/30 13:53
 */
@Component
public class HallSampleManager extends AbstractSampleManager {

    public void init() {
        log.info("开始加载大厅配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return HallConstant.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() throws Exception {
        GameDataManager.loadAllData(getSamplePath());
    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        Set<Class<? extends BaseCfgBean>> changeCfgBean =
            GameDataManager.getInstance().loadDataByChangeFileList(getSamplePath(),
                Collections.singletonList(file));
        return changeCfgBean.stream().map(aClass -> (Class<?>) aClass).collect(Collectors.toSet());
    }
}
