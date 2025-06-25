package com.jjg.game.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class StrBuilderEx {

  /**
   * 新建一个StringBuilder
   *
   * @return
   */
  public static final StrBuilderEx builder() {
    return new StrBuilderEx();
  }

  /**
   * ${1}, ${2}
   *
   * @param fmt
   * @param args
   * @return
   */
  public static final String str(String fmt, Object... args) {
    StrBuilderEx result = builder();
    result.ap(fmt, args);
    return result.toString();
  }

  /**
   * append Object
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(Object v) {
    sb.append(v);
    return this;
  }

  /**
   * append boolean
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(boolean v) {
    sb.append(v);
    return this;
  }

  /**
   * insert boolean
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, boolean v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append byte
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(byte v) {
    sb.append(v);
    return this;
  }

  /**
   * insert byte
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, byte v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append short
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(short v) {
    sb.append(v);
    return this;
  }

  /**
   * insert short
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, short v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append int
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(int v) {
    sb.append(v);
    return this;
  }

  /**
   * insert int
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, int v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append float
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(float v) {
    sb.append(v);
    return this;
  }

  /**
   * insert float
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, float v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append double
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(double v) {
    sb.append(v);
    return this;
  }

  /**
   * insert double
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, double v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append double decimalFormat
   *
   * @param v
   * @param df
   * @return
   */
  public final StrBuilderEx a(double v, DecimalFormat df) {
    String v2 = df.format(v);
    return a(v2);
  }

  /**
   * insert double decimalFormat
   *
   * @param offset
   * @param v
   * @param df
   * @return
   */
  public final StrBuilderEx a(int offset, double v, DecimalFormat df) {
    String v2 = df.format(v);
    return a(offset, v2);
  }

  /**
   * append BigDecimal decimalFormat
   *
   * @param v
   * @param df
   * @return
   */
  public final StrBuilderEx a(BigDecimal v, DecimalFormat df) {
    String v2 = df.format(v.doubleValue());
    return a(v2);
  }

  /**
   * insert BigDecimal decimalFormat
   *
   * @param offset
   * @param v
   * @param df
   * @return
   */
  public final StrBuilderEx a(int offset, BigDecimal v, DecimalFormat df) {
    String v2 = df.format(v.doubleValue());
    return a(offset, v2);
  }

  /**
   * append String
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(String v) {
    sb.append(v);
    return this;
  }

  /**
   * insert String
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, String v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append StringBuffer
   *
   * @param v
   * @return
   */
  public final StrBuilderEx a(StringBuffer v) {
    sb.append(v);
    return this;
  }

  /**
   * insert StringBuffer
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx a(int offset, StringBuffer v) {
    sb.insert(offset, v);
    return this;
  }

  /**
   * append String.fomart java格式化
   *
   * @param s
   * @param args
   * @return
   */
  public final StrBuilderEx a(String s, Object... args) {
    String s2 = String.format(s, args);
    return a(s2);
  }

  /**
   * append Crlf
   *
   * @return
   */
  public final StrBuilderEx an() {
    sb.append("\r\n");
    return this;
  }

  /**
   * append String vs Crlf
   *
   * @param v
   * @return
   */
  public final StrBuilderEx an(String v) {
    sb.append(v + "\r\n");
    return this;
  }

  /**
   * insert String + Crlf
   *
   * @param offset
   * @param v
   * @return
   */
  public final StrBuilderEx an(int offset, String v) {
    sb.insert(offset, v + "\r\n");
    return this;
  }

  /**
   * append String.format vs Crlf
   *
   * @param s
   * @param args
   * @return
   */
  public final StrBuilderEx an(String s, Object... args) {
    String s2 = String.format(s + "\r\n", args);
    return a(s2);
  }

  /**
   * insert String.format
   *
   * @param offset
   * @param s
   * @param args
   * @return
   */
  public final StrBuilderEx a(int offset, String s, Object... args) {
    String s2 = String.format(s, args);
    return a(offset, s2);
  }

  /**
   * insert String.format vs Crlf
   *
   * @param offset
   * @param s
   * @param args
   * @return
   */
  public final StrBuilderEx an(int offset, String s, Object... args) {
    String s2 = String.format(s + "\r\n", args);
    return a(offset, s2);
  }

  /**
   * append String
   *
   * @param fmt
   * @return
   */
  public final StrBuilderEx pn(String fmt) {
    return an(fmt);
  }

  /**
   * append ${1} ${2}... String vs Crlf
   *
   * @param fmt
   * @param args
   * @return
   */
  public final StrBuilderEx pn(String fmt, Object... args) {
    return ap(fmt + "\r\n", args);
  }

  /**
   * insert ${1} ${2}.... String vs Crlf 带换行符
   *
   * @param offset
   * @param fmt
   * @param args
   * @return
   */
  public final StrBuilderEx pn(int offset, String fmt, Object... args) {
    return ap(offset, fmt + "\r\n", args);
  }

  /**
   * append String
   *
   * @param fmt
   * @return
   */
  public StrBuilderEx ap(String fmt) {
    return a(fmt);
  }

  /**
   * append ${1} ${2} ... String 不带换行符
   *
   * @param fmt
   * @param args
   * @return
   */
  public StrBuilderEx ap(String fmt, Object... args) {
    try {
      String s2 = StrEx.fmt(fmt, args);
      return a(s2);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
    return this;
  }

  /**
   * insert ${1} ${2}... String
   *
   * @param offset
   * @param fmt
   * @param args
   * @return
   */
  public StrBuilderEx ap(int offset, String fmt, Object... args) {
    try {
      String s2 = StrEx.fmt(fmt, args);
      return a(offset, s2);
    } catch (Exception e) {
      LoggerUtils.LOGGER.error(ExceptionEx.e2s(e));
    }
    return this;
  }

  /**
   * replace String
   *
   * @param begin
   * @param end
   * @param s
   * @return
   */
  public final StrBuilderEx r(int begin, int end, String s) {
    sb.replace(begin, end, s);
    return this;
  }

  /**
   * setCharAt
   *
   * @param index
   * @param s
   * @return
   */
  public final StrBuilderEx c(int index, char s) {
    sb.setCharAt(index, s);
    return this;
  }

  /**
   * delete
   *
   * @param start
   * @param end
   * @return
   */
  public final StrBuilderEx d(int start, int end) {
    sb.delete(start, end);
    return this;
  }

  /**
   * deleteCharAt
   *
   * @param index
   * @return
   */
  public final StrBuilderEx d(int index) {
    sb.deleteCharAt(index);
    return this;
  }

  @Override
  public final String toString() {
    return sb.toString();
  }

  public final int len() {
    return sb.length();
  }

  public final int length() {
    return sb.length();
  }

  public void removeLeft(int i) {
    sb.replace(0, i, "");
  }

  public void removeRight(int i) {
    int len = sb.length();
    sb.replace(len - i, len, "");
  }

  private StringBuilder sb;

  private StrBuilderEx() {
    this.sb = new StringBuilder();
  }
}
