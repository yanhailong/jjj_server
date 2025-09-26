package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/25 17:00
 */
public record ChangeNodeDto(
    String name,
    int weight,
    //白名单ip
    List<String> ips,
    //白名单id
    List<String> ids
) {
}
