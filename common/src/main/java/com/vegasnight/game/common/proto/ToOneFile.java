package com.vegasnight.game.common.proto;

import com.vegasnight.game.common.utils.ClassUtils;
import com.vegasnight.game.common.utils.FileHelper;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 1.0
 */
public class ToOneFile {
    public static void main(String[] args) throws ClassNotFoundException {
        String searchPackage = args[0];
        String genToDir = args[1];
        String importFiles = null;
        if (args.length > 2) {
            importFiles = args[2];
        }

        boolean sort = false;
        if (args.length > 3) {
            sort = Boolean.parseBoolean(args[3]);
        }

        ToOneFile one = new ToOneFile();
        one.java2PbMessage(searchPackage, importFiles, genToDir, sort);
    }

    public void java2PbMessage(String searchPackage, String pkg, String genToDir, boolean sort) {
        String fileName = genToDir;
        String[] sps = searchPackage.split(";");
        List<Class<?>> classList = new ArrayList<>();
        for (String pk : sps) {
            Set<Class<?>> classes = ClassUtils.getAllClassByAnnotation(pk, ProtobufMessage.class);
            classList.addAll(classes);
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
        //String fileName = dir + "/Protocol.proto";

        for(Class clazz : classListTmp){
            Schema<?> schema = RuntimeSchema.getSchema(clazz);
            Annotation annotation = clazz.getAnnotation(ProtobufMessage.class);
            try {
                Class c = annotation.getClass();
                boolean toPbFile = (boolean)c.getMethod("toPbFile").invoke(annotation);
                if(!toPbFile){
                   continue;
                }
                System.out.println(clazz);
                Object obj1 = c.getMethod("resp").invoke(annotation);
                Object obj2 = c.getMethod("messageType").invoke(annotation);
                Object obj3 = c.getMethod("cmd").invoke(annotation);

                Object objDesc = null;
                Annotation descAnnotation = clazz.getAnnotation(ProtoDesc.class);
                if (descAnnotation != null) {
                    Class descClazz = descAnnotation.getClass();
                    objDesc = descClazz.getMethod("value").invoke(descAnnotation);
                }
//                Object obj4 = c.getMethod("node").invoke(annotation);

//                System.out.println(obj1);
//                System.out.println(obj2);
//                System.out.println(obj3);
//                System.out.println(obj4);
                if (!"0".equals(obj2.toString())) {
                    if (objDesc != null) {
                        String note = String.format("//%s,messageType=%s,cmd=%s,desc=%s", (boolean) obj1 ? "响应" : "请求", obj2, obj3, objDesc);
                        sb.append(note).append("\n");
                    } else {
                        String note = String.format("//%s,messageType=%s,cmd=%s", (boolean) obj1 ? "响应" : "请求", obj2, obj3);
                        sb.append(note).append("\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Java2Pb pbGen = new Java2Pb(schema, pkg).gen();
            String content = pbGen.toMesage();
            sb.append(content);
        }

        FileHelper.saveFile(fileName, sb.toString(), false);

    }
}
