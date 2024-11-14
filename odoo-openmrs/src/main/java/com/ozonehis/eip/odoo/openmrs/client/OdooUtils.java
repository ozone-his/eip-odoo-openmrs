/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public class OdooUtils {

    public static <T> T convertToObject(Map<String, Object> data, Class<T> objectClass) {
        ObjectMapper mapper = new ObjectMapper();
        log.debug("OdooUtils: Converting map {} to object {}", data, objectClass.getName());
        try {
            T obj = mapper.convertValue(data, objectClass);
            log.debug("OdooUtils: Converted map {} to object {}", data, obj);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error converting map %s to object: %s", data, e.getMessage()));
        }
    }

    public static Map<String, Object> convertObjectToMap(Object object) throws Exception {
        Map<String, Object> map = new HashMap<>();
        Field[] fields = object.getClass().getDeclaredFields();
        log.debug("OdooUtils: Converting object {} to map", object.getClass().getName());

        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(JsonProperty.class)) {
                JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
                String key = jsonProperty.value();
                Object value = field.get(object);
                map.put(key, value);
            }
        }
        log.debug("OdooUtils: Converted object {} to map {}", object.getClass().getName(), map);
        return map;
    }

    public static String convertEEE_MMM_ddDateToOdooFormat(String date) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            LocalDate localDate = LocalDate.parse(date, inputFormatter);
            return localDate.format(outputFormatter);
        } catch (DateTimeParseException e) {
            log.error("Cannot convert input date to Odoo date. Error: {}", e.getMessage());
            return ""; // Returning empty string if not able to parse input date
        }
    }
}
