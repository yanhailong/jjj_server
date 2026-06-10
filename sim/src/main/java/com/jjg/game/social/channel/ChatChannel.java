package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.data.ChatMessage;

/**
 * 聊天频道 SPI。
 * <p>
 * 每个频道负责: 自身规则校验({@link #validate})、消息投递({@link #dispatch})、历史拉取({@link #loadHistory})。
 * 通用的字数/频率校验由 {@code ChatService} 统一处理 (读取 {@link #maxContentLength()}/{@link #sendIntervalMs()}),
 * 频道只需实现自己特有的规则(联盟成员校验、私聊黑名单校验等)。
 * <p>
 * 新增频道实现本接口并交给 Spring 管理, 启动时由 {@code ChatChannelRegistry} 自动注册, 即插即用。
 *
 * @author 11
 * @date 2026/6/9
 */
public interface ChatChannel {

    /**
     * 频道类型
     */
    ChatChannelType type();

    /**
     * 单条消息最大字数, 0 表示不限
     */
    default int maxContentLength() {
        return 0;
    }

    /**
     * 同一玩家最小发送间隔(ms), 0 表示不限
     */
    default long sendIntervalMs() {
        return 0;
    }

    /**
     * 是否允许客户端主动发送 (系统频道为 false)
     */
    default boolean clientSendable() {
        return true;
    }

    /**
     * 频道特有校验 (成员资格、黑名单等)。返回 {@link Code#SUCCESS} 表示通过。
     *
     * @param sender   发送者
     * @param targetId 目标玩家id (私聊用, 其它频道为 0)
     * @param content  内容(已通过字数/频率校验)
     */
    default int validate(Player sender, long targetId, String content) {
        return Code.SUCCESS;
    }

    /**
     * 占用频道全局发送配额 (全服扇出型频道限制总量用)。
     * 在玩家个人频率校验通过后调用, 避免被个人限频拒绝的请求空耗全局配额。
     *
     * @return true 放行 / false 频道当前过载
     */
    default boolean tryAcquireGlobalQuota() {
        return true;
    }

    /**
     * 投递消息: 写缓存/落库 + 实时下发。消息的 id/时间/发送者信息已由上层填好。
     */
    void dispatch(ChatMessage msg);

    /**
     * 拉取历史。
     *
     * @param playerId 拉取者
     * @param targetId 私聊对端id (其它频道忽略)
     * @param cursor   分页游标 (私聊用; 全服频道忽略, 取最新 N 条)
     */
    ChatHistory loadHistory(long playerId, long targetId, String cursor);
}
