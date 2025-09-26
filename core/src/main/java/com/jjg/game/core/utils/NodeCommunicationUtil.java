package com.jjg.game.core.utils;

import cn.hutool.core.lang.generator.SnowflakeGenerator;
import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterMessage;
import com.jjg.game.common.pb.AbsNodeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 节点通信工具，通过阻塞队列实现同步通信，需要自己实现请求和响应
 *
 * @author lm
 * @date 2025/9/25 14:06
 */
public class NodeCommunicationUtil {
    private static final Logger log = LoggerFactory.getLogger(NodeCommunicationUtil.class);
    //请求等待队列
    private final static Map<Long, ArrayBlockingQueue<Object>> reqWaitMap = new ConcurrentHashMap<>();
    //id生成器
    private final static SnowflakeGenerator SNOWFLAKE = new SnowflakeGenerator(9, 0);

    public static Object sendAndGetResult(ClusterClient client, AbsNodeMessage message) {
        //生成一个随其请求id
        message.reqId = SNOWFLAKE.next();
        ClusterMessage clusterMessage = new ClusterMessage(message);
        ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<>(1);
        reqWaitMap.put(message.reqId, queue);
        try {
            log.info("发送请求 reqId:{}", message.reqId);
            client.write(clusterMessage);
            //等待500毫秒
            return queue.poll(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("节点发送消息失败 cmd={}", clusterMessage.getMsg().cmd);
        } finally {
            reqWaitMap.remove(message.reqId);
        }
        return null;
    }

    /**
     * 收到请求将结果返回
     *
     * @param reqId    请求id
     * @param response 响应结果
     */
    public static void addResponse(long reqId, Object response) {
        log.info("接收请求 reqId:{}", reqId);
        ArrayBlockingQueue<Object> queue = reqWaitMap.get(reqId);
        if (queue == null) {
            return;
        }
        boolean offer = queue.offer(response);
        if (!offer) {
            log.error("节点接收消息完成后 放入结果失败 reqId={}", reqId);
        }
    }
}
