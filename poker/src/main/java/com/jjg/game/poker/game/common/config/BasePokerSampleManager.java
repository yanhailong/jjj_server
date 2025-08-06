package com.jjg.game.poker.game.common.config;

import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.manager.AbstractSampleManager;

import com.jjg.game.poker.game.sample.GameDataManager;
import com.jjg.game.poker.game.sample.bean.BaseCfgBean;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import com.jjg.game.room.data.room.TablePlayerGameData;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基础的扑克类配置表管理
 *
 * @author 2CL
 */
@Component
public class BasePokerSampleManager extends AbstractSampleManager {
    @Override
    protected String getSamplePath() {
        return CoreConst.Common.SAMPLE_ROOT_PATH;
    }

    @Override
    protected void initSampleConfig() throws Exception {
        // 房间类的配置必须要加载房间的配置
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "poker";
        GameDataManager.loadAllData(sampleRoomResourcePath);
        TexasDataHelper.initData();
    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        String sampleRoomResourcePath = CoreConst.Common.SAMPLE_ROOT_PATH + "poker";
        Set<Class<? extends BaseCfgBean>> changeCfgBean =
                GameDataManager.getInstance().loadDataByChangeFileList(
                        sampleRoomResourcePath, Collections.singletonList(file));
        return changeCfgBean.stream().map(aClass -> (Class<?>) aClass).collect(Collectors.toSet());
    }
}
