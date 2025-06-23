
 package com.jjg.game.sample;

 import com.jjg.game.core.sample.Sample;
 import com.jjg.game.core.sample.SampleFactory;
 import com.jjg.game.core.sample.SampleFactoryImpl;
 import com.jjg.game.common.proto.ProtoDesc;
 import io.protostuff.Tag;
 import com.jjg.game.common.proto.ProtobufMessage;

/**
 * Auto generate by "Python tools"
 * @Date 2025-06-23 12:47:30
 */
 @ProtobufMessage
 public class ShopConfig extends Sample{
    public static SampleFactory<ShopConfig> factory = new SampleFactoryImpl<>();
    public static ShopConfig getShopConfig(int sid) {
        return (ShopConfig)factory.getSample(sid);
    }

    public static ShopConfig newShopConfig(int sid) {
        return (ShopConfig)factory.newSample(sid);
    }
 	@Tag(3)
	@ProtoDesc("图标")
	private String icon1;
	@Tag(4)
	@ProtoDesc("标签")
	private String icon2;
	@Tag(5)
	@ProtoDesc("道具奖励")
	private String item;
	@Tag(6)
	@ProtoDesc("购买价格")
	private int money;
	@Tag(7)
	@ProtoDesc("分类")
	private int type;
	@Tag(8)
	@ProtoDesc("天数规则")
	private int day;
	@Tag(9)
	@ProtoDesc("等级条件")
	private int level;
	@Tag(10)
	@ProtoDesc("指定日期开放")
	private String yymmdd;
	@Tag(11)
	@ProtoDesc("存在时间")
	private String duration;
	@Tag(12)
	@ProtoDesc("折扣原价")
	private String discount;
	@Tag(13)
	@ProtoDesc("折扣")
	private String discountratio;

 	public String getIcon1() {
		return this.icon1;
	}

	public void setIcon1(String icon1) {
		this.icon1 = icon1;
	}

	public String getIcon2() {
		return this.icon2;
	}

	public void setIcon2(String icon2) {
		this.icon2 = icon2;
	}

	public String getItem() {
		return this.item;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public int getMoney() {
		return this.money;
	}

	public void setMoney(int money) {
		this.money = money;
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getDay() {
		return this.day;
	}

	public void setDay(int day) {
		this.day = day;
	}

	public int getLevel() {
		return this.level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public String getYymmdd() {
		return this.yymmdd;
	}

	public void setYymmdd(String yymmdd) {
		this.yymmdd = yymmdd;
	}

	public String getDuration() {
		return this.duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getDiscount() {
		return this.discount;
	}

	public void setDiscount(String discount) {
		this.discount = discount;
	}

	public String getDiscountratio() {
		return this.discountratio;
	}

	public void setDiscountratio(String discountratio) {
		this.discountratio = discountratio;
	}


 }
