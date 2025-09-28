package com.jjg.game.gm.dto;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/25 16:59
 */
public record BlackListDto(
        int type,  //0.添加    1.删除
        //黑名单ip
        List<String> ips,
        //黑名单id
        List<Long> ids
){
}
