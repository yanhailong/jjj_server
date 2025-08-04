package com.jjg.game.room.manager;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.room.sample.GameDataManager;
import com.jjg.game.room.sample.bean.BaseCfgBean;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础的房间配置表管理，此类负责加载room相关的配置表，子类负责自己模块的配置表加载，子类需要保证一次加载所有的配置表
 *
 * @author 2CL
 */
public abstract class BaseRoomSampleManager extends AbstractSampleManager {

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
