package com.rehearsal.api.decart.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.decart.model.DecartSpec;
import com.rehearsal.domain.decart.usecase.GetDecartSpecUseCase;
import com.rehearsal.domain.decart.usecase.result.DecartSpecResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
  void issuesDecartTokenAndSpec() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/decart-token", VALID_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outfitId\":\"" + VALID_OUTFIT_ID + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.clientToken").value("test-client-token"))
        .andExpect(jsonPath("$.data.spec.model").value("lucy-vton-latest"))
        .andExpect(jsonPath("$.data.spec.prompt").isString())
        .andExpect(jsonPath("$.data.spec.referenceImageUrl").isString())
        .andExpect(jsonPath("$.data.spec.enhance").value(false));
  }

  @Test
  void returnsNotFoundWhenSessionDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/decart-token", "not-found-session")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outfitId\":\"" + VALID_OUTFIT_ID + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S001"))
        .andExpect(jsonPath("$.error.name").value("SESSION_NOT_FOUND"));
  }

  @Test
  void returnsConflictWhenSessionStateIsInvalid() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/decart-token", INVALID_STATE_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outfitId\":\"" + VALID_OUTFIT_ID + "\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("S002"))
        .andExpect(jsonPath("$.error.name").value("INVALID_SESSION_STATE"));
  }

  @Test
  void returnsBadRequestWhenOutfitIdIsBlank() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/decart-token", VALID_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outfitId\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C001"));
  }

  @Test
  void returnsNotFoundWhenOutfitDoesNotExist() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/sessions/{sessionId}/decart-token", VALID_SESSION_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"outfitId\":\"unknown_outfit\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C003"));
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    GetDecartSpecUseCase getDecartSpecUseCase() {
      return new TestGetDecartSpecUseCase();
    }
  }

  static class TestGetDecartSpecUseCase implements GetDecartSpecUseCase {

    @Override
    public DecartSpecResult getDecartSpec(String sessionId, String outfitId) {
      if (sessionId.equals("not-found-session")) {
        throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
      }
      if (sessionId.equals(INVALID_STATE_SESSION_ID)) {
        throw new BusinessException(ErrorCode.INVALID_SESSION_STATE);
      }
      if (!outfitId.equals(VALID_OUTFIT_ID)) {
        throw new BusinessException(ErrorCode.NOT_FOUND);
      }
      return new DecartSpecResult(
          "test-client-token",
          new DecartSpec(
              "lucy-vton-latest",
              "Substitute the current top with a navy blue blazer",
              "https://asset-store/outfits/jacket_01.png",
              false));
    }
  }
}
