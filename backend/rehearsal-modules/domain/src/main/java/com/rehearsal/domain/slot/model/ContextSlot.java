package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;
import java.util.List;

public record ContextSlot(
    @Description("Context slot DB id") Long id,
    @Description("LLM 응답과 서버 저장에 사용하는 내부 key") String slotKey,
    @Description("관리자와 화면에서 읽는 slot 이름") String label,
    @Description("Slot 값의 형태") SlotType slotType,
    @Description("LLM이 사용자 발화에서 이 slot 값을 추출할 때 참고하는 설명") String extractionHint,
    @Description("이 slot 값이 부족할 때 사용자에게 다시 물어볼 질문 힌트") String followUpHint,
    @Description("선택지가 아닌 literal fallback 값") String defaultLiteralValue,
    @Description("선택지 기반 fallback 값") ContextSlotOption defaultOption,
    @Description("이 slot에서 허용하는 선택지 목록") List<ContextSlotOption> options) {

  public ContextSlot {
    options = options == null ? List.of() : List.copyOf(options);
  }
}
