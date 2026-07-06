package com.rehearsal.api.situation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.situation.registry.SituationTypeDefinition;
import com.rehearsal.domain.situation.usecase.GetSituationTypesUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SituationTypeController.class)
@Import({
  GlobalExceptionHandler.class,
  ApiResponseBodyAdvice.class,
  SituationTypeControllerTest.TestUseCaseConfiguration.class
})
class SituationTypeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void returnsSituationTypes() throws Exception {
    mockMvc
        .perform(get("/api/v1/situation-types"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].key").value("date"))
        .andExpect(jsonPath("$.data[0].label").value("\uC18C\uAC1C\uD305"))
        .andExpect(jsonPath("$.data[0].gestureOrder").value(1))
        .andExpect(jsonPath("$.data[0].briefingTitle").isString())
        .andExpect(jsonPath("$.data[0].exampleAnswers[0]").isString())
        .andExpect(jsonPath("$.data[1].key").value("business_meeting"));
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    GetSituationTypesUseCase getSituationTypesUseCase() {
      return () ->
          List.of(
              new SituationTypeDefinition(
                  SituationType.DATE,
                  "\uC18C\uAC1C\uD305",
                  1,
                  "\uB0B4\uC77C\uC758 \uC18C\uAC1C\uD305\uC744 \uC9E7\uAC8C \uB9D0\uD574\uC8FC\uC138\uC694",
                  List.of(
                      "\uB0B4\uC77C \uC18C\uAC1C\uD305\uC774 \uC788\uB294\uB370 \uCCAB \uC778\uC0AC\uAC00 \uC5B4\uC0C9\uD560\uAE4C \uBD10 \uAC71\uC815\uB3FC\uC694.")),
              new SituationTypeDefinition(
                  SituationType.BUSINESS_MEETING,
                  "\uBE44\uC988\uB2C8\uC2A4 \uBBF8\uD305",
                  2,
                  "\uB0B4\uC77C\uC758 \uBE44\uC988\uB2C8\uC2A4 \uBBF8\uD305\uC744 \uC9E7\uAC8C \uB9D0\uD574\uC8FC\uC138\uC694",
                  List.of(
                      "\uB0B4\uC77C \uACE0\uAC1D \uBBF8\uD305\uC5D0\uC11C \uD575\uC2EC \uB0B4\uC6A9\uC744 \uCC28\uBD84\uD558\uAC8C \uC804\uB2EC\uD558\uACE0 \uC2F6\uC5B4\uC694.")));
    }
  }
}
