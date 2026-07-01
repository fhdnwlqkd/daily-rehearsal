package com.rehearsal.api.admin.slot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rehearsal.api.config.exception.GlobalExceptionHandler;
import com.rehearsal.api.config.response.ApiResponseBodyAdvice;
import com.rehearsal.api.slot.controller.AdminContextSlotController;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
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
class AdminContextSlotControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private GetContextSlotSchemaUseCase getContextSlotSchemaUseCase;
  @MockitoBean private ManageContextSlotSchemaUseCase manageContextSlotSchemaUseCase;

  @Test
  void listContextSlots() throws Exception {
    when(manageContextSlotSchemaUseCase.listSlots()).thenReturn(List.of(singleSelectSlot()));

    mockMvc
        .perform(get("/api/v1/admin/context-slots"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.slots[0].slotKey").value("situation_type"));
  }

  @Test
  void createContextSlot() throws Exception {
    when(manageContextSlotSchemaUseCase.createSlot(any())).thenReturn(singleSelectSlot());

    mockMvc
        .perform(
            post("/api/v1/admin/context-slots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "slotKey": "situation_type",
                      "label": "상황 유형",
                      "slotType": "SINGLE_SELECT",
                      "extractionHint": "사용자의 내일 상황을 분류한다.",
                      "followUpHint": "내일 어떤 상황인지 알려주세요."
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.slotKey").value("situation_type"));
  }

  @Test
  void updateContextSlot() throws Exception {
    when(manageContextSlotSchemaUseCase.updateSlot(any())).thenReturn(singleSelectSlot());

    mockMvc
        .perform(
            patch("/api/v1/admin/context-slots/{slotId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "label": "상황 유형",
                      "extractionHint": "사용자의 내일 상황을 분류한다.",
                      "followUpHint": "내일 어떤 상황인지 알려주세요.",
                      "defaultOptionId": 1
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.defaultOption.optionKey").value("presentation"));
  }

  @Test
  void createContextSlotOption() throws Exception {
    when(manageContextSlotSchemaUseCase.createOption(any())).thenReturn(singleSelectSlot());

    mockMvc
        .perform(
            post("/api/v1/admin/context-slots/{slotId}/options", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "optionKey": "presentation",
                      "label": "발표"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.options[0].optionKey").value("presentation"));
  }

  private ContextSlot singleSelectSlot() {
    ContextSlotOption presentation = new ContextSlotOption(1L, "presentation", "발표");
    return new ContextSlot(
        1L,
        "situation_type",
        "상황 유형",
        SlotType.SINGLE_SELECT,
        "사용자의 내일 상황을 분류한다.",
        "내일 어떤 상황인지 알려주세요.",
        null,
        presentation,
        List.of(presentation));
  }
}
