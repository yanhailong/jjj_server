package com.jjg.game.core.data;

import com.jjg.game.core.pb.NoticeTip;
import com.jjg.game.core.utils.TipUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * NotifyNoticeBuilder类用于构建NotifyNotice对象的构建器。
 * <p style="color:red">
 * 如果多语言id对应的文本中有多个参数 ,参数应该按照顺序{@link NoticeTipBuilder#addArg(int, String)}}
 */
public class NoticeTipBuilder {

    /**
     * 弹窗类型
     * <p>
     * 类型常量定义{@link TipUtils.TipType}
     */
    private int tipType;

    /**
     * 多语言id
     */
    private long languageId;

    /**
     * 参数
     */
    private final List<NoticeTip.TipArgs> tipArgs;

    public NoticeTipBuilder() {
        this.tipArgs = new ArrayList<>();
    }

    /**
     * 设置弹窗类型
     *
     * @param tipType 弹窗类型 {@link TipUtils.TipType}
     * @return 构建器实例
     */
    public NoticeTipBuilder tipType(int tipType) {
        this.tipType = tipType;
        return this;
    }

    /**
     * 设置多语言id
     *
     * @param languageId 多语言id
     * @return 构建器实例
     */
    public NoticeTipBuilder languageId(long languageId) {
        this.languageId = languageId;
        return this;
    }

    /**
     * 添加多语言id参数
     *
     * @param languageId 多语言id
     * @return 构建器实例
     */
    public NoticeTipBuilder addLanguageIdArg(long languageId) {
        NoticeTip.TipArgs arg = new NoticeTip.TipArgs();
        arg.setType(TipUtils.TipContextArgsType.LANGUAGE_ID);
        arg.setArg(String.valueOf(languageId));
        this.tipArgs.add(arg);
        return this;
    }

    /**
     * 添加自定义参数
     *
     * @param type 参数类型 {@link TipUtils.TipContextArgsType}
     * @param arg  参数值
     * @return 构建器实例
     */
    public NoticeTipBuilder addArg(int type, String arg) {
        NoticeTip.TipArgs tipArg = new NoticeTip.TipArgs();
        tipArg.setType(type);
        tipArg.setArg(arg);
        this.tipArgs.add(tipArg);
        return this;
    }

    /**
     * 构建NotifyNotice对象
     *
     * @return 构建完成的NotifyNotice对象
     */
    public NoticeTip build() {
        NoticeTip noticeTip = new NoticeTip();
        noticeTip.setTipType(this.tipType);
        noticeTip.setLanguageId(this.languageId);
        noticeTip.setTipArgs(this.tipArgs);
        return noticeTip;
    }

    /**
     * 创建构建器实例
     *
     * @return 构建器实例
     */
    public static NoticeTipBuilder builder() {
        return new NoticeTipBuilder();
    }

}
