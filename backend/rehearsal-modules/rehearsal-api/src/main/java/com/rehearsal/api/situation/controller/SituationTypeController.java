package com.rehearsal.api.situation.controller;

import com.rehearsal.api.situation.controller.dto.SituationTypeBriefingResponse;
import com.rehearsal.api.situation.controller.dto.SituationTypeResponse;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.situation.usecase.GetSituationTypeBriefingUseCase;
import com.rehearsal.domain.situation.usecase.GetSituationTypesUseCase;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/situation-types")
@RequiredArgsConstructor
public class SituationTypeController {

  private final GetSituationTypesUseCase getSituationTypesUseCase;
  private final GetSituationTypeBriefingUseCase getSituationTypeBriefingUseCase;

  @GetMapping
  public List<SituationTypeResponse> getSituationTypes() {
    return getSituationTypesUseCase.getSituationTypes().stream()
        .map(SituationTypeResponse::from)
        .toList();
  }

  @GetMapping("/{situationType}/briefing")
  public SituationTypeBriefingResponse getSituationTypeBriefing(
      @PathVariable @NotBlank String situationType) {
    SituationType situation =
        getSituationTypeBriefingUseCase.getSituationTypeBriefing(situationType);
    return SituationTypeBriefingResponse.from(situation);
  }
}
