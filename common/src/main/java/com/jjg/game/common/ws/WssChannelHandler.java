package com.jjg.game.common.ws;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

/**
 * @since 1.0
 */
public class WssChannelHandler extends ChannelInitializer<SocketChannel> {

    private String sslKeyPath;
    private String sslKeyPwd;
    private int timeout;

    public WssChannelHandler() {
    }

    public WssChannelHandler(String sslKeyPath, String sslKeyPwd, int timeout) {
        this.sslKeyPath = sslKeyPath;
        this.sslKeyPwd = sslKeyPwd;
        this.timeout = timeout;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // TODO Auto-generated method stub
//        SSLContext sslContext = createSslContext("JKS", sslKeyPath, sslKeyPwd);
//        //SSLEngine 此类允许使用ssl安全套接层协议进行安全通信            
//        SSLEngine engine = sslContext.createSSLEngine();
//        engine.setUseClientMode(false);
//        ch.pipeline().addLast(new SslHandler(engine));
        ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(this.timeout, 0, 0, TimeUnit.SECONDS));
        ch.pipeline().addLast("http-codec", new HttpServerCodec());
        ch.pipeline().addLast("aggregator", new HttpObjectAggregator(65536));
        ch.pipeline().addLast("http-chunked", new ChunkedWriteHandler());
        // 自动握手和协议自动解析组装
        ch.pipeline().addLast("websocket-handler", new WebSocketServerProtocolHandler("/", null, true));
        ch.pipeline().addLast("handler", new WebSocketServerHandler());
    }

    public static SSLContext createSslContext(String type, String path, String password) throws Exception {
        /// "JKS"
        KeyStore ks = KeyStore.getInstance(type);
        /// 证书存放地址
        InputStream ksInputStream = new FileInputStream(path);
        ks.load(ksInputStream, password.toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password.toCharArray());
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);
        return sslContext;
    }
}
