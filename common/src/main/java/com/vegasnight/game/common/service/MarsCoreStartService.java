package com.vegasnight.game.common.service;

import com.vegasnight.game.common.cluster.ClusterMessageDispacher;
import com.vegasnight.game.common.cluster.ClusterMessageHandler;
import com.vegasnight.game.common.cluster.ClusterSystem;
import com.vegasnight.game.common.curator.MarsCurator;
import com.vegasnight.game.common.curator.NodeManager;
import com.vegasnight.game.common.micservice.MicServiceManager;
import com.vegasnight.game.common.monitor.FileMonitor;
import com.vegasnight.game.common.timer.TimerCenter;
import com.vegasnight.game.common.utils.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * @author 11
 * @date 2022/6/9
 */
@Service
public class MarsCoreStartService {

    @Autowired
    private ClusterSystem clusterSystem;
    @Autowired
    private ClusterMessageDispacher clusterMessageDispacher;
    @Autowired
    private NodeManager nodeManager;
    @Autowired
    private MarsCurator marsCurator;
    @Autowired
    private MicServiceManager micServiceManager;
    @Autowired
    private FileMonitor fileMonitor;
    @Autowired
    private ClusterMessageHandler clusterMessageHandler;

    private TimerCenter timerCenter;

    /**
     * 启动时初始化
     * @param context
     */
    public void init(ApplicationContext context){
        init(context,true);
    }

    /**
     * 启动时初始化
     * @param context
     */
    public void init(ApplicationContext context,boolean clusterSystemOntimer){
        initTimerCenter();
        CommonUtil.setContext(context);
        clusterMessageHandler.init();
        clusterSystem.init(clusterSystemOntimer,this.timerCenter);
        clusterMessageDispacher.init(context);
        marsCurator.init(context);
        nodeManager.init(marsCurator);
        micServiceManager.init(context);
        fileMonitor.start();
    }

    public void initTimerCenter(){
        this.timerCenter.start();
    }

    public void shutdown(){}


    @Bean
    public TimerCenter timerCenter() {
        int cpuNum = Runtime.getRuntime().availableProcessors();
        return this.timerCenter = new TimerCenter("timer-center",cpuNum,cpuNum * 2,50);
    }
}
