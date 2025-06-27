package com.jjg.game.hall.manager;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.listener.GameSampleFileChangeListener;
import com.jjg.game.core.sample.SampleConfig;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.hall.dao.HallPoolDao;
import com.jjg.game.hall.sample.GameDataManager;
import com.jjg.game.hall.sample.bean.BaseCfgBean;
import com.jjg.game.hall.service.HallService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/5/29 14:45
 */
@Component
public class HallStartManager implements SmartLifecycle, ApplicationContextAware, GameSampleFileChangeListener {
    private static final Logger log = LoggerFactory.getLogger(HallStartManager.class);
    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private HallPoolDao hallPoolDao;
    @Autowired
    private HallService hallService;
    @Autowired
    private SampleConfig sampleConfig;
    private ApplicationContext context;

    private boolean running = false;

    @Override
    public void start() {
        marsCoreStartService.init(this.context,Collections.emptySet());
        coreStartService.init(this.context);
        hallPoolDao.initPool();
        loadGameDataConfig();
        hallService.init();

        running = true;
    }

    @Override
    public void stop() {
        marsCoreStartService.shutdown();
        coreStartService.shutdown();

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
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
    public void change(File changeFile) {
        String samplePath = sampleConfig.getSamplePath();
        if (samplePath == null) {
            throw new RuntimeException("samplePath is null");
        }
        try {
            Set<Class<? extends BaseCfgBean>> changeCfgBean = GameDataManager.getInstance().loadDataByChangeFileList(samplePath, Collections.singletonList(changeFile));
            Map<String, ConfigExcelChangeListener> configExcelChangeListeners = CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
            String simpleName = changeCfgBean.iterator().next().getSimpleName();
            configExcelChangeListeners.values().forEach(listener -> {
                listener.change(simpleName);
            });
        } catch (Exception e) {
            log.error("加载配置表单表: {} 时发生异常", changeFile.getName(), e);
            throw new IllegalArgumentException("加载配置表单表: " + changeFile.getName() + " 时发生异常", e);
        }
    }
}
