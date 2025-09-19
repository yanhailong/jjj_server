package com.jjg.game.core.pb.gm;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 通知轮播数据更新
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.TO_SERVER_CONST_TYPE, cmd = MessageConst.ToServer.NOTICE_ALL_UPDATE_CAROUSEL, resp = true,toPbFile = false)
@ProtoDesc("通知轮播数据更新")
public class NotifyCarouselUpdate extends AbstractNotice {

    /**
     * 变化的轮播数据
     */
    @ProtoDesc("变化的轮播数据")
    private List<CarouselUpdateInfo> carousel = new ArrayList<>();

    public NotifyCarouselUpdate() {
    }

    public List<CarouselUpdateInfo> getCarousel() {
        return carousel;
    }

    public void setCarousel(List<CarouselUpdateInfo> carousel) {
        this.carousel = carousel;
    }

    @Override
    public String toString() {
        return "NotifyCarouselUpdate{" +
                "carousel=" + carousel +
                '}';
    }
}
