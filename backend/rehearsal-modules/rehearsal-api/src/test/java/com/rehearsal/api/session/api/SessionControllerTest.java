package com.rehearsal.api.session.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.session.application.InMemorySessionStore;
import com.rehearsal.api.session.application.SessionService;
import com.rehearsal.api.session.controller.SessionController;
import com.rehearsal.api.slot.application.ContextSlotExtractionService;
import com.rehearsal.api.slot.application.result.ExtractContextSlotsResult;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(controllers = SessionController.class)
@Import({
  SessionService.class,
  InMemorySessionStore.class,
  GlobalExceptionHandler.class,
  ApiResponseBodyAdvice.class
})
class SessionControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private ContextSlotExtractionService contextSlotExtractionService;

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
  void submitBriefing_whenReadyForSimulation_transitionsToTransformationReady() throws Exception {
    when(contextSlotExtractionService.extract(any())).thenReturn(readyResult());
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"내일 발표가 있어요.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("TRANSFORMATION_READY"))
        .andExpect(jsonPath("$.data.contextStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.followUpAttempt").value(0))
        .andExpect(jsonPath("$.data.followUpQuestion").doesNotExist());
  }

  @Test
  void submitBriefing_whenMissingRequiredSlots_transitionsToFollowUpRequired() throws Exception {
    when(contextSlotExtractionService.extract(any())).thenReturn(followUpRequiredResult());
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"내일 중요한 일이 있어요.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("FOLLOW_UP_REQUIRED"))
        .andExpect(jsonPath("$.data.contextStatus").value("FOLLOW_UP_REQUIRED"))
        .andExpect(jsonPath("$.data.followUpAttempt").value(1))
        .andExpect(jsonPath("$.data.followUpQuestion").isString())
        .andExpect(jsonPath("$.data.missingRequiredSlotKeys[0]").value("critical_moment"));
  }

  @Test
  void submitBriefing_whenTranscriptIsBlank_returns400() throws Exception {
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitBriefing_whenSessionNotInBriefingStatus_returns409() throws Exception {
    when(contextSlotExtractionService.extract(any())).thenReturn(followUpRequiredResult());
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"내일 중요한 일이 있어요.\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"다시 브리핑 시도.\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"))
        .andExpect(jsonPath("$.error.name").value("INVALID_SESSION_STATUS"));
  }

  @Test
  void submitFollowUp_whenReadyForSimulation_transitionsToTransformationReady() throws Exception {
    when(contextSlotExtractionService.extract(any()))
        .thenReturn(followUpRequiredResult())
        .thenReturn(readyResult());
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"내일 중요한 일이 있어요.\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/follow-up", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"발표 도중 첫 질문에 답하는 순간이요.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("TRANSFORMATION_READY"))
        .andExpect(jsonPath("$.data.contextStatus").value("COMPLETED"))
        .andExpect(jsonPath("$.data.followUpAttempt").value(2));
  }

  @Test
  void submitFollowUp_whenTranscriptIsBlank_returns400() throws Exception {
    when(contextSlotExtractionService.extract(any())).thenReturn(followUpRequiredResult());
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"내일 중요한 일이 있어요.\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/follow-up", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void submitFollowUp_whenSessionNotInFollowUpRequiredStatus_returns409() throws Exception {
    String sessionId = createSessionAndGetId();

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/follow-up", sessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"답변입니다.\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"))
        .andExpect(jsonPath("$.error.name").value("INVALID_SESSION_STATUS"));
  }

  private String createSessionAndGetId() throws Exception {
    MvcResult result =
        mockMvc.perform(post("/api/v1/sessions")).andExpect(status().isOk()).andReturn();
    return com.jayway.jsonpath.JsonPath.read(
        result.getResponse().getContentAsString(), "$.data.sessionId");
  }

  private ExtractContextSlotsResult readyResult() {
    return new ExtractContextSlotsResult(
        "p1_offline_default",
        Map.of("situation_type", "presentation", "critical_moment", "첫 질문에 답하는 순간"),
        Map.of(),
        Map.of("situation_type", "presentation", "critical_moment", "첫 질문에 답하는 순간"),
        List.of(),
        null,
        true);
  }

  private ExtractContextSlotsResult followUpRequiredResult() {
    return new ExtractContextSlotsResult(
        "p1_offline_default",
        Map.of("situation_type", "presentation"),
        Map.of(),
        Map.of("situation_type", "presentation"),
        List.of("critical_moment"),
        "내일의 장면이 거의 완성됐어요. 마지막으로 가장 걱정되는 순간은 언제인가요?",
        false);
  }
}
