//package com.vegasnight.game.common.netty;
//
//import io.netty.bootstrap.Bootstrap;
//import io.netty.channel.*;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.SocketChannel;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.handler.codec.http.DefaultHttpHeaders;
//import io.netty.handler.codec.http.HttpClientCodec;
//import io.netty.handler.codec.http.HttpHeaders;
//import io.netty.handler.codec.http.HttpObjectAggregator;
//import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
//import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
//import io.netty.handler.codec.http.websocketx.WebSocketVersion;
//import io.netty.handler.logging.LogLevel;
//import io.netty.handler.logging.LoggingHandler;
//import io.netty.handler.stream.ChunkedWriteHandler;
//
//import java.net.URI;
//
///**
// * @author 11
// * @date 2022/10/11
// */
//public class NettyClient extends Thread{
//    private static final EventLoopGroup group = new NioEventLoopGroup();
//
//    public String ip;
//    private int port;
//
//    public NettyClient(String ip,int port){
//        this.ip = ip;
//        this.port = port;
//    }
//
//    @Override
//    public void run() {
//        final ClientHandler handler =new ClientHandler();
//        try {
//            Bootstrap bootstrap = new Bootstrap();
//            bootstrap.group(group).channel(NioSocketChannel.class)
//                    .option(ChannelOption.TCP_NODELAY, true)
//                    .option(ChannelOption.SO_KEEPALIVE,true)
//                    .handler(new LoggingHandler(LogLevel.INFO))
//                    .handler(new ChannelInitializer<SocketChannel>() {
//                        @Override
//                        protected void initChannel(SocketChannel ch) throws Exception {
//                            ChannelPipeline pipeline = ch.pipeline();
//                            // 添加一个http的编解码器
//                            pipeline.addLast(new HttpClientCodec());
//                            // 添加一个用于支持大数据流的支持
//                            pipeline.addLast(new ChunkedWriteHandler());
//                            // 添加一个聚合器，这个聚合器主要是将HttpMessage聚合成FullHttpRequest/Response
//                            pipeline.addLast(new HttpObjectAggregator(1024 * 64));
//                            pipeline.addLast(new TestClientEncoder());
//                            pipeline.addLast(handler);
//                        }
//                    });
//
//            URI websocketURI = new URI("ws://" + this.ip + ":" + this.port);
//            HttpHeaders httpHeaders = new DefaultHttpHeaders();
//            //进行握手
//            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13, (String)null, true,httpHeaders);
//            final Channel channel=bootstrap.connect(websocketURI.getHost(),websocketURI.getPort()).sync().channel();
//            handler.setHandshaker(handshaker);
//            handshaker.handshake(channel);
//            //阻塞等待是否握手成功
//            handler.handshakeFuture().sync();
//
//            System.out.println("111111");
//            // 等待连接被关闭
//            //channel.closeFuture().sync();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
