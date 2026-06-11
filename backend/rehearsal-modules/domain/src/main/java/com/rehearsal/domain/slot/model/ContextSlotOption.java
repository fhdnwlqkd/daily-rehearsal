package com.rehearsal.domain.slot.model;

import com.rehearsal.domain.core.annotation.Description;

public record ContextSlotOption(
    @Description("Context slot option DB id") Long id,
    @Description("LLM 결과, 서버 저장, 로그에서 사용하는 선택지 key") String optionKey,
    @Description("관리자와 화면에서 읽는 선택지 이름") String label) {}
