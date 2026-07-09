package com.rehearsal.api.rehearsal.controller;

import com.rehearsal.api.rehearsal.controller.dto.EvaluationRequest;
import com.rehearsal.api.rehearsal.controller.dto.NextOpponentLineJobResponse;
import com.rehearsal.api.rehearsal.controller.dto.SimulationStartResponse;
import com.rehearsal.api.rehearsal.controller.dto.TurnEvaluationJobResponse;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SimulationController {

  private final StartSimulationUseCase startSimulationUseCase;
  private final SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase;
  private final GetTurnEvaluationUseCase getTurnEvaluationUseCase;
  private final SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase;
  private final GetNextOpponentLineUseCase getNextOpponentLineUseCase;

  @PostMapping("/{sessionId}/simulation/start")
  public SimulationStartResponse start(@PathVariable @NotBlank String sessionId) {
    SimulationStart result = startSimulationUseCase.startSimulation(sessionId);
    return SimulationStartResponse.from(result);
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/simulation/turns/{turnNo}/evaluation")
  public TurnEvaluationJobResponse submitEvaluation(
      @PathVariable @NotBlank String sessionId,
      @PathVariable int turnNo,
      @Valid @RequestBody EvaluationRequest request) {
    TurnMetrics metrics = request.metrics() == null ? null : request.metrics().toDomain();
    TurnEvaluationJob job =
        submitTurnEvaluationUseCase.submit(sessionId, turnNo, request.transcript(), metrics);
    return TurnEvaluationJobResponse.from(job);
  }

  @GetMapping("/{sessionId}/simulation/turns/{turnNo}/evaluation")
  public TurnEvaluationJobResponse getEvaluation(
      @PathVariable @NotBlank String sessionId, @PathVariable int turnNo) {
    TurnEvaluationJob job = getTurnEvaluationUseCase.get(sessionId, turnNo);
    return TurnEvaluationJobResponse.from(job);
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/simulation/turns/{turnNo}/next-line")
  public NextOpponentLineJobResponse submitNextLine(
      @PathVariable @NotBlank String sessionId, @PathVariable int turnNo) {
    OpponentLineJob job = submitNextOpponentLineUseCase.submitNextLine(sessionId, turnNo);
    return NextOpponentLineJobResponse.from(job);
  }

  @GetMapping("/{sessionId}/simulation/turns/{turnNo}/next-line")
  public NextOpponentLineJobResponse getNextLine(
      @PathVariable @NotBlank String sessionId, @PathVariable int turnNo) {
    OpponentLineJob job = getNextOpponentLineUseCase.getNextLine(sessionId, turnNo);
    return NextOpponentLineJobResponse.from(job);
  }
}
