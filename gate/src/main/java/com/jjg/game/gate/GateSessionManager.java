package com.jjg.game.gate;

import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.gate.GateSession;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/5/23 16:52
 */
@Component
public class GateSessionManager implements TimerListener {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TimerCenter timerCenter;
    @Autowired
    public NodeConfig nodeConfig;

    private TimerEvent<String> closeEvent;
    private TimerEvent<String> userNumEvent;

    @Override
    public void onTimer(TimerEvent e) {
        if(e == closeEvent){
            int size = GateSession.gateSessionMap.size();
            log.info("节点权重={},当前session数量={}", nodeConfig.weight, size);
            if (nodeConfig.weight == 0 && size < 1) {
                System.exit(0);
            }
        }
    }

    public void init(){
        closeEvent = new TimerEvent<>(this, "check", 5).withTimeUnit(TimeUnit.MINUTES);
        timerCenter.add(closeEvent);
    }

    public void shutdown(){
        this.log.info("网关服务器开始关闭，当前在线人数：{}", GateSession.gateSessionMap.size());
        GateSession.gateSessionMap.forEach((k,v) -> {
            try {
                this.log.info("关闭用户连接,session={}", v);
                v.close();
            } catch (Exception var3) {
                this.log.warn("关闭连接错误,session={}",v, var3);
            }
        });
        this.log.info("关闭完成");
    }
}
