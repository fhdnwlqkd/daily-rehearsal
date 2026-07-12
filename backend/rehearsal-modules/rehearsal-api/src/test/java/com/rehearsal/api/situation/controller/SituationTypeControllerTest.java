package com.rehearsal.api.situation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.situation.usecase.GetSituationTypeBriefingUseCase;
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
        .andExpect(jsonPath("$.data[0].situationType").value("date"))
        .andExpect(jsonPath("$.data[0].label").value(SituationType.DATE.getDisplayName()))
        .andExpect(jsonPath("$.data[0].gestureOrder").doesNotExist())
        .andExpect(jsonPath("$.data[0].briefingTitle").doesNotExist())
        .andExpect(jsonPath("$.data[0].exampleAnswer").doesNotExist())
        .andExpect(jsonPath("$.data[1].situationType").value("business_meeting"));
  }

  @Test
  void returnsSituationTypeBriefing() throws Exception {
    mockMvc
        .perform(get("/api/v1/situation-types/date/briefing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.situationType").value("date"))
        .andExpect(
            jsonPath("$.data.briefingTitle").value(SituationType.DATE.getBriefingTitle()))
        .andExpect(jsonPath("$.data.exampleAnswer").isString())
        .andExpect(jsonPath("$.data.label").doesNotExist());
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    GetSituationTypesUseCase getSituationTypesUseCase() {
      return () -> List.of(SituationType.DATE, SituationType.BUSINESS_MEETING);
    }

    @Bean
    GetSituationTypeBriefingUseCase getSituationTypeBriefingUseCase() {
      return situationType -> SituationType.DATE;
    }
  }
}
