package com.vegasnight.game.common.net;

/**
 * Session 类抽象接口
 * <p>
 * 泛型类型 T 表示该session可发送的消息类型
 * <p>
 * 泛型类型 M 表示该session中连接可发送的消息类型
 * <p>
 * @since 1.0
 */
public abstract class Session<T, M> implements ConnectListener {
    protected String sessionId;
    protected Connect<M> connect;
    protected NetAddress address;
    protected Object reference;
    protected SessionListener sessionListener;

    public Session(String sessionId, Connect connect, NetAddress address) {
        this.sessionId = sessionId;
        this.connect = connect;
        this.address = address;
    }

    public abstract void send(T msg);

    public void close() {
        if (connect != null && connect.isActive()) {
            connect.close();
        }
    }

    @Override
    public void onConnectClose(Connect connect) {
        if (sessionListener != null) {
            sessionListener.onSessionClose(this);
        }
    }

    public String sessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Connect getConnect() {
        return connect;
    }

    public void setConnect(Connect connect) {
        this.connect = connect;
    }

    public NetAddress getAddress() {
        return address;
    }

    public void setAddress(NetAddress address) {
        this.address = address;
    }

    public Object getReference() {
        return reference;
    }

    public void setReference(Object reference) {
        this.reference = reference;
    }

    public SessionListener getSessionListener() {
        return sessionListener;
    }

    public void setSessionListener(SessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @Override
    public String toString() {
        return "Session{sessionId=" + sessionId + ", connect=" + connect + ", address=" + address + ",hash" + hashCode() + '}';
    }
}
