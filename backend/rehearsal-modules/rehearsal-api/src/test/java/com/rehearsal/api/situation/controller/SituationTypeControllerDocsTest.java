package com.rehearsal.api.situation.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.support.RestDocsEnumValues;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.situation.usecase.GetSituationTypeBriefingUseCase;
import com.rehearsal.domain.situation.usecase.GetSituationTypesUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs
@WebMvcTest(controllers = SituationTypeController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class SituationTypeControllerDocsTest {

  private static final String API_KEY = "test-api-key";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetSituationTypesUseCase getSituationTypesUseCase;
  @MockitoBean private GetSituationTypeBriefingUseCase getSituationTypeBriefingUseCase;

  @Test
  void getSituationTypes() throws Exception {
    given(getSituationTypesUseCase.getSituationTypes())
        .willReturn(List.of(SituationType.DATE, SituationType.INTERVIEW, SituationType.FIRST_DAY));

    mockMvc
        .perform(get("/api/v1/situation-types").header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].situationType").value("date"))
        .andExpect(jsonPath("$.data[0].label").value(SituationType.DATE.getDisplayName()))
        .andDo(
            document(
                "situation-types-list",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                relaxedResponseFields(
                    fieldWithPath("data[].situationType")
                        .description("상황 타입. 가능한 값: " + RestDocsEnumValues.situationTypes()),
                    fieldWithPath("data[].label").description("상황 표시 이름"))));
  }

  @Test
  void getSituationTypeBriefing() throws Exception {
    given(getSituationTypeBriefingUseCase.getSituationTypeBriefing("date"))
        .willReturn(SituationType.DATE);

    mockMvc
        .perform(
            get("/api/v1/situation-types/{situationType}/briefing", "date")
                .header("X-API-KEY", API_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.situationType").value("date"))
        .andExpect(jsonPath("$.data.briefingTitle").value(SituationType.DATE.getBriefingTitle()))
        .andExpect(jsonPath("$.data.exampleAnswer").value(SituationType.DATE.getExampleAnswer()))
        .andDo(
            document(
                "situation-type-briefing",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestHeaders(headerWithName("X-API-KEY").description("Client API key")),
                pathParameters(
                    parameterWithName("situationType")
                        .description("Situation type key. " + RestDocsEnumValues.situationTypes())),
                relaxedResponseFields(
                    fieldWithPath("data.situationType")
                        .description("상황 타입. 가능한 값: " + RestDocsEnumValues.situationTypes()),
                    fieldWithPath("data.briefingTitle").description("브리핑 질문"),
                    fieldWithPath("data.exampleAnswer").description("예시 답변"))));
  }
}
