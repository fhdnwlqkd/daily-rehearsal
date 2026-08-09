package com.rehearsal.api.ticket.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketPayload;
import com.rehearsal.domain.ticket.usecase.GetTicketGenerationUseCase;
import com.rehearsal.domain.ticket.usecase.SubmitTicketGenerationUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TicketController.class)
@Import({
  GlobalExceptionHandler.class,
  ApiResponseBodyAdvice.class,
  TicketControllerTest.TestUseCaseConfiguration.class
})
class TicketControllerTest {

  private static final String VALID_SESSION_ID = "valid-session-id";
  private static final String NOT_COMPLETED_SESSION_ID = "not-completed-session-id";
  private static final String NOT_FOUND_SESSION_ID = "not-found-session-id";

  @Autowired private MockMvc mockMvc;

  @Test
  void submitTicketReturnsAcceptedWithPendingStatus() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/ticket", VALID_SESSION_ID))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void submitTicketReturnsConflictWhenSimulationNotCompleted() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/ticket", NOT_COMPLETED_SESSION_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S009"));
  }

  @Test
  void getTicketReturnsCompletedResult() throws Exception {
    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}/ticket", VALID_SESSION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.title").isString())
        .andExpect(jsonPath("$.data.downloadUrl").isString())
        .andExpect(jsonPath("$.data.qrPayload").isString());
  }

  @Test
  void getTicketReturnsNotFoundWhenJobDoesNotExist() throws Exception {
    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}/ticket", NOT_FOUND_SESSION_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S010"));
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    SubmitTicketGenerationUseCase submitTicketGenerationUseCase() {
      return new TestSubmitTicketGenerationUseCase();
    }

    @Bean
    GetTicketGenerationUseCase getTicketGenerationUseCase() {
      return new TestGetTicketGenerationUseCase();
    }
  }

  static class TestSubmitTicketGenerationUseCase implements SubmitTicketGenerationUseCase {

    @Override
    public TicketJob submit(String sessionId) {
      if (sessionId.equals(NOT_COMPLETED_SESSION_ID)) {
        throw new BusinessException(ErrorCode.SIMULATION_NOT_COMPLETED);
      }
      return TicketJob.pending(sessionId);
    }
  }

  static class TestGetTicketGenerationUseCase implements GetTicketGenerationUseCase {

    @Override
    public TicketJob get(String sessionId) {
      if (sessionId.equals(NOT_FOUND_SESSION_ID)) {
        throw new BusinessException(ErrorCode.TICKET_JOB_NOT_FOUND);
      }
      return TicketJob.pending(sessionId)
          .complete(
              new TicketPayload(
                  "리허설 완료!",
                  "잘 하셨어요.",
                  false,
                  SituationType.DATE,
                  "test-outfit-id",
                  List.of(),
                  List.of(),
                  "http://localhost/mock-videos/test-session-id.webm",
                  true,
                  "http://localhost/mock-videos/test-session-id.webm",
                  "http://localhost/mock-videos/test-session-id.webm"));
    }
  }
}
