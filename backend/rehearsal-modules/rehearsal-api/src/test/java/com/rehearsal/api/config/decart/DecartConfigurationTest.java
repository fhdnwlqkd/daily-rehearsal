package com.rehearsal.api.config.decart;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.decart.port.DecartTokenPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DecartConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(DecartConfiguration.class);

  @Test
  void disabledConfigurationBlocksTokenIssuanceWithoutApiKey() {
    contextRunner
        .withPropertyValues("rehearsal.decart.enabled=false")
        .run(
            context ->
                assertThatThrownBy(() -> context.getBean(DecartTokenPort.class).issueClientToken())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Decart is disabled for this deployment"));
  }
}
