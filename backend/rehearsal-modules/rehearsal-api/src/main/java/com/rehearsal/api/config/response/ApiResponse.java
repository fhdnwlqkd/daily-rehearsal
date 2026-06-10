package com.rehearsal.api.config.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rehearsal.domain.core.exception.ErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ErrorResponse error) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static ApiResponse<Void> empty() {
    return new ApiResponse<>(true, null, null);
  }

  public static ApiResponse<Void> fail(ErrorCode errorCode) {
    return fail(errorCode, errorCode.getMessage());
  }

  public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
    return new ApiResponse<>(false, null, ErrorResponse.of(errorCode, message));
  }

  public static ApiResponse<Void> fail(ErrorResponse error) {
    return new ApiResponse<>(false, null, error);
  }
}
