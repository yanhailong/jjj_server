package com.jjg.game.alliance.service;

import com.jjg.game.alliance.data.AllianceData;
import com.jjg.game.social.service.AllianceMemberProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;

/**
 * 社交模块 {@link AllianceMemberProvider} 的真实实现。
 * <p>
 * 标记 @Primary 后, 社交侧预留的联盟聊天频道 ({@code AllianceChatChannel}) 与
 * 玩家信息卡的联盟名展示自动启用, 社交模块零改动 —— 这是该 SPI 当初预留的接入方式。
 * 全部查询走 {@link AllianceCacheService}, 不直达 Mongo。
 *
 * @author 11
 * @date 2026/6/11
 */
@Primary
@Component
public class AllianceSimMemberProvider implements AllianceMemberProvider {

    @Autowired
    private AllianceCacheService cacheService;

    @Override
    public long getAllianceId(long playerId) {
        return cacheService.getAllianceId(playerId);
    }

    @Override
    public String getAllianceName(long allianceId) {
        AllianceData alliance = cacheService.getAlliance(allianceId);
        return alliance == null ? null : alliance.getName();
    }

    @Override
    public Collection<Long> getMembers(long allianceId) {
        AllianceData alliance = cacheService.getAlliance(allianceId);
        if (alliance == null || alliance.getMembers() == null) {
            return Collections.emptyList();
        }
        return alliance.getMembers().keySet();
    }
}
