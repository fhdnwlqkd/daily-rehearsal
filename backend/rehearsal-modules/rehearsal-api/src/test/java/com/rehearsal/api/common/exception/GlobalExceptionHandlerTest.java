package com.rehearsal.api.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void handleBusinessException() throws Exception {
    mockMvc
        .perform(get("/test/business"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C006"))
        .andExpect(jsonPath("$.error.name").value("CONFLICT"))
        .andExpect(jsonPath("$.error.message").value("Session is already completed."));
  }

  @Test
  void handleMethodArgumentNotValidException() throws Exception {
    mockMvc
        .perform(post("/test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("C001"))
        .andExpect(jsonPath("$.error.name").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.error.details[0].field").value("transcript"));
  }

  @RestController
  public static class TestController {

    @GetMapping("/test/business")
    void businessException() {
      throw new BusinessException(ErrorCode.CONFLICT, "Session is already completed.");
    }

    @PostMapping("/test/validate")
    void validate(@Valid @RequestBody TestRequest request) {}
  }

  record TestRequest(@NotBlank String transcript) {}
}
