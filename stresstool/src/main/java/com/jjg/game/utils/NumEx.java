package com.jjg.game.utils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

public class NumEx {

  public static boolean stringToBool(String s, boolean v) {
    try {
      return Boolean.parseBoolean(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static boolean stringToBool(String s) {
    return stringToBool(s, false);
  }

  public static byte stringToByte(String s, byte v) {
    try {
      return Byte.parseByte(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static byte stringToByte(String s) {
    return stringToByte(s, (byte) 0);
  }

  public static byte[] stringToByte(String[] v) {
    byte[] r = new byte[v.length];
    int n = 0;
    for (String s : v) {
      r[n] = stringToByte(s);
      n++;
    }
    return r;
  }

  public static short stringToShort(String s, short v) {
    try {
      return Short.parseShort(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static short stringToShort(String s) {
    return stringToShort(s, (short) 0);
  }

  public static short[] stringToShort(String[] v) {
    short[] r = new short[v.length];
    int n = 0;
    for (String s : v) {
      r[n] = stringToShort(s);
      n++;
    }
    return r;
  }

  public static int stringToInt(String s, int v) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static int stringToInt(String s) {
    return stringToInt(s, 0);
  }

  public static int[] stringToInt(String[] v) {
    int[] r = new int[v.length];
    int n = 0;
    for (String s : v) {
      r[n] = stringToInt(s);
      n++;
    }
    return r;
  }

  public static long stringToLong(String s, long v) {
    try {
      return Long.parseLong(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static long stringToLong(String s) {
    return stringToLong(s, 0);
  }

  public static long[] stringToLong(String[] v) {
    long[] r = new long[v.length];
    int n = 0;
    for (String s : v) {
      r[n] = stringToLong(s);
      n++;
    }
    return r;
  }

  public static float stringToFloat(String s, float v) {
    try {
      return Float.parseFloat(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static float stringToFloat(String s) {
    return stringToFloat(s, (float) 0.0);
  }

  public static float[] stringToFloat(String[] v) {
    float[] r = new float[v.length];
    int n = 0;
    for (String s : v) {
      r[n] = stringToFloat(s);
      n++;
    }
    return r;
  }

  public static double stringToDouble(String s, double v) {
    try {
      return Double.parseDouble(s);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return v;
    }
  }

  public static double stringToDouble(String s) {
    return stringToDouble(s, 0.0);
  }

  public static double[] stringToDouble(String[] v) {
    double[] r = new double[v.length];
    int n = 0;
    for (String s : v) {
      r[n] = stringToDouble(s);
      n++;
    }
    return r;
  }

  private static final int read(InputStream input) throws IOException {
    int value = input.read();
    if (-1 == value) {
      throw new EOFException("Unexpected EOF reached");
    }
    return value;
  }

  public static void writeBytes(byte[] data, int offset, byte[] value) {
    System.arraycopy(value, 0, data, offset, value.length);
  }

  public static void writeBytes(byte[] data, Offset offset, byte[] value) {
    try {
      writeBytes(data, offset, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += value.length;
    }
  }

  public static byte[] readBytes(byte[] data, int offset, int len) {
    byte[] result = new byte[len];
    System.arraycopy(data, offset, result, 0, len);
    return result;
  }

  public static byte[] readBytes(byte[] data, Offset offset, int len) {
    try {
      return readBytes(data, offset.reader, len);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += len;
    }
    return new byte[0];
  }

  public static void writeUtf8(byte[] data, int offset, String value) {
    byte[] b = value.getBytes(StandardCharsets.UTF_8);
    writeInt(data, offset, b.length);
    writeBytes(data, offset + 4, b);
  }

  public static void writeUtf8(byte[] data, Offset offset, String value) {
    try {
      byte[] b = value.getBytes(StandardCharsets.UTF_8);
      writeInt(data, offset, b.length);
      writeBytes(data, offset, b);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
  }

  public static String readUtf8(byte[] data, int offset) {
    int len = readInt(data, offset);
    byte[] result = new byte[len];
    System.arraycopy(data, offset + 4, result, 0, len);
    return new String(result, StandardCharsets.UTF_8);
  }

  public static String readUtf8(byte[] data, Offset offset) {
    int len = 0;
    try {
      len = readInt(data, offset);
      byte[] result = new byte[len];
      System.arraycopy(data, offset.reader, result, 0, len);
      return new String(result, StandardCharsets.UTF_8);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += len;
    }
    return "";
  }

  public static void writeBool(byte[] data, int offset, boolean value) {
    data[offset] = (byte) (value ? 0x01 : 0x00);
  }

  public static void writeBool(byte[] data, Offset offset, boolean value) {
    try {
      writeBool(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 1;
    }
  }

  public static boolean readBool(byte[] data, int offset) {
    return (data[offset] & 0xff) == 0x01;
  }

  public static boolean readBool(byte[] data, Offset offset) {
    try {
      return readBool(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 1;
    }
    return false;
  }

  public static void write(byte[] data, int offset, boolean value) {
    writeBool(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, boolean value) {
    writeBool(data, offset, value);
  }

  public static void writeByte(byte[] data, int offset, int value) {
    data[offset] = (byte) value;
  }

  public static void writeByte(byte[] data, Offset offset, int value) {
    try {
      writeByte(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 1;
    }
  }

  public static byte readByte(byte[] data, int offset) {
    return (byte) ((data[offset] & 0xff));
  }

  public static byte readByte(byte[] data, Offset offset) {
    try {
      return readByte(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 1;
    }
    return 0;
  }

  public static void write(byte[] data, int offset, byte value) {
    writeByte(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, byte value) {
    writeByte(data, offset, value);
  }

  public static void writeShort(byte[] data, int offset, short value) {
    data[offset] = (byte) ((value >> 8) & 0xff);
    data[offset + 1] = (byte) ((value) & 0xff);
  }

  public static void writeShort(byte[] data, Offset offset, short value) {
    try {
      writeShort(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 2;
    }
  }

  public static void write(byte[] data, int offset, short value) {
    writeShort(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, short value) {
    writeShort(data, offset, value);
  }

  public static short readShort(byte[] data, int offset) {
    return (short) (((data[offset] & 0xff) << 8) + ((data[offset + 1] & 0xff)));
  }

  public static short readShort(byte[] data, Offset offset) {
    try {
      return readShort(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 2;
    }
    return 0;
  }

  public static int readUnsignedShort(byte[] data, int offset) {
    return (((data[offset] & 0xff) << 8) + ((data[offset + 1] & 0xff)));
  }

  public static int readUnsignedShort(byte[] data, Offset offset) {
    try {
      return readUnsignedShort(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 2;
    }
    return 0;
  }

  public static void writeUnsignedShort(byte[] data, int offset, int value) {
    data[offset] = (byte) ((value >> 24) & 0xff);
    data[offset + 1] = (byte) ((value >> 16) & 0xff);
  }

  public static void writeUnsignedShort(byte[] data, Offset offset, int value) {
    try {
      writeUnsignedShort(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 2;
    }
  }

  public static void writeInt(byte[] data, int offset, int value) {
    writeUnsignedShort(data,offset,value);
    data[offset + 2] = (byte) ((value >> 8) & 0xff);
    data[offset + 3] = (byte) ((value) & 0xff);
  }

  public static void writeInt(byte[] data, Offset offset, int value) {
    try {
      writeInt(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 4;
    }
  }

  public static void write(byte[] data, int offset, int value) {
    writeInt(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, int value) {
    writeInt(data, offset, value);
  }

  public static int readInt(byte[] data, int offset) {
    return (((data[offset] & 0xff) << 24)
        + ((data[offset + 1] & 0xff) << 16)
        + ((data[offset + 2] & 0xff) << 8)
        + ((data[offset + 3] & 0xff)));
  }

  public static int readInt(byte[] data, Offset offset) {
    try {
      return readInt(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 4;
    }
    return 0;
  }

  public static void writeLong(byte[] data, int offset, long value) {
    data[offset] = (byte) ((value >> 56) & 0xff);
    data[offset + 1] = (byte) ((value >> 48) & 0xff);
    data[offset + 2] = (byte) ((value >> 40) & 0xff);
    data[offset + 3] = (byte) ((value >> 32) & 0xff);
    data[offset + 4] = (byte) ((value >> 24) & 0xff);
    data[offset + 5] = (byte) ((value >> 16) & 0xff);
    data[offset + 6] = (byte) ((value >> 8) & 0xff);
    data[offset + 7] = (byte) ((value) & 0xff);
  }

  public static void writeLong(byte[] data, Offset offset, long value) {
    try {
      writeLong(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 8;
    }
  }

  public static void write(byte[] data, int offset, long value) {
    writeLong(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, long value) {
    writeLong(data, offset, value);
  }

  public static long readLong(byte[] data, int offset) {
    long high =
        ((long) (data[offset] & 0xff) << 24)
            + ((data[offset + 1] & 0xff) << 16)
            + ((data[offset + 2] & 0xff) << 8)
            + ((data[offset + 3] & 0xff));
    long low =
        ((long) (data[offset + 4] & 0xff) << 24)
            + ((data[offset + 5] & 0xff) << 16)
            + ((data[offset + 6] & 0xff) << 8)
            + ((data[offset + 7] & 0xff));
    return (high << 32) + (0xffffffffL & low);
  }

  public static long readLong(byte[] data, Offset offset) {
    try {
      return readLong(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 8;
    }
    return 0;
  }

  public static void writeFloat(byte[] data, int offset, float value) {
    writeInt(data, offset, Float.floatToIntBits(value));
  }

  public static void writeFloat(byte[] data, Offset offset, float value) {
    try {
      writeFloat(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 4;
    }
  }

  public static void write(byte[] data, int offset, float value) {
    writeFloat(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, float value) {
    writeFloat(data, offset, value);
  }

  public static float readFloat(byte[] data, int offset) {
    return Float.intBitsToFloat(readInt(data, offset));
  }

  public static float readFloat(byte[] data, Offset offset) {
    try {
      return readFloat(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 4;
    }
    return 0;
  }

  public static void writeDouble(byte[] data, int offset, double value) {
    writeLong(data, offset, Double.doubleToLongBits(value));
  }

  public static void writeDouble(byte[] data, Offset offset, double value) {
    try {
      writeDouble(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 8;
    }
  }

  public static void write(byte[] data, int offset, double value) {
    writeDouble(data, offset, value);
  }

  public static void write(byte[] data, Offset offset, double value) {
    writeDouble(data, offset, value);
  }

  public static double readDouble(byte[] data, int offset) {
    return Double.longBitsToDouble(readLong(data, offset));
  }

  public static double readDouble(byte[] data, Offset offset) {
    return Double.longBitsToDouble(readLong(data, offset));
  }

  public static void writeBytes(OutputStream output, byte[] value) throws IOException {
    output.write(value);
  }

  public static int readBytes(InputStream input, int len) throws IOException {
    byte[] b = new byte[len];
    return input.read(b);
  }

  public static void writeUtf8(OutputStream output, String value) throws IOException {
    byte[] b = value.getBytes(StandardCharsets.UTF_8);
    writeInt(output, b.length);
    output.write(b);
  }

  public static String readUtf8(InputStream input) throws IOException {
    int len = readInt(input);
    byte[] b = new byte[len];
    return new String(b, StandardCharsets.UTF_8);
  }

  public static void writeBool(OutputStream output, boolean value) throws IOException {
    output.write((byte) (value ? 1 : 0));
  }

  public static boolean readBool(InputStream input) throws IOException {
    return read(input) == 1;
  }

  public static void writeByte(OutputStream output, int value) throws IOException {
    output.write((byte) (value) & 0xff);
  }

  public static int readByte(InputStream input) throws IOException {
    return read(input);
  }

  public static void writeShort(OutputStream output, short value) throws IOException {
    output.write((byte) ((value >> 8) & 0xff));
    output.write((byte) ((value) & 0xff));
  }

  public static short readShort(InputStream input) throws IOException {
    return (short) (((read(input) & 0xff) << 8) + ((read(input) & 0xff)));
  }

  public static void writeInt(OutputStream output, int value) throws IOException {
    output.write((byte) ((value >> 24) & 0xff));
    output.write((byte) ((value >> 16) & 0xff));
    output.write((byte) ((value >> 8) & 0xff));
    output.write((byte) ((value) & 0xff));
  }

  public static int readInt(InputStream input) throws IOException {
    int value1 = read(input);
    int value2 = read(input);
    int value3 = read(input);
    int value4 = read(input);

    return ((value1 & 0xff) << 24)
        + ((value2 & 0xff) << 16)
        + ((value3 & 0xff) << 8)
        + ((value4 & 0xff));
  }

  public static void writeLong(OutputStream output, long value) throws IOException {
    output.write((byte) ((value >> 56) & 0xff));
    output.write((byte) ((value >> 48) & 0xff));
    output.write((byte) ((value >> 40) & 0xff));
    output.write((byte) ((value >> 32) & 0xff));
    output.write((byte) ((value >> 24) & 0xff));
    output.write((byte) ((value >> 16) & 0xff));
    output.write((byte) ((value >> 8) & 0xff));
    output.write((byte) ((value) & 0xff));
  }

  public static long readLong(InputStream input) throws IOException {
    byte[] bytes = new byte[8];
    for (int i = 0; i < 8; i++) {
      bytes[i] = (byte) read(input);
    }
    return readLong(bytes, 0);
  }

  public static void writeFloat(OutputStream output, float value) throws IOException {
    writeInt(output, Float.floatToIntBits(value));
  }

  public static float readFloat(InputStream input) throws IOException {
    return Float.intBitsToFloat(readInt(input));
  }

  public static void writeDouble(OutputStream output, double value) throws IOException {
    writeLong(output, Double.doubleToLongBits(value));
  }

  public static double readDouble(InputStream input) throws IOException {
    return Double.longBitsToDouble(readLong(input));
  }

  public static int readUnsignedShort(InputStream input) throws IOException {
    int value1 = read(input);
    int value2 = read(input);

    return (((value1 & 0xff) << 8) + ((value2 & 0xff)));
  }

  public static void writeUnsignedShort(OutputStream os, int value) throws IOException {
    byte[] data = new byte[2];
    data[0] = (byte) ((value >> 8) & 0xff);
    data[1] = (byte) ((value) & 0xff);
    os.write(data);
  }

  public static boolean isByte(String s) {
    try {
      Byte.parseByte(s);
      return true;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  public static boolean isShort(String s) {
    try {
      Short.parseShort(s);
      return true;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  public static boolean isInt(String s) {
    try {
      Integer.parseInt(s);
      return true;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  public static boolean isLong(String s) {
    try {
      Long.parseLong(s);
      return true;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  public static boolean isFloat(String s) {
    try {
      Float.parseFloat(s);
      return true;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  public static boolean isDouble(String s) {
    try {
      Double.parseDouble(s);
      return true;
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
      return false;
    }
  }

  static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat(".00");

  public static String formatDouble(double s) {
    return DECIMAL_FORMAT.format(s);
  }

  public static String fix6Int(int v) {
    return fixFillInt(v, 6);
  }

  public static String fixFillInt(int v, int n) {
    return String.format("%0" + n + "d", v);
  }

  public static String fix3Int(int v) {
    return fixFillInt(v, 3);
  }

  public static short swapShort(short value) {
    return (short) ((((value) & 0xff) << 8) + (((value >> 8) & 0xff)));
  }

  public static int swapInteger(int value) {
    return (((value) & 0xff) << 24)
        + (((value >> 8) & 0xff) << 16)
        + (((value >> 16) & 0xff) << 8)
        + (((value >> 24) & 0xff));
  }

  public static long swapLong(long value) {
    return (((value) & 0xff) << 56)
        + (((value >> 8) & 0xff) << 48)
        + (((value >> 16) & 0xff) << 40)
        + (((value >> 24) & 0xff) << 32)
        + (((value >> 32) & 0xff) << 24)
        + (((value >> 40) & 0xff) << 16)
        + (((value >> 48) & 0xff) << 8)
        + (((value >> 56) & 0xff));
  }

  public static float swapFloat(float value) {
    return Float.intBitsToFloat(swapInteger(Float.floatToIntBits(value)));
  }

  public static double swapDouble(double value) {
    return Double.longBitsToDouble(swapLong(Double.doubleToLongBits(value)));
  }

  public static void writeSwappedShort(byte[] data, int offset, short value) {
    data[offset] = (byte) ((value) & 0xff);
    data[offset + 1] = (byte) ((value >> 8) & 0xff);
  }

  public static void writeSwappedShort(byte[] data, Offset offset, short value) {
    try {
      writeSwappedShort(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 2;
    }
  }

  public static void writeSwapped(byte[] data, int offset, short value) {
    writeSwappedShort(data, offset, value);
  }

  public static void writeSwapped(byte[] data, Offset offset, short value) {
    writeSwappedShort(data, offset, value);
  }

  public static short readSwappedShort(byte[] data, int offset) {
    return (short) (((data[offset + 0] & 0xff)) + ((data[offset + 1] & 0xff) << 8));
  }

  public static short readSwappedShort(byte[] data, Offset offset) {
    try {
      return readSwappedShort(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 2;
    }
    return 0;
  }

  public static int readSwappedUnsignedShort(byte[] data, int offset) {
    return (((data[offset] & 0xff)) + ((data[offset + 1] & 0xff) << 8));
  }

  public static int readSwappedUnsignedShort(byte[] data, Offset offset) {
    try {
      return readSwappedUnsignedShort(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 2;
    }
    return 0;
  }

  public static void writeSwappedInteger(byte[] data, int offset, int value) {
    data[offset] = (byte) ((value) & 0xff);
    data[offset + 1] = (byte) ((value >> 8) & 0xff);
    data[offset + 2] = (byte) ((value >> 16) & 0xff);
    data[offset + 3] = (byte) ((value >> 24) & 0xff);
  }

  public static void writeSwappedInteger(byte[] data, Offset offset, int value) {
    try {
      writeSwappedInteger(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 4;
    }
  }

  public static void writeSwapped(byte[] data, int offset, int value) {
    writeSwappedInteger(data, offset, value);
  }

  public static void writeSwapped(byte[] data, Offset offset, int value) {
    writeSwappedInteger(data, offset, value);
  }

  public static int readSwappedInteger(byte[] data, int offset) {
    return (((data[offset] & 0xff))
        + ((data[offset + 1] & 0xff) << 8)
        + ((data[offset + 2] & 0xff) << 16)
        + ((data[offset + 3] & 0xff) << 24));
  }

  public static int readSwappedInteger(byte[] data, Offset offset) {
    try {
      return readSwappedInteger(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 4;
    }
    return 0;
  }

  public static long readSwappedUnsignedInteger(byte[] data, int offset) {
    long low =
        (((data[offset] & 0xff))
            + ((data[offset + 1] & 0xff) << 8)
            + ((data[offset + 2] & 0xff) << 16));
    long high = data[offset + 3] & 0xff;
    return (high << 24) + (0xffffffffL & low);
  }

  public static long readSwappedUnsignedInteger(byte[] data, Offset offset) {
    try {
      return readSwappedUnsignedInteger(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 8;
    }
    return 0;
  }

  public static void writeSwappedLong(byte[] data, int offset, long value) {
    data[offset] = (byte) ((value) & 0xff);
    data[offset + 1] = (byte) ((value >> 8) & 0xff);
    data[offset + 2] = (byte) ((value >> 16) & 0xff);
    data[offset + 3] = (byte) ((value >> 24) & 0xff);
    data[offset + 4] = (byte) ((value >> 32) & 0xff);
    data[offset + 5] = (byte) ((value >> 40) & 0xff);
    data[offset + 6] = (byte) ((value >> 48) & 0xff);
    data[offset + 7] = (byte) ((value >> 56) & 0xff);
  }

  public static void writeSwappedLong(byte[] data, Offset offset, long value) {
    try {
      writeSwappedLong(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 8;
    }
  }

  public static void writeSwapped(byte[] data, int offset, long value) {
    writeSwappedLong(data, offset, value);
  }

  public static void writeSwapped(byte[] data, Offset offset, long value) {
    writeSwappedLong(data, offset, value);
  }

  public static long readSwappedLong(byte[] data, int offset) {
    long low =
        ((data[offset] & 0xff))
            + ((data[offset + 1] & 0xff) << 8)
            + ((data[offset + 2] & 0xff) << 16)
            + ((long) (data[offset + 3] & 0xff) << 24);
    long high =
        ((data[offset + 4] & 0xff))
            + ((data[offset + 5] & 0xff) << 8)
            + ((data[offset + 6] & 0xff) << 16)
            + ((long) (data[offset + 7] & 0xff) << 24);
    return (high << 32) + (0xffffffffL & low);
  }

  public static long readSwappedLong(byte[] data, Offset offset) {
    try {
      return readSwappedLong(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 8;
    }
    return 0;
  }

  public static void writeSwappedFloat(byte[] data, int offset, float value) {
    writeSwappedInteger(data, offset, Float.floatToIntBits(value));
  }

  public static void writeSwappedFloat(byte[] data, Offset offset, float value) {
    try {
      writeSwappedFloat(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 4;
    }
  }

  public static void writeSwapped(byte[] data, int offset, float value) {
    writeSwappedFloat(data, offset, value);
  }

  public static void writeSwapped(byte[] data, Offset offset, float value) {
    writeSwappedFloat(data, offset, value);
  }

  public static float readSwappedFloat(byte[] data, int offset) {
    return Float.intBitsToFloat(readSwappedInteger(data, offset));
  }

  public static float readSwappedFloat(byte[] data, Offset offset) {
    try {
      return readSwappedFloat(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 4;
    }
    return 0;
  }

  public static void writeSwappedDouble(byte[] data, int offset, double value) {
    writeSwappedLong(data, offset, Double.doubleToLongBits(value));
  }

  public static void writeSwappedDouble(byte[] data, Offset offset, double value) {
    try {
      writeSwappedDouble(data, offset.writer, value);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.writer += 8;
    }
  }

  public static void writeSwapped(byte[] data, int offset, double value) {
    writeSwappedDouble(data, offset, value);
  }

  public static void writeSwapped(byte[] data, Offset offset, double value) {
    writeSwappedDouble(data, offset, value);
  }

  public static double readSwappedDouble(byte[] data, int offset) {
    return Double.longBitsToDouble(readSwappedLong(data, offset));
  }

  public static double readSwappedDouble(byte[] data, Offset offset) {
    try {
      return readSwappedDouble(data, offset.reader);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    } finally {
      offset.reader += 8;
    }
    return 0;
  }

  public static void writeSwappedShort(OutputStream output, short value) throws IOException {
    output.write((byte) ((value) & 0xff));
    output.write((byte) ((value >> 8) & 0xff));
  }

  public static short readSwappedShort(InputStream input) throws IOException {
    return (short) (((read(input) & 0xff)) + ((read(input) & 0xff) << 8));
  }

  public static int readSwappedUnsignedShort(InputStream input) throws IOException {
    int value1 = read(input);
    int value2 = read(input);

    return (((value1 & 0xff)) + ((value2 & 0xff) << 8));
  }

  public static void writeSwappedInteger(OutputStream output, int value) throws IOException {
    output.write((byte) ((value) & 0xff));
    output.write((byte) ((value >> 8) & 0xff));
    output.write((byte) ((value >> 16) & 0xff));
    output.write((byte) ((value >> 24) & 0xff));
  }

  public static int readSwappedInteger(InputStream input) throws IOException {
    int value1 = read(input);
    int value2 = read(input);
    int value3 = read(input);
    int value4 = read(input);

    return ((value1 & 0xff))
        + ((value2 & 0xff) << 8)
        + ((value3 & 0xff) << 16)
        + ((value4 & 0xff) << 24);
  }

  public static long readSwappedUnsignedInteger(InputStream input) throws IOException {
    int value1 = read(input);
    int value2 = read(input);
    int value3 = read(input);
    int value4 = read(input);

    long low = (((value1 & 0xff)) + ((value2 & 0xff) << 8) + ((value3 & 0xff) << 16));

    long high = value4 & 0xff;

    return (high << 24) + (0xffffffffL & low);
  }

  public static void writeSwappedLong(OutputStream output, long value) throws IOException {
    output.write((byte) ((value) & 0xff));
    output.write((byte) ((value >> 8) & 0xff));
    output.write((byte) ((value >> 16) & 0xff));
    output.write((byte) ((value >> 24) & 0xff));
    output.write((byte) ((value >> 32) & 0xff));
    output.write((byte) ((value >> 40) & 0xff));
    output.write((byte) ((value >> 48) & 0xff));
    output.write((byte) ((value >> 56) & 0xff));
  }

  public static long readSwappedLong(InputStream input) throws IOException {
    byte[] bytes = new byte[8];
    for (int i = 0; i < 8; i++) {
      bytes[i] = (byte) read(input);
    }
    return readSwappedLong(bytes, 0);
  }

  public static void writeSwappedFloat(OutputStream output, float value) throws IOException {
    writeSwappedInteger(output, Float.floatToIntBits(value));
  }

  public static float readSwappedFloat(InputStream input) throws IOException {
    return Float.intBitsToFloat(readSwappedInteger(input));
  }

  public static void writeSwappedDouble(OutputStream output, double value) throws IOException {
    writeSwappedLong(output, Double.doubleToLongBits(value));
  }

  public static double readSwappedDouble(InputStream input) throws IOException {
    return Double.longBitsToDouble(readSwappedLong(input));
  }

  public static Random RANDOM = new Random(System.currentTimeMillis());

  public static boolean nextBoolean() {
    return RANDOM.nextBoolean();
  }

  public static int nextInt() {
    return RANDOM.nextInt();
  }

  public static int nextInt(int max) {
    return RANDOM.nextInt(max);
  }

  public static int nextInt(int min, int max) {
    int v = max - min;
    return min + RANDOM.nextInt(v);
  }

  public static int nextInt(List<Integer> list) {
    int index = nextInt(list.size());
    return list.get(index);
  }

  public static int nextInt(int[] list) {
    int index = nextInt(list.length);
    return list[index];
  }

  public static long nextLong() {
    return RANDOM.nextLong();
  }

  public static double nextDouble() {
    return RANDOM.nextDouble();
  }

  public static double nextGaussian() {
    return RANDOM.nextGaussian();
  }

  /**
   * 计算距离
   *
   * @param x1 2
   * @param y1 2
   * @param x2 2
   * @param y2 2
   * @return int
   */
  public static int distance(int x1, int y1, int x2, int y2) {
    double v = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    return (int) v;
  }

  /**
   * 计算百分率
   *
   * @param v 1
   * @param max 1
   * @return 1
   */
  public static int percent(double v, double max) {
    if (v <= 0 || max <= 0) {
      return 0;
    }
    int r = (int) (v * 100 / max);
    return Math.min(r, 100);
  }

  public static int max(List<Integer> list) {
    int result = list.get(0);
    int size = list.size();
    for (int i = 1; i < size; i++) {
      int v = list.get(i);
      result = Math.max(v, result);
    }
    return result;
  }

  public static int max(int[] list) {
    int result = list[0];
    int size = list.length;
    for (int i = 1; i < size; i++) {
      int v = list[i];
      result = Math.max(v, result);
    }
    return result;
  }

  public static int min(List<Integer> list) {
    int result = list.get(0);
    int size = list.size();
    for (int i = 1; i < size; i++) {
      int v = list.get(1);
      result = Math.min(v, result);
    }
    return result;
  }

  public static int min(int[] list) {
    int result = list[0];
    int size = list.length;
    for (int i = 1; i < size; i++) {
      int v = list[i];
      result = Math.min(v, result);
    }
    return result;
  }

  public static String nStr(int n) {
    if (n < -1000000) {
      return ((int) (n / 1000000)) + "百万";
    }
    if (n < -10000) {
      return ((int) (n / 10000)) + "万";
    }
    if (n < -1000) {
      return ((int) (n / 1000)) + "千";
    }
    if (n < 0) {
      return "" + n;
    } else if (n < 1000) {
      return "" + n;
    } else if (n < 10000) {
      return ((int) (n / 1000)) + "千";
    } else if (n < 1000000) {
      return ((int) (n / 10000)) + "万";
    } else {
      return ((int) (n / 1000000)) + "百万";
    }
  }

  /** 在进制表示中的字符集合 */
  static final char[] DIGITS = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
    'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
  };

  public static String toOtherBaseString(long n, int base) {
    long num = 0;
    if (n < 0) {
      num = ((long) 2 * 0x7fffffff) + n + 2;
    } else {
      num = n;
    }
    char[] buf = new char[32];
    int charPos = 32;
    while ((num / base) > 0) {
      buf[--charPos] = DIGITS[(int) (num % base)];
      num /= base;
    }
    buf[--charPos] = DIGITS[(int) (num % base)];
    return new String(buf, charPos, (32 - charPos));
  }

  /**
   * 将其它进制的数字（字符串形式）转换为十进制的数字
   *
   * @param str 其它进制的数字（字符串形式）
   * @param base 指定的进制
   * @return
   */
  public static long toDecimalism(String str, int base) {
    char[] buf = new char[str.length()];
    str.getChars(0, str.length(), buf, 0);
    long num = 0;
    for (int i = 0; i < buf.length; i++) {
      for (int j = 0; j < DIGITS.length; j++) {
        if (DIGITS[j] == buf[i]) {
          num += (long) (j * Math.pow(base, buf.length - i - 1));
          break;
        }
      }
    }
    return num;
  }

  public static class Offset {
    public int reader;
    public int writer;

    @Override
    public String toString() {
      return StrBuilderEx.builder()
          .a("[reader:")
          .a(reader)
          .a(",writer:")
          .a(writer)
          .a("]")
          .toString();
    }
  }
}
