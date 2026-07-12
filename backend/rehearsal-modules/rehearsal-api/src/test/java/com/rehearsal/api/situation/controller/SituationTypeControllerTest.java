package com.rehearsal.api.situation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.situation.model.SituationType;
import com.rehearsal.domain.situation.registry.SituationTypeBriefingDefinition;
import com.rehearsal.domain.situation.registry.SituationTypeDefinition;
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
        .andExpect(jsonPath("$.data[0].label").value("소개팅"))
        .andExpect(jsonPath("$.data[0].gestureOrder").doesNotExist())
        .andExpect(jsonPath("$.data[0].briefingTitle").doesNotExist())
        .andExpect(jsonPath("$.data[0].exampleAnswers").doesNotExist())
        .andExpect(jsonPath("$.data[1].situationType").value("business_meeting"));
  }

  @Test
  void returnsSituationTypeBriefing() throws Exception {
    mockMvc
        .perform(get("/api/v1/situation-types/date/briefing"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.situationType").value("date"))
        .andExpect(jsonPath("$.data.briefingTitle").value("내일의 소개팅을 짧게 말해주세요"))
        .andExpect(jsonPath("$.data.exampleAnswers[0]").isString())
        .andExpect(jsonPath("$.data.label").doesNotExist());
  }

  @TestConfiguration
  static class TestUseCaseConfiguration {

    @Bean
    GetSituationTypesUseCase getSituationTypesUseCase() {
      return () ->
          List.of(
              new SituationTypeDefinition(SituationType.DATE, "소개팅"),
              new SituationTypeDefinition(SituationType.BUSINESS_MEETING, "비즈니스 미팅"));
    }

    @Bean
    GetSituationTypeBriefingUseCase getSituationTypeBriefingUseCase() {
      return situationType ->
          new SituationTypeBriefingDefinition(
              SituationType.DATE,
              "내일의 소개팅을 짧게 말해주세요",
              List.of("내일 소개팅이 있는데 첫 인사가 어색할까 봐 걱정돼요."));
    }
  }
}
