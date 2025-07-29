package com.jjg.game.poker.game.blackjack.manager;

import com.jjg.game.core.manager.AbstractSampleManager;
import com.jjg.game.poker.game.blackjack.constant.BlackJackConstant;
import com.jjg.game.poker.game.texas.constant.TexasConstant;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;

/**
 * @author 11
 * @date 2025/6/30 14:24
 */
@Component
public class BlackJackSampleManager extends AbstractSampleManager {
    public void init(){
        log.info("开始加载21点游戏配置..");
        super.init();
    }

    @Override
    protected String getSamplePath() {
        return BlackJackConstant.Common.SAMPLE_PATH;
    }

    @Override
    protected void initSampleConfig() {

    }

    @Override
    protected Set<Class<?>> reloadSampleOnExcelChange(File file) throws Exception {
        return Set.of();
    }


}
