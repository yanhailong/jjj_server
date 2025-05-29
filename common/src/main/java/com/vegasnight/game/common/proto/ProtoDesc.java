package com.vegasnight.game.common.proto;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.FIELD)
public @interface ProtoDesc {
    String value();
}
