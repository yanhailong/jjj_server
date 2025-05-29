package com.vegasnight.game.common.data;

/**
 *
 * @scene 1.0
 *
 */
public class ServerInfo {

    protected String host;

    protected int port;

    public ServerInfo() {
    }

    public ServerInfo(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
