package com.lzj.search.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 *  通用工具类
 *
 * @Author hugende
 */
public class CommonUtil {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final BigDecimal HUNDRED = new BigDecimal(100);

    static {
        OBJECT_MAPPER.configure(Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);
        OBJECT_MAPPER.setSerializationInclusion(Include.NON_ABSENT);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        OBJECT_MAPPER.setDateFormat(simpleDateFormat);
    }

    public static <T> T jsonToObject(String source, Class<T> targetClass) {
        try {
            return OBJECT_MAPPER.readValue(source, targetClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T jsonToObject(String value, TypeReference<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(value, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String objectToJson(Object target) {
        try {
            return OBJECT_MAPPER.writeValueAsString(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode jsonToTree(String jsonStr) {
        try {
            return OBJECT_MAPPER.readTree(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <S, T> T map(S source, Class<T> tClass) {
        try {
            T t = tClass.newInstance();
            BeanUtils.copyProperties(source, t);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
