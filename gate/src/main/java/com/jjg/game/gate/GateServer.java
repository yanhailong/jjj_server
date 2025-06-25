package com.jjg.game.gate;

import com.jjg.game.common.gate.GateChannelInitializer;
import com.jjg.game.common.netty.NettyServer;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import com.jjg.game.common.ws.WebSocketChildChannelHandler;
import com.jjg.game.common.ws.WssChannelHandler;
import io.netty.channel.ChannelInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author 11
 * @date 2025/5/23 16:50
 */
@Component
public class GateServer implements TimerListener {
    private Logger log = LoggerFactory.getLogger(getClass());
    @Autowired
    private GateConfig gateConfig;
    @Autowired
    private TimerCenter timerCenter;


    @Override
    public void onTimer(TimerEvent e) {
        start();
    }

    public void init() {
        this.timerCenter.add((new TimerEvent(this, 10, "")).withTimeUnit(TimeUnit.SECONDS));
    }

    public void start() {
        NettyServer wsServer;
        if (this.gateConfig.netAddress != null) {
            GateChannelInitializer initializer = new GateChannelInitializer();
            wsServer = new NettyServer(this.gateConfig.getNetAddress().getPort(), initializer);
            wsServer.start();
        }

        if (this.gateConfig.wsAddress != null) {
            ChannelInitializer wsc = new WebSocketChildChannelHandler(this.gateConfig.timeOutSecond);
            if (this.gateConfig.wss) {
                wsc = new WssChannelHandler(this.gateConfig.sslKeyPath, this.gateConfig.sslKeyPwd);
            }

            wsServer = new NettyServer(this.gateConfig.getWsAddress().getPort(), (ChannelInitializer)wsc);
            wsServer.start();
        }

        log.info("===============网关服务器启动完成，开始接收连接==============");
    }
}
