package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;

public enum RequiredLevel {
  @Description("필수 맥락. 값이 비어 있으면 follow-up 대상이 된다.")
  REQUIRED,

  @Description("가능하면 받고 싶은 맥락. 체험 진행을 막지는 않는다.")
  SOFT_REQUIRED,

  @Description("보조 맥락. 비어 있으면 기본값이나 fallback으로 진행한다.")
  OPTIONAL
}
