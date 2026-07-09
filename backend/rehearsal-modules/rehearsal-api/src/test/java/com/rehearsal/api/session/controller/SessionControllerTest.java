package com.rehearsal.api.session.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.extraction.model.ContextExtractionJob;
import com.rehearsal.domain.extraction.model.ContextExtractionJobType;
import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
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
  @Autowired private TestSessionUseCase testSessionUseCase;

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
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.jobId").isString())
        .andExpect(jsonPath("$.data.type").value("BRIEFING"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.finalContext").doesNotExist())
        .andExpect(jsonPath("$.data.briefingTranscript").doesNotExist());
  }

  @Test
  void getContextExtraction() throws Exception {
    String sessionId = createSessionId();
    String jobId = testSessionUseCase.seedCompletedContextExtractionJob(sessionId);

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/context-extractions/{jobId}", sessionId, jobId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.jobId").value(jobId))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.finalContext.situation_type").value("date"))
        .andExpect(jsonPath("$.data.finalContext.desired_persona").value("warm_natural"));
  }

  @Test
  void submitFollowUp() throws Exception {
    String sessionId = testSessionUseCase.seedFollowUpRequiredSession();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/follow-up", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"focus on first greeting\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.jobId").isString())
        .andExpect(jsonPath("$.data.type").value("FOLLOW_UP"))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.finalContext").doesNotExist());
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

    @Bean
    SubmitContextExtractionUseCase submitContextExtractionUseCase(
        TestSessionUseCase testSessionUseCase) {
      return testSessionUseCase;
    }

    @Bean
    GetContextExtractionUseCase getContextExtractionUseCase(TestSessionUseCase testSessionUseCase) {
      return testSessionUseCase;
    }
  }

  static class TestSessionUseCase
      implements CreateSessionUseCase,
          GetSessionUseCase,
          UpdateClientSessionUseCase,
          SubmitContextExtractionUseCase,
          GetContextExtractionUseCase {

    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ContextExtractionJob> jobs = new ConcurrentHashMap<>();

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
    public ContextExtractionJob submitBriefingExtraction(String sessionId, String transcript) {
      ClientSession session = getSession(sessionId);
      ContextExtractionJob job =
          ContextExtractionJob.pending(
              session.getSessionId(),
              session.getSituationType(),
              ContextExtractionJobType.BRIEFING);
      jobs.put(jobKey(sessionId, job.jobId()), job);
      return job;
    }

    @Override
    public ContextExtractionJob submitFollowUpExtraction(String sessionId, String transcript) {
      ClientSession session = getSession(sessionId);
      ContextExtractionJob job =
          ContextExtractionJob.pending(
              session.getSessionId(),
              session.getSituationType(),
              ContextExtractionJobType.FOLLOW_UP);
      jobs.put(jobKey(sessionId, job.jobId()), job);
      return job;
    }

    @Override
    public ContextExtractionJob get(String sessionId, String jobId) {
      ContextExtractionJob job = jobs.get(jobKey(sessionId, jobId));
      if (job == null) {
        throw new BusinessException(ErrorCode.CONTEXT_EXTRACTION_JOB_NOT_FOUND);
      }
      return job;
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
          ClientSession.builder()
              .sessionId(java.util.UUID.randomUUID().toString())
              .situationType(com.rehearsal.domain.situation.model.SituationType.DATE)
              .status(com.rehearsal.domain.session.model.SessionStatus.TRANSFORMATION_READY)
              .contextStatus(ContextStatus.COMPLETED)
              .partialContext(SessionContext.empty(SituationType.DATE))
              .finalContext(
                  SessionContext.from(SituationType.DATE, Map.of("situationType", "date")))
              .build();
      sessions.put(session.getSessionId(), session);
      return session.getSessionId();
    }

    String seedFollowUpRequiredSession() {
      ClientSession session = ClientSession.create(SituationType.DATE);
      session.startContextExtraction();
      session.requireFollowUp(
          SessionContext.from(
              SituationType.DATE,
              Map.of("situation_type", "date", "desired_persona", "warm_natural")),
          java.util.List.of("critical_moment"),
          java.util.List.of("Which moment are you most worried about?"));
      sessions.put(session.getSessionId(), session);
      return session.getSessionId();
    }

    String seedCompletedContextExtractionJob(String sessionId) {
      ClientSession session = getSession(sessionId);
      ContextExtractionJob job =
          ContextExtractionJob.pending(
                  sessionId, session.getSituationType(), ContextExtractionJobType.BRIEFING)
              .completeWithFinalContext(
                  SessionContext.from(
                      SituationType.DATE, Map.of("desired_persona", "warm_natural")));
      jobs.put(jobKey(sessionId, job.jobId()), job);
      return job.jobId();
    }

    private String jobKey(String sessionId, String jobId) {
      return sessionId + ":" + jobId;
    }
  }
}
