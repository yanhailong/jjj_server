package com.jjg.game.room;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.room.listener.RoomStartListener;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.service.CoreStartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author 11
 * @date 2025/6/17 13:25
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan({"com.jjg.game"})
public class RoomApp implements SmartLifecycle, ApplicationContextAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private NodeConfig nodeConfig;

    private ApplicationContext context;

    private boolean running = false;

    public static void main(String[] args) {
        SpringApplication.run(RoomApp.class, args);
    }

    @Override
    public void start() {
        //获取支持的游戏类型
        List<int[]> gameTypeList = new ArrayList<>();
        int len = 0;
        Map<String, RoomStartListener> startListenerMap = this.context.getBeansOfType(RoomStartListener.class);
        for(Map.Entry<String, RoomStartListener> en : startListenerMap.entrySet()){
            int[] arr = en.getValue().getGameTypes();
            if(arr != null && arr.length > 0){
                gameTypeList.add(arr);
                len += arr.length;
            }
        }

        if(len < 1){
            log.warn("没有找到可支持的游戏类型");
            return;
        }

        int[] gameTypeArr = new int[len];
        int index = 0;
        for(int[] gameArr : gameTypeList){
            for(int i : gameArr){
                gameTypeArr[index] = i;
                index++;
            }
        }
        this.nodeConfig.setGameTypes(gameTypeArr);

        marsCoreStartService.init(this.context);
        coreStartService.init(this.context);

        //调用启动方法
        for(Map.Entry<String, RoomStartListener> en : startListenerMap.entrySet()){
            en.getValue().start();
        }
        running = true;
    }

    @Override
    public void stop() {
        marsCoreStartService.shutdown();
        coreStartService.shutdown();

        Map<String, RoomStartListener> startListenerMap = this.context.getBeansOfType(RoomStartListener.class);
        for(Map.Entry<String, RoomStartListener> en : startListenerMap.entrySet()){
            en.getValue().shutdown();
        }

        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
