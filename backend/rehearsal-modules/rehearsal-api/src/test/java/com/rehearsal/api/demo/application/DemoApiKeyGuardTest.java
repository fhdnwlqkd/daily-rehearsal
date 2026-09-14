package com.rehearsal.api.demo.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class DemoApiKeyGuardTest {

  @Test
  void acceptsMatchingKey() {
    DemoApiKeyGuard guard = new DemoApiKeyGuard("demo-api-key");

    assertThatCode(() -> guard.verify("demo-api-key")).doesNotThrowAnyException();
  }

  @Test
  void rejectsMissingOrMismatchedKey() {
    DemoApiKeyGuard guard = new DemoApiKeyGuard("demo-api-key");

    assertThatThrownBy(() -> guard.verify(null))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.UNAUTHORIZED);
    assertThatThrownBy(() -> guard.verify("wrong-key"))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }

  @Test
  void failsClosedWhenServerKeyIsMissing() {
    DemoApiKeyGuard guard = new DemoApiKeyGuard("");

    assertThatThrownBy(() -> guard.verify(""))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }
}
