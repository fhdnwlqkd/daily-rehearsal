package com.rehearsal.domain.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  INVALID_REQUEST("C001", "Invalid request.", HttpStatus.BAD_REQUEST),
  INTERNAL_SERVER_ERROR("C002", "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR),
  NOT_FOUND("C003", "Resource not found.", HttpStatus.NOT_FOUND),
  METHOD_NOT_ALLOWED("C004", "Method not allowed.", HttpStatus.METHOD_NOT_ALLOWED),
  UNSUPPORTED_MEDIA_TYPE("C005", "Unsupported media type.", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
  CONFLICT("C006", "Conflict occurred.", HttpStatus.CONFLICT),
  UNAUTHORIZED("A001", "Authentication is required.", HttpStatus.UNAUTHORIZED),
  FORBIDDEN("A002", "Access is denied.", HttpStatus.FORBIDDEN),
  SESSION_NOT_FOUND("S001", "Session not found.", HttpStatus.NOT_FOUND),
  INVALID_SESSION_STATUS("S002", "Session is not in the expected status.", HttpStatus.CONFLICT);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;

  ErrorCode(String code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }
}
