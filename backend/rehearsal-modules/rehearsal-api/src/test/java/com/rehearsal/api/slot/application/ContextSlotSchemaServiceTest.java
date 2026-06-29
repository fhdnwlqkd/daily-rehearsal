package com.rehearsal.api.slot.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.rehearsal.domain.core.exception.BusinessException;
import com.rehearsal.domain.core.exception.ErrorCode;
import com.rehearsal.domain.slot.model.ContextSlot;
import com.rehearsal.domain.slot.model.ContextSlotOption;
import com.rehearsal.domain.slot.model.SlotType;
import com.rehearsal.domain.slot.repository.ContextSlotSchemaRepository;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotOptionCommand;
import com.rehearsal.domain.slot.usecase.command.CreateContextSlotSchemaCommand;
import com.rehearsal.domain.slot.usecase.command.UpdateContextSlotCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ContextSlotSchemaServiceTest {

  private final ContextSlotSchemaRepository repository = mock(ContextSlotSchemaRepository.class);
  private final ContextSlotSchemaService service = new ContextSlotSchemaService(repository);

  @Test
  void rejectsDuplicateSchemaKey() {
    when(repository.existsBySchemaKey("p1_offline_default")).thenReturn(true);

    assertThatThrownBy(
            () ->
                service.createSchema(
                    new CreateContextSlotSchemaCommand(
                        "p1_offline_default", "P1 Offline Default", 1, true)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_REQUEST);

    verify(repository).existsBySchemaKey("p1_offline_default");
    verifyNoMoreInteractions(repository);
  }

  @Test
  void rejectsDefaultOptionForTextSlot() {
    CreateContextSlotCommand command =
        new CreateContextSlotCommand(
            "critical_moment",
            "결정적 순간",
            SlotType.TEXT,
            "가장 흔들릴 수 있는 순간을 추출한다.",
            "가장 걱정되는 순간은 언제인가요?",
            null,
            1L);

    assertThatThrownBy(() -> service.createSlot(command))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_REQUEST);

    verifyNoMoreInteractions(repository);
  }

  @Test
  void rejectsOptionCreationForTextSlot() {
    when(repository.findSlotById(1L)).thenReturn(Optional.of(textSlot()));

    assertThatThrownBy(
            () -> service.createOption(new CreateContextSlotOptionCommand(1L, "foo", "Foo")))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_REQUEST);

    verify(repository).findSlotById(1L);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void rejectsDefaultOptionFromDifferentSlot() {
    ContextSlot slot = singleSelectSlot();
    ContextSlotOption otherSlotOption = new ContextSlotOption(99L, "other", "Other");
    when(repository.findSlotById(1L)).thenReturn(Optional.of(slot));
    when(repository.findOptionById(99L)).thenReturn(Optional.of(otherSlotOption));

    assertThatThrownBy(
            () ->
                service.updateSlot(
                    new UpdateContextSlotCommand(
                        1L, "상황 유형", "사용자의 내일 상황을 분류한다.", "내일 어떤 상황인지 알려주세요.", null, 99L)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_REQUEST);

    verify(repository).findSlotById(1L);
    verify(repository).findOptionById(99L);
    verifyNoMoreInteractions(repository);
  }

  @Test
  void updatesSlotWhenDefaultOptionBelongsToSlot() {
    ContextSlot slot = singleSelectSlot();
    when(repository.findSlotById(1L)).thenReturn(Optional.of(slot));
    when(repository.findOptionById(1L)).thenReturn(Optional.of(slot.defaultOption()));
    when(repository.updateSlot(org.mockito.ArgumentMatchers.any())).thenReturn(slot);

    ContextSlot result =
        service.updateSlot(
            new UpdateContextSlotCommand(
                1L, "상황 유형", "사용자의 내일 상황을 분류한다.", "내일 어떤 상황인지 알려주세요.", null, 1L));

    assertThat(result).isEqualTo(slot);
  }

  private ContextSlot textSlot() {
    return new ContextSlot(
        1L,
        "critical_moment",
        "결정적 순간",
        SlotType.TEXT,
        "가장 흔들릴 수 있는 순간을 추출한다.",
        "가장 걱정되는 순간은 언제인가요?",
        null,
        null,
        List.of());
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
