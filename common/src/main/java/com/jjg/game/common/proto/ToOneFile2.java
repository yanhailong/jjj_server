package com.jjg.game.common.proto;

import com.jjg.game.common.utils.ClassUtils;
import com.jjg.game.common.utils.FileHelper;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2022/5/11
 */
public class ToOneFile2 {

    public static void main(String[] args) {
        try {
            String searchPackage = args[0];
            String genToDir = args[1];
            String importFiles = null;
            if (args.length > 2) {
                importFiles = args[2];
            }
            boolean sort = true;
            if (args.length > 3) {
                sort = Boolean.parseBoolean(args[3]);
            }
            ToOneFile2 one = new ToOneFile2();
            one.java2PbMessage(searchPackage, importFiles, genToDir, sort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void java2PbMessage(String searchPackage, String pkg, String genToDir, boolean sort) throws Exception {
        String fileName = genToDir;
        String[] sps = searchPackage.split(";");
        List<Class<?>> classList = new ArrayList<>();
        for (String pk : sps) {
            String[] arr = pk.split(",");
            if (arr.length < 2) {
                Set<Class<?>> classes = ClassUtils.getAllClassByAnnotation(pk, ProtobufMessage.class);
                classList.addAll(classes);
            } else {
                String prefix = arr[0];
                String str2 = arr[1];
                str2 = str2.replaceAll("\\[", "");
                str2 = str2.replaceAll("\\]", "");
                String[] configClaArr = str2.split(",");
                for (String configCla : configClaArr) {
                    if (StringUtils.isEmpty(configCla)) {
                        continue;
                    }
                    String name = prefix + "." + configCla;
                    Class cla = Class.forName(name);
                    classList.add(cla);
                }
            }

        }
        Class[] classes1 = classList.toArray(new Class[0]);
        Arrays.sort(classes1, Comparator.comparing(Class::getName));
        List<Class> classListTmp;
        if (sort) {
            classListTmp = Arrays.asList(classes1).stream().sorted(Comparator.comparingInt(clazz -> {
                try {
                    Annotation annotation = clazz.getAnnotation(ProtobufMessage.class);
                    Class c = annotation.getClass();
                    Object obj3 = c.getMethod("cmd").invoke(annotation);
                    if (obj3 instanceof Integer) {
                        return (Integer) obj3;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return 0;
            })).collect(Collectors.toList());
        } else {
            classListTmp = Arrays.asList(classes1).stream().collect(Collectors.toList());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("syntax = \"proto3\";\r\r");

        if (StringUtils.isNotEmpty(pkg)) {
            sb.append("import \"").append(pkg).append("\";\r\r");
        }
        int maxMessageLen = 36;
        HashMap<Integer, String> cmdChecker = new HashMap<>();
        StringBuilder repeatIdCollector = new StringBuilder();

        sortMessages(classListTmp);

        for (Class clazz : classListTmp) {
            Schema<?> schema = RuntimeSchema.getSchema(clazz);
            Annotation annotation = clazz.getAnnotation(ProtobufMessage.class);

            int cmd = 0;

            try {
                Class c = annotation.getClass();
                boolean toPbFile = (boolean) c.getMethod("toPbFile").invoke(annotation);
                if (!toPbFile) {
                    continue;
                }
                Object obj1 = c.getMethod("resp").invoke(annotation);
                Object obj2 = c.getMethod("messageType").invoke(annotation);
                Object obj3 = c.getMethod("cmd").invoke(annotation);

                Object objDesc = null;
                Annotation descAnnotation = clazz.getAnnotation(ProtoDesc.class);
                if (descAnnotation != null) {
                    Class descClazz = descAnnotation.getClass();
                    objDesc = descClazz.getMethod("value").invoke(descAnnotation);
                }
                boolean isMessageStruct = "0".equals(obj2.toString());
                String clazzName = clazz.getSimpleName();
                int clazzNameLen = clazzName.length();
                int tabNum = (int) Math.ceil((maxMessageLen - clazzNameLen) / 4.0);
                StringBuilder logStr = new StringBuilder("ToPb " +
                    (isMessageStruct ? "消息结构体" : "协议类：") + clazzName);
                if (!isMessageStruct) {
                    cmd = Integer.parseInt(obj3.toString());
                    logStr.append("\t".repeat(tabNum)).append(" ID: ").append(cmd);
                    if (objDesc != null) {
                        String note = String.format("//%s,msgID=%s,desc=%s", (boolean) obj1 ? "响应" : "请求",
                            Context.instance.toHex(cmd), objDesc);
                        sb.append(note).append("\n");
                        logStr.append((boolean) obj1 ? " 响应协议 " : " 请求协议 ").append(" cmdHex: ")
                            .append(Context.instance.toHex(cmd))
                            .append(" 描述：")
                            .append(objDesc);
                    } else {
                        String note = String.format("//%s,msgID=%s", (boolean) obj1 ? "响应" : "请求",
                            Context.instance.toHex(cmd));
                        sb.append(note).append("\n");
                    }
                } else if (objDesc != null) {
                    String note = String.format("//%s", objDesc);
                    sb.append(note).append("\n");
                    logStr.append(" 描述：").append(objDesc);
                }
                System.out.println(logStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (cmd != 0) {
                if (!cmdChecker.containsKey(cmd)) {
                    cmdChecker.put(cmd, clazz.getSimpleName());
                } else {
                    repeatIdCollector
                        .append("\n协议ID：")
                        .append(cmd)
                        .append("重复，重复类：")
                        .append(cmdChecker.get(cmd))
                        .append("<=>")
                        .append(clazz.getSimpleName());
                }
            }
            Java2Pb2 pbGen = new Java2Pb2(schema, cmd).gen();
            String content = pbGen.toMesage();
            sb.append(content);
        }
        if (repeatIdCollector.isEmpty()) {
//            FileHelper.saveFile(fileName, sb.toString(), false);
        } else {
            throw new IllegalArgumentException(repeatIdCollector.toString());
        }
    }

    /**
     * 排序
     * @param classListTmp
     */
    public void sortMessages(List<Class> classListTmp) {
        classListTmp.sort((clazz1, clazz2) -> {
            try {
                Annotation annotation1 = clazz1.getAnnotation(ProtobufMessage.class);
                Annotation annotation2 = clazz2.getAnnotation(ProtobufMessage.class);

                Class c1 = annotation1.getClass();
                Class c2 = annotation2.getClass();

                Object obj21 = c1.getMethod("messageType").invoke(annotation1);
                Object obj22 = c2.getMethod("messageType").invoke(annotation2);

                boolean isMessage1 = "0".equals(obj21.toString());
                boolean isMessage2 = "0".equals(obj22.toString());

                if (isMessage1 && isMessage2) {  // 如果都是消息结构体，按类名首字母排序
                    return clazz1.getSimpleName().compareTo(clazz2.getSimpleName());
                }else if (!isMessage1 && !isMessage2) {  // 如果都是协议类，按cmdHex排序
                    Object obj31 = c1.getMethod("cmd").invoke(annotation1);
                    Object obj32 = c2.getMethod("cmd").invoke(annotation2);

                    int cmd1 = Integer.parseInt(obj31.toString());
                    int cmd2 = Integer.parseInt(obj32.toString());

                    return Integer.compare(cmd1, cmd2);
                }else if (isMessage1) {  // 消息结构体排在前面
                    return -1;
                }else {  // 协议类排在后面
                    return 1;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });
    }
}
