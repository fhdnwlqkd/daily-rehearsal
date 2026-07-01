package com.rehearsal.api.decart.controller;

import com.rehearsal.api.decart.controller.dto.DecartTokenResponse;
import com.rehearsal.api.decart.controller.dto.OutfitSpecResponse;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.usecase.GetOutfitSpecUseCase;
import com.rehearsal.domain.decart.usecase.IssueDecartTokenUseCase;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class DecartController {

  private final IssueDecartTokenUseCase issueDecartTokenUseCase;
  private final GetOutfitSpecUseCase getOutfitSpecUseCase;

  @PostMapping("/{sessionId}/decart-token")
  public DecartTokenResponse issueToken(@PathVariable @NotBlank String sessionId) {
    String clientToken = issueDecartTokenUseCase.issueDecartToken(sessionId);
    return new DecartTokenResponse(clientToken);
  }

  @GetMapping("/{sessionId}/outfit-spec")
  public OutfitSpecResponse getOutfitSpec(
      @PathVariable @NotBlank String sessionId, @RequestParam @NotBlank String outfitId) {
    DecartSpec spec = getOutfitSpecUseCase.getOutfitSpec(sessionId, outfitId);
    return OutfitSpecResponse.from(spec);
  }
}
