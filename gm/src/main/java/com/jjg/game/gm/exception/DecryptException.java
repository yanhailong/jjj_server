package com.jjg.game.gm.exception;

/**
 * @author lm
 * @date 2025/7/11 11:35
 */
public class DecryptException extends RuntimeException {
    public DecryptException(String message, Exception e) {
        super(message, e);
    }

    public DecryptException() {
    }
}
