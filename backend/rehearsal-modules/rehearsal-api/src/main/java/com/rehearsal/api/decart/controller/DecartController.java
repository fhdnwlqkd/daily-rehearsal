package com.rehearsal.api.decart.controller;

import com.rehearsal.api.decart.controller.dto.DecartSpecResponse;
import com.rehearsal.api.decart.controller.dto.DecartTokenRequest;
import com.rehearsal.domain.decart.usecase.GetDecartSpecUseCase;
import com.rehearsal.domain.decart.usecase.result.DecartSpecResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class DecartController {

  private final GetDecartSpecUseCase getDecartSpecUseCase;

  @PostMapping("/{sessionId}/decart-token")
  public DecartSpecResponse issue(
      @PathVariable @NotBlank String sessionId,
      @Valid @RequestBody DecartTokenRequest request) {
    DecartSpecResult result = getDecartSpecUseCase.getDecartSpec(sessionId, request.outfitId());
    return DecartSpecResponse.from(result);
  }
}
