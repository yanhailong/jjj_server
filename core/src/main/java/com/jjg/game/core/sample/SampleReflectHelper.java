package com.jjg.game.core.sample;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * sample映射
 * @author 11
 * @date 2025/6/6 17:47
 */
public class SampleReflectHelper {
    public static List<Sample> resolveSample(Class<?> clazz,
                                             String[] attributeNames, List<String[]> attributeValues) {
        List<Sample> samples = new ArrayList<Sample>(attributeValues.size());
        Sample sample = null;
        int n = 4;
        for (String[] values : attributeValues) {
            n++;
            try {
                sample = (Sample) clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (sample == null) {
                continue;
            }
            for (int i = 0; i < attributeNames.length; i++) {
                String attributeName = attributeNames[i].trim();
                String attributeValue = null;
                if (i < values.length) {
                    attributeValue = values[i].trim();
                }
                if (attributeNames[i].isEmpty() || attributeValue == null || attributeValue.isEmpty()) {
                    continue;
                }
                if (!sample.setAttribute(attributeName, attributeValue) && !set(sample, attributeName, attributeValue)) {
                    System.out.println("读取配置失败 ，className = " + clazz.getName() + ", attributeName = " + attributeName + ", attributeValue = " + attributeValue);
                }
            }
            if (sample.sid > 0) {
                samples.add(sample);
            }
        }
        return samples;
    }

    private static boolean set(Object instance, String fieldName, String value) {
        try {
            Field field = instance.getClass().getField(fieldName);
            Class<?> fieldClazz = field.getType();
            if (fieldClazz == List.class) {
                Type type = field.getGenericType();
                List list = (List) field.get(instance);
                if (list == null) {
                    list = new ArrayList();
                    field.set(instance, list);
                }
                Object vobj = value;
                // 如果是泛型参数的类型
                if (type instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) type;
                    //得到泛型里的class类型对象
                    Class<?> genericClazz = (Class<?>) pt.getActualTypeArguments()[0];
                    vobj = parseValue(genericClazz, value);
                }

                list.add(vobj);
            } else {
                Object vobj = parseValue(fieldClazz, value);
                field.set(instance, vobj);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Object parseValue(Type fieldClazz, String value) {
        if (fieldClazz == String.class) {
            return value;
        } else if (fieldClazz == int.class || fieldClazz == Integer.class) {
            return Double.valueOf(value).intValue();
        } else if (fieldClazz == byte.class || fieldClazz == Byte.class) {
            return Double.valueOf(value).byteValue();
        } else if (fieldClazz == boolean.class || fieldClazz == Boolean.class) {
            if (value == null || value.isEmpty() || "false".equalsIgnoreCase(value) || "0.0".equals(value) || "0".equals(value)) {
                return false;
            } else {
                return true;
            }
        } else if (fieldClazz == short.class || fieldClazz == Short.class) {
            return Double.valueOf(value).shortValue();
        } else if (fieldClazz == long.class || fieldClazz == Long.class) {
            return Double.valueOf(value).longValue();
        } else if (fieldClazz == float.class || fieldClazz == Float.class) {
            return Float.valueOf(value);
        } else if (fieldClazz == double.class || fieldClazz == Double.class) {
            return Double.valueOf(value);
        }
        return value;
    }
}
