package com.rehearsal.api.session.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.session.application.InMemorySessionStore;
import com.rehearsal.api.session.application.SessionService;
import com.rehearsal.api.session.controller.SessionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
}
