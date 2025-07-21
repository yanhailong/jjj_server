package com.jjg.game.common.netty;

import com.jjg.game.common.utils.OSUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import com.jjg.game.common.net.Connect;
import com.jjg.game.common.net.ConnectListener;
import com.jjg.game.common.net.NetAddress;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Nio 连接池
 *
 * @author nobody
 * @since 1.0
 */
public class ConnectPool<T extends NettyConnect<Object>> implements ConnectListener, TimerListener<String>,
    ChannelFutureListener {

    public final static int MAX_POOL_SIZE = 10;
    public final static int POOL_SIZE = 1;
    private static final EventLoopGroup WORKER_GROUP = OSUtils.IS_LINUX ? new EpollEventLoopGroup() :
        new NioEventLoopGroup();
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final NetAddress netAddress;
    private final NetAddress localAddress;
    private final ChannelInitializer<Channel> initializer;
    private final Random random = new Random();
    private final NetAddress startAddress;
    private int poolSize;
    private Bootstrap bootstrap;
    private List<T> connectList;
    private TimerCenter timerCenter;

    public ConnectPool(NetAddress netAddress, ChannelInitializer<Channel> initializer) {
        this(netAddress, null, null, initializer, POOL_SIZE);
    }

    public ConnectPool(NetAddress netAddress, NetAddress localAddress, NetAddress startAddress,
                       ChannelInitializer<Channel> initializer) {
        this(netAddress, localAddress, startAddress, initializer, POOL_SIZE);
    }

    public ConnectPool(NetAddress netAddress, NetAddress localAddress, NetAddress startAddress,
                       ChannelInitializer<Channel> initializer,
                       int poolSize) {
        this.netAddress = netAddress;
        this.initializer = initializer;
        this.poolSize = poolSize;
        this.localAddress = localAddress;
        this.startAddress = startAddress;
    }

    public ConnectPool<T> init() {
        //EventLoopGroup workerGroup = new NioEventLoopGroup();
        log.debug("startAddress={},localAddress={},netAddress={}", startAddress, localAddress, netAddress);
        if (startClient()) {
            bootstrap = new Bootstrap();
            bootstrap.group(WORKER_GROUP).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
                .handler(initializer);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        /*if (localAddress != null) {
            bootstrap.localAddress(localAddress.getHost(), localAddress.getPort());
        }*/
            bootstrap.connect(netAddress.getHost(), netAddress.getPort());
        }

        connectList = new CopyOnWriteArrayList<>();
        return this;
    }

    private boolean startClient() {
        if (localAddress == null) {
            return true;
        }

        if (startAddress == null) {
            return false;
        }
        return startAddress.equals(localAddress);
    }

    public ConnectPool<T> start(TimerCenter timerCenter) {
        log.debug("start net={}", netAddress);
        //create();
        if (timerCenter != null) {
            this.timerCenter = timerCenter;
            timerCenter.add(new TimerEvent<>(this, "", 30, Integer.MAX_VALUE, 5).withTimeUnit(TimeUnit.SECONDS));
        }
        return this;
    }

    public synchronized void addConnect(T connect) {
        connectList.add(connect);
        connect.addConnectListener(this);
    }

    public T getConnect() throws InterruptedException {
        if (connectList.isEmpty()) {
            create();
            return null;
        }
        if (connectList.size() == 1) {
            return connectList.get(0);
        }
        int index = random.nextInt(connectList.size());
        T connect = connectList.get(index);
        return connect;
    }

    /**
     * 同步获取连接
     *
     * @return
     * @throws InterruptedException
     */
    public T getConnectSync() throws InterruptedException {
        T c = getConnect();
        if (c == null) {
            c = (T) (bootstrap.connect().sync().channel().pipeline().get(NettyConnect.class));
            connectList.add(c);
        }
        return c;
    }

    private void create() {
        log.debug("开始创建连接,address={}", netAddress);
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            bootstrap.group(WORKER_GROUP).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true)
                .handler(initializer);
            bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        /*if (localAddress != null) {
            bootstrap.localAddress(localAddress.getHost(), localAddress.getPort());
        }*/
            bootstrap.connect(netAddress.getHost(), netAddress.getPort());
        }else {
            bootstrap.connect(netAddress.getHost(), netAddress.getPort()).addListener(this);
        }
    }

    public void close(T connect) {
        connectList.remove(connect);
    }

    @Override
    public <C extends Connect<Object>> void onConnectClose(C connect) {
        connectList.remove(connect);
    }

    @Override
    public void onTimer(TimerEvent<String> e) {
        if (connectList.isEmpty()) {
            log.warn("连接池为空");
            create();
        }
//        Ping ping = new Ping();
//        PFMessage pfMessage = new PFMessage(1, 1, ProtostuffUtil.serialize(ping));
//        ClusterMessage clusterMessage = new ClusterMessage(pfMessage);
//        for (NettyConnect connect : connectList) {
//            if (connect != null) {
//                connect.write(clusterMessage);
//            }
//        }
    }

    @Override
    public void operationComplete(ChannelFuture cf) throws Exception {
        if (cf.isSuccess()) {
            T connect = (T) cf.channel().pipeline().get(NettyConnect.class);
            connect.addConnectListener(this);
            //connectList.add(connect);
            //log.debug("连接创建成功,connect={}", connect);
        } else {
            if (cf.channel() != null) {
                cf.channel().close();
            }
            log.warn("连接创建失败", cf.cause());
        }
    }

    public void shutdown() {
        log.info("连接池关闭");
        if (timerCenter != null) {
            timerCenter.remove(this);
            for (T connect : connectList) {
                try {
                    connect.close();
                } catch (Exception e) {
                    log.warn("连接池关闭连接异常", e);
                }
            }
            connectList.clear();
        }
    }
}
