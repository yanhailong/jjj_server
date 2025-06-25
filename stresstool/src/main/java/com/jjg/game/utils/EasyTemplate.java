package com.jjg.game.utils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * @author 2CL
 */
public class EasyTemplate {
  public static class Cache {
    public byte[] b;
    public long lastModified;
  }

  public static final Map<String, Cache> CACHES = new Hashtable<String, Cache>();

  public static EasyTemplate easyTemplate = new EasyTemplate();

  public Cache newCache() {
    return new Cache();
  }

  public static String make(File file, Map<String, String> params, String encode) throws Exception {
    byte[] b = readFully(file);
    String s = new String(b, encode);
    return make(s, params);
  }

  public static String make2(File file, Map<String, String> params, String encode)
      throws Exception {
    String fname = file.getPath();

    byte[] b;
    if (CACHES.containsKey(fname)) {
      Cache c = CACHES.get(fname);
      if (c == null || c.lastModified < file.lastModified()) {
        c = easyTemplate.newCache();
        b = readFully(file);
        c.b = b;
        c.lastModified = file.lastModified();
        CACHES.put(fname, c);
      } else {
        b = c.b;
      }
    } else {
      Cache c = easyTemplate.newCache();
      b = readFully(file);
      c.b = b;
      c.lastModified = file.lastModified();
      CACHES.put(fname, c);
    }

    String s = new String(b, encode);
    return make(s, params);
  }

  public static <K, V> String make(String s, Map<K, V> params) throws Exception {
    if (s == null || s.isEmpty() || params == null || params.isEmpty()) {
      return s;
    }
    String mapBegin = "${";
    String listBegin = "$[";
    if (!s.contains(mapBegin) && !s.contains(listBegin)) {
      return s;
    }
    StringBuilder sb = new StringBuilder();
    sb.append(s);
    Set<Map.Entry<K, V>> entrys = params.entrySet();
    for (Map.Entry<K, V> e : entrys) {
      Object key = e.getKey();
      Object v = e.getValue();
      String k = "${" + key + "}";
      String k2 = "$[" + key + "]";
      String var = String.valueOf(v);

      int i1 = sb.indexOf(k);
      while (i1 >= 0) {
        int end = i1 + k.length();
        sb.replace(i1, end, var);
        i1 = sb.indexOf(k);
      }

      int i2 = sb.indexOf(k2);
      while (i2 >= 0) {
        int end = i2 + k2.length();
        sb.replace(i2, end, "\"" + var + "\"");
        i2 = sb.indexOf(k2);
      }
    }
    return sb.toString();
  }

  public static byte[] readFully(File f) throws Exception {
    if (f == null || !f.exists()) {
      throw new IOException("file no exists");
    }
    int len = (int) f.length();
    byte[] b = new byte[len];
    FileInputStream fis = new FileInputStream(f);
    DataInputStream dis = new DataInputStream(fis);
    dis.readFully(b);
    fis.close();

    return b;
  }

  public static Map<String, String> newMap() {
    return new HashMap<String, String>();
  }
}
