package com.jjg.game.core.utils;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.utils.CommonUtil;
import com.jjg.game.core.data.NoticeTipBuilder;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.pb.NoticeTip;
import org.springframework.context.ApplicationContext;

import java.util.function.Supplier;

/**
 * tip消息发送工具
 */
public class TipUtils {

    private static volatile ClusterSystem clusterSystemInstance;

    /**
     * 获取ClusterSystem实例（懒初始化，线程安全）
     */
    private static ClusterSystem getClusterSystem() {
        if (clusterSystemInstance == null) {
            synchronized (TipUtils.class) {
                if (clusterSystemInstance == null) {
                    ApplicationContext context = CommonUtil.getContext();
                    if (context == null) {
                        throw new IllegalStateException("ApplicationContext is not available");
                    }
                    clusterSystemInstance = context.getBean(ClusterSystem.class);
                }
            }
        }
        return clusterSystemInstance;
    }

    /**
     * 弹窗内容的参数类型
     */
    public interface TipContextArgsType {
        /**
         * 多语言id
         */
        int LANGUAGE_ID = 1;
        /**
         * 参数
         */
        int PARAMETER = 2;
    }

    /**
     * 弹窗类型
     */
    public interface TipType {
        /**
         * 弹出只有确定按钮的提示框
         */
        int ALERT = 1;
        /**
         * 弹出一行文本提示，并短暂停留消失，下一条消息直接覆盖
         */
        int TOAST = 2;
    }

    /**
     * 发送提示消息给指定玩家ID
     *
     * @param playerId 玩家ID
     * @param supplier NotifyNotice构建器
     */
    public static void sendTip(long playerId, int tipType, Supplier<NoticeTip> supplier) {
        getClusterSystem().sendToPlayer(supplier.get(), playerId);
    }

    /**
     * 发送提示消息给PlayerController
     *
     * @param playerController 玩家控制器
     * @param supplier         NotifyNotice构建器
     */
    public static void sendTip(PlayerController playerController, int tipType, Supplier<NoticeTip> supplier) {
        if (playerController != null) {
            playerController.send(supplier.get());
        }
    }

    /**
     * 发送简单提示消息给PlayerController
     *
     * @param playerController 玩家控制器
     * @param languageId       多语言ID
     */
    public static void sendTip(PlayerController playerController, int tipType, long languageId) {
        if (playerController != null) {
            NoticeTip notice = NoticeTipBuilder.builder().languageId(languageId).build();
            playerController.send(notice);
        }
    }

    /**
     * 发送简单提示消息给指定玩家ID
     *
     * @param playerId   玩家ID
     * @param languageId 多语言ID
     */
    public static void sendTip(long playerId, int tipType, long languageId) {
        NoticeTip notice = NoticeTipBuilder.builder().languageId(languageId).build();
        sendTip(playerId, tipType, () -> notice);
    }

    /**
     * 发送Toast提示消息给指定玩家ID
     *
     * @param playerId   玩家ID
     * @param languageId 多语言ID
     */
    public static void sendToastTip(long playerId, long languageId) {
        NoticeTip notice = NoticeTipBuilder.builder().languageId(languageId).build();
        sendTip(playerId, TipType.TOAST, () -> notice);
    }

}
