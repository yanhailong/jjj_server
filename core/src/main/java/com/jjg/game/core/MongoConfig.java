package com.jjg.game.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 11
 * @date 2025/8/2 14:45
 */
@Configuration
public class MongoConfig {
    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
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
        public AtomicBoolean convert(Boolean source) {
            return new AtomicBoolean(source);
        }
    }

    @WritingConverter
    static class AtomicIntegerToIntegerConverter implements Converter<AtomicInteger, Integer> {
        @Override
        public Integer convert(AtomicInteger source) {
            return source.get();
        }
    }

    @ReadingConverter
    static class IntegerToAtomicIntegerConverter implements Converter<Integer, AtomicInteger> {
        @Override
        public AtomicInteger convert(Integer source) {
            return new AtomicInteger(source);
        }
    }
}
