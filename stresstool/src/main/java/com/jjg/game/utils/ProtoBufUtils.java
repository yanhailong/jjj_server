/** */
package com.jjg.game.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang.StringUtils;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message.Builder;
import com.jjg.game.core.Log4jManager;

/**
 * @author 2CL
 */
public class ProtoBufUtils {

  /**
   * 解析出客户端的请求数据
   *
   * @param protoClass - 消息对象
   * @return
   */
  public static Map<Integer, ProtoClientMessage> parseClientMessage(Class<?> protoClass)
      throws Exception {
    Map<Integer, ProtoClientMessage> clientMsg = new HashMap<Integer, ProtoClientMessage>();
    // 解析此proto中所有message
    for (Class<?> msgClass : protoClass.getClasses()) {
      // 解析message中的对象
      for (Class<?> innerClass : msgClass.getClasses()) {
        // 如果该message中包含枚举类 MsgID, 就证明是属于交互数据的对象,而非单独的消息体
        if (innerClass.getSimpleName().equals("MsgID")) {
          // 获取 CS 类型的消息体
          for (Object obj : innerClass.getEnumConstants()) {
            // protobuf的枚举类型包含 value 和 index
            // 获取"private"类型的方法"values"
            Method m = obj.getClass().getDeclaredMethod("values", null);
            Object[] result = (Object[]) m.invoke(obj, null);
            List<?> list = Arrays.asList(result);
            Iterator<?> it = list.iterator();
            while (it.hasNext()) {
              Object objOne = it.next();
              Field value = objOne.getClass().getDeclaredField("value");

              // 设置为可强制访问
              value.setAccessible(true);
              clientMsg.put(
                  value.getInt(objOne),
                  new ProtoClientMessage(
                      value.getInt(objOne), createBuilder(parseMessageBuidler(msgClass))));
            }
          }
        }
      }
    }

    return clientMsg;
  }

  /**
   * 创建Builder数据
   *
   * @param builderClass builderClass
   * @return {@link Builder}
   */
  public static Builder createBuilder(Class<?> builderClass) throws Exception {

    // 一般Builder有3个构建函数,一个是默认的,一个是解析超类生成,还有个是单例defaultxxx
    // 这里从默认构造函数进行发射,默认的类似于xxxMessage.newBuilder()

    for (Constructor<?> constructor : builderClass.getDeclaredConstructors()) {
      // 默认构造函数参数数量为0
      if (constructor.getParameterCount() == 0) {
        // 它的默认构造函数是private类型,强制破解访问 否则 newInstance 将抛出InstantiationException
        constructor.setAccessible(true);

        // 创建一个Builder实例,等同xxxMessage.newBuilder()
        // 这里会有很多异常直接往外抛Exception
        Object builder = constructor.newInstance();

        // if (builderClass.getName().contains("Heros")) {
        // Log4jManager.getInstance();
        // }

        // 解析Builder中所有方法
        for (Method method : builder.getClass().getMethods()) {
          // 自定义的消息都会包含SET方法,repeated类型除外
          // 而Buidler本身包含了3个SET方法,分别是 setField
          // setRepeatedField setUnknownFields
          // 这里拿出所有SET方法并屏蔽上述3个方法

          String mname = method.getName();
          if (mname.toLowerCase().contains("othersid")) {
            Log4jManager.getInstance();
          }

          String mname3 = mname.substring(0, 3);

          if ((mname3.equals("set")
                  && !mname.equals("setField")
                  && !mname.equals("setRepeatedField")
                  && !mname.equals("setUnknownFields"))
              || (mname3.equals("add")
                  && !mname.substring(0, 6).equals("addAll")
                  && !mname.equals("addRepeatedField"))) {
            // System.out.println(mname);
            // 强制破解private方法,变成可访问可设置
            method.setAccessible(true);

            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && (mname3.equals("set"))) {
              try {
                Object value = randomParamenters(parameters[0].getName());
                if (value != null && !value.toString().equals("UNRECOGNIZED")) {
                  method.invoke(builder, value);
                }
              } catch (FilterMessageException e) {
                Log4jManager.getInstance().error(ExceptionEx.e2s(e));
                continue;
              }
            } else if ((parameters.length == 1
                && mname3.equals("add")
                && !mname.endsWith("Builder"))) {
              // 随机10以内设置repated数量
              int argsNum = RandomUtil.getRandomNumIntMax(10);
              Object[] objs = new Object[argsNum];
              for (int i = 0; i < objs.length; i++) {
                try {
                  objs[i] = randomParamenters(parameters[0].getName());
                  if (objs[i] == null) {

                    continue;
                  }
                  // System.out.println(method.getDeclaringClass().getName());
                  // System.out.println(method.getDeclaringClass().hashCode());
                  // System.out.println(method.getName());
                  // method.invoke(builder, objs[i], objs[i]);
                  // ((Hero.ResHeros.Builder) builder).addOthersId(1);
                  // System.out.println(method.getName() + "," + objs[i]);
                  // if (method.getName().equals("addEmbattleGridsBuilder")) {
                  // continue;
                  // }
                  method.invoke(builder, objs[i]);
                } catch (FilterMessageException e) {
                  // Log4jManager.getInstance().error(ExceptionEx.e2s(e));
                  continue;
                }
              }
            } else if (mname3.equals("set") && parameters.length == 2) {
              continue;
            } else {
              Log4jManager.getInstance()
                  .error(
                      "未知的方法类型 method:"
                          + method.getName()
                          + " class:"
                          + builderClass.getName()
                          + ",params:"
                          + parameters.length);
              continue;
            }
          }
        }
        return (Builder) builder;
      }
    }

    return null;
  }

  /** 客户端上行消息 */
  public static class ProtoClientMessage {

    private final int msgId;
    private final Builder builder;

    public ProtoClientMessage(int msgId, Builder builder) {
      this.msgId = msgId;
      this.builder = builder;
    }

    public int getMsgId() {
      return msgId;
    }

    public Builder getBuilder() {
      return builder;
    }

    public byte[] getBuilderBytes() {
      return builder.build().toByteArray();
    }
  }

  public static class FilterMessageException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  /**
   * 解析消息类中的Builder
   *
   * @param messageClass
   * @return
   */
  private static Class<?> parseMessageBuidler(Class<?> messageClass) {
    // 一般类中只会包含MsgID对象(如果没有内部构建消息体)和一个Builder对象
    for (Class<?> declared : messageClass.getDeclaredClasses()) {
      if (declared.getSimpleName().equals("Builder")) {
        return declared;
      }
    }
    return null;
  }

  /**
   * 随机参数值
   *
   * @param 参数名称
   * @return
   */
  private static Object randomParamenters(String paramenterName) throws Exception {
    // String 类型包含了参数 ByteString和String 对象类型包含了 对象本身 和 对象的Builder repeated类型没有set方法
    Random random = new Random();
    switch (paramenterName) {
      case "long":
        return Long.valueOf(random.nextInt(10000000));
      case "float":
        return Float.valueOf(random.nextInt(10000000));
      case "int":
        return Integer.valueOf(random.nextInt(10000));
      case "boolean":
        return Boolean.valueOf(random.nextBoolean());
      case "java.lang.String":
        return String.valueOf(random.nextInt());
      case "com.google.protobuf.ByteString":
        return null;
      default:
        return parseObjectParamenter(paramenterName);
    }
  }

  /**
   * 解析对象数据
   *
   * @param paramenterName
   * @return
   */
  private static Object parseObjectParamenter(String paramenterName) throws Exception {
    Class<?> innerClass = Class.forName(paramenterName);
    if (innerClass.isEnum()) {
      // 处理枚举
      Method method = innerClass.getMethod("values");
      Enum<?> inter[] = (Enum[]) method.invoke(null, null);
      return inter[new Random().nextInt(inter.length)];
    } else {
      // 处理对象数据,对象数据Builder会提供两个构造函数,过滤掉非builder的参数设置
      if (!paramenterName
          .substring(paramenterName.length() - "Builder".length(), paramenterName.length())
          .equals("Builder")) {
        throw new FilterMessageException();
      } else {
        return createBuilder(innerClass);
      }
    }
  }

  /** 给builder赋值 * */
  public static void setBuilderVaules(Builder builder, String value, int index) {
    if (builder.getAllFields().size() <= 0) return;
    String mesaageInfo = getValueByIndex(value, index);
    if (mesaageInfo == null || mesaageInfo.length() <= 0) return;
    String[] values = mesaageInfo.split("<>");
    int i = 0;
    for (Object o : builder.getAllFields().keySet()) {
      FieldDescriptor fieldDescriptor = (FieldDescriptor) o;
      if (values[i] == null || values[i].length() <= 0) continue;
      String[] trueValue = values[i].split("\\|&\\|");
      if (trueValue == null || trueValue.length <= 0) continue;
      for (int j = 0; j < trueValue.length; j++) {
        try {
          builder =
              assembleValue(
                  builder,
                  fieldDescriptor.getName(),
                  trueValue[j],
                  fieldDescriptor.getJavaType().toString().toLowerCase(),
                  StringUtils.substringAfter(
                      fieldDescriptor.toProto().getLabel().toString(), "LABEL_"));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      i++;
    }
  }

  /** 根据位置获取当前的参数 * */
  public static String getValueByIndex(String value, int index) {
    if (value == null || value.length() <= 0) return null;
    // String[] values = value.split("\\$");
    String[] values = value.split("\\|\\$\\|");
    if (values.length <= index) return null;
    return values[index];
  }

  /** 组装值 * */
  public static Builder assembleValue(
      Builder builder, String methodName, String value, String basicType, String messageType)
      throws Exception {
    for (Constructor<?> constructor : builder.getClass().getDeclaredConstructors()) {
      // 默认构造函数参数数量为0
      if (constructor.getParameterCount() == 0) {
        constructor.setAccessible(true);
        for (Method method : builder.getClass().getMethods()) {
          String mname = method.getName();
          String mname3 = mname.substring(0, 3);

          if ((mname3.equals("set")
                  && !mname.equals("setField")
                  && !mname.equals("setRepeatedField")
                  && !mname.equals("setUnknownFields"))
              || (mname3.equals("add")
                  && !mname.substring(0, 6).equals("addAll")
                  && !mname.equals("addRepeatedField"))) {
            // 强制破解private方法,变成可访问可设置
            method.setAccessible(true);
            if (("set" + methodName).toLowerCase().equals(method.getName().toLowerCase())
                && !"repeated".equals(messageType.toLowerCase())) {
              assignment(method, basicType, value, builder);
              continue;
            }
            if (("add" + methodName).toLowerCase().equals(method.getName().toLowerCase())) {
              assignment(method, basicType, value, builder);
              continue;
            }
          }
        }
        return (Builder) builder;
      }
    }
    return null;
  }

  /** 根据类型去设置当前的值 * */
  public static void assignment(Method method, String basicType, String value, Object builder)
      throws Exception {
    if ("int".equals(basicType)) {
      method.invoke(builder, Integer.parseInt(value));
    } else if ("string".equals(basicType)) {
      method.invoke(builder, value);
    } else if ("long".equals(basicType)) {
      method.invoke(builder, Long.valueOf(value));
    } else if ("boolean".equals(basicType)) {
      method.invoke(builder, Integer.parseInt(value));
    } else if ("java.lang.String".equals(basicType)) {
      method.invoke(builder, value);
    } else if ("com.google.protobuf.ByteString".equals(basicType)) {
      // method.invoke(builder, value);
    } else {
    }
  }
}
