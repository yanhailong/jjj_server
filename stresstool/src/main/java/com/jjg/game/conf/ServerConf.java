package com.jjg.game.conf;

import java.util.Objects;

/**
 * @function 服务器配置
 */
public final class ServerConf {

    /**
     * 服务器名称
     */
    private String name;

    /**
     * 分组类型
     */
    private String type;

    private String loginServerIp;

    private int loginServerPort;

    public ServerConf(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public ServerConf(int loginServerPort, String loginServerIp, String type, String name) {
        this.loginServerPort = loginServerPort;
        this.loginServerIp = loginServerIp;
        this.type = type;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLoginServerIp() {
        return loginServerIp;
    }

    public void setLoginServerIp(String loginServerIp) {
        this.loginServerIp = loginServerIp;
    }

    public int getLoginServerPort() {
        return loginServerPort;
    }

    public void setLoginServerPort(int loginServerPort) {
        this.loginServerPort = loginServerPort;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServerConf)) {
            return false;
        }
        return this.name.equals(((ServerConf) obj).name) && this.type.equals(((ServerConf) obj).type);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(this.name);
        hash = 89 * hash + Objects.hashCode(this.type);
        return hash;
    }
}
