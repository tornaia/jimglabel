package com.github.tornaia.jimglabel.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class SerializerUtils {

    private static final int LOG_JSON_MAX_LENGTH = 1000;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public SerializerUtils() {
        objectMapper.configure(JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String toJSON(Object object) {
        if (object == null) {
            throw new SerializerException("Failed to serialize null object");
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SerializerException("Failed to serialize object", e);
        }
    }

    public <T> T toObject(String json, Class<T> clazz) {
        if (json == null) {
            throw new SerializerException("Failed to deserialize null string to " + clazz.getCanonicalName());
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            String truncatedJson = truncateStringCleverly(json);
            String truncatedMessage = StringUtils.truncate(e.getMessage(), LOG_JSON_MAX_LENGTH);
            throw new SerializerException("Failed to deserialize string: '" + truncatedJson + "', to: " + clazz.getSimpleName() + " reason: " + truncatedMessage);
        }
    }

    public <T> T toObject(InputStream inputStream, Class<T> clazz) {
        if (inputStream == null) {
            throw new SerializerException("Failed to deserialize null string to " + clazz.getCanonicalName());
        }

        try {
            String json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            return toObject(json, clazz);
        } catch (IOException e) {
            String truncatedMessage = StringUtils.truncate(e.getMessage(), LOG_JSON_MAX_LENGTH);
            throw new SerializerException("Failed to deserialize inputStream: '" + inputStream + "', to: " + clazz.getSimpleName() + " reason: " + truncatedMessage);
        }
    }

    private String truncateStringCleverly(String json) {
        return json.length() < LOG_JSON_MAX_LENGTH ? json : StringUtils.truncate(json, LOG_JSON_MAX_LENGTH / 2) + "..." + json.substring(json.length() - LOG_JSON_MAX_LENGTH / 2);
    }
}
