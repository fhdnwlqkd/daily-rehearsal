package com.rehearsal.api.session.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionStatus;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
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
        .perform(post("/api/v1/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").isString())
        .andExpect(jsonPath("$.data.channel").value("P1_OFFLINE"))
        .andExpect(jsonPath("$.data.status").value("BRIEFING"))
        .andExpect(jsonPath("$.data.contextStatus").value("NOT_STARTED"))
        .andExpect(jsonPath("$.data.followUpAttempt").value(0));
  }

  @Test
  void getSession() throws Exception {
    MvcResult createResult =
        mockMvc.perform(post("/api/v1/sessions")).andExpect(status().isOk()).andReturn();

    String sessionId =
        com.jayway.jsonpath.JsonPath.read(
            createResult.getResponse().getContentAsString(), "$.data.sessionId");

    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}", sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.status").value("BRIEFING"))
        .andExpect(jsonPath("$.data.contextStatus").value("NOT_STARTED"))
        .andExpect(jsonPath("$.data.followUpAttempt").value(0));
  }

  @Test
  void getSessionWhenSessionNotFound() throws Exception {
    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}", "missing-session-id"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S001"))
        .andExpect(jsonPath("$.error.name").value("SESSION_NOT_FOUND"))
        .andExpect(jsonPath("$.error.message").value("Session not found."));
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
        .andExpect(jsonPath("$.data.briefingTranscript").value("tomorrow interview rehearsal"));
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
        mockMvc.perform(post("/api/v1/sessions")).andExpect(status().isOk()).andReturn();
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
    public ClientSession createSession() {
      ClientSession session = ClientSession.create(ClientSession.DEFAULT_CHANNEL);
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
      if (session.getStatus() != SessionStatus.BRIEFING) {
        throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
      }

      session.updateBriefingTranscript(transcript);
      session.updateStatus(SessionStatus.CONTEXT_EXTRACTING);
      session.updateContextStatus(ContextStatus.EXTRACTING);
      sessions.put(session.getSessionId(), session);
      return session;
    }
  }
}
