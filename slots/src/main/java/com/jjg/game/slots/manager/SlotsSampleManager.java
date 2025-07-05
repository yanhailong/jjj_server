package com.jjg.game.slots.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.slots.constant.SlotsConst;
import com.jjg.game.slots.sample.GameDataManager;
import com.jjg.game.slots.sample.bean.BaseCfgBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/7/1 16:36
 */
@Component
public class SlotsSampleManager extends AbstractSampleManager {
    public void init(){
        log.info("开始加载slots游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return SlotsConst.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSmapleConfig() {
        try {
            GameDataManager.loadAllData(getSamplePath());
        } catch (Exception e) {
            log.error("加载配置表失败");
        }
    }

    @Override
    protected void sampleChange(File file) {
        try{
            Set<Class<? extends BaseCfgBean>> changeCfgBean = GameDataManager.getInstance().loadDataByChangeFileList(getSamplePath(), Collections.singletonList(file));
            Map<String, ConfigExcelChangeListener> configExcelChangeListeners = CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
            configExcelChangeListeners.values().forEach(listener -> {
                listener.change(changeCfgBean.iterator().next().getSimpleName());
            });
        }catch (Exception e){
            log.error("",e);
        }
    }
}
