package com.vegasnight.game.common.ws;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import com.vegasnight.game.common.gate.GateSession;
import com.vegasnight.game.common.protostuff.PFMessage;

/**
 * @since 1.0
 */
public class WSGateSession extends GateSession {

    @Override
    protected void login(PFMessage msg) {
        AttributeKey<String> key = AttributeKey.valueOf("X-Real_IP");
        if (ctx.channel().hasAttr(key)) {
            Attribute<String> attribute = ctx.channel().attr(key);
            if (attribute != null && attribute.get() != null) {
                remoteAddress.setHost(attribute.get());
            }
        }
        super.login(msg);
    }
}
