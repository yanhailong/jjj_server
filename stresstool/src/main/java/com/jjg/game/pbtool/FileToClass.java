package com.jjg.game.pbtool;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2025/6/12 11:37
 */
public class FileToClass {
  public static void main(String[] args) {
    try {
      String pbPath = args[0];
      String genToDir = args[1];
      String packageStr = args[2];

      new FileToClass().pbToJavaClass(pbPath, genToDir, packageStr);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void pbToJavaClass(String pbPath, String genToDir, String packageStr) throws Exception {
    File pbFile = new File(pbPath);
    if (!pbFile.exists()) {
      System.out.println("该目录不存在 pbPath = " + pbPath);
      return;
    }

    if (!pbFile.isDirectory()) {
      System.out.println("非目录 pbPath = " + pbPath);
      return;
    }

    File[] files = pbFile.listFiles();
    for (File file : files) {
      String fileName = file.getName();
      if (!fileName.endsWith(".proto")) {
        continue;
      }

      BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
      List<String> list = bufferedReader.lines().collect(Collectors.toList());
      Map<String, List<String>> map = findMessage(list);
      List<MessageInfo> messageInfoList = coverToMessageInfo(map, packageStr);
      writeToFile(messageInfoList, genToDir, packageStr);
    }
  }

  /**
   * 找出 message或者enum 块
   *
   * @param list
   */
  private static Map<String, List<String>> findMessage(List<String> list) {
    Map<String, List<String>> map = new HashMap<>();
    String currentMsg = null;

    String lastLine = null;

    for (String str : list) {
      if (currentMsg == null) {
        if (str.startsWith("message")) {
          String[] parts = str.split("\\s+"); // 按一个或多个空格分割
          String messageName = parts[1];
          currentMsg = messageName;

          List<String> tempList = map.computeIfAbsent(messageName, k -> new ArrayList<>());
          if (lastLine != null && lastLine.startsWith("//")) {
            tempList.add(lastLine);
          }

          //                    tempList.add(messageName);
        } else if (str.startsWith("enum")) {
          String[] parts = str.split("\\s+"); // 按一个或多个空格分割
          String enumName = parts[1];
          currentMsg = enumName;
          List<String> tempList = map.computeIfAbsent(enumName, k -> new ArrayList<>());
          if (lastLine != null && lastLine.startsWith("//")) {
            tempList.add(lastLine);
          }
          //                    tempList.add(enumName);
        }
      } else {
        str = str.trim();
        if (str.startsWith("}")) {
          currentMsg = null;
        } else if (str.startsWith("enum E_MsgID")) {
          continue;
        } else {
          List<String> tempList = map.get(currentMsg);
          if (tempList == null) {
            System.out.println("未找到与 " + currentMsg + " 相关的list");
            return Collections.EMPTY_MAP;
          }
          tempList.add(str);
        }
      }

      lastLine = str;
    }
    return map;
  }

  private static List<MessageInfo> coverToMessageInfo(
      Map<String, List<String>> map, String packageStr) {
    List<MessageInfo> messageInfoList = new ArrayList<>();
    for (Map.Entry<String, List<String>> en : map.entrySet()) {
      String messageName = en.getKey();
      List<String> messageList = en.getValue();

      MessageInfo messageInfo = new MessageInfo();
      messageInfo.name = messageName;

      messageInfo.isEnum = messageName.endsWith("Enum");

      int beginIndex = 1;
      String firstLine = messageList.get(0);
      if (firstLine.startsWith("//")) {
        String[] arr = firstLine.split(",");
        if (arr.length != 3) {
          if (arr.length == 1) {
            messageInfo.desc = arr[0];
          } else {
            System.out.println("message 的注释长度错误 messageName = " + messageName);
            return Collections.EMPTY_LIST;
          }
        } else {
          String[] arr1 = arr[1].split("=");
          messageInfo.msgIdHex = arr1[1];

          String[] arr2 = arr[2].split("=");
          messageInfo.desc = arr2[1];
        }
      } else {
        beginIndex = 0;
        //                System.out.println(JSON.toJSONString(messageInfo,true));
      }

      if (messageInfo.isEnum) {

      } else {
        for (int i = beginIndex; i < messageList.size(); i++) {
          String s = messageList.get(i);
          String[] arr = s.split("\\s+"); // 按一个或多个空格分割

          FieldInfo fieldInfo = new FieldInfo();

          int index = 0;
          if (arr[index].equals("repeated")) {
            fieldInfo.isList = true;
            messageInfo.hasList = true;
            index++;
          }

          fieldInfo.type = mapProtoTypeToJava(arr[index], fieldInfo.isList);
          boolean obj = map.containsKey(arr[index]);
          if (obj) {
            messageInfo.importList.add(packageStr + "." + arr[index]);
          }

          index++;
          fieldInfo.name = arr[index];
          index++;

          if (arr.length >= index + 3) {
            //                        System.out.println(arr[index+2]);
            fieldInfo.comment = arr[index + 2];
            //                        System.out.println(JSON.toJSONString(fieldInfo,true));
          }

          messageInfo.fields.add(fieldInfo);
        }
      }

      messageInfoList.add(messageInfo);
      //            System.out.println(JSON.toJSONString(messageInfo,true));
      //            break;
    }

    return messageInfoList;
  }

  private static String mapProtoTypeToJava(String protoType, boolean isList) {
    if (isList) {
      return switch (protoType.toLowerCase()) {
        case "string" -> "String";
        case "int32" -> "Integer";
        case "int64" -> "Long";
        case "bool" -> "Boolean";
        case "double" -> "Double";
        case "float" -> "Dloat";
        case "bytes" -> "Byte[]";
        default -> protoType; // For custom types/enums
      };
    } else {
      return switch (protoType.toLowerCase()) {
        case "string" -> "String";
        case "int32" -> "int";
        case "int64" -> "long";
        case "bool" -> "boolean";
        case "double" -> "double";
        case "float" -> "float";
        case "bytes" -> "byte[]";
        default -> protoType; // For custom types/enums
      };
    }
  }

  private static void writeToFile(
      List<MessageInfo> messageInfoList, String genToDir, String packageStr) {
    try {
      StringBuffer sb = new StringBuffer();
      for (MessageInfo info : messageInfoList) {
        sb.setLength(0);
        sb.append("package " + packageStr + ";\n\n");

        sb.append("import com.fomo.game.clientcore.proto.AbstractMessage;\n");

        if (StringUtils.isNotEmpty(info.msgIdHex)) {
          sb.append("import com.jjg.game.common.proto.ProtobufMessage;\n");
        }
        sb.append("import com.jjg.game.common.proto.ProtoDesc;\n");
        for (String pkg : info.importList) {
          sb.append("import ").append(pkg).append(";\n");
        }

        if (info.hasList) {
          sb.append("\n");
          sb.append("import java.util.List;\n");
        }

        sb.append("\n\n");

        File file = new File(genToDir + info.name + ".java");
        if (file.exists()) {
          file.delete();
        }
        file.createNewFile();

        if (StringUtils.isNotEmpty(info.msgIdHex)) {
          int cmd = Integer.decode(info.msgIdHex);
          sb.append("@ProtobufMessage(messageType = 0x")
              .append(cmd >> 12)
              .append(", cmd = 0x")
              .append(Integer.toHexString(cmd))
              .append(")\n");
        }
        if (StringUtils.isNotEmpty(info.desc)) {
          sb.append("@ProtoDesc(\"").append(info.desc.replace("//", "")).append("\")\n");
        }

        if (info.isEnum) {
          sb.append("public enum " + info.name + " {\n");
        } else {
          sb.append("public class " + info.name + " extends AbstractMessage {\n");
        }

        if (info.fields != null) {
          for (FieldInfo fieldInfo : info.fields) {
            if (StringUtils.isNotEmpty(fieldInfo.comment)) {
              sb.append("    @ProtoDesc(\"").append(fieldInfo.comment).append("\")\n");
            }

            if (fieldInfo.isList) {
              sb.append("    public ")
                  .append("List<")
                  .append(fieldInfo.type)
                  .append("> ")
                  .append(fieldInfo.name)
                  .append(";\n");
            } else {
              sb.append("    public ")
                  .append(fieldInfo.type)
                  .append(" ")
                  .append(fieldInfo.name)
                  .append(";\n");
            }
          }
        }

        sb.append("}");

        FileWriter fw = new FileWriter(file);
        fw.write(sb.toString());

        System.out.println(file.getName() + " 写入完毕");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
