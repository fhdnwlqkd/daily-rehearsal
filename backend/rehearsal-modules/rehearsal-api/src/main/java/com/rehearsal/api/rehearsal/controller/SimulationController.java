package com.rehearsal.api.rehearsal.controller;

import com.rehearsal.api.rehearsal.controller.dto.EvaluationRequest;
import com.rehearsal.api.rehearsal.controller.dto.EvaluationResponse;
import com.rehearsal.api.rehearsal.controller.dto.SimulationStartResponse;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.usecase.EvaluateTurnUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
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
public class SimulationController {

  private final StartSimulationUseCase startSimulationUseCase;
  private final EvaluateTurnUseCase evaluateTurnUseCase;

  @PostMapping("/{sessionId}/simulation/start")
  public SimulationStartResponse start(@PathVariable @NotBlank String sessionId) {
    SimulationStart result = startSimulationUseCase.startSimulation(sessionId);
    return SimulationStartResponse.from(result);
  }

  @PostMapping("/{sessionId}/simulation/turns/{turnNo}/evaluation")
  public EvaluationResponse evaluate(
      @PathVariable @NotBlank String sessionId,
      @PathVariable int turnNo,
      @Valid @RequestBody EvaluationRequest request) {
    TurnMetrics metrics = request.metrics() == null ? null : request.metrics().toDomain();
    TurnEvaluationResult result =
        evaluateTurnUseCase.evaluateTurn(sessionId, turnNo, request.transcript(), metrics);
    return EvaluationResponse.from(result);
  }
}
