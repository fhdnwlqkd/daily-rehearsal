package com.rehearsal.api.decart.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.usecase.GetOutfitSpecUseCase;
import com.rehearsal.domain.decart.usecase.IssueDecartTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DecartController.class)
@Import({
  GlobalExceptionHandler.class,
  ApiResponseBodyAdvice.class,
  DecartControllerTest.TestUseCaseConfiguration.class
})
class DecartControllerTest {

  private static final String VALID_SESSION_ID = "valid-session-id";
  private static final String INVALID_STATE_SESSION_ID = "invalid-state-session-id";
  private static final String VALID_OUTFIT_ID = "presentation_jacket_01";

  @Autowired private MockMvc mockMvc;

  @Test
  void issuesDecartToken() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/decart-token", VALID_SESSION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.clientToken").value("test-client-token"));
  }

  @Test
  void issueTokenReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/decart-token", "not-found-session"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S001"));
  }

  @Test
  void issueTokenReturnsConflictWhenSessionStateIsInvalid() throws Exception {
    mockMvc
        .perform(post("/api/v1/sessions/{sessionId}/decart-token", INVALID_STATE_SESSION_ID))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"));
  }

  @Test
  void returnsOutfitSpec() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/outfit-spec", VALID_SESSION_ID)
                .param("outfitId", VALID_OUTFIT_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.model").value("lucy-vton-latest"))
        .andExpect(jsonPath("$.data.prompt").isString())
        .andExpect(jsonPath("$.data.referenceImageUrl").isString())
        .andExpect(jsonPath("$.data.enhance").value(false));
  }

  @Test
  void getOutfitSpecReturnsNotFoundWhenSessionDoesNotExist() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/outfit-spec", "not-found-session")
                .param("outfitId", VALID_OUTFIT_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S001"));
  }

  @Test
  void getOutfitSpecReturnsBadRequestWhenOutfitIdIsBlank() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/outfit-spec", VALID_SESSION_ID).param("outfitId", ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C001"));
  }

  @Test
  void getOutfitSpecReturnsNotFoundForUnknownOutfit() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/sessions/{sessionId}/outfit-spec", VALID_SESSION_ID)
                .param("outfitId", "unknown_outfit"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C003"));
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    IssueDecartTokenUseCase issueDecartTokenUseCase() {
      return new TestIssueDecartTokenUseCase();
    }

    @Bean
    GetOutfitSpecUseCase getOutfitSpecUseCase() {
      return new TestGetOutfitSpecUseCase();
    }
  }

  static class TestIssueDecartTokenUseCase implements IssueDecartTokenUseCase {

    @Override
    public String issueDecartToken(String sessionId) {
      if (sessionId.equals("not-found-session")) {
        throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
      }
      if (sessionId.equals(INVALID_STATE_SESSION_ID)) {
        throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
      }
      return "test-client-token";
    }
  }

  static class TestGetOutfitSpecUseCase implements GetOutfitSpecUseCase {

    @Override
    public DecartSpec getOutfitSpec(String sessionId, String outfitId) {
      if (sessionId.equals("not-found-session")) {
        throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
      }
      if (!outfitId.equals(VALID_OUTFIT_ID)) {
        throw new BusinessException(ErrorCode.NOT_FOUND);
      }
      return new DecartSpec(
          "lucy-vton-latest",
          "Substitute the current top with a navy blue blazer",
          "https://asset-store/outfits/jacket_01.png",
          false);
    }
  }
}
