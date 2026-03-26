package com.jjg.game.common.mongo;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author lm
 * @date 2026/3/23 16:32
 */
@Configuration
public class MongoAtomicBooleanConfig {

    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(List.of(
                new AtomicBooleanToBooleanConverter(),
                new BooleanToAtomicBooleanConverter()
        ));
    }

    @WritingConverter
    static class AtomicBooleanToBooleanConverter implements Converter<AtomicBoolean, Boolean> {
        @Override
        public Boolean convert(AtomicBoolean source) {
            return source.get();
        }
    }

    @ReadingConverter
    static class BooleanToAtomicBooleanConverter implements Converter<Boolean, AtomicBoolean> {
        @Override
        public AtomicBoolean convert(@NonNull Boolean source) {
            return new AtomicBoolean(source);
        }
    }
}