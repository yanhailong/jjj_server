package com.jjg.game.room;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.constant.CoreConst;
import com.jjg.game.core.exception.GameSampleException;
import com.jjg.game.room.constant.RoomConstant;
import com.jjg.game.room.listener.IRoomStartListener;
import com.jjg.game.common.service.MarsCoreStartService;
import com.jjg.game.core.service.CoreStartService;
import com.jjg.game.room.listener.RoomEventListener;
import com.jjg.game.room.sample.GameDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;

import java.util.*;

/**
 * @author 11
 * @date 2025/6/17 13:25
 */
@SpringBootApplication(exclude = {QuartzAutoConfiguration.class})
@ComponentScan({"com.jjg.game"})
@Order(2)
public class RoomApp implements SmartLifecycle, ApplicationContextAware {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private MarsCoreStartService marsCoreStartService;
    @Autowired
    private CoreStartService coreStartService;
    @Autowired
    private NodeConfig nodeConfig;
    @Autowired
    private RoomEventListener roomEventListener;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private ApplicationContext context;

    private boolean running = false;

    public static void main(String[] args) {
        SpringApplication.run(RoomApp.class, args);
    }

    @Override
    public void start() {
        //获取支持的游戏类型
        Set<Integer> gameTypeSet = gameTypeList();
        if (gameTypeSet.isEmpty()) {
            log.warn("代码中没有可支持的游戏类型");
            return;
        }

        //将代码中支持的游戏类型，和配置中的游戏类型对比，能检查配置是否错误
        if (this.nodeConfig.getGameTypes() == null || this.nodeConfig.getGameTypes().length < 1) {
            log.warn("在 nodeconfig.json的 gameTypes 中没有配置开启哪些游戏");
            return;
        }

        boolean checkOk = true;
        for (int gameType : this.nodeConfig.getGameTypes()) {
            boolean remove = gameTypeSet.remove(gameType);
            if (!remove) {
                log.warn("本代码不支持 nodeconfig.json的 gameTypes 中 配置的 {} 游戏类型", gameType);
                checkOk = false;
            }
        }
        if (!checkOk) {
            return;
        }

        //每个游戏都会实现 IRoomStartListener 这个接口
        Map<String, IRoomStartListener> startListenerMap = this.context.getBeansOfType(IRoomStartListener.class);
        if (startListenerMap.isEmpty()) {
            log.warn("没有找到 IRoomStartListener 的实现类，启动失败....");
            return;
        }

        //不需要启动的游戏的消息类型
        Set<Integer> noStartGameMsgTypeSet = getNoStartGameMsgTypeSet(gameTypeSet);

        marsCoreStartService.init(this.context, noStartGameMsgTypeSet);
        coreStartService.init(this.context);
        roomEventListener.init();
        //调用启动方法
        for (Map.Entry<String, IRoomStartListener> en : startListenerMap.entrySet()) {
            en.getValue().start();
        }
        running = true;
    }

    @Override
    public void stop() {
        marsCoreStartService.shutdown();
        coreStartService.shutdown();

        Map<String, IRoomStartListener> startListenerMap = this.context.getBeansOfType(IRoomStartListener.class);
        for (Map.Entry<String, IRoomStartListener> en : startListenerMap.entrySet()) {
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

    /**
     * 获取代码中支持的游戏类型
     *
     * @return
     */
    private Set<Integer> gameTypeList() {
        Set<Integer> gameTypeSet = new HashSet<>();
        Map<String, IRoomStartListener> startListenerMap = this.context.getBeansOfType(IRoomStartListener.class);
        for (Map.Entry<String, IRoomStartListener> en : startListenerMap.entrySet()) {
            Integer[] arr = en.getValue().getGameTypes();
            if (arr != null && arr.length != 0) {
                gameTypeSet.addAll(Arrays.asList(arr));
            }
        }
        return gameTypeSet;
    }

    //找到不需要启动的游戏的消息类型
    private Set<Integer> getNoStartGameMsgTypeSet(Set<Integer> noStartGameSet) {
        Set<Integer> set = new HashSet<>();
        for (int gameType : noStartGameSet) {
            Integer msgType = CoreConst.gameTypeToMsgTypeMap.get(gameType);
            if (msgType != null) {
                set.add(msgType);
                log.debug("根据配置，不需要加载的消息类型  gameType = {},messageType = 0x{}", gameType, Integer.toHexString(msgType));
            }
        }
        return set;
    }
}
