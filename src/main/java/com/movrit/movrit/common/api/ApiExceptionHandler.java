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

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ProblemDetail handleStatus(ResponseStatusException exception) {
    ProblemDetail problem = ProblemDetail.forStatus(exception.getStatusCode());
    problem.setTitle(exception.getStatusCode().toString());
    problem.setDetail(exception.getReason());
    problem.setType(URI.create("about:blank"));
    return problem;
  }

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
