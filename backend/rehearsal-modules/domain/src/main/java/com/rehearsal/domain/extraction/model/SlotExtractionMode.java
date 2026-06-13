package com.rehearsal.domain.extraction.model;

import com.rehearsal.domain.core.annotation.Description;

@Description("slot 추출이 최초 브리핑인지 follow-up 답변 처리인지 나타내는 mode")
public enum SlotExtractionMode {
  @Description("사용자의 첫 브리핑 transcript에서 전체 slot 값을 추출한다.")
  INITIAL,

  @Description("서버 follow-up 질문에 대한 답변으로 기존 slot 값을 보강한다.")
  FOLLOW_UP
}
