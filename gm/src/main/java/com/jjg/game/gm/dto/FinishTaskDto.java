package com.jjg.game.gm.dto;

/** 后台完成通用任务请求。 */
public record FinishTaskDto(long playerId, int operationType, String taskIds) {
}
