package com.jjg.game.core.net.connect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jjg.game.common.protostuff.ProtostuffUtil;
import com.jjg.game.conf.IMainWindow;
import com.jjg.game.core.Log4jManager;
import com.jjg.game.core.net.codec.*;
import com.jjg.game.core.robot.RobotThread;
import com.jjg.game.core.robot.RobotThreadFactory;
import com.jjg.game.core.robot.StressRobotManager;
import com.jjg.game.pbmsg.hall.ReqChooseGame;
import com.jjg.game.utils.GsonUtils;
import com.jjg.game.utils.HttpRequestUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author unknown
 * @function 模拟客户端建立socket连接
 */
public class GameRobotClient {

    private static final boolean USE_WEB_SOCKET = true;
    private static final Logger log = LoggerFactory.getLogger(GameRobotClient.class);
    public static Bootstrap bootstrap;
    public static EventLoopGroup group;

    public static void init() {
        if (USE_WEB_SOCKET) {
            createWebSocketClientConn(null);
        } else {
            createTcpClientConn();
        }
    }

    public static void createTcpClientConn() {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline channelPipeline = socketChannel.pipeline();
                channelPipeline.addLast("Decoder", new ExternalTcpDecoder());
                channelPipeline.addLast("Encoder", new ExternalTcpEncoder());
                channelPipeline.addLast("BusinessHandler", new ClientHandler());
                channelPipeline.addLast(new HttpObjectAggregator(8192));
            }
        });

        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    }

    public static void createWebSocketClientConn(URI uri) {
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        WebSocketClientHandler handler = new WebSocketClientHandler();
        bootstrap.group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel socketChannel) {
                ChannelPipeline channelPipeline = socketChannel.pipeline();
                //channelPipeline.addLast("decoder", new ExternalTcpDecoder());
                // 添加 HTTP 编解码器
                channelPipeline.addLast("http-codec", new HttpClientCodec());
                // 聚合http信息为完整信息
                channelPipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                channelPipeline.addLast("websocket-handler", new WebSocketClientProtocolHandler(
                        uri, WebSocketVersion.V13, null,
                        true,         // allowExtensions
                        new DefaultHttpHeaders(),  // 额外头部
                        65536
                ));
                // 数据压缩
                channelPipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
                channelPipeline.addLast("encoder", new WebSocketMsgEncoder());
                channelPipeline.addLast("handler", handler);
            }
        });
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    }

    public static synchronized void run(IMainWindow window, RobotThread ctx) throws Exception {
        String baseLoginRequestUrl = window.isLocalLogin() ? window.getRunConf().localLoginUrl : window.getRunConf().devLoginUrl;

        // 构建登录地址
        String loginUrl = buildLoginUrl(baseLoginRequestUrl);
        // 请求登录获取sdkToken
        JsonObject resultJson = sendHttp(window, loginUrl, ctx);
        if (resultJson == null) {
            Log4jManager.getInstance().error(window, "向登录服请求进入游戏,获取result失败");
            ctx.serverId = 0;
            ctx.serverIp = null;
            ctx.serverPort = 0;
            return;
        }
        JsonObject dataObject = resultJson.get("data").getAsJsonObject();
        // 请求进入游戏 设置游戏服id,ip,端口和获取登录token
        String token = dataObject.get("token").getAsString();
        URI uri = new URI(dataObject.get("gameserver").getAsString());
        ctx.serverIp = uri.getHost();
        ctx.serverPort = uri.getPort();
        // 设置登录所需的token
        ctx.setToken(token);
        ctx.getPlayer().getPlayerInfo().setPid(dataObject.get("playerId").getAsInt());
        // 构建socket地址数据
        Log4jManager.getInstance().info(window, "登陆,准备连接网关服socket:" + ctx.getName() + ",ip:" + ctx.serverIp + ",port:" + ctx.serverPort);
        createWebSocketClientConn(uri);
        if (window.getConsole().isLoginQue()) {
            // 排队连接游戏服 需等上一个机器人完成连接再继续
            connGameByLoginQueue(uri, window, ctx);
        } else {
            // 正常登录游戏服 无需等上一个机器人完成连接就可继续
            connGameByNormalLogin(uri, window, ctx);
        }
    }

    /**
     * 登录失败后记录失败次数
     *
     * @param ctx 机器人数据
     */
    private static void loginFailedRecord(RobotThread ctx) {
        if (StressRobotManager.failedServerIp == null) {
            StressRobotManager.failedServerIp = ctx.serverIp;
            StressRobotManager.failedServerPort = ctx.serverPort;
            StressRobotManager.failedConnectCount = 1;
        } else if (ctx.serverIp.equals(StressRobotManager.failedServerIp) && ctx.serverPort == StressRobotManager.failedServerPort) {
            StressRobotManager.failedConnectCount++;
        }
    }

    /**
     * 登录需等待当前连接完成
     *
     * @param uri    地址
     * @param window window
     * @param ctx    机器人数据
     * @throws InterruptedException e
     */
    private static void connGameByLoginQueue(URI uri, IMainWindow window, RobotThread ctx) throws InterruptedException {
        InetSocketAddress inet = new InetSocketAddress(uri.getHost(), uri.getPort());
        Channel channel = bootstrap.connect(inet).sync().channel();
        if (channel.isActive()) {
            log.info("netty连接成功");
            initChanel(window, ctx, channel);
            Log4jManager.getInstance().info(window, "登陆, 连接游戏服socket成功:" + ctx.getName() + ",ip:" + ctx.serverIp + ",port:" + ctx.serverPort + ",channelId:" + ctx.getChannel().hashCode());
        } else {
            Log4jManager.getInstance().warn(window, "登陆, 连接游戏服socket失败:" + ctx.getName() + ",ip:" + ctx.serverIp + ",port:" + ctx.serverPort);
            // 记录失败的服务器连接信息
            loginFailedRecord(ctx);
        }
    }

    /**
     * 请求连接游戏服无需等待当前连接完成
     *
     * @param uri    地址
     * @param window window
     * @param ctx    机器人数据
     */
    private static void connGameByNormalLogin(URI uri, IMainWindow window, RobotThread ctx) throws InterruptedException {
        InetSocketAddress inet = new InetSocketAddress(uri.getHost(), uri.getPort());
        Log4jManager.getInstance().info(window, "登陆,准备连接游戏服,非排队登陆." + ctx.getName() + ",ip:" + ctx.serverIp + ",port:" + ctx.serverPort);
        ChannelFuture channelFuture = bootstrap.connect(inet);
        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                initChanel(window, ctx, channelFuture.channel());
                Log4jManager.getInstance().info(window, "登陆, 连接游戏服socket成功:" + ctx.getName() + ",ip:" + ctx.serverIp + ",port:" + ctx.serverPort + ",channelId:" + ctx.getChannel().hashCode());
            } else {
                Log4jManager.getInstance().error(window, "登陆, 连接游戏服socket失败:" + ctx.getName() + ",ip:" + ctx.serverIp + ",port:" + ctx.serverPort);

                // 记录失败的服务器连接信息
                loginFailedRecord(ctx);
            }
        });
    }

    /**
     * 构建向登录服请求登录的地址
     *
     * @param url 登录服地址
     * @return 请求登录地址
     */
    protected static String buildLoginUrl(String url) {
        return url + "guestlogin";
    }

    protected static void initChanel(IMainWindow window, RobotThread ctx, Channel channel) {
        // Log4jManager.getInstance().info(window,
        // "_initChanel,serverIp:" + serverIp + " serverPort:" + serverPort);
        if (channel.isActive()) {
            ctx.channel = channel;
            RobotThreadFactory.putRobot(ctx.channel.id().asLongText(), ctx);
            StressRobotManager.instance().addRobot(ctx);
            window.getCtx().addConnection();
            window.getCtx().addConnectionTots();
            ctx.run(false);
            Log4jManager.getInstance().info(window, "登陆,连接游戏服成功:" + ctx.getName());
        } else {
            Log4jManager.getInstance().warn(window, "登陆,连接游戏服失败:" + ctx.getName());
        }
    }

    private static JsonObject sendHttp(IMainWindow window, String url, RobotThread ctx) {
        Map<String, String> params = new HashMap<>();
        params.put("guest", ctx.getAccountId());
        String jsonData = GsonUtils.toJson(params);
        Map<String, String> messageHeaderProperty = new HashMap<>(1);
        messageHeaderProperty.put("Content-Type", "application/json");
        String responseText = HttpRequestUtils.sendPost(url, jsonData, 0, messageHeaderProperty);
        if (responseText == null) {
            // 如果是请求异常，异常日志会在 HttpRequestUtils 中打印错误日志
            Log4jManager.getInstance().error(window, "登陆失败,请求地址: " + url);
            return null;
        }

        JsonObject resultJsonObject = GsonUtils.fromJson(responseText, JsonObject.class);
        JsonElement statusElement = resultJsonObject.get("code");
        if (statusElement == null || statusElement.getAsInt() != HttpURLConnection.HTTP_OK) {
            Log4jManager.getInstance().error(window, "登陆失败,用户中心返回.status is null! response text: " + responseText);
            return null;
        }

        return resultJsonObject;
    }
}
