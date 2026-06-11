package com.rehearsal.api.admin.slot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.slot.controller.AdminContextSlotSchemaController;
import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.model.ContextSlotSchemaItem;
import com.rehearsal.domain.slot.model.RequiredLevel;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.usecase.GetContextSlotSchemaUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminContextSlotSchemaController.class)
@Import({GlobalExceptionHandler.class, ApiResponseBodyAdvice.class})
class AdminContextSlotSchemaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;

  @Test
  void getContextSlotSchema() throws Exception {
    when(getContextSlotSchemaUseCase.getContextSlotSchema(any())).thenReturn(contextSlotSchema());

    mockMvc
        .perform(get("/api/v1/admin/context-slot-schemas/{schemaKey}", "p1_offline_default"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.schemaKey").value("p1_offline_default"))
        .andExpect(jsonPath("$.data.maxFollowUpAttempt").value(1))
        .andExpect(jsonPath("$.data.items[0].requiredLevel").value("REQUIRED"))
        .andExpect(jsonPath("$.data.items[0].slot.slotKey").value("situation_type"))
        .andExpect(jsonPath("$.data.items[0].slot.options[0].optionKey").value("presentation"));
  }

  private ContextSlotSchema contextSlotSchema() {
    ContextSlotOption presentationOption = new ContextSlotOption(1L, "presentation", "발표");
    ContextSlot situationType =
        new ContextSlot(
            1L,
            "situation_type",
            "상황 유형",
            SlotType.SINGLE_SELECT,
            "사용자의 내일 상황을 분류한다.",
            "내일 어떤 상황인지 알려주세요.",
            null,
            presentationOption,
            List.of(presentationOption));
    ContextSlot criticalMoment =
        new ContextSlot(
            2L,
            "critical_moment",
            "결정적 순간",
            SlotType.TEXT,
            "가장 흔들릴 수 있는 순간을 추출한다.",
            "가장 걱정되는 순간은 언제인가요?",
            "첫 반응을 말해야 하는 순간",
            null,
            List.of());

    return new ContextSlotSchema(
        1L,
        "p1_offline_default",
        "P1 Offline Default Context Slot Schema",
        1,
        true,
        List.of(
            new ContextSlotSchemaItem(1L, situationType, RequiredLevel.REQUIRED, 10, true),
            new ContextSlotSchemaItem(2L, criticalMoment, RequiredLevel.REQUIRED, 20, true)));
  }
}
