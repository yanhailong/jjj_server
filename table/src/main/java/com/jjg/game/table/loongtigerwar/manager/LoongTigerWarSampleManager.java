package com.jjg.game.table.loongtigerwar.manager;

import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.table.loongtigerwar.constant.LoongTigerWarConstant;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @author 11
 * @date 2025/6/30 10:38
 */
@Component
public class LoongTigerWarSampleManager extends AbstractSampleManager {

    public void init(){
        log.info("开始加载龙虎斗游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return LoongTigerWarConstant.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() {
        try {
//            GameDataManager.loadAllData(getSamplePath());
        } catch (Exception e) {
            log.error("加载配置表失败");
        }
    }

    @Override
    protected void sampleChange(File file) {
        try{
//            Set<Class<? extends BaseCfgBean>> changeCfgBean = GameDataManager.getInstance().loadDataByChangeFileList(getSamplePath(), Collections.singletonList(file));
//            Map<String, ConfigExcelChangeListener> configExcelChangeListeners = CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
//            configExcelChangeListeners.values().forEach(listener -> {
//                listener.change(changeCfgBean.iterator().next().getSimpleName());
//            });
        }catch (Exception e){
            log.error("",e);
        }
    }
}
