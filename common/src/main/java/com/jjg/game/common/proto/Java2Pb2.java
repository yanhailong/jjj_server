package com.jjg.game.common.proto;

import io.protostuff.Schema;
import io.protostuff.WireFormat;
import io.protostuff.runtime.EnumIO;
import io.protostuff.runtime.HasSchema;
import io.protostuff.runtime.RuntimeSchema;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author 11
 * @date 2022/5/11
 */
public class Java2Pb2 {
    /* 协议*/
    public final Schema<?> schema;
    /* 本类message*/
    public StringBuilder message = new StringBuilder();

    private int cmd;

    public Java2Pb2(Schema<?> schema, int cmd) {
        if (!(schema instanceof RuntimeSchema)) {
            throw new IllegalArgumentException("schema instance must be a RuntimeSchema");
        }

        this.schema = schema;
        this.cmd = cmd;
    }

    public Java2Pb2 gen() {
        generateInternal();
        return this;
    }

    public String toMesage() {
        return message.toString();
    }

    protected void generateInternal() {
        if (schema.typeClass().isEnum()) {
            doGenerateEnum(schema.typeClass());
        } else {
            doGenerateMessage(schema);
        }
    }

    protected void doGenerateEnum(Class<?> enumClass) {

        message.append("enum ").append(enumClass.getSimpleName()).append(" {").append("\n");

        for (Object val : enumClass.getEnumConstants()) {
            Enum<?> v = (Enum<?>) val;
            String desc = "";
            message.append("  ").append(val).append(" = ").append(v.ordinal()).append(";");
            try {
                Field field =  enumClass.getDeclaredField((v.name()));
                ProtoDesc protoDesc = field.getAnnotation(ProtoDesc.class);
                if (protoDesc != null) {
                    desc = protoDesc.value();
                }
            } catch (Exception e) {
                throw new RuntimeException("generate enum fail", e);
            }
            if (!StringUtils.isEmpty(desc)) {
                message.append("  //").append(desc);
            }
            message.append("\n");
        }

        message.append("}").append("\n\n");

    }

    private void addReqMap(String name){
        if(cmd < 1){
            return;
        }

        Context.instance.getReqMap().put(cmd,name);
        /*if(name.endsWith("Req")){
            //String tmpName =  name.substring(0,name.length() - 3);
            Context.instance.getReqMap().put(cmd,name);
        }else if(name.endsWith("Resp")){
            //String tmpName =  name.substring(0,name.length() - 4);

        }*/
    }

    protected void doGenerateMessage(Schema<?> schema) {

        if (!(schema instanceof RuntimeSchema)) {
            throw new IllegalStateException("invalid schema type " + schema.getClass());
        }

        RuntimeSchema<?> runtimeSchema = (RuntimeSchema<?>) schema;

        String messageName = runtimeSchema.messageName();

        addReqMap(messageName);
        message.append("message ").append(messageName).append(" {").append("\n");

        if(cmd > 0){
            message.append("  enum E_MsgID {def = 0; msgID = ").append(cmd).append(";};\n");
        }


        try {
            List<? extends io.protostuff.runtime.Field<?>> fields = ((RuntimeSchema<?>) schema).getFields();
//            Field fieldsField = MappedSchema.class.getDeclaredField("fields");
//            fieldsField.setAccessible(true);
//            MappedSchema.Field<?>[] fields = (MappedSchema.Field<?>[]) fieldsField
//                    .get(runtimeSchema);

            int fieldCount = ((RuntimeSchema<?>) schema).getFieldCount();

//            io.protostuff.runtime.Field<?> fieldByNumber = ((RuntimeSchema<?>) schema).getFieldByNumber(0);

            for (int i = 1; i <= fieldCount; ++i) {
//            for(io.protostuff.runtime.Field field : fields){
                String desc = "";
                io.protostuff.runtime.Field<?> field = ((RuntimeSchema<?>) schema).getFieldByNumber(i);
                String fieldType = null;
                if (field.type == WireFormat.FieldType.ENUM) {

                    Field reflectionField = field.getClass().getDeclaredField("val$eio");
                    reflectionField.setAccessible(true);
                    EnumIO enumIO = (EnumIO) reflectionField.get(field);
                    fieldType = enumIO.enumClass.getSimpleName();

                    try {
                        Field fieldTmp = schema.typeClass().getDeclaredField(field.name);
                        ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                        if (protoDesc != null) {
                            desc = protoDesc.value();
                        }
                    } catch (NoSuchFieldException e) {

                    }
                } else if (field.type == WireFormat.FieldType.MESSAGE) {
                    if (field.repeated) {
                        Field typeClassField = field.getClass().getField("typeClass");
                        typeClassField.setAccessible(true);
                        Class<?> tmpClass = (Class<?>) typeClassField.get(field);
                        fieldType = tmpClass.getSimpleName();
                    } else {
                        Pair<RuntimeFieldType, Class<?>> normField = ReflectionUtil.normalizeFieldClass(field);
                        if (normField == null) {
                            throw new IllegalStateException(
                                    "unknown fieldClass " + field.getClass());
                        }

                        Class<?> fieldClass = normField.getSecond();
                        if (normField.getFirst() == RuntimeFieldType.RuntimeRepeatedField) {

                        } else if (normField.getFirst() == RuntimeFieldType.RuntimeMessageField) {

                            Field typeClassField = fieldClass.getDeclaredField("typeClass");
                            typeClassField.setAccessible(true);
                            Class<?> typeClass = (Class<?>) typeClassField.get(field);

                            Field hasSchemaField = fieldClass.getDeclaredField("hasSchema");
                            hasSchemaField.setAccessible(true);

                            HasSchema<?> hasSchema = (HasSchema<?>) hasSchemaField.get(field);
                            Schema<?> fieldSchema = hasSchema.getSchema();
                            fieldType = fieldSchema.messageName();

                        } else {
                            throw new IllegalStateException("field type not support, typeclass=" + schema.typeClass() + ",fieldName=" + field.name);
                        }
                    }


                    try {
                        Field fieldTmp = schema.typeClass().getDeclaredField(field.name);
                        ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                        if (protoDesc != null) {
                            desc = protoDesc.value();
                        }
                    } catch (NoSuchFieldException e) {

                    }
                } else if(field.type == WireFormat.FieldType.INT32){
                    if(messageName.endsWith("Req")){
                        fieldType = WireFormat.FieldType.UINT32.toString().toLowerCase();
                    }else {
                        fieldType = WireFormat.FieldType.INT32.toString().toLowerCase();
                    }

                    Class clazz = schema.typeClass();
                    Field fieldTmp = null;
                    try {
                        fieldTmp = clazz.getDeclaredField(field.name);

                    } catch (NoSuchFieldException e) {
                        try{
                            fieldTmp = clazz.getSuperclass().getDeclaredField(field.name);
                        }catch (NoSuchFieldException e1){
                            try{
                                fieldTmp = clazz.getSuperclass().getSuperclass().getDeclaredField(field.name);
                            }catch (NoSuchFieldException e2){

                            }
                        }

                    }

                    if(fieldTmp != null){
                        ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                        if (protoDesc != null) {
                            desc = protoDesc.value();
                        }
                    }
                } else if(field.type == WireFormat.FieldType.INT64){
                    if(messageName.endsWith("Req")){
                        fieldType = WireFormat.FieldType.UINT64.toString().toLowerCase();
                    }else {
                        fieldType = WireFormat.FieldType.INT64.toString().toLowerCase();
                    }
                    Class clazz = schema.typeClass();

                    try {
                        Field fieldTmp = clazz.getDeclaredField(field.name);
                        ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                        if (protoDesc != null) {
                            desc = protoDesc.value();
                        }
                    } catch (NoSuchFieldException e) {

                    }
                } else {
                    fieldType = field.type.toString().toLowerCase();
                    Class clazz = schema.typeClass();
                    try {
                        Field fieldTmp = clazz.getDeclaredField(field.name);
                        ProtoDesc protoDesc = fieldTmp.getAnnotation(ProtoDesc.class);
                        if (protoDesc != null) {
                            desc = protoDesc.value();
                        }
                    } catch (NoSuchFieldException e) {

                    }
                }

                message.append("  ");

                if (field.repeated) {
                    message.append("repeated ");
                } else {
//                    message.append("optional ");
                }
//                }
                message.append(fieldType).append(" ").append(field.name).append(" = ").append(field.number).append(";");
                if (!StringUtils.isEmpty(desc)) {
                    message.append("  //").append(desc);
                }
                message.append("\n");
            }

        } catch (Exception e) {
            throw new RuntimeException("generate proto fail", e);
        }

        message.append("}").append("\n\n");

    }
}
