package com.movrit.movrit.common.api;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** API 오류를 화면이 읽기 쉬운 공통 형식(ProblemDetail)으로 바꿔 응답합니다. */
@RestControllerAdvice
public class ApiExceptionHandler {

  /** 서비스에서 던진 ResponseStatusException의 상태 코드와 안내 문구를 그대로 응답합니다. 화면은 detail 값을 사용자 안내로 사용합니다. */
  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleStatus(ResponseStatusException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(exception.getStatusCode());
    problem.setTitle(exception.getStatusCode().toString());
    problem.setDetail(exception.getReason());
    problem.setType(URI.create("about:blank"));
    return problem;
  }

  /** 요청 본문 검증(@Valid) 실패를 400으로 응답하고, 어떤 입력칸이 잘못됐는지 fieldErrors에 담아 줍니다. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problem.setTitle("입력값을 확인해 주세요.");
    problem.setDetail("요청 본문의 필수값 또는 형식이 올바르지 않습니다.");

    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(error.getField(), error.getDefaultMessage());
    }
    problem.setProperty("fieldErrors", errors);
    return problem;
  }
}
