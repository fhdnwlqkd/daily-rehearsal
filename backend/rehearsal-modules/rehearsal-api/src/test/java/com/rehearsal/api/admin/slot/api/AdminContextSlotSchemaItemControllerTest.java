package com.rehearsal.api.admin.slot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.slot.controller.AdminContextSlotController;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import com.rehearsal.domain.slot.usecase.ManageContextSlotSchemaUseCase;
import java.util.List;
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
class AdminContextSlotSchemaItemControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;
  @MockitoBean private ManageContextSlotSchemaUseCase manageContextSlotSchemaUseCase;

  @Test
  void updateContextSlotSchemaItem() throws Exception {
    when(manageContextSlotSchemaUseCase.updateSchemaItem(any())).thenReturn(contextSlotSchema());

    mockMvc
        .perform(
            patch("/api/v1/admin/context-slot-schema-items/{itemId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "requiredLevel": "OPTIONAL",
                      "priority": 20,
                      "active": false
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.items[0].requiredLevel").value("OPTIONAL"))
        .andExpect(jsonPath("$.data.items[0].active").value(false));
  }

  private ContextSlotSchema contextSlotSchema() {
    ContextSlot slot =
        new ContextSlot(
            1L,
            "critical_moment",
            "결정적 순간",
            SlotType.TEXT,
            "가장 흔들릴 수 있는 순간을 추출한다.",
            "가장 걱정되는 순간은 언제인가요?",
            null,
            null,
            List.of());
    return new ContextSlotSchema(
        1L,
        "p1_offline_default",
        "P1 Offline Default Context Slot Schema",
        1,
        true,
        List.of(new ContextSlotSchemaItem(1L, slot, RequiredLevel.OPTIONAL, 20, false)));
  }
}
