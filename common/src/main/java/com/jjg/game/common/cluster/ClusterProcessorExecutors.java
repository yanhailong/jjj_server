package com.jjg.game.common.cluster;

import com.jjg.game.common.concurrent.BaseProcessor;
import com.jjg.game.common.concurrent.processor.GameProcessor;
import com.jjg.game.common.concurrent.processor.HallProcessor;
import com.jjg.game.common.config.NodeConfig;
import com.jjg.game.common.curator.NodeType;
import com.jjg.game.common.utils.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 节点线程(分发/执行)器 TODO 需要添加线程卡死检测逻辑
 *
 * @author 2CL
 */
public class ClusterProcessorExecutors {

    private static final Logger log = LoggerFactory.getLogger(ClusterProcessorExecutors.class);
    private NodeConfig nodeConfig;

    /**
     * 线程池大小
     */
    private static final int THREAD_POOL_NUM;

    /**
     * 逻辑处理线程池
     */
    private final Map<Integer, ? extends BaseProcessor> processorPool = new ConcurrentHashMap<>(THREAD_POOL_NUM);

    static {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        if (availableProcessors < 4) {
            availableProcessors = 4;
        } else {
            availableProcessors = availableProcessors / 2 + 2;
        }
        THREAD_POOL_NUM = availableProcessors;
    }

    /**
     * 需要的线程模块
     */
    public enum ProcessorModule {
        // 大厅线程配置
        HALL_PROCESSOR(NodeType.HALL, HallProcessor::new),
        // 游戏线程配置
        GAME_PROCESSOR(NodeType.GAME, GameProcessor::new),
        ;

        @FunctionalInterface
        interface ProcessorProducer<T extends BaseProcessor> {
            T createNewProcessor(int threadId);
        }

        /**
         * 节点类型
         */
        private final NodeType nodeType;
        /**
         * 线程生成器
         */
        private final ProcessorProducer<?> processorProducer;

        ProcessorModule(NodeType nodeType, ProcessorProducer<?> processorProducer) {
            this.nodeType = nodeType;
            this.processorProducer = processorProducer;
        }

        public <T extends BaseProcessor> T getModuleProcessor(int threadId) {
            return (T) processorProducer.createNewProcessor(threadId);
        }

        public NodeType getNodeType() {
            return nodeType;
        }

        public static ProcessorModule getModuleByNodeType(NodeType nodeType) {
            for (ProcessorModule value : values()) {
                if (value.nodeType.equals(nodeType)) {
                    return value;
                }
            }
            return null;
        }
    }

    /**
     * 获取通过session中的workId获取对应的逻辑线程
     */
    public <T extends BaseProcessor> T getProcessorById(long workId) {
        // 默认节点的工作ID是大于0的
        if (workId == 0) {
            return null;
        }
        int threadId = calcThreadId(workId);
        if (processorPool.containsKey(threadId)) {
            return (T) processorPool.get(threadId);
        }
        if (nodeConfig == null) {
            nodeConfig = CommonUtil.getContext().getBean(NodeConfig.class);
        }
        NodeType nodeType = NodeType.getNodeTypeByName(nodeConfig.getType());
        if (nodeType == null) {
            log.error("未定义的节点配置: {}", nodeConfig.getType());
            throw new RuntimeException("未定义的节点配置: " + nodeConfig.getType());
        }
        ProcessorModule processorModule = ProcessorModule.getModuleByNodeType(nodeType);
        if (processorModule == null) {
            log.error("节点: {} 未找到线程基础配置,将走默认逻辑线程: {}", nodeType.name(), Thread.currentThread().getName());
            return null;
        }
        return (T) processorPool.computeIfAbsent(threadId, k -> processorModule.getModuleProcessor(threadId));
    }

    /**
     * 计算需要放入的线程ID,当前暂时通过PFSession的workId进行绑定,后续如果需要平衡负载,可通过processors中的
     * processor对象动态计算
     */
    private int calcThreadId(long workId) {
        return Math.toIntExact(workId % THREAD_POOL_NUM);
    }

    /**
     * 获取单例
     */
    public static ClusterProcessorExecutors getInstance() {
        return Singleton.INSTANCE.getInstance();
    }

    public enum Singleton {

        INSTANCE;

        final ClusterProcessorExecutors instance;

        Singleton() {
            this.instance = new ClusterProcessorExecutors();
        }

        public ClusterProcessorExecutors getInstance() {
            return instance;
        }
    }
}
