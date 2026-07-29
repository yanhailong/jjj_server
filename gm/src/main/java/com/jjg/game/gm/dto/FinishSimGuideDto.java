package com.jjg.game.gm.dto;

/**
 * 后台完成模拟经营新手引导请求。
 *
 * @param playerId 玩家ID
 * @param operationType 操作类型：1=完成全部引导，2=完成指定引导
 * @param guideIds 指定引导ID，多个ID使用英文逗号分隔；完成全部时可不传
 */
public record FinishSimGuideDto(
        long playerId,
        int operationType,
        String guideIds
) {
}