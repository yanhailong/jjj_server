package com.jjg.game.table.betsample;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.table.betsample.sample.GameDataManager;
import com.jjg.game.table.betsample.sample.bean.BaseCfgBean;
import com.jjg.game.table.common.BaseTableSampleManager;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

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
    protected void initSampleConfig() throws Exception {
        super.initSampleConfig();
        // 房间类的配置必须要加载房间的配置
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "betsample";
        GameDataManager.loadAllData(sampleRoomResourcePath);
    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        Set<Class<?>> parentSampleSet = super.reloadSampleOnExcelChange(file);
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "betsample";
        Set<Class<? extends BaseCfgBean>> changeCfgBean =
            GameDataManager.getInstance().loadDataByChangeFileList(
                sampleRoomResourcePath, Collections.singletonList(file));
        parentSampleSet.addAll(changeCfgBean);
        return parentSampleSet.stream().map(aClass -> (Class<?>) aClass).collect(Collectors.toSet());
    }
}
