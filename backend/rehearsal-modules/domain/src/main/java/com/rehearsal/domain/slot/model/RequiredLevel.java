package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;

public enum RequiredLevel {
  @Description("수집 시도 필수 맥락. 값이 비어 있으면 follow-up 대상이며 재시도 소진 후에는 null로 진행할 수 있다.")
  REQUIRED,

  @Description("가능하면 받고 싶은 맥락. 체험 진행을 막지는 않는다.")
  SOFT_REQUIRED,

  @Description("보조 맥락. 비어 있어도 follow-up 없이 진행한다.")
  OPTIONAL
}
