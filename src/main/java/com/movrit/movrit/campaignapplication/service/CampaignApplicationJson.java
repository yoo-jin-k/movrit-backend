package com.movrit.movrit.campaignapplication.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * 신청서 양식·신청 답변을 DB의 JSON 문자열로 저장하고 다시 읽는 도우미입니다.
 *
 * <p>양식(체험 방식·질문·동의 문서)과 신청 답변은 구조가 캠페인마다 달라 JSON으로 보관합니다. 변환에 실패하면 내부 오류(500)로 안내합니다.
 */
@Component
@RequiredArgsConstructor
class CampaignApplicationJson {
  private final ObjectMapper objectMapper;

  /** 값을 JSON 문자열로 바꿉니다. */
  String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청서 데이터를 저장하지 못했습니다.");
    }
  }

  /** JSON 문자열을 지정한 형식으로 읽습니다. */
  <T> T read(String json, TypeReference<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JacksonException exception) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "신청서 데이터를 읽지 못했습니다.");
    }
  }
}
