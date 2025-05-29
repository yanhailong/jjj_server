package com.vegasnight.game.common.proto;


import io.protostuff.Input;
import io.protostuff.Output;
import io.protostuff.Pipe;
import io.protostuff.WireFormat;
import io.protostuff.runtime.Delegate;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @since 1.0
 */
public class AtomicLongDelegate implements Delegate<AtomicLong> {
    public WireFormat.FieldType getFieldType() {
        return WireFormat.FieldType.INT64;
    }

    public Class<?> typeClass() {
        return AtomicLong.class;
    }

    public AtomicLong readFrom(Input input) throws IOException {
        return new AtomicLong(input.readInt64());
    }

    public void writeTo(Output output, int number, AtomicLong value,
                        boolean repeated) throws IOException {
        output.writeInt64(number, value.longValue(), repeated);
    }

    public void transfer(Pipe pipe, Input input, Output output, int number,
                         boolean repeated) throws IOException {
        output.writeInt64(number, input.readInt64(), repeated);
    }
}
