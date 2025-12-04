package com.jjg.game.common.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjg.game.common.utils.ObjectMapperUtil;
import io.micrometer.common.util.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.redisson.connection.ConnectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * redisson配置
 *
 * @author 2CL
 */
@Configuration
public class RedissonConfig {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Value("${spring.data.redis.host:}")
    private String redisAddress;
    @Value("${spring.data.redis.port:0}")
    private int redisPort;
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    @Value("${spring.data.redis.database:0}")
    private int redisDb;
    @Value("${spring.data.redis.cluster.nodes:}")
    private String clusterNodes;
    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;
    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config;
        if (!StringUtils.isEmpty(sentinelNodes)) {
            config = configureSentinelMode();
        }else if(StringUtils.isEmpty(clusterNodes)){
            config = configureSingleMode();
        }else {
            config = configureClusterMode();
        }

        // 使用与RedisConfig相同的序列化方式
        ObjectMapper mapper = ObjectMapperUtil.getDefualtConfigObjectMapper();
        Codec codec = new JsonJacksonCodec(mapper);
        config.setCodec(codec);

        config.setConnectionListener(new ConnectionListener() {
            @Override
            public void onConnect(InetSocketAddress addr) {
                log.debug("redisson已连接 addr = {}",addr);
            }

            @Override
            public void onDisconnect(InetSocketAddress addr) {
                log.debug("redisson连接断开 addr = {}",addr);
            }
        });
        return Redisson.create(config);
    }

    /**
     * 创建单节点配置
     * @return
     */
    private Config configureSingleMode() {
        Config config = new Config();

        String redissonAddr;
        if(redisAddress.startsWith("master")){
            redissonAddr = "rediss://" + redisAddress + ":" + redisPort;
        }else {
            redissonAddr = "redis://" + redisAddress + ":" + redisPort;
        }

        config.useSingleServer().setAddress(redissonAddr)
                .setPassword(redisPassword)
                .setDatabase(redisDb);
        return config;
    }

    /**
     * 创建哨兵模式配置
     * @return
     */
    private Config configureSentinelMode() {
        Config config = new Config();
        String[] nodes = sentinelNodes.split(",");
        String[] nodeAddresses = Arrays.stream(nodes)
                .map(node -> "redis://" + node.trim())
                .toArray(String[]::new);

        config.useSentinelServers()
                .setMasterName(sentinelMaster) // 设置主节点名称
                .addSentinelAddress(nodeAddresses) // 设置哨兵节点列表
                .setPassword(redisPassword)
                .setDatabase(redisDb)
                .setScanInterval(2000); // 哨兵节点状态扫描间隔
        return config;
    }

    /**
     * 创建集群配置
     * @return
     */
    private Config configureClusterMode() {
        Config config = new Config();
        List<String> nodes = Arrays.asList(clusterNodes.split(","));
        String[] nodeAddresses = nodes.stream()
                .map(node -> "redis://" + node.trim())
                .toArray(String[]::new);
        config.useClusterServers()
                .setPassword(redisPassword)
                .addNodeAddress(nodeAddresses)
                .setScanInterval(2000); // 集群状态扫描间隔
        return config;
    }
}
