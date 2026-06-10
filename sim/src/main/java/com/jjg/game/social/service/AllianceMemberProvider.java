package com.jjg.game.social.service;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * 联盟成员解析 SPI。
 * <p>
 * 项目当前无联盟系统, 提供默认空实现 {@link DefaultAllianceMemberProvider} (联盟聊天因此返回"暂无联盟")。
 * 联盟系统就绪后实现本接口并标记 {@code @Primary}, {@code AllianceChatChannel} 与玩家信息卡的联盟名即可自动启用。
 *
 * @author 11
 * @date 2026/6/9
 */
public interface AllianceMemberProvider {

    /**
     * 玩家所在联盟id, 0 表示无联盟
     */
    long getAllianceId(long playerId);

    /**
     * 联盟名称, 无则返回 null
     */
    String getAllianceName(long allianceId);

    /**
     * 联盟成员id (用于联盟聊天广播), 无则返回空集合
     */
    Collection<Long> getMembers(long allianceId);

    /**
     * 默认实现: 无联盟。
     */
    @Component
    class DefaultAllianceMemberProvider implements AllianceMemberProvider {
        @Override
        public long getAllianceId(long playerId) {
            return 0;
        }

        @Override
        public String getAllianceName(long allianceId) {
            return null;
        }

        @Override
        public Collection<Long> getMembers(long allianceId) {
            return Collections.emptyList();
        }
    }
}
