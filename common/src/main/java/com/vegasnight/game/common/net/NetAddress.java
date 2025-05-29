package com.vegasnight.game.common.net;

/**
 * @since 1.0
 */
public class NetAddress {

    private String host;

    private int port;

    public NetAddress() {
//		System.out.println("wo bei gou zhao liao");
    }

    public NetAddress(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
//		System.out.println("set host");
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
//		System.out.println("set port");
        this.port = port;
    }

    @Override
    public String toString() {
        return "[host=" + host + ", port=" + port + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetAddress that = (NetAddress) o;

        if (port != that.port) return false;
        return host != null ? host.equals(that.host) : that.host == null;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + port;
        return result;
    }
}
