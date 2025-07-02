package com.jjg.game.core.constant;

/**
 * 游戏错误码枚举
 *
 * @author 2CL
 */
public enum EGameErrorCode {
    // 核心错误码
    CORE_SUCCESS(Code.SUCCESS, "成功"),
    CORE_FAIL(Code.FAIL, "失败"),
    CORE_ERROR_REQ(Code.ERROR_REQ, "错误的请求"),
    CORE_PARAM_ERROR(Code.PARAM_ERROR, "参数错误"),
    CORE_EXIST(Code.EXIST, "已存在"),
    CORE_NOT_FOUND(Code.NOT_FOUND, "未找到"),
    CORE_FORBID(Code.FORBID, "禁止"),
    CORE_REPEAT_OP(Code.REPEAT_OP, "重复操作"),
    CORE_EXPIRE(Code.EXPIRE, "过期"),
    CORE_NOT_ENOUGH(Code.NOT_ENOUGH, "余额不足"),
    CORE_VIP_NOT_ENOUGH(Code.VIP_NOT_ENOUGH, "Vip等级不足"),
    CORE_EXCEPTION(Code.EXCEPTION, "服务器错误"),
    ;
    final int code;
    final String errorInfo;

    EGameErrorCode(int code, String errorInfo) {
        this.code = code;
        this.errorInfo = errorInfo;
    }

    /**
     * 通过错误码获取异常枚举信息
     *
     * @param errCode 错误码
     * @return 枚举类
     */
    public static EGameErrorCode fromCode(int errCode) {
        for (EGameErrorCode value : values()) {
            if (value.code == errCode) {
                return value;
            }
        }
        throw new IllegalArgumentException("异常错误码 " + errCode);
    }

    public int getCode() {
        return code;
    }

    public String getErrorInfo() {
        return errorInfo;
    }
}
