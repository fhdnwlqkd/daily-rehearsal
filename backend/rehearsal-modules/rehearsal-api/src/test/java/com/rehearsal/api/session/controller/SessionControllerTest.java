package com.rehearsal.api.session.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
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
  @Autowired private TestSessionUseCase testSessionUseCase;

  @Test
  void createSession() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").isString())
        .andExpect(jsonPath("$.data.situationType").value("date"))
        .andExpect(jsonPath("$.data.status").doesNotExist())
        .andExpect(jsonPath("$.data.contextStatus").doesNotExist())
        .andExpect(jsonPath("$.data.followUpAttempt").doesNotExist());
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
        .andExpect(jsonPath("$.data.status").doesNotExist())
        .andExpect(jsonPath("$.data.contextStatus").doesNotExist())
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

  @Test
  void confirmOutfit() throws Exception {
    String sessionId = testSessionUseCase.seedTransformationReadySession();

    mockMvc
        .perform(
            patch("/api/v1/sessions/{sessionId}/outfit", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedOutfitId\":\"presentation_jacket_01\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.status").doesNotExist())
        .andExpect(jsonPath("$.data.selectedOutfitId").doesNotExist());
  }

  @Test
  void confirmOutfitWhenInvalidSessionState() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            patch("/api/v1/sessions/{sessionId}/outfit", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedOutfitId\":\"presentation_jacket_01\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"))
        .andExpect(jsonPath("$.error.name").value("INVALID_SESSION_STATE"));
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
      ClientSession session = ClientSession.create();
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

    @Override
    public ClientSession confirmOutfit(String sessionId, String selectedOutfitId) {
      ClientSession session = getSession(sessionId);
      session.confirmOutfit(selectedOutfitId);
      sessions.put(session.getSessionId(), session);
      return session;
    }

    String seedTransformationReadySession() {
      ClientSession session =
          ClientSession.restore(
              ClientSession.Snapshot.builder()
                  .sessionId(java.util.UUID.randomUUID().toString())
                  .situationType(com.rehearsal.domain.situation.model.SituationType.DATE)
                  .status(com.rehearsal.domain.session.model.SessionStatus.TRANSFORMATION_READY)
                  .contextStatus(ContextStatus.COMPLETED)
                  .partialContext(Map.of())
                  .finalContext(Map.of("situationType", "date"))
                  .build());
      sessions.put(session.getSessionId(), session);
      return session.getSessionId();
    }
  }
}
