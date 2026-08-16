package com.rehearsal.api.rehearsal.controller;

import com.rehearsal.api.rehearsal.controller.dto.EvaluationRequest;
import com.rehearsal.api.rehearsal.controller.dto.OpponentLineResponse;
import com.rehearsal.api.rehearsal.controller.dto.SimulationStartResponse;
import com.rehearsal.api.rehearsal.controller.dto.TurnEvaluationResponse;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.usecase.FinishSimulationUseCase;
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
  private final FinishSimulationUseCase finishSimulationUseCase;
  private final SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase;
  private final GetTurnEvaluationUseCase getTurnEvaluationUseCase;
  private final SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase;
  private final GetNextOpponentLineUseCase getNextOpponentLineUseCase;
  private final SessionReader sessionReader;

  @PostMapping("/{sessionId}/simulation/start")
  public SimulationStartResponse start(@PathVariable @NotBlank String sessionId) {
    SimulationStart result = startSimulationUseCase.startSimulation(sessionId);
    return SimulationStartResponse.from(result);
  }

  @PostMapping("/{sessionId}/simulation/finish")
  public void finish(@PathVariable @NotBlank String sessionId) {
    finishSimulationUseCase.finishSimulation(sessionId);
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/simulation/turns/{turnNo}/evaluation")
  public TurnEvaluationResponse submitEvaluation(
      @PathVariable @NotBlank String sessionId,
      @PathVariable int turnNo,
      @Valid @RequestBody EvaluationRequest request) {
    TurnMetrics metrics = request.metrics() == null ? null : request.metrics().toDomain();
    SimulationTurnAttempt attempt =
        submitTurnEvaluationUseCase.submit(sessionId, turnNo, request.transcript(), metrics);
    return TurnEvaluationResponse.from(sessionId, turnNo, attempt, false);
  }

  @GetMapping("/{sessionId}/simulation/turns/{turnNo}/evaluation")
  public TurnEvaluationResponse getEvaluation(
      @PathVariable @NotBlank String sessionId, @PathVariable int turnNo) {
    SimulationTurnAttempt attempt = getTurnEvaluationUseCase.get(sessionId, turnNo);
    boolean turnCompleted = sessionReader.get(sessionId).getCurrentTurn() > turnNo;
    return TurnEvaluationResponse.from(sessionId, turnNo, attempt, turnCompleted);
  }

  @ResponseStatus(HttpStatus.ACCEPTED)
  @PostMapping("/{sessionId}/simulation/turns/{turnNo}/next-line")
  public OpponentLineResponse submitNextLine(
      @PathVariable @NotBlank String sessionId, @PathVariable int turnNo) {
    SimulationTurn turn = submitNextOpponentLineUseCase.submitNextLine(sessionId, turnNo);
    return OpponentLineResponse.from(turn);
  }

  @GetMapping("/{sessionId}/simulation/turns/{turnNo}/next-line")
  public OpponentLineResponse getNextLine(
      @PathVariable @NotBlank String sessionId, @PathVariable int turnNo) {
    SimulationTurn turn = getNextOpponentLineUseCase.getNextLine(sessionId, turnNo);
    return OpponentLineResponse.from(turn);
  }
}
