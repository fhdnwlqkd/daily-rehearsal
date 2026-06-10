package com.rehearsal.api.config.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.exception.ErrorCode;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    String code, String name, String message, List<FieldErrorDetail> details) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return of(errorCode, errorCode.getMessage());
  }

  public static ErrorResponse of(ErrorCode errorCode, String message) {
    return new ErrorResponse(errorCode.getCode(), errorCode.name(), message, null);
  }

  public static ErrorResponse of(
      ErrorCode errorCode, String message, List<FieldErrorDetail> details) {
    return new ErrorResponse(errorCode.getCode(), errorCode.name(), message, details);
  }
}
