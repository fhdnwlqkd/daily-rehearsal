package com.rehearsal.api.situation.controller;

import com.rehearsal.api.situation.controller.dto.SituationTypeResponse;
import com.rehearsal.domain.situation.usecase.GetSituationTypesUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/situation-types")
@RequiredArgsConstructor
public class SituationTypeController {

  private final GetSituationTypesUseCase getSituationTypesUseCase;

  @GetMapping
  public List<SituationTypeResponse> getSituationTypes() {
    return getSituationTypesUseCase.getSituationTypes().stream()
        .map(SituationTypeResponse::from)
        .toList();
  }
}
