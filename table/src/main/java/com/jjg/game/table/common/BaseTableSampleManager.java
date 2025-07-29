package com.jjg.game.table.common;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.BaseCfgBean;
import com.jjg.game.table.common.data.TableSampleDataHolder;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础的牌桌类配置表管理
 *
 * @author 2CL
 */
public abstract class BaseTableSampleManager extends AbstractSampleManager {

    @Override
    protected void initSampleConfig() throws Exception {
        // 房间类的配置必须要加载房间的配置
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "common";
        GameDataManager.loadAllData(sampleRoomResourcePath);
    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "common";
        Set<Class<? extends BaseCfgBean>> changeCfgBean =
            GameDataManager.getInstance().loadDataByChangeFileList(
                sampleRoomResourcePath, Collections.singletonList(file));
        return changeCfgBean.stream().map(aClass -> (Class<?>) aClass).collect(Collectors.toSet());
    }
}
