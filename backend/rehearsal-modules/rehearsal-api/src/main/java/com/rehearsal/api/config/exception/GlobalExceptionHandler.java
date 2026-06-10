package com.rehearsal.api.config.exception;

import com.rehearsal.api.config.response.ApiResponse;
import com.rehearsal.api.config.response.ErrorResponse;
import com.rehearsal.api.config.response.FieldErrorDetail;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice(basePackages = "com.rehearsal.api")
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
    ErrorCode errorCode = exception.getErrorCode();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(ApiResponse.fail(errorCode, exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    List<FieldErrorDetail> details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(this::toFieldErrorDetail)
            .toList();

    ErrorResponse error =
        ErrorResponse.of(
            ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage(), details);
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus())
        .body(ApiResponse.fail(error));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
      ConstraintViolationException exception) {
    List<FieldErrorDetail> details =
        exception.getConstraintViolations().stream()
            .map(
                violation ->
                    new FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()))
            .toList();

    ErrorResponse error =
        ErrorResponse.of(
            ErrorCode.INVALID_REQUEST, ErrorCode.INVALID_REQUEST.getMessage(), details);
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus())
        .body(ApiResponse.fail(error));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleInvalidRequestException(Exception exception) {
    return ResponseEntity.status(ErrorCode.INVALID_REQUEST.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.INVALID_REQUEST));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
      HttpRequestMethodNotSupportedException exception) {
    return ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.METHOD_NOT_ALLOWED));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupportedException(
      HttpMediaTypeNotSupportedException exception) {
    return ResponseEntity.status(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
      NoResourceFoundException exception) {
    return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.NOT_FOUND));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
    log.error("Unhandled exception occurred.", exception);
    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
        .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
  }

  private FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
    return new FieldErrorDetail(
        fieldError.getField(), fieldError.getDefaultMessage(), fieldError.getRejectedValue());
  }
}
