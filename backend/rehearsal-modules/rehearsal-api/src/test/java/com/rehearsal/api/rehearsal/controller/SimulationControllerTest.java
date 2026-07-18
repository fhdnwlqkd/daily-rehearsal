package com.rehearsal.api.rehearsal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.SimulationTurn;
import com.rehearsal.domain.rehearsal.model.SimulationTurnAttempt;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
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
  @MockitoBean private SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase;
  @MockitoBean private GetTurnEvaluationUseCase getTurnEvaluationUseCase;
  @MockitoBean private SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase;
  @MockitoBean private GetNextOpponentLineUseCase getNextOpponentLineUseCase;

  @Test
  void startSimulation() throws Exception {
    given(startSimulationUseCase.startSimulation("session-id"))
        .willReturn(new SimulationStart("session-id", 1, 3, "first line"));

    mockMvc
        .perform(post("/api/v1/sessions/session-id/simulation/start"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.currentTurn").value(1))
        .andExpect(jsonPath("$.data.opponentLine").value("first line"));
  }

  @Test
  void submitAndPollEvaluation() throws Exception {
    SimulationTurnAttempt attempt = SimulationTurnAttempt.pending(1L, 1, "answer");
    given(submitTurnEvaluationUseCase.submit(anyString(), anyInt(), anyString(), any()))
        .willReturn(attempt);
    given(getTurnEvaluationUseCase.get("session-id", 1)).willReturn(attempt);

    mockMvc
        .perform(
            post("/api/v1/sessions/session-id/simulation/turns/1/evaluation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"answer\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.attemptNo").value(1));

    mockMvc
        .perform(get("/api/v1/sessions/session-id/simulation/turns/1/evaluation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void submitAndPollOpponentLine() throws Exception {
    SimulationTurn turn = SimulationTurn.pending("session-id", 2);
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
}
