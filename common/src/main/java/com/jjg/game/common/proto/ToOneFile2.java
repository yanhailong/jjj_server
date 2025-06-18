package com.jjg.game.common.proto;

import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.commons.lang3.StringUtils;
import com.jjg.game.common.utils.ClassUtils;
import com.jjg.game.common.utils.FileHelper;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 11
 * @date 2022/5/11
 */
public class ToOneFile2 {
    public static void main(String[] args) {
        try{
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

            ToOneFile2 one = new ToOneFile2();
            one.java2PbMessage(searchPackage, importFiles, genToDir, sort);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void java2PbMessage(String searchPackage, String pkg, String genToDir, boolean sort) throws Exception{
        String fileName = genToDir;
        String[] sps = searchPackage.split(";");
        List<Class<?>> classList = new ArrayList<>();
        for (String pk : sps) {
            String[] arr = pk.split(",");
            if(arr.length < 2){
                Set<Class<?>> classes = ClassUtils.getAllClassByAnnotation(pk, ProtobufMessage.class);
                classList.addAll(classes);
            }else {
                String prefix = arr[0];
                String str2 = arr[1];
                str2 = str2.replaceAll("\\[","");
                str2 = str2.replaceAll("\\]","");
                String[] configClaArr = str2.split(",");
                for(String configCla : configClaArr){
                    if(StringUtils.isEmpty(configCla)){
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
        //String fileName = dir + "/Protocol.proto";

//        sb.append("package ").append(fileSimpleName(fileName)).append(";\r\r");
        sb.append("syntax = \"proto3\";\r\r");

        if(StringUtils.isNotEmpty(pkg)){
            sb.append("import \"").append(pkg).append("\";\r\r");
        }

        for(Class clazz : classListTmp){
            Schema<?> schema = RuntimeSchema.getSchema(clazz);
            Annotation annotation = clazz.getAnnotation(ProtobufMessage.class);

            int cmd = 0;

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
                    cmd = Integer.parseInt(obj3.toString());
                    if (objDesc != null) {
                        String note = String.format("//%s,msgID=%s,desc=%s", (boolean) obj1 ? "响应" : "请求", Context.instance.toHex(cmd), objDesc);
                        sb.append(note).append("\n");
                    } else {
                        String note = String.format("//%s,msgID=%s", (boolean) obj1 ? "响应" : "请求", Context.instance.toHex(cmd));
                        sb.append(note).append("\n");
                    }
                } else if (objDesc != null){
                    String note = String.format("//%s", objDesc);
                    sb.append(note).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Java2Pb2 pbGen = new Java2Pb2(schema, cmd).gen();
            String content = pbGen.toMesage();
            sb.append(content);
        }

//        handReqMap(sb,fileName);
        FileHelper.saveFile(fileName, sb.toString(), false);

    }

//    public static StringBuilder handReqMap(StringBuilder sb,String fileName){
//        if(Context.instance.getReqMap().size() < 1){
//            return sb;
//        }
//
//        String[] arr = fileName.split("\\\\");
//        String msgName = arr[arr.length - 1].replace(".proto","") + "";
//        msgName = Context.instance.toUpperFirst(msgName) + "MsgIds";
//
//        sb.append("enum ").append(msgName).append(" {\r");
//
//        for(Map.Entry<Integer,String> en : Context.instance.getReqMap().entrySet()){
//            sb.append("  ").append(en.getValue()).append(" = ").append(Context.instance.toHex(en.getKey())).append(";\r");
//        }
//
//        sb.append("}");
//        return sb;
//    }
//
//    private static String fileSimpleName(String fileName){
//        String[] arr = fileName.split("\\\\");
//        return arr[arr.length - 1].replace(".proto","") + "";
//    }
}
