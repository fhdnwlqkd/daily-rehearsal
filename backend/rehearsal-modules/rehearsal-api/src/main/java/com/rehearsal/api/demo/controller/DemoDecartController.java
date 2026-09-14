package com.rehearsal.api.demo.controller;

import com.rehearsal.api.demo.application.DemoApiKeyGuard;
import com.rehearsal.api.demo.controller.dto.DemoDecartTokenResponse;
import com.rehearsal.domain.decart.usecase.IssueDemoDecartTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
public class DemoDecartController {

  private final IssueDemoDecartTokenUseCase issueDemoDecartTokenUseCase;
  private final DemoApiKeyGuard demoApiKeyGuard;

  @PostMapping("/decart-token")
  public DemoDecartTokenResponse issueToken(
      @RequestHeader(value = DemoApiKeyGuard.HEADER_NAME, required = false) String demoApiKey) {
    demoApiKeyGuard.verify(demoApiKey);
    return new DemoDecartTokenResponse(issueDemoDecartTokenUseCase.issueDemoDecartToken());
  }
}
