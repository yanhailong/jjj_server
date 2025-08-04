package com.jjg.game.common.utils;

public class BitUtils {

    /**
     * 读取 int 类型值的第 n 位（0 = 最低位）
     */
    public static int getBit(int value, int n) {
        return (value >>> n) & 1;
    }

    /**
     * 读取 long 类型值的第 n 位（0 = 最低位）
     */
    public static int getBit(long value, int n) {
        return (int) ((value >>> n) & 1);
    }

    /**
     * 读取 short 类型值的第 n 位（0 = 最低位）
     */
    public static int getBit(short value, int n) {
        return (value >>> n) & 1;
    }

    /**
     * 读取 byte 类型值的第 n 位（0 = 最低位）
     */
    public static int getBit(byte value, int n) {
        return (value >>> n) & 1;
    }

    /**
     * 读取 float 类型值的第 n 位（0 = 最低位）
     */
    public static int getBit(float value, int n) {
        int bits = Float.floatToIntBits(value);
        return getBit(bits, n);
    }

    /**
     * 读取 int 类型值的第 n 位（0 = 最低位）
     */
    public static int getBit(double value, int n) {
        long bits = Double.doubleToLongBits(value);
        return getBit(bits, n);
    }

    /**
     * 写入 int 类型值的第 n 位 为 1
     */
    public static int setBitTrue(int value, int n) {
        return value | (1 << n);
    }

    /**
     * 写入 int 类型值的第 n 位 为 0
     */
    public static int setBitFalse(int value, int n) {
        return (value & ~(1 << n));
    }

    /**
     * 写入 long 类型值的第 n 位 为 1
     */
    public static long setBitTrue(long value, int n) {
        return (value | (1L << n));
    }


    /**
     * 写入 long 类型值的第 n 位 为 1
     */
    public static long setBitFalse(long value, int n) {
        return (value & ~(1L << n));
    }

    /**
     * 写入 short 类型值的第 n 位 为 1
     */
    public static short setBitTrue(short value, int n) {
        int result = (value | (1 << n));
        return (short) result;
    }

    /**
     * 写入 short 类型值的第 n 位 为 1
     */
    public static short setBitFalse(short value, int n) {
        int result = (value & ~(1 << n));
        return (short) result;
    }

    /**
     * 写入 byte 类型值的第 n 位 为 1
     */
    public static byte setBitTrue(byte value, int n) {
        int result = (value | (1 << n));
        return (byte) result;
    }

    /**
     * 写入 byte 类型值的第 n 位 为 1
     */
    public static byte setBitFalse(byte value, int n) {
        int result = (value & ~(1 << n));
        return (byte) result;
    }

    /**
     * 写入 float 类型值的第 n 位 为 1
     */
    public static float setBitTrue(float value, int n) {
        int bits = Float.floatToIntBits(value);
        bits = setBitTrue(bits, n);
        return Float.intBitsToFloat(bits);
    }


    /**
     * 写入 float 类型值的第 n 位 为 1
     */
    public static float setBitFalse(float value, int n) {
        int bits = Float.floatToIntBits(value);
        bits = setBitFalse(bits, n);
        return Float.intBitsToFloat(bits);
    }

    /**
     * 写入 double 类型值的第 n 位 为 1
     */
    public static double setBitTrue(double value, int n) {
        long bits = Double.doubleToLongBits(value);
        bits = setBitTrue(bits, n);
        return Double.longBitsToDouble(bits);
    }


    /**
     * 写入 double 类型值的第 n 位 为 1
     */
    public static double setBitFalse(double value, int n) {
        long bits = Double.doubleToLongBits(value);
        bits = setBitFalse(bits, n);
        return Double.longBitsToDouble(bits);
    }
}
