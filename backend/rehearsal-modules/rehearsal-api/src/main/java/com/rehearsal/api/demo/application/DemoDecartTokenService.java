package com.rehearsal.api.demo.application;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.decart.port.DecartTokenPort;
import com.rehearsal.domain.decart.usecase.IssueDemoDecartTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Description("DB 세션 없이 발표 데모의 Decart client token만 발급하는 service")
@Service
@RequiredArgsConstructor
public class DemoDecartTokenService implements IssueDemoDecartTokenUseCase {

  private final DecartTokenPort decartTokenPort;

  @Override
  public String issueDemoDecartToken() {
    return decartTokenPort.issueClientToken();
  }
}
