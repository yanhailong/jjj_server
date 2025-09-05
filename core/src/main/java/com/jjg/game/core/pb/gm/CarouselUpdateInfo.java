package com.jjg.game.core.pb.gm;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.data.Carousel;

/**
 * 轮播数据变化
 */
@ProtobufMessage
@ProtoDesc("轮播数据变化")
public class CarouselUpdateInfo {

    /**
     * 变化类型
     */
    @ProtoDesc("变化类型")
    private CarouselUpdateType type;

    /**
     * 最新的数据
     */
    @ProtoDesc("最新的数据,只有新增和更新会有数据,删除只有id")
    private Carousel carousel;

    public CarouselUpdateInfo() {
    }

    public CarouselUpdateInfo(CarouselUpdateType type, Carousel carousel) {
        this.type = type;
        this.carousel = carousel;
    }

    /**
     * 轮播数据变化类型
     */
    @ProtobufMessage
    @ProtoDesc("轮播数据变化类型")
    public enum CarouselUpdateType {

        @ProtoDesc("更新")
        UPDATE(1, "更新"),

        @ProtoDesc("删除")
        DELETE(2, "删除"),
        ;
        private final int code;

        private final String desc;

        CarouselUpdateType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }

    public CarouselUpdateType getType() {
        return type;
    }

    public void setType(CarouselUpdateType type) {
        this.type = type;
    }

    public Carousel getCarousel() {
        return carousel;
    }

    public void setCarousel(Carousel carousel) {
        this.carousel = carousel;
    }

    @Override
    public String toString() {
        return "CarouselUpdateInfo{" +
                "type=" + type +
                ", carousel=" + carousel +
                '}';
    }
}
