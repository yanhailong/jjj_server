package com.jjg.game.dollarexpress.manager;

import com.jjg.game.core.listener.ConfigExcelChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 11
 * @date 2025/6/23 10:31
 */
@Component
public class DollarExpressGenerateManager implements ConfigExcelChangeListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 生成结果集
     * @param count
     */
    public void generate(int count) {
        for(int i=0;i<count;i++) {
            generateOne();
        }
    }

    private void generateOne(){
        try{

        }catch (Exception e){
            log.error("",e);
        }
    }


    @Override
    public void change(String className) {

    }
}
