package com.rehearsal.api.ticket.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.ticket.model.ChangeCard;
import com.rehearsal.domain.ticket.model.TicketJob;
import com.rehearsal.domain.ticket.model.TicketPayload;
import com.rehearsal.domain.ticket.model.TicketSnapshot;
import com.rehearsal.domain.ticket.usecase.GetTicketGenerationUseCase;
import com.rehearsal.domain.ticket.usecase.SubmitTicketGenerationUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs
@WebMvcTest(controllers = TicketController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class TicketControllerDocsTest {

  private static final String API_KEY = "test-api-key";
  private static final String SESSION_ID = "session-id";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private SubmitTicketGenerationUseCase submitTicketGenerationUseCase;
  @MockitoBean private GetTicketGenerationUseCase getTicketGenerationUseCase;

  @Test
  void submitTicketGeneration() throws Exception {
    given(submitTicketGenerationUseCase.submit(SESSION_ID))
        .willReturn(TicketJob.pending(SESSION_ID));

    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/ticket", SESSION_ID).header("X-API-KEY", API_KEY))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andDo(
            document(
                "ticket-submit",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("세션 ID")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"),
                    fieldWithPath("data.status").description("티켓 생성 상태. 최초에는 PENDING"))));
  }

  @Test
  void pollCompletedTicketGeneration() throws Exception {
    given(getTicketGenerationUseCase.get(SESSION_ID)).willReturn(completedTicket());

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/ticket", SESSION_ID).header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.snapshot.selectedOutfitLabel").value("네이비 정장"))
        .andExpect(jsonPath("$.data.changeCard.ifThenPlan").isString())
        .andDo(
            document(
                "ticket-poll-completed",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(parameterWithName("sessionId").description("세션 ID")),
                relaxedResponseFields(
                    fieldWithPath("data.sessionId").description("세션 ID"),
                    fieldWithPath("data.status").description("티켓 생성 상태"),
                    fieldWithPath("data.snapshot.situationLabel").description("상황"),
                    fieldWithPath("data.snapshot.criticalMoment").description("내일의 중요한 순간"),
                    fieldWithPath("data.snapshot.desiredPersonaLabel").description("목표 인상"),
                    fieldWithPath("data.snapshot.selectedOutfitLabel").description("선택한 스타일"),
                    fieldWithPath("data.changeCard.todayAction").description("오늘의 행동 변화"),
                    fieldWithPath("data.changeCard.tomorrowAttitude").description("내일 유지할 태도"),
                    fieldWithPath("data.changeCard.ifThenPlan").description("If-Then 계획"),
                    fieldWithPath("data.videoUrl").description("업로드된 영상 URL"),
                    fieldWithPath("data.videoAvailable").description("영상 사용 가능 여부"),
                    fieldWithPath("data.downloadUrl").description("모바일 다운로드 페이지 URL"),
                    fieldWithPath("data.qrPayload").description("QR 코드에 인코딩할 URL"))));
  }

  private TicketJob completedTicket() {
    return TicketJob.pending(SESSION_ID)
        .complete(
            new TicketPayload(
                new TicketSnapshot("소개팅", "첫 인사", "따뜻하고 자연스럽게", "네이비 정장"),
                new ChangeCard("첫 문장을 천천히 시작하기", "여유 있게 듣기", "긴장되면 숨을 고르고 말하기"),
                false,
                "https://video.example.com/session-id.webm",
                true,
                "http://32.236.96.32:3000/download/session-id",
                "http://32.236.96.32:3000/download/session-id"));
  }
}
