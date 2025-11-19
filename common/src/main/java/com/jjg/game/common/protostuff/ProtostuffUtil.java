package com.jjg.game.common.protostuff;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 序列化工具类（基于 Protostuff 实现）
 *
 * @since 1.0.0
 */
public final class ProtostuffUtil {
    private static final Logger log = LoggerFactory.getLogger(ProtostuffUtil.class);
    private static Map<Class<?>, Schema<?>> cachedSchema = new ConcurrentHashMap<>();

    private static Objenesis objenesis = new ObjenesisStd(true);
    private static final ThreadLocal<LinkedBuffer> LOCAL_BUFFER =
            ThreadLocal.withInitial(() -> LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));

    private static final boolean USE_COMPRESSION = true;
    private static final int COMPRESSION_LEVEL = Deflater.BEST_SPEED;

    private static LinkedBuffer getBuffer() {
        return LOCAL_BUFFER.get().clear();
    }

    @SuppressWarnings("unchecked")
    private static <T> Schema<T> getSchema(Class<T> cls) {
        return (Schema<T>) cachedSchema.computeIfAbsent(cls, RuntimeSchema::createFrom);
    }

    /**
     * 序列化（对象 -> 字节数组）
     */
    @SuppressWarnings("unchecked")
    public static <T> byte[] serialize(T obj) {
        Class<T> cls = (Class<T>) obj.getClass();
        LinkedBuffer buffer = getBuffer();
        try {
            Schema<T> schema = getSchema(cls);
            return ProtobufIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        } finally {
            buffer.clear();
        }
    }

    /**
     * 反序列化（字节数组 -> 对象）
     */
    public static <T> T deserialize(byte[] data, Class<T> cls) {
        try {
            T message = objenesis.newInstance(cls);
            Schema<T> schema = getSchema(cls);
            ProtobufIOUtil.mergeFrom(data, message, schema);
            return message;
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }


    /**
     * 序列化并压缩
     */
    public static <T> byte[] serializeWithCompression(T obj) {
        byte[] serialized = serialize(obj);
        return compress(serialized);
    }

    /**
     * 解压并反序列化
     */
    public static <T> T deserializeWithCompression(byte[] compressedData, Class<T> cls) {
        byte[] data = decompress(compressedData);
        return deserialize(data, cls);
    }

    private static byte[] compress(byte[] data) {
        if (!USE_COMPRESSION) return data;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzipOS = new GZIPOutputStream(baos) {
                 { def.setLevel(COMPRESSION_LEVEL); }
             }) {
            gzipOS.write(data);
            gzipOS.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("压缩失败", e);
        }
    }

    private static byte[] decompress(byte[] compressedData) {
        if (!USE_COMPRESSION) return compressedData;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipIS = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzipIS.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("解压失败", e);
        }
    }


}
