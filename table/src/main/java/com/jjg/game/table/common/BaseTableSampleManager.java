package com.jjg.game.table.common;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.table.baccarat.sample.bean.BaseCfgBean;
import com.jjg.game.table.common.data.TableSampleDataHolder;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 基础的牌桌类配置表管理
 *
 * @author 2CL
 */
public abstract class BaseTableSampleManager extends AbstractSampleManager {

    @Override
    protected void initSampleConfig() {
        // 房间类的配置必须要加载房间的配置
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "common";
        try {
            GameDataManager.loadAllData(sampleRoomResourcePath);
            TableSampleDataHolder.cacheBetActionData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sampleChange(File file) {
        try {
            Set<Class<? extends BaseCfgBean>> changeCfgBean =
                com.jjg.game.table.baccarat.sample.GameDataManager.getInstance().loadDataByChangeFileList(getSamplePath(),
                    Collections.singletonList(file));
            Map<String, ConfigExcelChangeListener> configExcelChangeListeners =
                CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
            configExcelChangeListeners.values().forEach(listener -> {
                listener.change(changeCfgBean.iterator().next().getSimpleName());
            });
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
