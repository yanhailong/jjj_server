package com.vegasnight.game.common.net;

/**
 * <p>连接接口</p>
 * <p>
 * @since 1.0
 */
public interface Connect<T> {

    /**
     * 写消息方法
     *
     * @param msg
     * @return
     */
    boolean write(T msg);

    /**
     * 关闭连接
     *
     * @return
     */
    void close();

    /**
     * 连接是否有效
     *
     * @return
     */
    boolean isActive();

    /**
     * 连接的地址
     *
     * @return
     */
    NetAddress address();

    /**
     * 当连接关闭
     */
    void onClose();

    /**
     * 当连接被创建
     */
    void onCreate();

    void addConnectListener(ConnectListener connectListener);

    void removeConnectListener(ConnectListener connectListener);
}
