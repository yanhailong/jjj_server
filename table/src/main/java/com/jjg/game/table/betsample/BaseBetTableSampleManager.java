package com.jjg.game.table.betsample;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BaseCfgBean;
import com.jjg.game.table.common.BaseTableSampleManager;
import com.jjg.game.table.common.data.TableSampleDataHolder;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 基础的牌桌类配置表管理
 *
 * @author 2CL
 */
@Repository
public class BaseBetTableSampleManager extends BaseTableSampleManager {

    @Override
    protected String getSamplePath() {
        return CoreConst.Common.SAMPLE_ROOT_PATH;
    }

    @Override
    protected void initSampleConfig() {
        super.initSampleConfig();
        // 房间类的配置必须要加载房间的配置
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "betsample";
        try {
            GameDataManager.loadAllData(sampleRoomResourcePath);
            TableSampleDataHolder.cacheBetActionData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sampleChange(File file) {
        super.sampleChange(file);
        try {
            String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "betsample";
            Set<Class<? extends BaseCfgBean>> changeCfgBean =
                GameDataManager.getInstance().loadDataByChangeFileList(sampleRoomResourcePath,
                    Collections.singletonList(file));
            Map<String, ConfigExcelChangeListener> configExcelChangeListeners =
                CommonUtil.getContext().getBeansOfType(ConfigExcelChangeListener.class);
            configExcelChangeListeners.values().forEach(listener -> listener.change(changeCfgBean.iterator().next().getSimpleName()));
        } catch (Exception e) {
            log.error("基础押注类的配置表变化时，加载出现异常", e);
        }
    }
}
