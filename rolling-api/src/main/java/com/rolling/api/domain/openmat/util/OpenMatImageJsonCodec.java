package com.rolling.api.domain.openmat.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.List;

public final class OpenMatImageJsonCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private OpenMatImageJsonCodec() {
    }

    public static String writeStringListJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("오픈매트 이미지 JSON 직렬화에 실패했습니다", e);
        }
    }

    public static List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<String> value = OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
            return value == null ? List.of() : List.copyOf(value);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
