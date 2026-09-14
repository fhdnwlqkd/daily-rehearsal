package com.rehearsal.api.demo.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rehearsal.domain.decart.port.DecartTokenPort;
import org.junit.jupiter.api.Test;

class DemoDecartTokenServiceTest {

  @Test
  void issuesTokenWithoutSessionData() {
    DecartTokenPort port = () -> "demo-client-token";
    DemoDecartTokenService service = new DemoDecartTokenService(port);

    assertThat(service.issueDemoDecartToken()).isEqualTo("demo-client-token");
  }
}
