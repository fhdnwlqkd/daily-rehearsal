package com.rehearsal.api.rehearsal.controller;

import com.rehearsal.api.rehearsal.controller.dto.SimulationStartResponse;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SimulationController {

  private final StartSimulationUseCase startSimulationUseCase;

  @PostMapping("/{sessionId}/simulation/start")
  public SimulationStartResponse start(@PathVariable @NotBlank String sessionId) {
    SimulationStart result = startSimulationUseCase.startSimulation(sessionId);
    return SimulationStartResponse.from(result);
  }
}
