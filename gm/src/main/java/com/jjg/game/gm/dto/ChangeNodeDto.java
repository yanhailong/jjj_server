package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/25 17:00
 */
public record ChangeNodeDto(
    //节点名
    String name,
    //权重
    int weight,
    //白名单ip
    List<String> whiteIpList,
    //白名单id
    List<String> whiteIdList
) {
}
