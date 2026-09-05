package com.rehearsal.api.demo.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.decart.usecase.IssueDemoDecartTokenUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DemoDecartController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class DemoDecartControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private IssueDemoDecartTokenUseCase issueDemoDecartTokenUseCase;

  @Test
  void issuesDemoDecartToken() throws Exception {
    given(issueDemoDecartTokenUseCase.issueDemoDecartToken()).willReturn("demo-client-token");

    mockMvc
        .perform(post("/api/v1/demo/decart-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.clientToken").value("demo-client-token"));
  }
}
