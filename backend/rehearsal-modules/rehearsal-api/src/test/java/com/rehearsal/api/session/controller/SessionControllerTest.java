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
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.GetSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.session.usecase.command.CompleteSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.CreateSessionCommand;
import com.rehearsal.domain.session.usecase.command.GetSessionCommand;
import com.rehearsal.domain.session.usecase.command.UpdateBriefingTranscriptCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFeedbackResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateFinalResultCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSelectedOutfitCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSessionContextCommand;
import com.rehearsal.domain.session.usecase.command.UpdateSimulationDraftCommand;
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
  void createSessionWithChannel() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"channel\":\"P1_TEST\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.channel").value("P1_TEST"));
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
  void updateBriefingTranscript() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/sessions/{sessionId}/briefing-transcript", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"briefingTranscript\":\"tomorrow interview rehearsal\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.status").value("CONTEXT_EXTRACTING"))
        .andExpect(jsonPath("$.data.contextStatus").value("EXTRACTING"))
        .andExpect(jsonPath("$.data.briefingTranscript").value("tomorrow interview rehearsal"));
  }

  @Test
  void updateContextWhenFollowUpRequired() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/sessions/{sessionId}/context", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "partialContext": {"situation_type": "interview"},
                      "missingRequiredSlotKeys": ["desired_persona"],
                      "followUpQuestion": "What impression do you want to give?"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("FOLLOW_UP_REQUIRED"))
        .andExpect(jsonPath("$.data.contextStatus").value("FOLLOW_UP_REQUIRED"))
        .andExpect(jsonPath("$.data.followUpAttempt").value(1))
        .andExpect(jsonPath("$.data.partialContext.situation_type").value("interview"))
        .andExpect(jsonPath("$.data.missingRequiredSlotKeys[0]").value("desired_persona"))
        .andExpect(
            jsonPath("$.data.followUpQuestion").value("What impression do you want to give?"));
  }

  @Test
  void completeContext() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/sessions/{sessionId}/context/complete", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"finalUserContext\":{\"situation_type\":\"interview\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("TRANSFORMATION_READY"))
        .andExpect(jsonPath("$.data.contextStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.finalUserContext.situation_type").value("interview"));
  }

  @Test
  void updateSelectedOutfit() throws Exception {
    String sessionId = createSessionId();

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(
                    "/api/v1/sessions/{sessionId}/selected-outfit", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedOutfitId\":\"outfit-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REHEARSAL_READY"))
        .andExpect(jsonPath("$.data.selectedOutfitId").value("outfit-1"));
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
    public ClientSession createSession(CreateSessionCommand command) {
      ClientSession session = ClientSession.create(command.channel());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession getSession(GetSessionCommand command) {
      ClientSession session = sessions.get(command.sessionId());
      if (session == null) {
        throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
      }
      return session;
    }

    @Override
    public ClientSession updateBriefingTranscript(UpdateBriefingTranscriptCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .updateBriefingTranscript(command.briefingTranscript());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession updateContext(UpdateSessionContextCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .updateContext(
                  command.partialContext(),
                  command.missingRequiredSlotKeys(),
                  command.followUpQuestion());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession completeContext(CompleteSessionContextCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .completeContext(command.finalUserContext());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession updateSelectedOutfit(UpdateSelectedOutfitCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .updateSelectedOutfit(command.selectedOutfitId());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession updateSimulationDraft(UpdateSimulationDraftCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .updateSimulationDraft(command.simulationDraft());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession updateFeedbackResult(UpdateFeedbackResultCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .updateFeedbackResult(command.feedbackResult());
      sessions.put(session.getSessionId(), session);
      return session;
    }

    @Override
    public ClientSession updateFinalResult(UpdateFinalResultCommand command) {
      ClientSession session =
          getSession(new GetSessionCommand(command.sessionId()))
              .updateFinalResult(command.finalResult());
      sessions.put(session.getSessionId(), session);
      return session;
    }
  }
}
