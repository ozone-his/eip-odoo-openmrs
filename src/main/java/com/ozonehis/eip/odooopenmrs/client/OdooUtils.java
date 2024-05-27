package com.ozonehis.eip.odooopenmrs.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdooUtils {

    private static final Logger log = LoggerFactory.getLogger(OdooUtils.class);

    public static <T> T convertToObject(Map<String, Object> data, Class<T> objectClass) {
        ObjectMapper mapper = new ObjectMapper();
        log.info("OdooUtils: Converting map {} to object {}", data, objectClass.getName());
        try {
            T obj = mapper.convertValue(data, objectClass);
            log.info("OdooUtils: Converted map {} to object {}", data, obj);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error converting map %s to object: %s", data, e.getMessage()));
        }
    }

    public static Map<String, Object> convertObjectToMap(Object object) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        log.info("OdooUtils: Converting object {} to map", object.getClass().getName());

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(JsonProperty.class)) {
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                String key = jsonProperty.value();
                Object value = field.get(object);
                map.put(key, value);
            }
        }
        log.info("OdooUtils: Converted object {} to map {}", object.getClass().getName(), map);
        return map;
    }
}
