package com.vegasnight.game.common.proto;


import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * @since 1.0
 */
public class Java2PbMessage {
    /* 协议*/
    public final Schema<?> schema;
    /* proto pakage*/
    public String packageName;
    /* 生成java类的包名*/
    public String javaPackageName;
    /* 输出java的类名*/
    public String outerClassName = null;
    /* 依赖的其他message*/
    public Set<String> dependencyMessages = new HashSet<>();
    /* 本类message*/
    public StringBuilder message = new StringBuilder();

    public Java2PbMessage(Schema<?> schema, String pkg) {
        if (!(schema instanceof RuntimeSchema)) {
            throw new IllegalArgumentException("schema instance must be a RuntimeSchema");
        }

        this.schema = schema;
        Class<?> typeClass = schema.typeClass();
        this.javaPackageName = typeClass.getPackage().getName();
        this.outerClassName = typeClass.getSimpleName();
        this.packageName = pkg;
    }

    public Java2PbMessage gen() {
        generateInternal();
        return this;
    }

    public String toMesage() {
        StringBuilder output = new StringBuilder();
        output.append("package ").append(packageName).append(";\n\n");
        //output.append("import \"proto/protostuff-default.proto\";\n\n");
        //导入依赖
        String format = "import \"%s.proto\";\n";
        if (!dependencyMessages.isEmpty()) {
            dependencyMessages.forEach(name -> {
                output.append(String.format(format, name));
            });
        }
        output.append("\noption java_package = \"").append(javaPackageName).append("\";\n");
        if (outerClassName != null) {
            output.append("option java_outer_classname=\"").append(outerClassName).append("\";\n");
        }
        output.append("\n");
        output.append(this.message);
        return output.toString();
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
            message.append("  ").append(val).append(" = ").append(v.ordinal()).append(";\n");
        }

        message.append("}").append("\n\n");

    }

    protected void doGenerateMessage(Schema<?> schema) {

//        if (!(schema instanceof RuntimeSchema)) {
//            throw new IllegalStateException("invalid schema type " + schema.getClass());
//        }
//
//        RuntimeSchema<?> runtimeSchema = (RuntimeSchema<?>) schema;
//
//        message.append("message ").append(runtimeSchema.messageName()).append(" {").append("\n");
//
//        try {
//            Field fieldsField = MappedSchema.class.getDeclaredField("fields");
//            fieldsField.setAccessible(true);
//            com.dyuproject.protostuff.runtime.MappedSchema.Field<?>[] fields = (com.dyuproject.protostuff.runtime.MappedSchema.Field<?>[]) fieldsField
//                    .get(runtimeSchema);
//
//            for (int i = 0; i != fields.length; ++i) {
//
//                com.dyuproject.protostuff.runtime.MappedSchema.Field<?> field = fields[i];
//                String fieldType = null;
//                if (field.type == WireFormat.FieldType.ENUM) {
//
//                    Field reflectionField = field.getClass().getDeclaredField("val$eio");
//                    reflectionField.setAccessible(true);
//                    EnumIO enumIO = (EnumIO) reflectionField.get(field);
//                    fieldType = enumIO.enumClass.getSimpleName();
//                    dependencyMessages.add(fieldType);
//                } else if (field.type == WireFormat.FieldType.MESSAGE) {
//                    if (field.repeated) {
//                        Field typeClassField = field.getClass().getField("typeClass");
//                        typeClassField.setAccessible(true);
//                        Class<?> tmpClass = (Class<?>) typeClassField.get(field);
//                        fieldType = tmpClass.getSimpleName();
//                        //将依赖的部分添加在此处
//                        if (!dependencyMessages.contains(fieldType)) {
//                            dependencyMessages.add(fieldType);
//                        }
//                    } else {
//                        Pair<RuntimeFieldType, Class<?>> normField = ReflectionUtil.normalizeFieldClass(field);
//                        if (normField == null) {
//                            throw new IllegalStateException(
//                                    "unknown fieldClass " + field.getClass());
//                        }
//
//                        Class<?> fieldClass = normField.getSecond();
//                        if (normField.getFirst() == RuntimeFieldType.RuntimeRepeatedField) {
//
//                        } else if (normField.getFirst() == RuntimeFieldType.RuntimeMessageField) {
//
//                            Field typeClassField = fieldClass.getDeclaredField("typeClass");
//                            typeClassField.setAccessible(true);
//                            Class<?> typeClass = (Class<?>) typeClassField.get(field);
//
//                            Field hasSchemaField = fieldClass.getDeclaredField("hasSchema");
//                            hasSchemaField.setAccessible(true);
//
//                            HasSchema<?> hasSchema = (HasSchema<?>) hasSchemaField.get(field);
//                            Schema<?> fieldSchema = hasSchema.getSchema();
//                            fieldType = fieldSchema.messageName();
//
//                            //将依赖的部分添加在此处
//                            if (!dependencyMessages.contains(fieldType)) {
//                                dependencyMessages.add(fieldType);
//                            }
//                        } else {
//                            throw new IllegalStateException("field type not support, typeclass=" + schema.typeClass() + ",fieldName=" + field.name);
//                        }
//                    }
//                } else {
//                    fieldType = field.type.toString().toLowerCase();
//                }
//
//                message.append("  ");
//
//                if (field.type == WireFormat.FieldType.ENUM) {
//                    message.append("required ");
//                } else {
//                    if (field.repeated) {
//                        message.append("repeated ");
//                    } else {
//                        message.append("optional ");
//                    }
//                }
//                message.append(fieldType).append(" ").append(field.name).append(" = ").append(field.number).append(";\n");
//
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("generate proto fail", e);
//        }
//
//        message.append("}").append("\n\n");

    }
}

