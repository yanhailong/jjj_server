package com.jjg.game.slots.utils;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.lz4.LZ4SafeDecompressor;

/**
 * @author 11
 * @date 2025/11/17 17:39
 */
public class LZ4CompressionUtil {
    // 使用最快的LZ4实例
    private static final LZ4Factory lz4Factory = LZ4Factory.fastestInstance();

    // 快速压缩器（速度优先）
    private static final LZ4Compressor fastCompressor = lz4Factory.fastCompressor();

    // 高压缩率压缩器（压缩率优先）
    private static final LZ4Compressor highCompressor = lz4Factory.highCompressor();

    // 快速解压器
    private static final LZ4FastDecompressor fastDecompressor = lz4Factory.fastDecompressor();

    // 安全解压器（处理损坏数据）
    private static final LZ4SafeDecompressor safeDecompressor = lz4Factory.safeDecompressor();

    /**
     * 快速压缩（速度优先）
     */
    public static byte[] compressFast(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        int maxCompressedLength = fastCompressor.maxCompressedLength(data.length);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = fastCompressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);

        // 返回实际压缩后的数据
        byte[] result = new byte[compressedLength];
        System.arraycopy(compressed, 0, result, 0, compressedLength);
        return result;
    }

    /**
     * 高压缩率压缩（压缩率优先）
     */
    public static byte[] compressHigh(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }

        int maxCompressedLength = highCompressor.maxCompressedLength(data.length);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength = highCompressor.compress(data, 0, data.length, compressed, 0, maxCompressedLength);

        byte[] result = new byte[compressedLength];
        System.arraycopy(compressed, 0, result, 0, compressedLength);
        return result;
    }

    /**
     * 快速解压（需要知道原始数据长度）
     */
    public static byte[] decompressFast(byte[] compressedData, int originalLength) {
        if (compressedData == null || compressedData.length == 0) {
            return compressedData;
        }

        byte[] restored = new byte[originalLength];
        fastDecompressor.decompress(compressedData, 0, restored, 0, originalLength);
        return restored;
    }

    /**
     * 安全解压（不需要知道原始数据长度）
     */
    public static byte[] decompressSafe(byte[] compressedData) {
        if (compressedData == null || compressedData.length == 0) {
            return compressedData;
        }

        // 预估解压后的大小（通常是压缩数据的2-5倍）
        int estimatedSize = compressedData.length * 5;
        byte[] buffer = new byte[estimatedSize];

        int decompressedLength = safeDecompressor.decompress(compressedData, 0, compressedData.length,
                buffer, 0, estimatedSize);

        // 返回实际解压的数据
        byte[] result = new byte[decompressedLength];
        System.arraycopy(buffer, 0, result, 0, decompressedLength);
        return result;
    }

    /**
     * 带数据校验的安全解压
     */
    public static byte[] decompressWithValidation(byte[] compressedData, int originalLength) {
        try {
            // 尝试快速解压
            return decompressFast(compressedData, originalLength);
        } catch (Exception e) {
            // 快速解压失败，使用安全解压
            byte[] result = decompressSafe(compressedData);
            if (result.length != originalLength) {
                throw new IllegalStateException("解压后数据长度验证失败", e);
            }
            return result;
        }
    }
}

