package com.vegasnight.game.gate;

import com.vegasnight.game.common.net.NetAddress;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 *
 * @author 11
 * @date 2025/5/23 16:51
 */
@ConfigurationProperties(prefix = "gate")
@Component
public class GateConfig {
    public NetAddress netAddress;
    public NetAddress wsAddress;
    public boolean wss;
    public String sslKeyPath = "";
    public String sslKeyPwd = "123456";
    public int timeOutSecond = 60;

    public NetAddress getNetAddress() {
        return this.netAddress;
    }

    public void setNetAddress(NetAddress netAddress) {
        this.netAddress = netAddress;
    }

    public NetAddress getWsAddress() {
        return this.wsAddress;
    }

    public void setWsAddress(NetAddress wsAddress) {
        this.wsAddress = wsAddress;
    }

    public int getTimeOutSecond() {
        return this.timeOutSecond;
    }

    public void setTimeOutSecond(int timeOutSecond) {
        this.timeOutSecond = timeOutSecond;
    }
}
