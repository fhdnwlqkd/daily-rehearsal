package com.rehearsal.api.rehearsal.controller;

import static com.rehearsal.api.support.SimulationTestFixtures.pendingTurn;
import static com.rehearsal.api.support.SimulationTestFixtures.plan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.session.application.SessionReader;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.model.TurnGenerationMode;
import com.rehearsal.domain.rehearsal.usecase.FinishSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SimulationController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class SimulationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StartSimulationUseCase startSimulationUseCase;
  @MockitoBean private FinishSimulationUseCase finishSimulationUseCase;
  @MockitoBean private SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase;
  @MockitoBean private GetTurnEvaluationUseCase getTurnEvaluationUseCase;
  @MockitoBean private SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase;
  @MockitoBean private GetNextOpponentLineUseCase getNextOpponentLineUseCase;
  @MockitoBean private SessionReader sessionReader;

  @Test
  void startSimulation() throws Exception {
    given(startSimulationUseCase.startSimulation("session-id"))
        .willReturn(
            new SimulationStart("session-id", 1, 3, TurnGenerationMode.STATIC, plan("first line")));

    mockMvc
        .perform(post("/api/v1/sessions/session-id/simulation/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.currentTurn").value(1))
        .andExpect(jsonPath("$.data.generationMode").value("STATIC"))
        .andExpect(jsonPath("$.data.opponentLine").value("first line"));
  }

  @Test
  void finishSimulation() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/session-id/simulation/finish"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(finishSimulationUseCase).finishSimulation("session-id");
  }

  @Test
  void submitAndPollEvaluation() throws Exception {
    SimulationTurnAttempt attempt = SimulationTurnAttempt.pending(1L, 1, "answer");
    given(submitTurnEvaluationUseCase.submit(anyString(), anyInt(), anyString(), any()))
        .willReturn(attempt);
    given(getTurnEvaluationUseCase.get("session-id", 1)).willReturn(attempt);
    ClientSession session = org.mockito.Mockito.mock(ClientSession.class);
    given(session.getCurrentTurn()).willReturn(1);
    given(sessionReader.get("session-id")).willReturn(session);

    mockMvc
        .perform(
            post("/api/v1/sessions/session-id/simulation/turns/1/evaluation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"answer\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.attemptNo").value(1))
        .andExpect(jsonPath("$.data.turnCompleted").value(false));

    mockMvc
        .perform(get("/api/v1/sessions/session-id/simulation/turns/1/evaluation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.turnCompleted").value(false));
  }

  @Test
  void submitAndPollOpponentLine() throws Exception {
    SimulationTurn turn = pendingTurn("session-id", 2);
    given(submitNextOpponentLineUseCase.submitNextLine("session-id", 2)).willReturn(turn);
    given(getNextOpponentLineUseCase.getNextLine("session-id", 2)).willReturn(turn);

    mockMvc
        .perform(post("/api/v1/sessions/session-id/simulation/turns/2/next-line"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status").value("PENDING"));

    mockMvc
        .perform(get("/api/v1/sessions/session-id/simulation/turns/2/next-line"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void rejectsBlankEvaluationTranscript() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions/session-id/simulation/turns/1/evaluation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false));
  }
}
