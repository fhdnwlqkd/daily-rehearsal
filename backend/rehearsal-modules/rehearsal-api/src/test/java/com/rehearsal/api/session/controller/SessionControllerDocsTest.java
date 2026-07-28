package com.rehearsal.api.session.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
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
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
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
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
                requestFields(fieldWithPath("transcript").description("follow-up 답변 transcript")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"),
                    fieldWithPath("data.status")
                        .description(
                            "context 추출 상태. 가능한 값: "
                                + RestDocsEnumValues.names(ContextStatus.class)))));
  }

  @Test
  void pollExtractingContext() throws Exception {
    ContextCollectionState state =
        new ContextCollectionState(
            SESSION_ID,
            ContextStatus.EXTRACTING,
            SessionContext.empty(SituationType.DATE),
            List.of(),
            List.of());
    given(getContextExtractionUseCase.getContext(SESSION_ID)).willReturn(state);

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/context", SESSION_ID).header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
        .andExpect(jsonPath("$.data.status").value("EXTRACTING"))
        .andDo(
            document(
                "context-poll-extracting",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.status")
                        .description(
                            "Context extraction status. Values: "
                                + RestDocsEnumValues.names(ContextStatus.class)),
                    fieldWithPath("data.context").description("Collected context values so far"))));
  }

  @Test
  void pollFollowUpRequiredContext() throws Exception {
    ContextCollectionState state =
        new ContextCollectionState(
            SESSION_ID,
            ContextStatus.FOLLOW_UP_REQUIRED,
            SessionContext.from(SituationType.DATE, Map.of("critical_moment", "first greeting")),
            List.of("desired_persona"),
            List.of("What impression would you like to make?"));
    given(getContextExtractionUseCase.getContext(SESSION_ID)).willReturn(state);

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/context", SESSION_ID).header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("FOLLOW_UP_REQUIRED"))
        .andExpect(jsonPath("$.data.missingSlotKeys[0]").value("desired_persona"))
        .andExpect(jsonPath("$.data.followUpQuestions[0]").isNotEmpty())
        .andDo(
            document(
                "context-poll-follow-up-required",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("Session ID"),
                    fieldWithPath("data.status")
                        .description(
                            "Context extraction status. Values: "
                                + RestDocsEnumValues.names(ContextStatus.class)),
                    fieldWithPath("data.context").description("Collected context values so far"),
                    fieldWithPath("data.missingSlotKeys")
                        .description("Required slot keys still missing"),
                    fieldWithPath("data.followUpQuestions")
                        .description("Fixed follow-up questions"))));
  }

  @Test
  void pollCompletedContext() throws Exception {
    ContextCollectionState state =
        new ContextCollectionState(
            SESSION_ID,
            ContextStatus.COMPLETED,
            SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural")),
            List.of(),
            List.of());
    given(getContextExtractionUseCase.getContext(SESSION_ID)).willReturn(state);

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/context", SESSION_ID).header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.context.situation_type").value("date"))
        .andExpect(jsonPath("$.data.context.desired_persona").value("warm_natural"))
        .andExpect(jsonPath("$.data.missingSlotKeys").doesNotExist())
        .andDo(
            document(
                "context-poll-completed",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
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

  @Test
  void confirmOutfit() throws Exception {
    ClientSession session = ClientSession.create(SituationType.DATE);
    given(
            updateClientSessionUseCase.confirmOutfit(
                session.getSessionId(), "presentation_jacket_01"))
        .willReturn(session);

    mockMvc
        .perform(
            patch("/api/v1/sessions/{sessionId}/outfit", session.getSessionId())
                .header("X-API-KEY", API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedOutfitId\":\"presentation_jacket_01\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sessionId").value(session.getSessionId()))
        .andDo(
            document(
                "outfit-confirm",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("Session ID")),
                requestFields(
                    fieldWithPath("selectedOutfitId").description("사용자가 확정한 옷 후보 ID")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"))));
  }
}
