package com.rehearsal.api.demo.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Description("프론트 서버만 데모 토큰을 요청할 수 있도록 전용 API key를 검증하는 guard")
@Component
public class DemoApiKeyGuard {

  public static final String HEADER_NAME = "X-DEMO-KEY";

  private final byte[] expectedKey;

  public DemoApiKeyGuard(@Value("${DEMO_API_KEY:}") String expectedKey) {
    this.expectedKey = expectedKey.trim().getBytes(StandardCharsets.UTF_8);
  }

  public void verify(String providedKey) {
    byte[] provided =
        providedKey == null ? new byte[0] : providedKey.trim().getBytes(StandardCharsets.UTF_8);
    if (expectedKey.length == 0 || !MessageDigest.isEqual(expectedKey, provided)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
  }
}
