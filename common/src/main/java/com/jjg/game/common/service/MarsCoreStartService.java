package com.jjg.game.common.service;

import com.jjg.game.common.cluster.ClusterMessageDispacher;
import com.jjg.game.common.cluster.ClusterMessageHandler;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.curator.NodeManager;
import com.jjg.game.common.monitor.FileMonitor;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.utils.CommonUtil;
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
