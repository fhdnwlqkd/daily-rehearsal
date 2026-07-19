package com.rehearsal.api.session.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.support.RestDocsEnumValues;
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

  private static final String API_KEY = "test-api-key";
  private static final String SESSION_ID = "session-id";

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
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situationType\":\"date\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
        .andExpect(jsonPath("$.data.situationType").value("date"))
        .andDo(
            document(
                "session-create",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("situationType")
                        .description("상황 타입. 가능한 값: " + RestDocsEnumValues.situationTypes())),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("생성된 세션 ID"),
                    fieldWithPath("data.situationType")
                        .description("상황 타입. 가능한 값: " + RestDocsEnumValues.situationTypes()))));
  }

  @Test
  void submitBriefing() throws Exception {
    ClientSession session = session();
    session.startContextExtraction();
    given(submitContextExtractionUseCase.submitBriefingExtraction(any(), any()))
        .willReturn(session);

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", session.getSessionId())
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"briefing transcript\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(session.getSessionId()))
        .andExpect(jsonPath("$.data.status").value("EXTRACTING"))
        .andDo(
            document(
                "context-submit-briefing",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(fieldWithPath("transcript").description("브리핑 답변 transcript")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"),
                    fieldWithPath("data.status")
                        .description(
                            "context 추출 상태. 가능한 값: "
                                + RestDocsEnumValues.names(ContextStatus.class)))));
  }

  @Test
  void submitFollowUp() throws Exception {
    ClientSession session = session();
    session.startContextExtraction();
    session.requireFollowUp();
    session.startFollowUpMerge();
    given(submitContextExtractionUseCase.submitFollowUpExtraction(any(), any()))
        .willReturn(session);

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/follow-up", session.getSessionId())
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"warm_natural\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(session.getSessionId()))
        .andExpect(jsonPath("$.data.status").value("MERGING"))
        .andDo(
            document(
                "context-submit-follow-up",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(fieldWithPath("transcript").description("follow-up 답변 transcript")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"),
                    fieldWithPath("data.status")
                        .description(
                            "context 추출 상태. 가능한 값: "
                                + RestDocsEnumValues.names(ContextStatus.class)))));
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
        .perform(
            get("/api/v1/sessions/{sessionId}/context", "session-id").header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value("session-id"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.context.situation_type").value("date"))
        .andExpect(jsonPath("$.data.context.desired_persona").value("warm_natural"))
        .andExpect(jsonPath("$.data.missingSlotKeys").doesNotExist())
        .andDo(
            document(
                "context-poll",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"),
                    fieldWithPath("data.status")
                        .description(
                            "context 추출 상태. 가능한 값: "
                                + RestDocsEnumValues.names(ContextStatus.class)),
                    fieldWithPath("data.context").description("추출된 context 값"))));
  }

  private ClientSession session() {
    return ClientSession.builder()
        .sessionId(SESSION_ID)
        .situationType(SituationType.DATE)
        .status(com.rehearsal.domain.session.model.SessionStatus.BRIEFING)
        .contextStatus(ContextStatus.NOT_STARTED)
        .build();
  }
}
