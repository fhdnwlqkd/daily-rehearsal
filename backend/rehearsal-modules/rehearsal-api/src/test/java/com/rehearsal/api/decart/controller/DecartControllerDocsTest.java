package com.rehearsal.api.decart.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.model.OutfitCandidate;
import com.rehearsal.domain.decart.usecase.GetOutfitCandidatesUseCase;
import com.rehearsal.domain.decart.usecase.GetOutfitSpecUseCase;
import com.rehearsal.domain.decart.usecase.IssueDecartTokenUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureRestDocs
@WebMvcTest(controllers = DecartController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class DecartControllerDocsTest {

  private static final String SESSION_ID = "session-id";
  private static final String OUTFIT_ID = "presentation_jacket_01";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IssueDecartTokenUseCase issueDecartTokenUseCase;
  @MockitoBean private GetOutfitSpecUseCase getOutfitSpecUseCase;
  @MockitoBean private GetOutfitCandidatesUseCase getOutfitCandidatesUseCase;

  @Test
  void issueDecartToken() throws Exception {
    given(issueDecartTokenUseCase.issueDecartToken(SESSION_ID)).willReturn("test-client-token");

    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/decart-token", SESSION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.clientToken").value("test-client-token"))
        .andDo(
            document(
                "decart-token-issue",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  void getOutfitCandidates() throws Exception {
    given(getOutfitCandidatesUseCase.getOutfitCandidates(SESSION_ID))
        .willReturn(
            List.of(
                new OutfitCandidate(
                    OUTFIT_ID, "네이비 재킷", "https://asset-store/thumbnails/jacket_01.png", true)));

    mockMvc
        .perform(get("/api/v1/sessions/{sessionId}/outfits", SESSION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].outfitId").value(OUTFIT_ID))
        .andExpect(jsonPath("$.data[0].label").value("네이비 재킷"))
        .andExpect(jsonPath("$.data[0].defaultOutfit").value(true))
        .andDo(
            document(
                "outfits-list",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }

  @Test
  void getOutfitSpec() throws Exception {
    given(getOutfitSpecUseCase.getOutfitSpec(SESSION_ID, OUTFIT_ID))
        .willReturn(
            new DecartSpec(
                "lucy-vton-latest",
                "Substitute the current top with a navy blue blazer",
                "https://asset-store/outfits/jacket_01.png",
                false));

    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/outfit-spec", SESSION_ID)
                .param("outfitId", OUTFIT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.model").value("lucy-vton-latest"))
        .andExpect(jsonPath("$.data.enhance").value(false))
        .andDo(
            document(
                "outfit-spec-get",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint())));
  }
}
