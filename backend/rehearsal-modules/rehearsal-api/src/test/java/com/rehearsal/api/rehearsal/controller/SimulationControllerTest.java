package com.rehearsal.api.rehearsal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SimulationController.class)
@Import({
  GlobalExceptionHandler.class,
  ApiResponseBodyAdvice.class,
  SimulationControllerTest.TestUseCaseConfiguration.class
})
class SimulationControllerTest {

  private static final String VALID_SESSION_ID = "valid-session-id";
  private static final String INVALID_STATE_SESSION_ID = "invalid-state-session-id";

  @Autowired private MockMvc mockMvc;

  @Test
  void startsSimulation() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/simulation/start", VALID_SESSION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(VALID_SESSION_ID))
        .andExpect(jsonPath("$.data.currentTurn").value(1))
        .andExpect(jsonPath("$.data.maxTurn").value(3))
        .andExpect(jsonPath("$.data.opponentLine").isString());
  }

  @Test
  void startSimulationReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/simulation/start", "not-found-session"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S001"));
  }

  @Test
  void startSimulationReturnsConflictWhenSessionStateIsInvalid() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/simulation/start", INVALID_STATE_SESSION_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"));
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    StartSimulationUseCase startSimulationUseCase() {
      return new TestStartSimulationUseCase();
    }
  }

  static class TestStartSimulationUseCase implements StartSimulationUseCase {

    @Override
    public SimulationStart startSimulation(String sessionId) {
      if (sessionId.equals("not-found-session")) {
        throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
      }
      if (sessionId.equals(INVALID_STATE_SESSION_ID)) {
        throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
      }
      return new SimulationStart(sessionId, 1, 3, "안녕하세요, 만나서 반가워요! 오늘 어떻게 지내셨어요?");
    }
  }
}
