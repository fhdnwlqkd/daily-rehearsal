package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("slot 값이 사용자 발화 추출값인지, 기본값인지, 비어 있는 값인지 나타내는 출처")
public enum ContextSlotValueSource {
  EXTRACTED,
  DEFAULT_OPTION,
  DEFAULT_LITERAL,
  EMPTY
}
