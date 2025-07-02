package com.jjg.game.core.exception;

/**
 * 游戏配置表相关异常
 *
 * @author 2CL
 */
public class GameSampleException extends RuntimeException {

    public GameSampleException(String message) {
        super(message);
    }

    public GameSampleException(Exception e) {
        super(e);
    }

    @Override
    public String getMessage() {
        return "【游戏配置表异常】" + super.getMessage();
    }
}
