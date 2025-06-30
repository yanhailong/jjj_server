package com.jjg.game.slots.game.dollarexpress.manager;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.slots.game.dollarexpress.constant.DollarExpressConst;
import com.jjg.game.slots.game.dollarexpress.sample.GameDataManager;
import com.jjg.game.slots.game.dollarexpress.sample.bean.BaseCfgBean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author 11
 * @date 2025/6/30 10:38
 */
@Component
public class DollarExpressSampleManager extends AbstractSampleManager {

    public void init(){
        log.info("开始加载美元快递游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return DollarExpressConst.Common.SAMPLE_PATH;
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
