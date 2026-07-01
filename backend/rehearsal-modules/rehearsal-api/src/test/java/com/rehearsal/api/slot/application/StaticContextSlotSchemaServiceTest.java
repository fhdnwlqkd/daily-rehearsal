package com.rehearsal.api.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlotSchema;
import com.rehearsal.domain.slot.usecase.command.GetContextSlotSchemaCommand;
import org.junit.jupiter.api.Test;

class StaticContextSlotSchemaServiceTest {

  private final StaticContextSlotSchemaService service = new StaticContextSlotSchemaService();

  @Test
  void getsDateSchemaFromStaticRegistry() {
    ContextSlotSchema schema =
        service.getContextSlotSchema(new GetContextSlotSchemaCommand("date"));

    assertThat(schema.schemaKey()).isEqualTo("date");
    assertThat(schema.maxFollowUpAttempt()).isEqualTo(1);
    assertThat(schema.items())
        .extracting(item -> item.slot().slotKey())
        .containsExactly("desired_persona", "critical_moment", "outfit_direction");
  }

  @Test
  void getsBusinessMeetingSchemaFromStaticRegistry() {
    ContextSlotSchema schema =
        service.getContextSlotSchema(new GetContextSlotSchemaCommand("business_meeting"));

    assertThat(schema.schemaKey()).isEqualTo("business_meeting");
    assertThat(schema.items())
        .extracting(item -> item.slot().slotKey())
        .containsExactly("desired_persona", "critical_moment", "outfit_direction");
  }

  @Test
  void throwsWhenSchemaKeyIsUnknown() {
    assertThatThrownBy(
            () -> service.getContextSlotSchema(new GetContextSlotSchemaCommand("unknown")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.NOT_FOUND);
  }
}
