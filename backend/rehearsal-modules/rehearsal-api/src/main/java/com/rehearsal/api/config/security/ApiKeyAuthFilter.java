package com.rehearsal.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.api.config.response.ApiResponse;
import com.rehearsal.domain.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthFilter extends OncePerRequestFilter {

  private final ApiKeyAuthProperties properties;
  private final ObjectMapper objectMapper;

  public ApiKeyAuthFilter(ApiKeyAuthProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !properties.isEnabled();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isAuthorized(request)) {
      writeUnauthorized(response);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private boolean isAuthorized(HttpServletRequest request) {
    String expectedKey = properties.getKey();
    if (expectedKey == null || expectedKey.isBlank()) {
      return false;
    }
    String providedKey = request.getHeader(properties.getHeaderName());
    if (providedKey == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expectedKey.getBytes(StandardCharsets.UTF_8), providedKey.getBytes(StandardCharsets.UTF_8));
  }

  private void writeUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response
        .getWriter()
        .write(objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.UNAUTHORIZED)));
  }
}
