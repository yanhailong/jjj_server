package com.jjg.game.gm.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * @author 11
 * @date 2025/8/6 13:47
 */
public record MailDto(
//        @Positive(message = "0. 指定邮件  1.全服邮件")
        int type,
        List<Long> playerIds,
        //发送时间
        String sendTime,
        @NotBlank(message = "邮件标题不能为空")
        String title,
        @NotBlank(message = "邮件内容不能为空")
        String content,
        //道具列表
        List<long[]> items
) {
}
