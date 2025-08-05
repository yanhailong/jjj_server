package com.jjg.game.poker.game.texas.manager;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import com.jjg.game.poker.game.sample.bean.PokerPoolCfg;
import com.jjg.game.poker.game.texas.data.TexasDataHelper;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/30 14:24
 */
@Component
public class TexasSampleManager implements ConfigExcelChangeListener {


    @Override
    public void initSampleCallbackCollector() {
//        addSampleFileObserveWithCallBack(PokerPoolCfg.EXCEL_NAME, TexasDataHelper::initData);
    }
}
