package com.rehearsal.api.config.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.rehearsal.api")
@RequiredArgsConstructor
public class ApiResponseBodyAdvice implements ResponseBodyAdvice<Object> {

  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body instanceof ApiResponse<?>) {
      return body;
    }

    ApiResponse<?> apiResponse =
        Objects.isNull(body) ? ApiResponse.empty() : ApiResponse.success(body);
    if (StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
      response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
      return writeAsJson(apiResponse);
    }

    return apiResponse;
  }

  private String writeAsJson(ApiResponse<?> apiResponse) {
    try {
      return objectMapper.writeValueAsString(apiResponse);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to write API response as JSON.", exception);
    }
  }
}
