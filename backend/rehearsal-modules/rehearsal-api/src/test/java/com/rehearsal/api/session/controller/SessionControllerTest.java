package com.rehearsal.api.session.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.rehearsal.domain.session.usecase.UploadSessionVideoUseCase;
import com.rehearsal.domain.situation.model.SituationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SessionController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class SessionControllerTest {

  private static final String SESSION_ID = "session-id";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateSessionUseCase createSessionUseCase;
  @MockitoBean private UpdateClientSessionUseCase updateClientSessionUseCase;
  @MockitoBean private SubmitContextExtractionUseCase submitContextExtractionUseCase;
  @MockitoBean private GetContextExtractionUseCase getContextExtractionUseCase;
  @MockitoBean private UploadSessionVideoUseCase uploadSessionVideoUseCase;

  @Test
  void createSession() throws Exception {
    given(createSessionUseCase.createSession(any()))
        .willReturn(ClientSession.create(SituationType.DATE));

    mockMvc
        .perform(
            post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"situationType\":\"date\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").isString())
        .andExpect(jsonPath("$.data.situationType").value("date"));
  }

  @Test
  void submitBriefingReturnsExtractingStateWithoutJobId() throws Exception {
    ClientSession session = extractingSession();
    given(submitContextExtractionUseCase.submitBriefingExtraction(anyString(), anyString()))
        .willReturn(session);

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/briefing", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"transcript\":\"briefing transcript\"}"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
        .andExpect(jsonPath("$.data.status").value("EXTRACTING"))
        .andExpect(jsonPath("$.data.jobId").doesNotExist());
  }

  @Test
  void getContextPollsSessionAndContextRows() throws Exception {
    ContextCollectionState state =
        new ContextCollectionState(
            SESSION_ID,
            ContextStatus.COMPLETED,
            SessionContext.from(SituationType.DATE, Map.of("desired_persona", "warm_natural")),
            List.of(),
            List.of());
    given(getContextExtractionUseCase.getContext(SESSION_ID)).willReturn(state);

    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}/context", SESSION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.context.situation_type").value("date"))
        .andExpect(jsonPath("$.data.context.desired_persona").value("warm_natural"));
  }

  @Test
  void confirmOutfitReturnsOnlySessionId() throws Exception {
    ClientSession session =
        ClientSession.builder()
            .sessionId(SESSION_ID)
            .situationType(SituationType.DATE)
            .status(com.rehearsal.domain.session.model.SessionStatus.REHEARSAL_READY)
            .contextStatus(ContextStatus.COMPLETED)
            .selectedOutfitId("outfit-1")
            .build();
    given(updateClientSessionUseCase.confirmOutfit(anyString(), anyString())).willReturn(session);

    mockMvc
        .perform(
            patch("/api/v1/sessions/{sessionId}/outfit", SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"selectedOutfitId\":\"outfit-1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
        .andExpect(jsonPath("$.data.selectedOutfitId").doesNotExist());
  }

  @Test
  void uploadVideo() throws Exception {
    ClientSession session = rehearsalReadySession();
    session.assignVideoUrl("http://localhost/mock-videos/" + SESSION_ID + ".webm");
    given(uploadSessionVideoUseCase.upload(anyString(), any(), anyString(), anyString()))
        .willReturn(session);

    mockMvc
        .perform(
            multipart("/api/v1/sessions/{sessionId}/video", SESSION_ID)
                .file(
                    new MockMultipartFile(
                        "file", "recording.webm", "video/webm", "video-bytes".getBytes())))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.data.sessionId").value(SESSION_ID))
        .andExpect(jsonPath("$.data.videoUrl").isString())
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void uploadVideoWhenFileIsEmpty() throws Exception {
    mockMvc
        .perform(
            multipart("/api/v1/sessions/{sessionId}/video", SESSION_ID)
                .file(new MockMultipartFile("file", "recording.webm", "video/webm", new byte[0])))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S008"));
  }

  private ClientSession extractingSession() {
    ClientSession session =
        ClientSession.builder()
            .sessionId(SESSION_ID)
            .situationType(SituationType.DATE)
            .status(com.rehearsal.domain.session.model.SessionStatus.BRIEFING)
            .contextStatus(ContextStatus.NOT_STARTED)
            .build();
    session.startContextExtraction();
    return session;
  }

  private ClientSession rehearsalReadySession() {
    return ClientSession.builder()
        .sessionId(SESSION_ID)
        .situationType(SituationType.DATE)
        .status(com.rehearsal.domain.session.model.SessionStatus.REHEARSAL_READY)
        .contextStatus(ContextStatus.COMPLETED)
        .selectedOutfitId("outfit-1")
        .build();
  }
}
