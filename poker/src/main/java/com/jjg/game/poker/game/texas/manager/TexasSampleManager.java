package com.jjg.game.poker.game.texas.manager;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.poker.game.texas.sample.GameDataManager;
import com.jjg.game.poker.game.texas.sample.bean.BaseCfgBean;
import com.jjg.game.poker.game.texas.sample.bean.PokerPoolCfg;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/30 14:24
 */
@Component
public class TexasSampleManager extends AbstractSampleManager implements ConfigExcelChangeListener {


    @Override
    public void initSampleCallbackCollector() {
        addSampleFileObserveWithCallBack(PokerPoolCfg.EXCEL_NAME, TexasDataHelper::initData);
    }

    public void init() {
        log.info("开始加载德州游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return TexasConstant.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() {
        try {
            GameDataManager.loadAllData(getSamplePath());
        } catch (Exception exception) {
            log.error("加载配置表失败: {}", exception.getMessage(), exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        Set<Class<? extends BaseCfgBean>> changeCfgBean =
                GameDataManager.getInstance().loadDataByChangeFileList(
                        getSamplePath(), Collections.singletonList(file));
        return changeCfgBean.stream().map(aClass -> (Class<?>) aClass).collect(Collectors.toSet());
    }

}
