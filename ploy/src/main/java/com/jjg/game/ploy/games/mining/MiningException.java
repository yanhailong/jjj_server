package com.jjg.game.ploy.games.mining;

import com.jjg.game.core.constant.Code;

public class MiningException extends RuntimeException {
    public final int code;
    public MiningException(String reason) { this(Code.PARAM_ERROR, reason); }
    public MiningException(int code, String reason) {
        super(reason);
        this.code = code;
    }
}
