package com.rehearsal.api.rehearsal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.rehearsal.controller.dto.EvaluationRequest;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import com.rehearsal.domain.rehearsal.model.OpponentLineResult;
import com.rehearsal.domain.rehearsal.model.SimulationStart;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationJob;
import com.rehearsal.domain.rehearsal.model.TurnEvaluationResult;
import com.rehearsal.domain.rehearsal.model.TurnMetrics;
import com.rehearsal.domain.rehearsal.usecase.GetNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.GetTurnEvaluationUseCase;
import com.rehearsal.domain.rehearsal.usecase.StartSimulationUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitNextOpponentLineUseCase;
import com.rehearsal.domain.rehearsal.usecase.SubmitTurnEvaluationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
  private static final String TURN_MISMATCH_SESSION_ID = "turn-mismatch-session-id";
  private static final String AI_FAILURE_SESSION_ID = "ai-failure-session-id";
  private static final int NOT_FOUND_TURN_NO = 99;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

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

  @Test
  void submitEvaluationReturnsAcceptedWithPendingStatus() throws Exception {
    EvaluationRequest request = new EvaluationRequest("네, 여유 있게 도착했어요.", null);

    mockMvc
        .perform(
            post(
                    "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation",
                    VALID_SESSION_ID,
                    1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void submitEvaluationReturnsConflictWhenTurnNoMismatches() throws Exception {
    EvaluationRequest request = new EvaluationRequest("transcript", null);

    mockMvc
        .perform(
            post(
                    "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation",
                    TURN_MISMATCH_SESSION_ID,
                    2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S004"));
  }

  @Test
  void submitEvaluationReturnsBadRequestWhenTranscriptBlank() throws Exception {
    EvaluationRequest request = new EvaluationRequest(" ", null);

    mockMvc
        .perform(
            post(
                    "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation",
                    VALID_SESSION_ID,
                    1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C001"));
  }

  @Test
  void getEvaluationReturnsCompletedResult() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation",
                VALID_SESSION_ID,
                1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.success").value(true))
        .andExpect(jsonPath("$.data.feedback").isString());
  }

  @Test
  void getEvaluationReturnsFailedResultWithFixedMessage() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation",
                AI_FAILURE_SESSION_ID,
                1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("FAILED"))
        .andExpect(jsonPath("$.data.message").value("다시 시도해보세요."));
  }

  @Test
  void getEvaluationReturnsNotFoundWhenJobDoesNotExist() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/evaluation",
                VALID_SESSION_ID,
                NOT_FOUND_TURN_NO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S005"));
  }

  @Test
  void submitNextLineReturnsAcceptedWithPendingStatus() throws Exception {
    mockMvc
        .perform(
            post(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/next-line",
                VALID_SESSION_ID,
                1))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void submitNextLineReturnsConflictWhenTurnNoMismatches() throws Exception {
    mockMvc
        .perform(
            post(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/next-line",
                TURN_MISMATCH_SESSION_ID,
                2))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S004"));
  }

  @Test
  void getNextLineReturnsCompletedResult() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/next-line",
                VALID_SESSION_ID,
                1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.opponentLine").isString());
  }

  @Test
  void getNextLineReturnsNotFoundWhenJobDoesNotExist() throws Exception {
    mockMvc
        .perform(
            get(
                "/api/v1/sessions/{sessionId}/simulation/turns/{turnNo}/next-line",
                VALID_SESSION_ID,
                NOT_FOUND_TURN_NO))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S006"));
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    StartSimulationUseCase startSimulationUseCase() {
      return new TestStartSimulationUseCase();
    }

    @Bean
    SubmitTurnEvaluationUseCase submitTurnEvaluationUseCase() {
      return new TestSubmitTurnEvaluationUseCase();
    }

    @Bean
    GetTurnEvaluationUseCase getTurnEvaluationUseCase() {
      return new TestGetTurnEvaluationUseCase();
    }

    @Bean
    SubmitNextOpponentLineUseCase submitNextOpponentLineUseCase() {
      return new TestSubmitNextOpponentLineUseCase();
    }

    @Bean
    GetNextOpponentLineUseCase getNextOpponentLineUseCase() {
      return new TestGetNextOpponentLineUseCase();
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

  static class TestSubmitTurnEvaluationUseCase implements SubmitTurnEvaluationUseCase {

    @Override
    public TurnEvaluationJob submit(
        String sessionId, int turnNo, String userTranscript, TurnMetrics metrics) {
      if (sessionId.equals(TURN_MISMATCH_SESSION_ID)) {
        throw new BusinessException(ErrorCode.SIMULATION_TURN_MISMATCH);
      }
      return TurnEvaluationJob.pending(sessionId, turnNo);
    }
  }

  static class TestGetTurnEvaluationUseCase implements GetTurnEvaluationUseCase {

    @Override
    public TurnEvaluationJob get(String sessionId, int turnNo) {
      if (sessionId.equals(VALID_SESSION_ID) && turnNo == NOT_FOUND_TURN_NO) {
        throw new BusinessException(ErrorCode.TURN_EVALUATION_JOB_NOT_FOUND);
      }
      if (sessionId.equals(AI_FAILURE_SESSION_ID)) {
        return TurnEvaluationJob.pending(sessionId, turnNo).fail("Gemini timeout");
      }
      return TurnEvaluationJob.pending(sessionId, turnNo)
          .complete(new TurnEvaluationResult(true, "자연스러운 답변입니다.", false));
    }
  }

  static class TestSubmitNextOpponentLineUseCase implements SubmitNextOpponentLineUseCase {

    @Override
    public OpponentLineJob submitNextLine(String sessionId, int turnNo) {
      if (sessionId.equals(TURN_MISMATCH_SESSION_ID)) {
        throw new BusinessException(ErrorCode.SIMULATION_TURN_MISMATCH);
      }
      return OpponentLineJob.pending(sessionId, turnNo);
    }
  }

  static class TestGetNextOpponentLineUseCase implements GetNextOpponentLineUseCase {

    @Override
    public OpponentLineJob getNextLine(String sessionId, int turnNo) {
      if (sessionId.equals(VALID_SESSION_ID) && turnNo == NOT_FOUND_TURN_NO) {
        throw new BusinessException(ErrorCode.NEXT_LINE_JOB_NOT_FOUND);
      }
      return OpponentLineJob.pending(sessionId, turnNo)
          .complete(new OpponentLineResult("다음 발화입니다.", false));
    }
  }
}
