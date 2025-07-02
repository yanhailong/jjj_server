package com.jjg.game.table.baccarat;

import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.table.baccarat.constant.BaccaratConstant;
import com.jjg.game.table.baccarat.sample.GameDataManager;
import com.jjg.game.table.baccarat.sample.bean.BaseCfgBean;
import com.jjg.game.table.common.BaseTableSampleManager;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 百家乐配置管理器
 *
 * @author 2CL
 */
@Component
public class BaccaratSampleManager extends BaseTableSampleManager {


    @Override
    protected String getSamplePath() {
        return BaccaratConstant.BACCARAT_SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() {
        super.initSampleConfig();
        try {
            // 百家乐需要的表
            GameDataManager.loadAllData(getSamplePath());
        } catch (Exception e) {
            log.error("配置表加载异常");
            throw new RuntimeException(e);
        }
    }
}
