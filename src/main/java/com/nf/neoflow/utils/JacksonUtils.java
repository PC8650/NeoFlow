package com.nf.neoflow.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * 工具类
 */
@Slf4j
public class JacksonUtils {

    public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    /**
     * obj => json
     * @param obj
     * @return
     */
    public static String toJson(Object obj){
        if (ObjectUtils.isEmpty(obj)){
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        }catch (JsonProcessingException e) {
            log.error(String.format("obj=[%s]",obj.toString()),e);
        }
        return null;
    }

    /**
     * json => obj
     * @param json
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T toObj(String json,Class<T> tClass){
        if (StringUtils.isBlank(json)){
            return null;
        }
        try {
            return MAPPER.readValue(json,tClass);
        } catch (IOException e) {
            log.error(String.format("json=[%s]",json),e);
        }
        return null;
    }

    /**
     * json => 集合
     * @param json
     * @param collectionClass 集合Class
     * @param tClass 集合元素Class
     * @param <T>
     * @return
     */
    public static <T> Collection<T> toObj(String json, Class<? extends Collection> collectionClass, Class<T>tClass){
        if (StringUtils.isBlank(json)){
            return null;
        }
        try {
            return MAPPER.readValue(json,getCollectionType(collectionClass,tClass));
        } catch (IOException e) {
            log.error(String.format("json=[%s]",json),e);
        }
        return null;
    }

    public static Map objToMap(Object obj){
        if (ObjectUtils.isEmpty(obj)){
            return null;
        }
        try {
            return MAPPER.convertValue(obj,Map.class);
        }catch (IllegalArgumentException e) {
            log.error(String.format("obj=[%s]",obj.toString()),e);
        }
        return null;
    }

    /**
     * json => Map
     * @param json
     * @param mapClass
     * @param keyClass
     * @param valueClass
     * @param <K>
     * @param <V>
     * @return
     * @throws IOException
     */
    public static <K, V> Map<K, V> toObj(String json, Class<? extends Map> mapClass, Class<K> keyClass,
                                         Class<V> valueClass){
        if (StringUtils.isBlank(json)){
            return null;
        }
        JavaType javaType = MAPPER.getTypeFactory().constructMapType(mapClass, keyClass, valueClass);
        try {
            return MAPPER.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            log.error(String.format("json=[%s]",json),e);
        }
        return null;
    }

    /**
     * 获取JavaType
     * @param collectionClass 集合Class
     * @param elementClasses 元素Class
     * @return
     */
    public static JavaType getCollectionType(Class<?> collectionClass, Class<?>... elementClasses){
        return MAPPER.getTypeFactory().constructParametricType(collectionClass,elementClasses);
    }


}
