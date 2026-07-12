package com.rehearsal.api.session.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.extraction.usecase.GetContextExtractionUseCase;
import com.rehearsal.domain.extraction.usecase.SubmitContextExtractionUseCase;
import com.rehearsal.domain.session.model.ClientSession;
import com.rehearsal.domain.session.model.ContextCollectionState;
import com.rehearsal.domain.session.model.ContextStatus;
import com.rehearsal.domain.session.model.SessionContext;
import com.rehearsal.domain.session.usecase.CreateSessionUseCase;
import com.rehearsal.domain.session.usecase.UpdateClientSessionUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs
@WebMvcTest(controllers = SessionController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class SessionControllerDocsTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateSessionUseCase createSessionUseCase;
  @MockitoBean private UpdateClientSessionUseCase updateClientSessionUseCase;
  @MockitoBean private SubmitContextExtractionUseCase submitContextExtractionUseCase;
  @MockitoBean private GetContextExtractionUseCase getContextExtractionUseCase;

  @Test
  void createSession() throws Exception {
    ClientSession mockSession = ClientSession.create(SituationType.DATE);
    given(createSessionUseCase.createSession(any())).willReturn(mockSession);

    mockMvc
        .perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situationType\":\"date\"}"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "session-create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  void submitBriefing() throws Exception {
    ClientSession session = ClientSession.create(SituationType.DATE);
    session.startContextExtraction();
    given(submitContextExtractionUseCase.submitBriefingExtraction(any(), any()))
        .willReturn(session);

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", session.getSessionId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"briefing transcript\"}"))
        .andExpect(status().isAccepted())
        .andDo(
            document(
                "context-submit-briefing",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  void pollContext() throws Exception {
    ContextCollectionState state =
        new ContextCollectionState(
            "session-id",
            ContextStatus.COMPLETED,
            SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural")),
            List.of(),
            List.of());
    given(getContextExtractionUseCase.getContext("session-id")).willReturn(state);

    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}/context", "session-id"))
        .andExpect(status().isOk())
        .andDo(
            document(
                "context-poll",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }
}
