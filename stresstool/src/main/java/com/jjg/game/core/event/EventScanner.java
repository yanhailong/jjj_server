package com.jjg.game.core.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.jjg.game.core.robot.StressRobotManager;
import com.jjg.game.utils.PackageUtils;
import com.jjg.game.utils.ProtoBufUtils;
import com.jjg.game.utils.ProtoBufUtils.ProtoClientMessage;

/**
 * @author 2CL
 * @function 事件扫描器
 */
public final class EventScanner {

  public static int requestOnceEventCount = 0;

  /** 响应事件类模板集合 */
  private static final List<Class<? extends AbstractEvent<?>>> RESP_EVENT_CLASSES =
      new ArrayList<>();

  /** 可请求多次的事件类模板集合 */
  private static final Map<FunctionType, List<Class<? extends AbstractEvent<?>>>>
      REQ_MULTIPLE_EVENT_CLASSES = new HashMap<>();

  /** 只可请求一次的事件类模板集合 */
  private static final List<Class<? extends AbstractEvent<?>>> REQ_ONCE_EVENT_CLASSES =
      new ArrayList<>();

  /** 客户端消息 */
  private static final Map<Integer, ProtoClientMessage> CLIENT_MESSAGE_MAP =
      new HashMap<Integer, ProtoClientMessage>();

  /** 客户端消息 */
  private static List<ProtoClientMessage> clientMessages = new ArrayList<ProtoClientMessage>();

  /** 响应消息 */
  private static final Map<Integer, Class<?>> RESP_MESSAGE_MAP = new ConcurrentHashMap<>();

  /** 已选择的多次请求事件类模板集合 */
  private static Map<FunctionType, List<Class<? extends AbstractEvent<?>>>>
      selectedReqMultipleEventClasses = new HashMap<>();

  /** 已选择的一次请求事件类模板集合 */
  private static final Map<FunctionType, List<Class<? extends AbstractEvent<?>>>>
          SELECTED_REQ_ONCE_EVENT_CLASSES = new HashMap<>();

  public static List<Class<? extends AbstractEvent<?>>> getResponseEventClasses() {
    return RESP_EVENT_CLASSES;
  }

  public static Map<FunctionType, List<Class<? extends AbstractEvent<?>>>>
  getRequestMultipleEventClasses() {
    return REQ_MULTIPLE_EVENT_CLASSES;
  }

  public static List<Class<? extends AbstractEvent<?>>> getRequestOnceEventClasses() {
    return REQ_ONCE_EVENT_CLASSES;
  }

  public static Map<Integer, ProtoClientMessage> getClientMessageMap() {
    return CLIENT_MESSAGE_MAP;
  }

  public static List<ProtoClientMessage> getClientMessages() {
    return clientMessages;
  }

  public static Class<?> getRespMessageClass(int msgId) {
    return RESP_MESSAGE_MAP.get(msgId);
  }

  public static Map<FunctionType, List<Class<? extends AbstractEvent<?>>>>
  getSelectedRequestMultipleEventClasses() {
    return selectedReqMultipleEventClasses;
  }

  public static Map<FunctionType, List<Class<? extends AbstractEvent<?>>>>
      getSelectedRequestOnceEventClasses() {
    return SELECTED_REQ_ONCE_EVENT_CLASSES;
  }

  /** 初始化加载事件类模板 */
  public static void initEventClasses() {
    requestOnceEventCount = 0;
    Set<Class<?>> classes = PackageUtils.getClasses("com.jjg.game.logic");
    Iterator<Class<?>> it = classes.iterator();
    Class<?> clazz;
    FunctionType functionType;
    while (it.hasNext()) {
      clazz = it.next();
      FuncTestEvent funcTestEventClazz = clazz.getAnnotation(FuncTestEvent.class);
      if (null != funcTestEventClazz) {
        List<Class<? extends AbstractEvent<?>>> clazzList;
        switch (funcTestEventClazz.eventT()) {
          case RESPONSE:
            RESP_EVENT_CLASSES.add((Class<? extends AbstractEvent<?>>) clazz);
            break;
          case REQUEST_REPEAT:
            functionType = funcTestEventClazz.functionT();
            clazzList = REQ_MULTIPLE_EVENT_CLASSES.get(functionType);
            if (null == clazzList) {
              clazzList = new ArrayList<>();
              REQ_MULTIPLE_EVENT_CLASSES.put(functionType, clazzList);
            }
            clazzList.add((Class<? extends AbstractEvent<?>>) clazz);
            break;
          case REQUEST_ONCE:
            requestOnceEventCount++;
            REQ_ONCE_EVENT_CLASSES.add((Class<? extends AbstractEvent<?>>) clazz);
            break;
          default:
            break;
        }
      }
    }
  }

  /**
   * 初始化消息
   *
   * @throws Exception
   */
  public static void initMessageClasses() throws Exception {
    Set<Class<?>> classes = PackageUtils.getClasses("phanta.dal2.protobuf.c2s");
    Iterator<Class<?>> it = classes.iterator();
    Class<?> clazz = null;
    while (it.hasNext()) {
      clazz = it.next();
      // TODO 过滤登陆相关协议

      Map<Integer, ProtoClientMessage> clazzMap = ProtoBufUtils.parseClientMessage(clazz);
      // TODO 根据协议ID过滤
      CLIENT_MESSAGE_MAP.putAll(clazzMap);
    }
    clientMessages = CLIENT_MESSAGE_MAP.values().stream().toList();
  }

  /**
   * 初始化消息
   *
   * @throws Exception
   */
  public static void initRespMessageClasses() throws Exception {
    Set<Class<?>> classes = PackageUtils.getClasses("org.game.protobuf.s2c");
    Iterator<Class<?>> it = classes.iterator();
    Class<?> clazz;
    while (it.hasNext()) {
      clazz = it.next();
      // 解析此proto中所有message
      for (Class<?> msgClass : clazz.getClasses()) {
        // 解析message中的对象
        for (Class<?> innerClass : msgClass.getClasses()) {
          // 如果该message中包含枚举类 MsgID, 就证明是属于交互数据的对象,而非单独的消息体
          if ("MsgID".equals(innerClass.getSimpleName())) {
            // 获取 CS 类型的消息体
            for (Object obj : innerClass.getEnumConstants()) {
              // protobuf的枚举类型包含 value 和 index
              // 获取"private"类型的方法"values"
              Method m = obj.getClass().getDeclaredMethod("values", (Class<?>) null);
              Object[] result = (Object[]) m.invoke(obj, (Object) null);
              for (Object objOne : result) {
                Field value = objOne.getClass().getDeclaredField("value");

                // 设置为可强制访问
                value.setAccessible(true);
                RESP_MESSAGE_MAP.put(value.getInt(objOne), msgClass);
              }
            }
          }
        }
      }
    }
  }

  public static void setFunctionMultpleEvents(List<String> fNames) {
    // List<String> fNames = new ArrayList<>();
    // for (int i = 0; i < FunctionWindow.selectedList.size(); i++) {
    // fNames.add(FunctionWindow.selectedList.get(i));
    // }
    selectedReqMultipleEventClasses = new HashMap<>();
    if (null != fNames && !fNames.isEmpty()) {
      fNames.forEach(
          fName -> {
            Iterator<Entry<FunctionType, List<Class<? extends AbstractEvent<?>>>>> multipleIterator =
                REQ_MULTIPLE_EVENT_CLASSES.entrySet().iterator();
            multiple_in_loop:
            while (multipleIterator.hasNext()) {
              Entry<FunctionType, List<Class<? extends AbstractEvent<?>>>> entry =
                  multipleIterator.next();
              if (entry.getKey().fName.equals(fName.split("_")[0])) {
                selectedReqMultipleEventClasses.putIfAbsent(entry.getKey(), entry.getValue());
                break multiple_in_loop;
              }
            }
          });
    }
    Set<FunctionType> functionTypes = new HashSet<FunctionType>();
    assert fNames != null;
    for (String s : fNames) {
      for (FunctionType functionType : FunctionType.values()) {
        if (s.split("_")[0].equals(functionType.fName)) {
          functionTypes.add(functionType);
        }
      }
    }
    StressRobotManager.instance().refill(functionTypes);
  }
}
