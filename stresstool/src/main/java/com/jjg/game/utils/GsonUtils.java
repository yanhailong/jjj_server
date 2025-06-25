package com.jjg.game.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.lang.reflect.Type;

/**
 * GSON 工具
 *
 * @author 2CL
 */
public class GsonUtils {

  private static final Gson GSON = new Gson().newBuilder().disableHtmlEscaping().create();

  public static <T> T fromJson(String json, Class<T> clazz) {
    return (T) GSON.fromJson(json, clazz);
  }

  public static String toJson(Object obj) {
    return GSON.toJson(obj);
  }

  public static JsonElement toJsonTree(Object obj) {
    return GSON.toJsonTree(obj);
  }

  public static JsonObject toJsonObject(String json) {
    return JsonParser.parseString(json).getAsJsonObject();
  }

  public static <T> T fromJson(String json, Type type) {
    return (T) GSON.fromJson(json, type);
  }
}
