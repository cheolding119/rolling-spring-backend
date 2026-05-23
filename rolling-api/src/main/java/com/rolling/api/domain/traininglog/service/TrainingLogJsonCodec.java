package com.rolling.api.domain.traininglog.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLink;
import org.springframework.util.StringUtils;

import java.util.List;

final class TrainingLogJsonCodec {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<TrainingLogChecklistItem>> CHECKLIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<TrainingLogExternalLink>> EXTERNAL_LINK_TYPE = new TypeReference<>() {};

    private TrainingLogJsonCodec() {
    }

    static String writeChecklistJson(List<TrainingLogChecklistItem> checklist) {
        return writeJsonOrNull(checklist);
    }

    static List<TrainingLogChecklistItem> readChecklist(String checklistJson) {
        return readJsonList(checklistJson, CHECKLIST_TYPE);
    }

    static String writeStringListJson(List<String> values) {
        return writeJsonOrNull(values);
    }

    static List<String> readStringList(String json) {
        return readJsonList(json, STRING_LIST_TYPE);
    }

    static String writeExternalLinksJson(List<TrainingLogExternalLink> externalLinks) {
        return writeJsonOrNull(externalLinks);
    }

    static List<TrainingLogExternalLink> readExternalLinks(String externalLinksJson) {
        return readJsonList(externalLinksJson, EXTERNAL_LINK_TYPE);
    }

    private static String writeJsonOrNull(Object value) {
        if (value instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("훈련 기록 JSON 직렬화에 실패했습니다", e);
        }
    }

    private static <T> List<T> readJsonList(String json, TypeReference<List<T>> typeReference) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            List<T> value = OBJECT_MAPPER.readValue(json, typeReference);
            return value == null ? List.of() : List.copyOf(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("훈련 기록 JSON 역직렬화에 실패했습니다", e);
        }
    }
}
