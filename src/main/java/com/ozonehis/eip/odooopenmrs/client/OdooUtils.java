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
        try {
            log.info("OdooUtils: convertToObject Converted class : {}", mapper.convertValue(data, objectClass));
            return mapper.convertValue(data, objectClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Object> convertObjectToMap(Object object) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(JsonProperty.class)) {
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                String key = jsonProperty.value();
                Object value = field.get(object);
                map.put(key, value);
            }
        }
        log.info("OdooUtils: convertObjectToMap Converted object : {}", map);
        return map;
    }
}
