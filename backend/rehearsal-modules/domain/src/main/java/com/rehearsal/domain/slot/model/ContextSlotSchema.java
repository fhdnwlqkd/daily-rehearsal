package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;
import java.util.List;

public record ContextSlotSchema(
    @Description("Context slot schema DB id") Long id,
    @Description("P1, P2처럼 slot 묶음을 조회할 때 사용하는 schema key") String schemaKey,
    @Description("관리자가 읽는 schema 이름") String name,
    @Description("부족한 맥락을 보강하기 위해 허용하는 최대 follow-up 횟수") int maxFollowUpAttempt,
    @Description("Schema 활성 여부") boolean active,
    @Description("Schema에 포함된 slot 항목 목록") List<ContextSlotSchemaItem> items) {

  public ContextSlotSchema {
    items = items == null ? List.of() : List.copyOf(items);
  }
}
