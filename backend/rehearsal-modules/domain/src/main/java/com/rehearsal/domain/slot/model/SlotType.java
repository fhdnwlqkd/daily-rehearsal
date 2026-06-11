package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;

public enum SlotType {
  @Description("자유 텍스트 값")
  TEXT,

  @Description("허용된 선택지 중 하나를 고르는 값")
  SINGLE_SELECT
}
