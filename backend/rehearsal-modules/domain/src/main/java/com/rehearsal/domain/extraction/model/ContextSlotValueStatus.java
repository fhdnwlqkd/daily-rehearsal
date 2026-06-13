package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("slot 값이 유효하게 채워졌는지, 누락됐는지, 기본값으로 보정됐는지 나타내는 상태")
public enum ContextSlotValueStatus {
  FILLED,
  MISSING,
  DEFAULTED,
  INVALID
}
