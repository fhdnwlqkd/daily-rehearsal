package com.rehearsal.api.admin.slot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.slot.controller.AdminContextSlotController;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.ManageContextSlotSchemaUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("slot-admin")
@WebMvcTest(controllers = AdminContextSlotController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class AdminContextSlotOptionControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;
  @MockitoBean private ManageContextSlotSchemaUseCase manageContextSlotSchemaUseCase;

  @Test
  void updateContextSlotOption() throws Exception {
    when(manageContextSlotSchemaUseCase.updateOption(any()))
        .thenReturn(new ContextSlotOption(1L, "presentation", "발표"));

    mockMvc
        .perform(
            patch("/api/v1/admin/context-slot-options/{optionId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"label\":\"발표\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.optionKey").value("presentation"));
  }
}
