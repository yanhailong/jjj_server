package com.jjg.game.table.animals.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 飞禽走兽历史记录bean
 *
 * @author 2CL
 */
@ProtobufMessage()
@ProtoDesc("飞禽走兽历史记录bean")
public class AnimalsHistoryBean {

    @ProtoDesc("下注ID")
    public int betIdxId;

    @ProtoDesc("动物ID 1-鹦鹉 2-鸽子 5-老虎 6-耗牛 7-火烈鸟 8-老鹰 9-金鲨 10-银鲨 11-狮子 12-熊 13-通杀 14-通赔")
    public int animalId;
}
