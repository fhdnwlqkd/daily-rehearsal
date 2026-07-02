package com.rehearsal.api.session.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = SessionController.class)
@Import({
  GlobalExceptionHandler.class,
  ApiResponseBodyAdvice.class,
  SessionControllerTest.TestUseCaseConfiguration.class
})
class SessionControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void createSession() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situationType\":\"date\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").isString())
        .andExpect(jsonPath("$.data.situationType").value("date"))
        .andExpect(jsonPath("$.data.status").doesNotExist())
        .andExpect(jsonPath("$.data.contextStatus").doesNotExist())
        .andExpect(jsonPath("$.data.followUpAttempt").doesNotExist());
  }

  @Test
  void createSessionWhenSituationTypeIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situationType\":\"unknown\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C001"))
        .andExpect(jsonPath("$.error.name").value("INVALID_REQUEST"));
  }

  @Test
  void createSessionWhenSituationTypeIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situationType\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C001"));
  }

  @Test
  void submitBriefing() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"tomorrow interview rehearsal\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.status").value("CONTEXT_EXTRACTING"))
        .andExpect(jsonPath("$.data.contextStatus").value("EXTRACTING"))
        .andExpect(jsonPath("$.data.briefingTranscript").doesNotExist());
  }

  @Test
  void submitBriefingWhenInvalidSessionState() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"first briefing\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"second briefing\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"))
        .andExpect(jsonPath("$.error.name").value("INVALID_SESSION_STATE"))
        .andExpect(jsonPath("$.error.message").value("Invalid session state."));
  }

  private String createSessionId() throws Exception {
    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"situationType\":\"date\"}"))
            .andExpect(status().isOk())
            .andReturn();
    return com.jayway.jsonpath.JsonPath.read(
        createResult.getResponse().getContentAsString(), "$.data.sessionId");
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    TestSessionUseCase testSessionUseCase() {
      return new TestSessionUseCase();
    }

    @Bean
    CreateSessionUseCase createSessionUseCase(TestSessionUseCase testSessionUseCase) {
      return testSessionUseCase;
    }

    @Bean
    GetSessionUseCase getSessionUseCase(TestSessionUseCase testSessionUseCase) {
      return testSessionUseCase;
    }

    @Bean
    UpdateClientSessionUseCase updateClientSessionUseCase(TestSessionUseCase testSessionUseCase) {
      return testSessionUseCase;
    }
  }

  static class TestSessionUseCase
      implements CreateSessionUseCase, GetSessionUseCase, UpdateClientSessionUseCase {

    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    @Override
    public ClientSession createSession(SituationType situationType) {
      ClientSession session = ClientSession.create(situationType);
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession getSession(String sessionId) {
      ClientSession session = sessions.get(sessionId);
      if (session == null) {
        throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
      }
      return session;
    }

    @Override
    public ClientSession submitBriefing(String sessionId, String transcript) {
      ClientSession session = getSession(sessionId);
      session.startContextExtraction();
      sessions.put(session.getSessionId(), session);
      return session;
    }
  }
}
